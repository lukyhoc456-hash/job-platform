package com.stu.job_platform.controller;

import com.stu.job_platform.dto.ProfileRequest;
import com.stu.job_platform.entity.Recruiter;
import com.stu.job_platform.service.ProfileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/v1/profile")
public class ProfileController {

    @Autowired private ProfileService profileService; 

    // 1. Hàm lấy thông tin hồ sơ
    @GetMapping("/{userId}/{role}")
    public ResponseEntity<?> getProfile(@PathVariable Integer userId, @PathVariable String role) {
        ProfileRequest dto = profileService.getProfileData(userId, role);
        if (dto == null) return ResponseEntity.badRequest().body("Tài khoản không tồn tại!");
        return ResponseEntity.ok(dto);
    }

    // 2. Hàm cập nhật chữ thô
    @PutMapping("/{userId}/{role}")
    public ResponseEntity<?> updateProfile(@PathVariable Integer userId, @PathVariable String role, @RequestBody ProfileRequest dto) {
        boolean isUpdated = profileService.updateProfileData(userId, role, dto);
        if (!isUpdated) return ResponseEntity.badRequest().body("Tài khoản không tồn tại!");
        return ResponseEntity.ok("Đã lưu thay đổi thành công!");
    }

    //upload cv file for candidate
    @PostMapping("/{userId}/upload-cv")
    public ResponseEntity<?> uploadCv(@PathVariable Integer userId,
                                    @RequestParam("file") org.springframework.web.multipart.MultipartFile file) {
        try {
            ProfileRequest updated = profileService.uploadCv(userId, file);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Lỗi upload CV!");
        }
    }

    // 3. Hàm duyệt từng ô độc lập
    @PostMapping("/verify-field/{userId}")
    public ResponseEntity<?> verifyField(@PathVariable Integer userId, @RequestBody Map<String, String> request) {
        String fieldType = request.get("fieldType");
        String value = request.get("value");

        // Gọi Service lấy thông tin check điều kiện, Controller không gọi trực tiếp Repo 
        Recruiter rec = profileService.getRecruiterById(userId);
        if (rec == null) return ResponseEntity.badRequest().body("Hồ sơ không tồn tại!");

        // Lớp lọc cứng đầu vào tại Controller (Input Validation)
        if ("taxCode".equals(fieldType)) {
            if (value.length() != 10 && value.length() != 13) {
                return ResponseEntity.ok(Map.of("status", "FAILED", "reason", "MST phải đúng 10 hoặc 13 số!"));
            }
            if (rec.getCompanyName() == null || rec.getCompanyName().isEmpty()) {
                return ResponseEntity.ok(Map.of("status", "FAILED", "reason", "Hãy duyệt Tên trước!"));
            }
        }

        if ("websiteUrl".equals(fieldType)) {
            String val = value.toLowerCase();
            if (val.contains("zalo.me") || val.contains("facebook.com") || val.contains("linkedin.com")) {
                return ResponseEntity.ok(Map.of("status", "FAILED", "reason", "Không chấp nhận mạng xã hội!"));
            }
            if (rec.getCompanyName() == null || rec.getTaxCode() == null) {
                return ResponseEntity.ok(Map.of("status", "FAILED", "reason", "Hãy duyệt Tên và Mã thuế trước!"));
            }
        }

        // Đẩy xuống Service xử lý AI và DB
        try {
            Map<String, Object> result = profileService.verifyFieldLogic(userId, fieldType, value);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.ok(Map.of("status", "FAILED", "reason", "Lỗi xử lý hệ thống!"));
        }
    }


    @PostMapping("/change-password/{userId}")
    public ResponseEntity<?> changePassWord(@PathVariable Integer userId, @RequestBody Map<String, String> request) {
        try{
            profileService.changePassword(userId, request.get("oldPassword"), request.get("newPassword"));
            return ResponseEntity.ok(Map.of("message", "Đổi mật khẩu thành công!"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }catch (Exception e) { 
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(Map.of("message", "Lỗi đổi mật khẩu!"));
        }
    }

    @PostMapping("/{userId}/upload-logo")
    public ResponseEntity<?> uploadLogo(@PathVariable Integer userId,
                                        @RequestParam("file") org.springframework.web.multipart.MultipartFile file) {
        try {
            ProfileRequest updated = profileService.uploadLogo(userId, file);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Lỗi upload logo!");
        }
    }
}