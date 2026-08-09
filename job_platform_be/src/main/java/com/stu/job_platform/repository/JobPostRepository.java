package com.stu.job_platform.repository;

import com.stu.job_platform.entity.JobPost;
import com.stu.job_platform.entity.Recruiter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface JobPostRepository extends JpaRepository<JobPost, Integer> {

    // Lấy danh sách bài đăng theo recruiter
    List<JobPost> findByRecruiterId(Integer recruiterId);

    // Lấy bài đăng active (status = 1)
    Page<JobPost> findByStatus(Integer status, Pageable pageable);

    // Tìm kiếm nâng cao: theo từ khóa (title hoặc jdText), danh mục, địa điểm, loại
    // công việc
    // sửa lại searchJobs: thêm điều kiện lọc vipOnly
    @Query("SELECT j FROM JobPost j WHERE j.status = 1 " +
        "AND (:keyword IS NULL OR LOWER(j.title) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
        "    OR LOWER(cast(j.jdText as string)) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
        "AND (:categoryId IS NULL OR j.jobCategory.id = :categoryId) " +
        "AND (:industryId IS NULL OR j.jobCategory.industry.id = :industryId) " +
        "AND (:location IS NULL OR LOWER(j.location) LIKE LOWER(CONCAT('%', :location, '%'))) " +
        "AND (:jobType IS NULL OR j.jobType = :jobType) " +
        "AND (:vipOnly = false OR (j.recruiter.vipStatus = 1 AND j.recruiter.vipUntil > CURRENT_TIMESTAMP))")
        Page<JobPost> searchJobs(
                @Param("keyword") String keyword,
                @Param("categoryId") Integer categoryId,
                @Param("industryId") Integer industryId,
                @Param("location") String location,
                @Param("jobType") String jobType,
                @Param("vipOnly") boolean vipOnly,
                Pageable pageable);


    // Đếm số bài đăng theo recruiter
    long countByRecruiterId(Integer recruiterId);

    // Lấy bài đăng nổi bật (mới nhất, active)
    List<JobPost> findTop10ByStatusOrderByCreatedAtDesc(Integer status);

    boolean existsByJobCode(String jobCode);

    Optional<JobPost> findByJobCode(String jobCode);

    List<JobPost> findByStatus(Integer status);


    long countByRecruiterIdAndStatus(Integer recruiterId, Integer status);

    long countByRecruiterIdAndStatusNot(Integer recruiterId, Integer status);


    @Query("SELECT DISTINCT j.jobType FROM JobPost j WHERE j.status = 1 AND j.jobType IS NOT NULL")
    List<String> findDistinctJobTypes();

    long countByStatus(Integer status);

    @Query("SELECT COUNT(DISTINCT j.recruiter.id) FROM JobPost j WHERE j.status = 1")
    long countDistinctActiveRecruiters();


     /** Lấy job của recruiter đang VIP còn hiệu lực, mới nhất trước */
    @Query("SELECT j FROM JobPost j WHERE j.status = 1 " +
            "AND j.recruiter.vipStatus = 1 " +
            "AND j.recruiter.vipUntil > CURRENT_TIMESTAMP " +
            "ORDER BY j.createdAt DESC")
    List<JobPost> findVipJobs(Pageable pageable);
}
