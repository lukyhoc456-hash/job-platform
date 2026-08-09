package com.stu.job_platform.service;

import org.springframework.web.client.HttpClientErrorException;
import jakarta.persistence.criteria.CriteriaBuilder.In;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.client.RestTemplate;
import com.stu.job_platform.entity.Recruiter;
import com.stu.job_platform.repository.RecruiterRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import java.util.*;

@Service
public class AiVerificationService {
    

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private RecruiterRepository recruiterRepository;

    @Autowired
    private GroqKeyManager groqKeyManager;

    public String scrapeWebsiteText(String url, Integer userId) {
        try {
            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .timeout(8000)
                    .ignoreHttpErrors(true)
                    .get();
            return doc.select("h1, h2, footer, p").text();
        } catch (Exception e) {
            try{
                String sql = "Insert into bug_reports (user_id, title, description, status) values (?, ?, ?, ?)";
                jdbcTemplate.update(sql, userId, "WEB_BLOCKED_BY_WALL", "Website chặn bot cào dữ liệu tại URL: " + url + ". Chi tiết: " + e.getMessage(), "pending");
            } catch (Exception ex) {
                System.err.println("Không thể lưu log lỗi vào db: " + ex.getMessage());
            }
            return "WEB_BLOCKED_BY_WALL";
        }
    }

    public String verifyFieldWithAi(String fieldType, String value, Recruiter rec, String companyEmail) {
        String apiUrl = "https://api.groq.com/openai/v1/chat/completions";
        String prompt = "";

        if ("companyName".equals(fieldType)) {
            String emailDomain = (companyEmail != null && companyEmail.contains("@")) 
                                ? companyEmail.substring(companyEmail.indexOf("@") + 1) : "n/a";

            prompt = String.format(
                "Mày là hệ thống AI kiểm định thông tin doanh nghiệp Việt Nam.\n" +
                "Dữ liệu cần thẩm định:\n" +
                "- Tên công ty: '%s'\n" +
                "- Mã số thuế đã nhập: '%s'\n" +
                "- Domain email đã nhập: '%s'\n\n" +
                "YÊU CẦU THẨM ĐỊNH:\n" +
                "1. Kiểm tra tên công ty có thật, đang hoạt động và có liên quan đến MST hoặc Domain email trên không.\n" +
                "2. Nếu tên công ty là tên giả, hoặc không khớp với MST/Domain email -> match_percentage: 0.\n" +
                "3. Nếu thông tin đồng bộ, hợp lệ -> match_percentage: 100.\n" +
                "Trả về JSON chuẩn:\n" +
                "{\n  \"match_percentage\": <0-100>,\n  \"reason\": \"<lý do chi tiết bằng tiếng Việt>\"\n}",
                value, rec.getTaxCode(), emailDomain
            );
        } else if ("taxCode".equals(fieldType)) {
            String emailDomain = (companyEmail != null && companyEmail.contains("@")) 
                                 ? companyEmail.substring(companyEmail.indexOf("@") + 1) : "n/a";

            prompt = String.format(
                "Mày là chuyên gia thẩm định doanh nghiệp. Dữ liệu đầu vào: [Tên công ty: '%s', MST cần check: '%s', Domain email: '%s']\n\n" +
                "YÊU CẦU BẮT BUỘC:\n" +
                "1. Mã số thuế doanh nghiệp Việt Nam PHẢI có độ dài chính xác 10 hoặc 13 chữ số. Nếu thừa hoặc thiếu -> match_percentage: 0.\n" +
                "2. CẤM tự ý sửa lỗi gõ nhầm của người dùng. Nếu nhập thừa số so với quy chuẩn MST -> match_percentage: 0.\n" +
                "3. Kiểm tra xem MST này có thuộc về tên công ty '%s' không.\n" +
                "4. Đối chiếu MST này với Domain email '%s'. Nếu không khớp/không tồn tại -> match_percentage: 0.\n" +
                "5. Nếu mọi thứ khớp và hợp lệ -> match_percentage: 100.\n" +
                "Trả về JSON chuẩn: {\"match_percentage\": ..., \"reason\": \"<lý do chi tiết>\"}",
                rec.getCompanyName(), value, emailDomain, rec.getCompanyName(), emailDomain
            );
        } else if ("websiteUrl".equals(fieldType)) {

            String emailDomain = (companyEmail != null && companyEmail.contains("@"))
                    ? companyEmail.substring(companyEmail.indexOf("@") + 1)
                    : "n/a";

            String websiteDomain = value
                    .replace("https://", "")
                    .replace("http://", "")
                    .replace("www.", "");

            if (websiteDomain.contains("/")) {
                websiteDomain = websiteDomain.substring(0, websiteDomain.indexOf("/"));
            }

            // Chặn MXH
            List<String> socialDomains = Arrays.asList(
                    "facebook.com",
                    "linkedin.com",
                    "zalo.me",
                    "tiktok.com",
                    "instagram.com",
                    "youtube.com",
                    "x.com",
                    "twitter.com"
            );

            for (String social : socialDomains) {
                if (websiteDomain.contains(social)) {
                    return """
                    {
                    "match_percentage": 0,
                    "reason": "Website nhập vào là mạng xã hội hoặc nền tảng trung gian, không phải website doanh nghiệp."
                    }
                    """;
                }
            }

            // Cào dữ liệu website
            String websiteContent = scrapeWebsiteText(value, rec.getId());

            if ("WEB_BLOCKED_BY_WALL".equals(websiteContent)) {

                String status = rec.getStatusTrust();

                if (status == null) {
                    status = "website_pending";
                } else if (!status.contains("website_pending")) {
                    status += ",website_pending";
                }

                rec.setStatusTrust(status);
                recruiterRepository.save(rec);

                return """
                {
                "match_percentage": 50,
                "reason":"Website chặn truy cập tự động. Chờ admin kiểm duyệt thủ công."
                }
                """;
            }

            prompt = String.format(
                    """
                    Mày là chuyên gia thẩm định website doanh nghiệp.

                    Dữ liệu gốc:
                    - Tên công ty: "%s"
                    - MST: "%s"
                    - Domain email: "%s"

                    Website cần thẩm định:
                    "%s"
                
                    Domain website:
                    "%s"

                    Nội dung website đã thu thập:

                    %s

                    YÊU CẦU BẮT BUỘC:

                    1. Xác định website này có phải website chính thức của công ty hay không.
                    2. Nếu website là MXH, website trung gian hoặc không liên quan công ty -> match_percentage = 0.
                    3. Đối chiếu tên công ty với nội dung website.
                    4. Đối chiếu domain website với domain email.
                    5. Nếu website thể hiện rõ đây là website chính thức của công ty và thông tin đồng nhất -> match_percentage = 100.

                    Chỉ trả về JSON:

                    {
                    "match_percentage": <0-100>,
                    "reason": "<giải thích bằng tiếng Việt>"
                    }
                    """,
                    rec.getCompanyName(),
                    rec.getTaxCode(),
                    emailDomain,
                    value,
                    websiteDomain,
                    websiteContent
            );
        }else {
            return "{\"match_percentage\":0,\"reason\":\"Invalid fieldType\"}";
        }

        // Gọi API Groq
        return callGroqWithFailover(apiUrl, prompt, 0);
    }
    private String callGroqWithFailover(String apiUrl, String prompt, int attempt) {
        GroqKeyManager.KeyState key = groqKeyManager.pickKey();

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + key.apiKey);

        Map<String, Object> requestBody = Map.of(
            "model", "llama-3.3-70b-versatile", 
            "messages", List.of(Map.of("role", "user", "content", prompt)),
            "temperature", 0.1
        );

        try {
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(apiUrl, entity, Map.class);

            groqKeyManager.recordSuccess(key, response.getHeaders());

            Map<String, Object> responseBody = (Map<String, Object>) response.getBody();
            List<?> choices = (List<?>) responseBody.get("choices");
            Map<?, ?> firstChoice = (Map<?, ?>) choices.get(0);
            Map<?, ?> message = (Map<?, ?>) firstChoice.get("message");
            return message.get("content").toString().trim();

        } catch (HttpClientErrorException e) {
            groqKeyManager.recordRateLimited(key, e.getResponseHeaders());

            if (attempt < 3) {
                return callGroqWithFailover(apiUrl, prompt, attempt + 1);
            }
            return "{\"match_percentage\": 0, \"reason\": \"Tất cả API key đều đang hết quota, vui lòng thử lại sau!\"}";

        } catch (Exception e) {
            e.printStackTrace();
            return "{\"match_percentage\": 0, \"reason\": \"Lỗi kết nối API AI!\"}";
        }
    }
}