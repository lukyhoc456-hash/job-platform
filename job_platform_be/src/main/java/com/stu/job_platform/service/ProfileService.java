package com.stu.job_platform.service;

import com.stu.job_platform.dto.ProfileRequest;
import com.stu.job_platform.entity.*;
import com.stu.job_platform.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.beans.factory.annotation.Value;
import java.util.*;

@Service
public class ProfileService {

    private final PasswordEncoder passwordEncoder;
    @Autowired private UserRepository userRepository;
    @Autowired private CandidateRepository candidateRepository;
    @Autowired private RecruiterRepository recruiterRepository;
    @Autowired private AiVerificationService aiVerificationService;
    @Autowired private FileUploadService fileUploadService;

    @Value("${file.upload-dir}")
    String uploadDir;
    
    private final ObjectMapper objectMapper = new ObjectMapper();

    // Lấy thông tin hồ sơ đổ lên giao diện
    public ProfileRequest getProfileData(Integer userId, String role) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) return null;

        ProfileRequest dto = new ProfileRequest();
        dto.setEmail(user.getEmail());
        dto.setName(user.getName()); 
        dto.setRole(user.getRole());

        if ("recruiter".equals(role)) {
            // Tìm không thấy thì tạo mới hoàn toàn để không bị lỗi null property
            Recruiter rec = recruiterRepository.findById(userId).orElse(null);
            if (rec != null) {
                dto.setCompanyName(rec.getCompanyName());
                dto.setCompanyEmail(rec.getCompanyEmail());
                dto.setTaxCode(rec.getTaxCode());
                dto.setWebsiteUrl(rec.getWebsiteUrl());
                dto.setStatus(rec.getStatusTrust() != null ? rec.getStatusTrust() : "pending");
                dto.setPoint(rec.getPoint() != null ? rec.getPoint() : 80);
                dto.setVipStatus(rec.getVipStatus());
                dto.setVipUntil(rec.getVipUntil());
                dto.setCompanyLogo(rec.getLogo());
            } else {
                // Nếu chưa có bản ghi dưới DB con, trả về giá trị mặc định tránh sập FE
                dto.setCompanyName("");
                dto.setCompanyEmail(user.getEmail()); // Lấy tạm mail gốc
                dto.setTaxCode("");
                dto.setWebsiteUrl("");
                dto.setStatus("pending");
                dto.setPoint(80);
            }
        }else {
            Candidate can = candidateRepository.findById(userId).orElse(new Candidate());
            dto.setPhone(can.getPhone());
            dto.setAddress(can.getAddress());
            dto.setCvFileName(can.getCvPath());
            dto.setStatus(user.getStatus() != null ? user.getStatus().toString() : "1");
        }
        return dto;
    }

    // Cập nhật thông tin chữ thô (Mất focus)
    public boolean updateProfileData(Integer userId, String role, ProfileRequest dto) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) return false;

        user.setName(dto.getName()); 
        userRepository.save(user);

        if ("recruiter".equals(role)) {
            Recruiter rec = recruiterRepository.findById(userId).orElse(new Recruiter());
            rec.setId(userId);
            
            String currentStatus = rec.getStatusTrust();
            if (currentStatus == null || "pending".equals(currentStatus) || "verified".equals(currentStatus)) currentStatus = "";
            Set<String> verifiedFields = new HashSet<>(Arrays.asList(currentStatus.split(",")));
            verifiedFields.remove("");
            verifiedFields.remove("pending"); 
            verifiedFields.remove("verified");

            if (rec.getCompanyName() != null && !rec.getCompanyName().equals(dto.getCompanyName())) verifiedFields.remove("name");
            if (rec.getTaxCode() != null && !rec.getTaxCode().equals(dto.getTaxCode())) verifiedFields.remove("tax");
            if (rec.getWebsiteUrl() != null && !rec.getWebsiteUrl().equals(dto.getWebsiteUrl())) verifiedFields.remove("website");

            rec.setCompanyName(dto.getCompanyName());
            rec.setCompanyEmail(dto.getCompanyEmail());
            rec.setTaxCode(dto.getTaxCode());
            rec.setWebsiteUrl(dto.getWebsiteUrl());
            
            if (verifiedFields.isEmpty()) {
                rec.setStatusTrust("pending");
            } else {
                rec.setStatusTrust(String.join(",", verifiedFields));
            }

            int extra = 0;
            if (verifiedFields.contains("name")) extra += 10;
            if (verifiedFields.contains("tax")) extra += 20;
            if (verifiedFields.contains("website")) extra += 10;
            rec.setPoint(60 + extra);
            
            recruiterRepository.save(rec);
        } else {
            Candidate can = candidateRepository.findById(userId).orElse(new Candidate());
            can.setId(userId);
            can.setPhone(dto.getPhone());
            can.setAddress(dto.getAddress());
            candidateRepository.save(can);
        }
        return true;
    }


    //upload CV file for candidate
    private static final List<String> ALLOWED_CV_EXT = List.of(".pdf", ".doc", ".docx");

    ProfileService(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    public ProfileRequest uploadCv(Integer userId, org.springframework.web.multipart.MultipartFile file) throws java.io.IOException {
        Candidate can = candidateRepository.findById(userId).orElse(new Candidate());
        can.setId(userId);

        String originalName = file.getOriginalFilename();
        if (originalName == null || !originalName.contains(".")) {
            throw new IllegalArgumentException("Tên file không hợp lệ");
        }
        String ext = originalName.substring(originalName.lastIndexOf('.')).toLowerCase();
        if (!ALLOWED_CV_EXT.contains(ext)) {
            throw new IllegalArgumentException("Chỉ chấp nhận PDF hoặc Word");
        }
        if (file.getSize() > 5 * 1024 * 1024) {
            throw new IllegalArgumentException("File vượt quá 5MB");
        }

        java.nio.file.Path dirPath = java.nio.file.Paths.get(uploadDir, "cv");
        if (!java.nio.file.Files.exists(dirPath)) {
            java.nio.file.Files.createDirectories(dirPath);
        }

        String fileName = "cv_" + userId + "_" + UUID.randomUUID() + ext;
        java.nio.file.Path filePath = dirPath.resolve(fileName);
        file.transferTo(filePath.toFile());

        if (can.getCvPath() != null) {
            try {
                java.nio.file.Files.deleteIfExists(dirPath.resolve(can.getCvPath()));
            } catch (Exception ignored) {}
        }

        can.setCvPath(fileName);
        candidateRepository.save(can);

        // Trả lại profile mới nhất, đồng bộ format với getProfileData/updateProfileData
        return getProfileData(userId, "candidate");
    }

    // Upload logo for recruiter
    private static final List<String> ALLOWED_LOGO_EXT = List.of(".jpg", ".jpeg", ".png");
    // Upload logo for recruiter
    public ProfileRequest uploadLogo(Integer userId, MultipartFile file) {
        Recruiter rec = recruiterRepository.findById(userId).orElse(new Recruiter());
        rec.setId(userId);

        String originalName = file.getOriginalFilename();
        if (originalName == null || !originalName.contains(".")) {
            throw new IllegalArgumentException("Tên file không hợp lệ");
        }
        String ext = originalName.substring(originalName.lastIndexOf('.')).toLowerCase();
        if (!ALLOWED_LOGO_EXT.contains(ext)) {
            throw new IllegalArgumentException("Chỉ chấp nhận JPG hoặc PNG");
        }
        if (file.getSize() > 2 * 1024 * 1024) {
            throw new IllegalArgumentException("File vượt quá 2MB");
        }

        String oldLogo = rec.getLogo();

        String newLogoPath = fileUploadService.uploadFile(file, "logo");
        rec.setLogo(newLogoPath);
        recruiterRepository.save(rec);

        if (oldLogo != null && !oldLogo.isBlank()) {
            fileUploadService.deleteFile(oldLogo);
        }

        return getProfileData(userId, "recruiter");
    }

    // Hàm lấy nhanh thông tin Recruiter phục vụ cho việc kiểm tra tuần tự ở Controller
    public Recruiter getRecruiterById(Integer userId) {
        return recruiterRepository.findById(userId).orElse(null);
    }

    // Xử lý AI Verification (Giữ nguyên logic của mày)
    public Map<String, Object> verifyFieldLogic(Integer userId, String fieldType, String value) throws Exception {
        Recruiter rec = recruiterRepository.findById(userId).orElse(null);
        if (rec == null) return Map.of("status", "FAILED", "reason", "Hồ sơ không tồn tại!");

        String aiRawJson =
        aiVerificationService.verifyFieldWithAi(
            fieldType,
            value, // URL gốc
            rec,
            rec.getCompanyEmail()
        );
        String cleanJson = aiRawJson.trim();
        int firstBrace = cleanJson.indexOf("{");
        int lastBrace = cleanJson.lastIndexOf("}");
        cleanJson = (firstBrace != -1 && lastBrace != -1) ? cleanJson.substring(firstBrace, lastBrace + 1) : "{\"match_percentage\": 0}";

        Map<String, Object> aiMap = objectMapper.readValue(cleanJson, Map.class);
        int matchPercentage = Integer.parseInt(aiMap.getOrDefault("match_percentage", 0).toString());

        Set<String> verifiedFields = new HashSet<>(Arrays.asList((rec.getStatusTrust() == null ? "" : rec.getStatusTrust()).split(",")));
        verifiedFields.remove("");
        verifiedFields.remove("pending"); 
        verifiedFields.remove("verified");

        if (matchPercentage >= 90) {
            if ("companyName".equals(fieldType)) verifiedFields.add("name");
            else if ("taxCode".equals(fieldType)) verifiedFields.add("tax");
            else if ("websiteUrl".equals(fieldType)){ 
                verifiedFields.add("website");
                verifiedFields.remove("website_pending"); // remove pending if it was there
            }

            if (verifiedFields.containsAll(Set.of("name", "tax", "website"))) {
                rec.setStatusTrust("verified," + String.join(",", verifiedFields)); // "verified,name,tax,website"
                rec.setPoint(100);
            } else {
                rec.setStatusTrust("pending," + String.join(",", verifiedFields)); // "pending,name" hoặc "pending,name,tax"
                rec.setPoint(60 + (verifiedFields.contains("name") ? 10 : 0)
                                + (verifiedFields.contains("tax") ? 20 : 0)
                                + (verifiedFields.contains("website") ? 10 : 0));
            }
            
            recruiterRepository.save(rec);
            
            return Map.of("status", "SUCCESS", "newStatus", rec.getStatusTrust(), "newPoint", rec.getPoint());
        } else {
            if ("companyName".equals(fieldType)) verifiedFields.remove("name");
            else if ("taxCode".equals(fieldType)) verifiedFields.remove("tax");
            else if ("websiteUrl".equals(fieldType)) verifiedFields.remove("website");

            rec.setStatusTrust("pending," + String.join(",", verifiedFields)); // vẫn pending
            rec.setPoint(60 + (verifiedFields.contains("name") ? 10 : 0)
                            + (verifiedFields.contains("tax") ? 20 : 0)
                            + (verifiedFields.contains("website") ? 10 : 0));
            recruiterRepository.save(rec);
            
            return Map.of("status", "FAILED", "reason", aiMap.getOrDefault("reason", "Thông tin không khớp!"));
        }
    }

    public void changePassword(Integer userId, String oldPassword, String newPassword) throws Exception {
        User user = userRepository.findById(userId).orElseThrow(()-> new IllegalArgumentException("Tài khoản không tồn tại"));

        if(!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new IllegalArgumentException("Mật khẩu không đúng");
        }
        if(passwordEncoder.matches(newPassword, user.getPassword())) {
            throw new IllegalArgumentException("Mật khẩu mới không được trùng mật khẩu cũ");
        }

        String hasStringgNewPassword = passwordEncoder.encode(newPassword);
        user.setPassword(hasStringgNewPassword);
        userRepository.save(user);
    }
}