package com.stu.job_platform.service;

import com.stu.job_platform.dto.ApiResponse;
import com.stu.job_platform.dto.JobPostResponse;
import com.stu.job_platform.dto.UpdateCandidateRequest;
import com.stu.job_platform.dto.UpdateRecruiterRequest;
import com.stu.job_platform.entity.BugReport;
import com.stu.job_platform.entity.ErrorLog;
import com.stu.job_platform.entity.Industry;
import com.stu.job_platform.entity.JobCategory;
import com.stu.job_platform.entity.Recruiter;
import com.stu.job_platform.entity.User;
import com.stu.job_platform.repository.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class AdminService {

    private final JobCategoryRepository jobCategoryRepository;
    private final IndustryRepository industryRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private JobPostRepository jobPostRepository;
    @Autowired private ApplicationRepository applicationRepository;
    @Autowired private BugReportRepository bugReportRepository;
    @Autowired private CandidateRepository candidateRepository;
    @Autowired private RecruiterRepository recruiterRepository;
    @Autowired private JobPostService jobPostService;
    @Autowired private RefreshTokenRepository refreshTokenRepository;

    AdminService(IndustryRepository industryRepository, JobCategoryRepository jobCategoryRepository) {
        this.industryRepository = industryRepository;
        this.jobCategoryRepository = jobCategoryRepository;
    }

    // ===== USERS =====

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public List<Map<String, Object>> getCandidates() {
        // Lấy tất cả candidates 1 lần, tránh N+1
        List<User> candidates = userRepository.findByRoleIgnoreCase("candidate");

        // Load toàn bộ candidate profiles 1 lần
        Map<Integer, com.stu.job_platform.entity.Candidate> profileMap = new HashMap<>();
        candidateRepository.findAll().forEach(c -> profileMap.put(c.getId(), c));

        return candidates.stream().map(u -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", u.getId());
            map.put("name", u.getName());
            map.put("email", u.getEmail());
            map.put("role", u.getRole());
            map.put("status", u.getStatus());

            var profile = profileMap.get(u.getId());
            if (profile != null) {
                map.put("phone", profile.getPhone());
                map.put("address", profile.getAddress());
                map.put("cvPath", profile.getCvPath());
            } else {
                map.put("phone", "Chưa cập nhật");
                map.put("address", "Chưa cập nhật");
                map.put("cvPath", null);
            }
            return map;
        }).toList();
    }
    //sửa thông tin ứng cử viên
    public void updateCandidate(Integer candidateId, UpdateCandidateRequest request) {
        User candidate = userRepository.findById(candidateId).orElseThrow(() -> new RuntimeException("Ứng viên không tồn tại!"));
        candidate.setName(request.getName());
        candidate.setEmail(request.getEmail());
        userRepository.save(candidate);   
    }

    public List<Map<String, Object>> getRecruiters() {
        List<User> recruiters = userRepository.findByRoleIgnoreCase("recruiter");

        // Load toàn bộ recruiter profiles 1 lần
        Map<Integer, com.stu.job_platform.entity.Recruiter> profileMap = new HashMap<>();
        recruiterRepository.findAll().forEach(r -> profileMap.put(r.getId(), r));

        return recruiters.stream().map(u -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", u.getId());
            map.put("name", u.getName());
            map.put("email", u.getEmail());
            map.put("role", u.getRole());
            map.put("status", u.getStatus());

            var profile = profileMap.get(u.getId());
            if (profile != null) {
                map.put("companyName", profile.getCompanyName());
                map.put("taxCode", profile.getTaxCode());
                map.put("companyEmail", profile.getCompanyEmail());
                map.put("websiteUrl", profile.getWebsiteUrl());
                map.put("logo", profile.getLogo());
                map.put("statusTrust", profile.getStatusTrust());
                map.put("point", profile.getPoint());

            } else {
                map.put("companyName", "Chưa tạo hồ sơ DN");
                map.put("taxCode", "N/A");
                map.put("statusTrust", null);
            }
            return map;
        }).toList();
    }

    //sửa thông tin nhà tuyển dụng
    public void updateRecruiter(Integer recruiterId, UpdateRecruiterRequest request) {
        User recruiter = userRepository.findById(recruiterId).orElseThrow(() -> new RuntimeException("Nhà tuyển dụng không tồn tại!"));
        recruiter.setName(request.getName());
        recruiter.setEmail(request.getEmail());
        userRepository.save(recruiter);

        Recruiter rec = recruiterRepository.findById(recruiterId).orElseThrow(() -> new RuntimeException("Hồ sơ nhà tuyển dụng không tồn tại!"));
        rec.setCompanyName(request.getCompanyName());
        rec.setTaxCode(request.getTaxCode());
        rec.setWebsiteUrl(request.getWebsiteUrl());

        boolean name    = Boolean.TRUE.equals(request.getVerifiedName());
        boolean tax     = Boolean.TRUE.equals(request.getVerifiedTax());
        boolean website = Boolean.TRUE.equals(request.getVerifiedWebsite());

        if (name && tax && website) {
            rec.setStatusTrust("verified");
            rec.setPoint(100);
        } else {
            // Lưu dạng "pending,name,tax" để giữ track field nào đã tích
            Set<String> fields = new HashSet<>();
            if (name)    fields.add("name");
            if (tax)     fields.add("tax");
            if (website) fields.add("website");
            fields.add("pending");
            rec.setStatusTrust(String.join(",", fields));
            int extra = (name ? 10 : 0) + (tax ? 20 : 0) + (website ? 10 : 0);
            rec.setPoint(60 + extra);
        }
        recruiterRepository.save(rec);
    }
    public String getRoleById(Integer id) {
    return userRepository.findById(id)
        .orElseThrow(() -> new RuntimeException("User không tồn tại"))
        .getRole();
    }

    @Transactional
    public void toggleUserStatus(Integer userId, Integer status) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Tài khoản không tồn tại!"));
        user.setStatus(status);
        userRepository.save(user);
        // Khi khóa tài khoản (status = 0), xóa refresh token để buộc đăng xuất ngay
        if (status == 0) {
            refreshTokenRepository.deleteByUser(user);
        }
    }

    public void deleteUser(Integer userId) {
        if (!userRepository.existsById(userId)) {
            throw new RuntimeException("Tài khoản không tồn tại!");
        }
        userRepository.deleteById(userId);
    }

    // ===== JOBS =====

    public Page<JobPostResponse> getAllJobs(Pageable pageable) {
        return jobPostService.getAllJobsAdmin(pageable);
    }

    public void toggleJobStatus(Integer jobId, Integer status) {
        jobPostService.toggleJobStatus(jobId, status);
    }

    // ===== ERROR LOGS =====

    public List<BugReport> getAllBugReports() {
        List<BugReport> bugs = new ArrayList<>(bugReportRepository.findAll());
        Collections.reverse(bugs);
        return bugs;
    }

    public void updateBugStatus(Integer id, String status) {
        BugReport bug = bugReportRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Bug không tồn tại!"));
        bug.setStatus(status);
        bugReportRepository.save(bug);
    }

    // ===== STATS =====

    public Map<String, Object> getStats() {
        long pendingBugs = bugReportRepository.findAll().stream()
        .filter(bug -> "pending".equalsIgnoreCase(bug.getStatus()))
        .count();

        return Map.of(
                "totalUsers", userRepository.count(),
                "totalJobs", jobPostRepository.count(),
                "totalApplications", applicationRepository.count(),
                "pendingBugs", pendingBugs
        );
    }

    public List<Industry> getAllIndustries() {
        return industryRepository.findAll();
    }
    public void addIndustry(String name) {
        Industry industry = new Industry();
        industry.setName(name);
        industry.setStatus(1); 
        industryRepository.save(industry);
    }
    public void updateIndustry(Integer id, String name, Integer status) {
        Industry industry = industryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Ngành nghề không tồn tại!"));
        industry.setName(name);
        industry.setStatus(status);
        industryRepository.save(industry);
    }
    public void deleteIndustry(Integer id) {
       Industry i = industryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Ngành nghề không tồn tại!"));
        i.setStatus(0); // Đặt trạng thái thành inactive thay vì xóa
        industryRepository.save(i);
    }


    public List<JobCategory> getAllJobCategories(){
        return jobCategoryRepository.findAll();
    }
    public void addJobCategory(String name, Integer industryId) {

        JobCategory jobCategory = new JobCategory();
        jobCategory.setName(name);
        jobCategory.setStatus(1); // Mặc định là active
        if(industryId != null) {
            Industry industry = industryRepository.findById(industryId)
                    .orElseThrow(() -> new RuntimeException("Ngành nghề không tồn tại!"));
            jobCategory.setIndustry(industry);
        }
        jobCategoryRepository.save(jobCategory);
    }

    public void updateJobCategory(Integer id, String name) {
        JobCategory jobCategory = jobCategoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Danh mục việc làm không tồn tại!"));
        jobCategory.setName(name);
        jobCategoryRepository.save(jobCategory);
    }
    public void deleteJobCategory(Integer id) {
        JobCategory jobCategory = jobCategoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Danh mục việc làm không tồn tại!"));
        jobCategory.setStatus(0); // Đặt trạng thái thành inactive thay vì xóa
        jobCategoryRepository.save(jobCategory);
    }
}