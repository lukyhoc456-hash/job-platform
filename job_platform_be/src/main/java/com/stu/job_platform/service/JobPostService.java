package com.stu.job_platform.service;

import com.stu.job_platform.dto.JobPostRequest;
import com.stu.job_platform.dto.JobPostResponse;
import com.stu.job_platform.entity.JobCategory;
import com.stu.job_platform.entity.JobPost;
import com.stu.job_platform.entity.Recruiter;
import com.stu.job_platform.repository.ApplicationRepository;
import com.stu.job_platform.repository.JobCategoryRepository;
import com.stu.job_platform.repository.JobPostRepository;
import com.stu.job_platform.repository.RecruiterRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

@Service
public class JobPostService {

    private final ContentModerationService contentModerationService;
    @Autowired
    private JobPostRepository jobPostRepository;
    @Autowired
    private RecruiterRepository recruiterRepository;
    @Autowired
    private JobCategoryRepository jobCategoryRepository;
    @Autowired
    private ApplicationRepository applicationRepository;

    private static final String CHARACTERS =
        "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

    JobPostService(ContentModerationService contentModerationService) {
        this.contentModerationService = contentModerationService;
    }

    private String generateJobCode() {

        Random random = new Random();

        String code;

        do {

            StringBuilder builder = new StringBuilder("JP-");

            for (int i = 0; i < 6; i++) {
                builder.append(
                        CHARACTERS.charAt(
                                random.nextInt(CHARACTERS.length())
                        )
                );
            }

            code = builder.toString();

        } while (jobPostRepository.existsByJobCode(code));

        return code;
    }

    /**
     * Tạo bài đăng mới (Recruiter)
     */
    public JobPostResponse createJobPost(Integer recruiterId, JobPostRequest request) {
        Recruiter recruiter = recruiterRepository.findById(recruiterId)
                .orElseThrow(() -> new RuntimeException("Nhà tuyển dụng không tồn tại!"));

        boolean isVerified = recruiter.getStatusTrust() != null  && recruiter.getStatusTrust().contains("verified");
        int currentTrustPoint = recruiter.getPoint() != null ? recruiter.getPoint() : 0;
        if(currentTrustPoint < 90 && !isVerified){
            long activeJobCount = jobPostRepository.countByRecruiterIdAndStatusNot(recruiterId, -1);
            if(activeJobCount >= 1){
                throw new RuntimeException("Tài khoản của bạn chưa xác thực đầy đủ, vui lòng xác thực để đăng thêm bài tuyển dụng. Hiện tại bạn chỉ được đăng 1 bài tuyển dụng khi chưa xác thực!");
            }
        }
        
        if(contentModerationService.containsBannedWord(
            request.getTitle(),
            request.getJdText(),
            request.getRequirements(),
            request.getBenefits()
        )){
            throw new RuntimeException("Nội dung bài đăng chứa từ cấm. Vui lòng kiểm tra lại!");
        }
        
        JobPost jobPost = new JobPost();
        jobPost.setTitle(request.getTitle());
        jobPost.setSalary(request.getSalary());
        jobPost.setLocation(request.getLocation());
        jobPost.setJobType(request.getJobType());
        jobPost.setExperienceLevel(request.getExperienceLevel());
        jobPost.setJdText(request.getJdText());
        jobPost.setRequirements(request.getRequirements());
        jobPost.setBenefits(request.getBenefits());
        jobPost.setStatus(1); // Active mặc định
        jobPost.setJobCode(generateJobCode());
        jobPost.setRecruiter(recruiter);

        if (request.getCategoryId() != null) {
            JobCategory category = jobCategoryRepository.findById(request.getCategoryId()).orElse(null);
            jobPost.setJobCategory(category);
        }

        JobPost saved = jobPostRepository.save(jobPost);
        return toResponse(saved);
    }

    /**
     * Cập nhật bài đăng (Recruiter chỉ sửa được bài của mình)
     */
    public JobPostResponse updateJobPost(Integer jobId, Integer recruiterId, JobPostRequest request) {
        JobPost jobPost = jobPostRepository.findById(jobId)
                .orElseThrow(() -> new RuntimeException("Bài đăng không tồn tại!"));

        if (!jobPost.getRecruiter().getId().equals(recruiterId)) {
            throw new RuntimeException("Bạn không có quyền sửa bài đăng này!");
        }

        if(contentModerationService.containsBannedWord(
            request.getTitle(),
            request.getJdText(),
            request.getRequirements(),
            request.getBenefits()
        )){
            throw new RuntimeException("Nội dung bài đăng chứa từ cấm. Vui lòng kiểm tra lại!");
        }

        jobPost.setTitle(request.getTitle());
        jobPost.setSalary(request.getSalary());
        jobPost.setLocation(request.getLocation());
        jobPost.setJobType(request.getJobType());
        jobPost.setExperienceLevel(request.getExperienceLevel());
        jobPost.setJdText(request.getJdText());
        jobPost.setRequirements(request.getRequirements());
        jobPost.setBenefits(request.getBenefits());

        if (request.getCategoryId() != null) {
            JobCategory category = jobCategoryRepository.findById(request.getCategoryId()).orElse(null);
            jobPost.setJobCategory(category);
        }

        JobPost saved = jobPostRepository.save(jobPost);
        return toResponse(saved);
    }

    /**
     * Xóa bài đăng (soft delete: đổi status = -1)
     */
    public void deleteJobPost(Integer jobId, Integer recruiterId) {
        JobPost jobPost = jobPostRepository.findById(jobId)
                .orElseThrow(() -> new RuntimeException("Bài đăng không tồn tại!"));

        if (!jobPost.getRecruiter().getId().equals(recruiterId)) {
            throw new RuntimeException("Bạn không có quyền xóa bài đăng này!");
        }

        jobPost.setStatus(-1); // Soft delete
        jobPostRepository.save(jobPost);
    }

    /**
     * Lấy chi tiết 1 bài đăng
     */
    public JobPostResponse getJobPost(Integer jobId) {
        JobPost jobPost = jobPostRepository.findById(jobId)
                .orElseThrow(() -> new RuntimeException("Bài đăng không tồn tại!"));
        return toResponse(jobPost);
    }

    /**
     * Lấy chi tiết bài đăng theo job_code (dùng cho AI panel)
     */
    public JobPostResponse getJobPostByCode(String jobCode) {
        JobPost jobPost = jobPostRepository.findByJobCode(jobCode)
                .orElseThrow(() -> new RuntimeException("Mã bài đăng không tồn tại!"));
        return toResponse(jobPost);
    }

    /**
     * Lấy danh sách bài đăng active (cho trang chủ)
     */
    public Page<JobPostResponse> getActiveJobs(Pageable pageable) {
        return jobPostRepository.findByStatus(1, pageable).map(this::toResponse);
    }

    /**
     * Tìm kiếm nâng cao
     */
    public Page<JobPostResponse> searchJobs(String keyword, Integer categoryId, Integer industryId,
                                             String location, String jobType, boolean vipOnly, Pageable pageable) {
        return jobPostRepository.searchJobs(keyword, categoryId, industryId, location, jobType, vipOnly, pageable)
                .map(this::toResponse);
    }

    /**
     * Lấy N bài đăng của các recruiter đang VIP (dùng cho component "Đối Tác Việc Làm Tốt")
     */
    public List<JobPostResponse> getVipJobs(int limit) {
        return jobPostRepository.findVipJobs(PageRequest.of(0, limit)).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Lấy bài đăng của Recruiter (quản lý bài đăng cá nhân)
     */
    public List<JobPostResponse> getJobsByRecruiter(Integer recruiterId) {
        return jobPostRepository.findByRecruiterId(recruiterId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Lấy bài đăng nổi bật (10 bài mới nhất)
     */
    public List<JobPostResponse> getFeaturedJobs() {
        return jobPostRepository.findTop10ByStatusOrderByCreatedAtDesc(1).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Admin: Lấy tất cả bài đăng
     */
    public Page<JobPostResponse> getAllJobsAdmin(Pageable pageable) {
        return jobPostRepository.findAll(pageable).map(this::toResponse);
    }

    /**
     * Admin: Ẩn/Hiện bài đăng
     */
    public void toggleJobStatus(Integer jobId, Integer status) {
        JobPost jobPost = jobPostRepository.findById(jobId)
                .orElseThrow(() -> new RuntimeException("Bài đăng không tồn tại!"));
        jobPost.setStatus(status);
        jobPostRepository.save(jobPost);
    }

    // ===== Helper: Chuyển Entity → Response DTO =====
    private JobPostResponse toResponse(JobPost jobPost) {
        JobPostResponse dto = new JobPostResponse();
        dto.setId(jobPost.getId());
        dto.setTitle(jobPost.getTitle());
        dto.setSalary(jobPost.getSalary());
        dto.setLocation(jobPost.getLocation());
        dto.setLocationCity(extractCity(jobPost.getLocation()));
        dto.setLocationAddress(extractAddress(jobPost.getLocation())); // thêm dòng này
        dto.setJobType(jobPost.getJobType());
        dto.setExperienceLevel(jobPost.getExperienceLevel());
        dto.setJdText(jobPost.getJdText());
        dto.setRequirements(jobPost.getRequirements());
        dto.setBenefits(jobPost.getBenefits());
        dto.setStatus(jobPost.getStatus());
        dto.setCreatedAt(jobPost.getCreatedAt());
        dto.setJobCode(jobPost.getJobCode());

        // Gom thông tin recruiter
        if (jobPost.getRecruiter() != null) {
            dto.setRecruiterId(jobPost.getRecruiter().getId());
            dto.setCompanyName(jobPost.getRecruiter().getCompanyName());
            dto.setCompanyLogo(jobPost.getRecruiter().getLogo());
            dto.setCompanyPoint(jobPost.getRecruiter().getPoint());
        }

        // Gom thông tin category
        if (jobPost.getJobCategory() != null) {
            dto.setCategoryId(jobPost.getJobCategory().getId());
            dto.setCategoryName(jobPost.getJobCategory().getName());
        }

        if (jobPost.getRecruiter() != null) {
            dto.setRecruiterId(jobPost.getRecruiter().getId());
            dto.setCompanyName(jobPost.getRecruiter().getCompanyName());
            dto.setCompanyLogo(jobPost.getRecruiter().getLogo());
            dto.setCompanyPoint(jobPost.getRecruiter().getPoint());

            boolean isVip = jobPost.getRecruiter().getVipStatus() != null
                    && jobPost.getRecruiter().getVipStatus() == 1
                    && jobPost.getRecruiter().getVipUntil() != null
                    && jobPost.getRecruiter().getVipUntil().isAfter(LocalDateTime.now());
            dto.setVip(isVip);
        }

        // Đếm số ứng tuyển
        dto.setApplicationCount(applicationRepository.countByJobPostId(jobPost.getId()));

        return dto;
    }

    private String extractCity(String location){
        if(location== null||location.isBlank())return null;
        int start = location.lastIndexOf('(');
        int end = location.lastIndexOf(')');
        if(start>=0&&end>start){
            return location.substring(start+1, end).trim();
        }
        return location;
    }

    private String extractAddress(String location) {
        if (location == null || location.isBlank()) return null;
        int start = location.lastIndexOf('(');
        if (start > 0) {
            return location.substring(0, start).trim();
        }
        return location; // fallback cho dữ liệu chưa chuẩn hóa
    }


    /**
     * Lấy danh sách các loại hình công việc (job type) đang tồn tại thực tế
     */
    public List<String> getAllJobTypes() {
        return jobPostRepository.findDistinctJobTypes();
    }
    /**
     * Thống kê công khai cho trang chủ (tổng job, công ty, lượt ứng tuyển)
     */
    public java.util.Map<String, Long> getPublicStats() {
        java.util.Map<String, Long> stats = new java.util.HashMap<>();
        stats.put("totalJobs", jobPostRepository.countByStatus(1));
        stats.put("totalCompanies", jobPostRepository.countDistinctActiveRecruiters());
        stats.put("totalApplications", applicationRepository.count()); // count() có sẵn từ JpaRepository
        return stats;
    }
}
