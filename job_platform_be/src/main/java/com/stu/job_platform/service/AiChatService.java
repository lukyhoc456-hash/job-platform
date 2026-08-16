package com.stu.job_platform.service;

import com.stu.job_platform.entity.AiConversation;
import com.stu.job_platform.entity.User;
import com.stu.job_platform.repository.AiConversationRepository;
import com.stu.job_platform.repository.UserRepository;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import com.stu.job_platform.entity.Candidate;
import com.stu.job_platform.entity.JobPost;
import com.stu.job_platform.repository.CandidateRepository;
import com.stu.job_platform.repository.JobPostRepository;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.web.multipart.MultipartFile;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import com.stu.job_platform.entity.AiConversation;
import com.stu.job_platform.repository.AiConversationRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;  

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service xử lý tương tác AI Chatbot (Trợ lý tìm việc)
 * Hỗ trợ: tìm việc bằng ngôn ngữ tự nhiên, tóm tắt CV, gợi ý JD
 */
@Service
public class AiChatService {


    @Autowired
    private JobPostRepository jobPostRepository;

    @Autowired
    private CandidateRepository candidateRepository;

    @Autowired 
    private AiConversationRepository aiConversationRepository;

    @Autowired 
    private UserRepository userRepository;

    @Autowired
    private GroqKeyManager groqKeyManager;


    @Value("${file.upload-dir}")
    private String uploadDir;


    // ── Endpoint 1: Upload file mới ──
    public String evaluateWithFile(MultipartFile cvFile, String jobCode) throws IOException {
        String cvText = extractTextFromMultipart(cvFile);
        return evaluate(cvText, jobCode);
    }

    // ── Endpoint 2: Dùng CV profile sẵn ──
    public String evaluateWithProfileCv(Integer candidateId, String jobCode) throws IOException {
        Candidate candidate = candidateRepository.findById(candidateId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy ứng viên!"));

        String cvFileName = candidate.getCvPath();
        if (cvFileName == null) throw new RuntimeException("Bạn chưa có CV trong hồ sơ!");

        // Ghép đúng path giống lúc lưu: uploadDir/cv/fileName
        File file = java.nio.file.Paths.get(uploadDir, "cv", cvFileName).toFile();

        if (!file.exists()) {
            throw new RuntimeException("Không tìm thấy file CV trên server, vui lòng upload lại!");
        }

        String cvText = cvFileName.endsWith(".pdf")
                ? extractTextFromPdf(file)
                : extractTextFromDocx(file);

        return evaluate(cvText, jobCode);
    }

    // ── Gọi Groq AI ──
    private String evaluate(String cvText, String jobCode) {
        JobPost job = jobPostRepository.findByJobCode(jobCode)
                .orElseThrow(() -> new RuntimeException("Mã bài đăng không tồn tại!"));

        String prompt = """
            Bạn là chuyên gia tuyển dụng.

            Hãy đánh giá CV với bài tuyển dụng dưới đây.

            === BÀI TUYỂN DỤNG ===
            Vị trí: %s
            Mô tả: %s
            Yêu cầu: %s

            === CV ===
            %s

            Chỉ trả lời bằng tiếng Việt.

            BẮT BUỘC trả lời đúng định dạng sau, không giải thích dài dòng:

            🎯 Điểm phù hợp: XX/100

            ✅ Điểm mạnh:
            - ...
            - ...

            ❌ Còn thiếu:
            - ...
            - ...

            💡 Gợi ý:
            - ...
            - ...

            Quy tắc:
            - Tổng độ dài dưới 120 từ.
            - Mỗi mục tối đa 2 gạch đầu dòng.
            - Không mở đầu bằng "Tôi sẽ đánh giá..."
            - Không viết đoạn văn.
            - Nếu CV rất phù hợp vẫn phải nêu ít nhất 1 điểm cần cải thiện.
            - Giữa các mục phải có dòng trống.
            - Không thêm bất kỳ thông tin nào ngoài các mục trên.
            """.formatted(
                job.getTitle(),
                job.getJdText(),
                job.getRequirements(),
                cvText
            );
        return callGroq(
            "Bạn là chuyên gia tuyển dụng, trả lời bằng tiếng Việt, rõ ràng và chuyên nghiệp.",
            prompt, 0.3, 1024
        );
    }

    // ── Extract text ──
    private String extractTextFromMultipart(MultipartFile file) throws IOException {
        String name = file.getOriginalFilename();
        if (name == null) throw new RuntimeException("File không hợp lệ!");
        if (name.endsWith(".pdf")) {
            try (PDDocument doc = PDDocument.load(file.getInputStream())) {
                return new PDFTextStripper().getText(doc);
            }
        } else if (name.endsWith(".docx")) {
            try (XWPFDocument doc = new XWPFDocument(file.getInputStream())) {
                return new XWPFWordExtractor(doc).getText();
            }
        }
        throw new RuntimeException("Chỉ hỗ trợ PDF và DOCX!");
    }

    private String extractTextFromPdf(File file) throws IOException {
        try (PDDocument doc = PDDocument.load(file)) {
            return new PDFTextStripper().getText(doc);
        }
    }

    private String extractTextFromDocx(File file) throws IOException {
        try (XWPFDocument doc = new XWPFDocument(new FileInputStream(file))) {
            return new XWPFWordExtractor(doc).getText();
        }
    }

    public String findMatchingJobs(MultipartFile cvFile) throws IOException {
        String cvText = extractTextFromMultipart(cvFile);
        return findMatchingJobs(cvText); // gọi qua bản overload bên dưới
    }

    // ── Tìm job bằng CV có sẵn trong hồ sơ (chỉ khác cách lấy cvText) ──
    public String findMatchingJobsWithProfileCv(Integer candidateId) throws IOException {
        Candidate candidate = candidateRepository.findById(candidateId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy ứng viên!"));

        String cvFileName = candidate.getCvPath();
        if (cvFileName == null) throw new RuntimeException("Bạn chưa có CV trong hồ sơ!");

        File file = java.nio.file.Paths.get(uploadDir, "cv", cvFileName).toFile();
        if (!file.exists()) throw new RuntimeException("Không tìm thấy file CV trên server, vui lòng upload lại!");

        String cvText = cvFileName.endsWith(".pdf")
                ? extractTextFromPdf(file)
                : extractTextFromDocx(file);

        return findMatchingJobs(cvText);
    }
    // ── Tìm job phù hợp với CV ──
    public String findMatchingJobs(String cvText) {

        List<JobPost> activeJobs = jobPostRepository.findByStatus(1);
        if (activeJobs.isEmpty()) {
            return "[]";
        }

        // Gom danh sách job thành text ngắn gọn để AI dễ đối chiếu
        StringBuilder jobListText = new StringBuilder();
        for (JobPost job : activeJobs) {
            jobListText.append(String.format(
                "- Mã: %s | Vị trí: %s | Mô tả: %s | Yêu cầu: %s\n",
                job.getJobCode(),
                job.getTitle(),
                truncate(job.getJdText(), 200),
                truncate(job.getRequirements(), 200)
            ));
        }

        String prompt = """
            Bạn là hệ thống gợi ý việc làm. Dưới đây là nội dung CV của ứng viên và danh sách các bài tuyển dụng đang mở.

            === NỘI DUNG CV ===
            %s

            === DANH SÁCH BÀI TUYỂN DỤNG ===
            %s

            YÊU CẦU ĐÁNH GIÁ NGHIÊM NGẶT:
            Với mỗi bài tuyển dụng, hãy tính toán %% độ phù hợp dựa trên các tiêu chí sau:
            1. Định hướng & Kỹ năng cốt lõi (Core Stack): Dựa vào thời gian làm việc, dự án thực tế và chức danh.
            2. Cảnh báo Keyword lướt qua: Nếu một công nghệ (ví dụ: React, Vue, Python...) chỉ xuất hiện 1-2 dòng ngắn, nằm ở mục sở thích/kỹ năng phụ và KHÔNG có dự án/kinh nghiệm thực tế đi kèm -> KHÔNG ĐƯỢC tính công nghệ đó vào điểm phù hợp chính.
            3. Trường hợp lệch định hướng: Nếu CV chủ yếu làm Backend (Spring Boot, Java...) nhưng bài đăng tuyển Frontend (React, Angular...), tuyệt đối KHÔNG tính điểm cao dù CV có nhắc nhẹ đến Frontend.

            Chỉ chọn các mã bài đăng (jobCode) có độ phù hợp ước tính TRÊN 50%% (tối đa 5 mã).
            Nếu không có bài nào đạt trên 50%%, trả về mảng rỗng.

            CHỈ trả về JSON thuần, không giải thích, đúng định dạng:
            ["JP-XXXXXX", "JP-YYYYYY"]
            """.formatted(cvText, jobListText.toString());

        try {
            String content = callGroq(
                "Bạn chỉ trả về JSON thuần, không thêm bất kỳ text giải thích nào.",
                prompt, 0.2, 512
            );
            return content.replaceAll("```json", "").replaceAll("```", "").trim();
        } catch (Exception e) {
            e.printStackTrace();
            return "[]";
        }
    }

    private String truncate(String text, int maxLen) {
        if (text == null) return "";
        return text.length() > maxLen ? text.substring(0, maxLen) + "..." : text;
    }


    // ═══════════════════════════════════════════════════════════════
// THÊM VÀO CUỐI AiChatService.java (trước dấu } cuối cùng)
// ═══════════════════════════════════════════════════════════════


    // ── PHỎNG VẤN THỬ: Bước 1 ───────────────────────────────────────────────
    public Map<String, String> startInterview(Integer userId, MultipartFile cvFile) throws IOException {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng!"));

        String cvText = extractTextFromMultipart(cvFile);
        if (cvText.isBlank()) throw new RuntimeException("Không đọc được nội dung CV!");

        String prompt = """
            Bạn là chuyên gia phỏng vấn tuyển dụng.
            Dưới đây là nội dung CV của ứng viên:

            === CV ===
            %s

            Hãy đặt đúng 1 câu hỏi phỏng vấn cụ thể dựa trên kỹ năng hoặc kinh nghiệm trong CV này.
            Câu hỏi phải bằng tiếng Việt, ngắn gọn (1–2 câu).
            CHỈ trả về câu hỏi, không thêm bất kỳ text nào khác.
            """.formatted(cvText);

        String question = callGroq(
            "Bạn là chuyên gia phỏng vấn, chỉ trả về câu hỏi phỏng vấn bằng tiếng Việt.",
            prompt, 0.7, 256
        );

        String sessionId = UUID.randomUUID().toString();
        saveConversation(user, "assistant", question, "interview_question", sessionId);

        return Map.of("question", question, "sessionId", sessionId);
    }

    // ── PHỎNG VẤN THỬ: Bước 2 ───────────────────────────────────────────────
    public Map<String, String> submitAnswer(Integer userId, String sessionId, String question, String answer) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng!"));

        // 2a: Check on-topic trước (tiết kiệm token)
        String checkPrompt = """
            Câu hỏi phỏng vấn: %s
            Câu trả lời của ứng viên: %s

            Kiểm tra xem câu trả lời có liên quan đến câu hỏi phỏng vấn không.
            Lạc đề nếu: spam, hỏi ngược, nội dung vô nghĩa, hoàn toàn không liên quan.

            CHỈ trả về JSON thuần:
            {"isOnTopic": true} hoặc {"isOnTopic": false, "reason": "lý do ngắn gọn bằng tiếng Việt"}
            """.formatted(question, answer);

        String checkRaw = callGroq(
            "Bạn chỉ trả về JSON thuần, không thêm bất kỳ text nào khác.",
            checkPrompt, 0.1, 128
        );

        boolean isOnTopic = checkRaw.contains("\"isOnTopic\": true") || checkRaw.contains("\"isOnTopic\":true");
        if (!isOnTopic) {
            String reason = "Câu trả lời lạc chủ đề, vui lòng trả lời lại!";
            try {
                int ri = checkRaw.indexOf("\"reason\"");
                if (ri != -1) {
                    int s = checkRaw.indexOf("\"", ri + 9) + 1;
                    int e = checkRaw.indexOf("\"", s);
                    if (s > 0 && e > s) reason = checkRaw.substring(s, e);
                }
            } catch (Exception ignored) {}
            return Map.of("status", "OFF_TOPIC", "reason", reason);
        }

        // 2b: Lưu câu trả lời của ứng viên
        saveConversation(user, "user", answer, "interview_answer", sessionId);

        // 2c: AI đánh giá
        String evalPrompt = """
            Bạn là chuyên gia phỏng vấn tuyển dụng.

            Câu hỏi: %s
            Câu trả lời của ứng viên: %s

            Đánh giá câu trả lời theo định dạng sau, bằng tiếng Việt:

            ⭐ Điểm: X/10

            ✅ Điểm tốt:
            - ...

            💡 Cần cải thiện:
            - ...

            📌 Gợi ý trả lời tốt hơn:
            (1–2 câu ngắn gọn)

            Quy tắc: Dưới 100 từ, không viết đoạn văn dài.
            """.formatted(question, answer);

        String evaluation = callGroq(
            "Bạn là chuyên gia phỏng vấn, đánh giá ngắn gọn và chuyên nghiệp bằng tiếng Việt.",
            evalPrompt, 0.4, 512
        );

        // 2d: Lưu đánh giá của AI
        saveConversation(user, "assistant", evaluation, "interview_evaluation", sessionId);

        return Map.of("status", "ON_TOPIC", "evaluation", evaluation);
    }

    // ── HELPER dùng chung ─────────────────────────────────────────────────────
    private String callGroq(String systemPrompt, String userPrompt, double temperature, int maxTokens) {
        return callGroq(systemPrompt, userPrompt, temperature, maxTokens, 0);
    }

    private String callGroq(String systemPrompt, String userPrompt, double temperature, int maxTokens, int attempt) {
        GroqKeyManager.KeyState key = groqKeyManager.pickKey();

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + key.apiKey);

        Map<String, Object> body = Map.of(
            "model", "openai/gpt-oss-120b",
            "messages", List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user",   "content", userPrompt)
            ),
            "temperature", temperature,
            "max_tokens",  maxTokens
        );

        try {
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(
                "https://api.groq.com/openai/v1/chat/completions", entity, Map.class);

            groqKeyManager.recordSuccess(key, response.getHeaders());

            List<?> choices = (List<?>) response.getBody().get("choices");
            Map<?, ?> message = (Map<?, ?>) ((Map<?, ?>) choices.get(0)).get("message");
            return message.get("content").toString().trim();

        } catch (HttpClientErrorException e) {
            groqKeyManager.recordRateLimited(key, e.getResponseHeaders());
            if (attempt < 3) {
                return callGroq(systemPrompt, userPrompt, temperature, maxTokens, attempt + 1);
            }
            throw new RuntimeException("Tất cả API key đều đang hết quota, vui lòng thử lại sau!");
        } catch (org.springframework.web.client.HttpServerErrorException e) {
            System.out.println("⚠️ [AiChatService] Groq Server bị lỗi " + e.getStatusCode() + "! Thử xoay Key khác...");
            groqKeyManager.recordRateLimited(key, e.getResponseHeaders());
            if (attempt < 3) {
                return callGroq(systemPrompt, userPrompt, temperature, maxTokens, attempt + 1);
            }
            throw new RuntimeException("Server lỗi, vui lòng thử lại sau!");
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Lỗi kết nối AI. Vui lòng thử lại!");
        }
    }

    private void saveConversation(User user, String role, String content, String featureContext, String sessionId) {
        AiConversation conv = new AiConversation();
        conv.setUser(user);
        conv.setRole(role);
        conv.setContent(content);
        conv.setFeatureContext(featureContext);
        conv.setSessionId(sessionId);
        aiConversationRepository.save(conv);
    }

    // ── PHỎNG VẤN THỬ: AI tự sinh câu trả lời khi ứng viên không biết ──────
    public Map<String, String> generateAnswerForUser(Integer userId, String sessionId, String question) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng!"));

        String prompt = """
            Bạn là chuyên gia phỏng vấn tuyển dụng.
            Hãy trả lời câu hỏi phỏng vấn sau như thể bạn là một ứng viên có kinh nghiệm,
            trả lời tự nhiên, đúng trọng tâm, bằng tiếng Việt.

            Câu hỏi: %s

            CHỈ trả về nội dung câu trả lời (2–4 câu), không thêm tiêu đề, không giải thích thêm.
            """.formatted(question);

        String aiAnswer = callGroq(
            "Bạn đóng vai ứng viên trả lời phỏng vấn, ngắn gọn, tự nhiên, bằng tiếng Việt.",
            prompt, 0.6, 300
        );

        saveConversation(user, "assistant", aiAnswer, "interview_ai_generated_answer", sessionId);

        return Map.of("answer", aiAnswer);
    }
}