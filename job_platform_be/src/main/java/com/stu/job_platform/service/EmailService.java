package com.stu.job_platform.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class EmailService {

    @Value("${RESEND_API_KEY:}")
    private String resendApiKey;

    public void sendOtpEmail(String toEmail, String otpCode) throws Exception {
        RestTemplate restTemplate = new RestTemplate();
        String url = "https://api.resend.com/emails";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(resendApiKey);

        Map<String, Object> body = new HashMap<>();
        // Note for Resend sandbox: domains are unverified by default, you can only send from onboarding@resend.dev
        body.put("from", "onboarding@resend.dev");
        body.put("to", List.of(toEmail));
        body.put("subject", "Mã OTP xác thực tài khoản (Job Platform)");
        body.put("html", "<p>Mã OTP của bạn là: <b style=\"color:red; font-size:20px;\">"
                + otpCode + "</b>. Có hiệu lực 5 phút.</p>");

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
        
        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new Exception("Thất bại khi gửi qua Resend: " + response.getBody());
        }
    }
}