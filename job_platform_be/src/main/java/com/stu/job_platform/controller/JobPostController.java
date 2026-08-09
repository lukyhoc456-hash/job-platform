package com.stu.job_platform.controller;

import com.stu.job_platform.dto.ApiResponse;
import com.stu.job_platform.dto.JobPostRequest;
import com.stu.job_platform.dto.JobPostResponse;
import com.stu.job_platform.service.JobPostService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/jobs")
public class JobPostController {

    @Autowired
    private JobPostService jobPostService;

    // ===== PUBLIC APIs (Không cần đăng nhập) =====

    /**
     * Tìm kiếm bài đăng (Trang danh sách việc làm)
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Page<JobPostResponse>>> searchJobs(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer categoryId,
            @RequestParam(required = false) Integer industryId,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) String jobType,
            @RequestParam(required = false, defaultValue = "false") boolean vip,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<JobPostResponse> result = jobPostService.searchJobs(keyword, categoryId, industryId, location, jobType, vip, pageable);
        return ResponseEntity.ok(ApiResponse.success("Tìm kiếm thành công!", result));
    }

    /**
     * Lấy bài đăng nổi bật (Trang chủ)
     */
    @GetMapping("/featured")
    public ResponseEntity<ApiResponse<List<JobPostResponse>>> getFeaturedJobs() {
        return ResponseEntity.ok(ApiResponse.success(jobPostService.getFeaturedJobs()));
    }
    /**
     * Thống kê công khai (Trang chủ)
     */
    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<java.util.Map<String, Long>>> getPublicStats() {
        return ResponseEntity.ok(ApiResponse.success(jobPostService.getPublicStats()));
    }

    /**
     * Xem chi tiết bài đăng
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<JobPostResponse>> getJobDetail(@PathVariable Integer id) {
        return ResponseEntity.ok(ApiResponse.success(jobPostService.getJobPost(id)));
    }

    /**
     * Lấy chi tiết bài đăng theo job_code (dùng cho AI match panel)
     */
    @GetMapping("/by-code/{jobCode}")
    public ResponseEntity<ApiResponse<JobPostResponse>> getJobByCode(@PathVariable String jobCode) {
        return ResponseEntity.ok(ApiResponse.success(jobPostService.getJobPostByCode(jobCode)));
    }

    // ===== RECRUITER APIs (Cần đăng nhập + role RECRUITER) =====

    /**
     * Tạo bài đăng mới
     */
    @PostMapping
    @PreAuthorize("hasRole('RECRUITER')")
    public ResponseEntity<ApiResponse<JobPostResponse>> createJob(
            Authentication auth, @Valid @RequestBody JobPostRequest request) {
        Integer recruiterId = (Integer) auth.getPrincipal();
        return ResponseEntity.ok(ApiResponse.success("Tạo bài đăng thành công!", 
                jobPostService.createJobPost(recruiterId, request)));
    }

    /**
     * Cập nhật bài đăng
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('RECRUITER')")
    public ResponseEntity<ApiResponse<JobPostResponse>> updateJob(
            @PathVariable Integer id, Authentication auth, @Valid @RequestBody JobPostRequest request) {
        Integer recruiterId = (Integer) auth.getPrincipal();
        return ResponseEntity.ok(ApiResponse.success("Cập nhật bài đăng thành công!", 
                jobPostService.updateJobPost(id, recruiterId, request)));
    }

    /**
     * Xóa bài đăng (soft delete)
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('RECRUITER')")
    public ResponseEntity<ApiResponse<?>> deleteJob(@PathVariable Integer id, Authentication auth) {
        Integer recruiterId = (Integer) auth.getPrincipal();
        jobPostService.deleteJobPost(id, recruiterId);
        return ResponseEntity.ok(ApiResponse.success("Xóa bài đăng thành công!"));
    }

    /**
     * Lấy danh sách bài đăng của recruiter (quản lý)
     */
    @GetMapping("/my-posts")
    @PreAuthorize("hasRole('RECRUITER')")
    public ResponseEntity<ApiResponse<List<JobPostResponse>>> getMyPosts(Authentication auth) {
        Integer recruiterId = (Integer) auth.getPrincipal();
        return ResponseEntity.ok(ApiResponse.success(jobPostService.getJobsByRecruiter(recruiterId)));
    }


    /**
     * Lấy job của các recruiter VIP (component "Từ Đối Tác Việc Làm Tốt")
     */
    @GetMapping("/vip")
    public ResponseEntity<ApiResponse<List<JobPostResponse>>> getVipJobs(
            @RequestParam(defaultValue = "8") int limit) {
        return ResponseEntity.ok(ApiResponse.success(jobPostService.getVipJobs(limit)));
    }

}
