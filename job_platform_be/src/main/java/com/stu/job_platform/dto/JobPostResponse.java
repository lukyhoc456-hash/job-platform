package com.stu.job_platform.dto;

import java.time.LocalDateTime;

/**
 * DTO trả về thông tin bài đăng tuyển dụng cho Front-end
 */
public class JobPostResponse {
    private Integer id;
    private String title;
    private String salary;
    private String location;
    private String locationCity; // Thành phố đã tách ra, dùng để hiển thị/filter nhanh
    private String locationAddress; // Phần địa chỉ, đã bỏ tên thành phố trong ngoặc
    private String jobType;
    private String experienceLevel;
    private String jdText;
    private String requirements;
    private String benefits;
    private Integer status;
    private LocalDateTime createdAt;
    private String jobCode;


    // Thông tin nhà tuyển dụng (gom gọn)
    private Integer recruiterId;
    private String companyName;
    private String companyLogo;
    private Integer companyPoint;

    // Thông tin danh mục
    private Integer categoryId;
    private String categoryName;

    // Thống kê
    private long applicationCount;

    // Thông tin VIP
    private boolean vip;

    // Getter & Setter
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getSalary() { return salary; }
    public void setSalary(String salary) { this.salary = salary; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public String getLocationCity() { return locationCity; }
    public void setLocationCity(String locationCity) { this.locationCity = locationCity; }
    public String getLocationAddress() { return locationAddress; }
    public void setLocationAddress(String locationAddress) { this.locationAddress = locationAddress; }
    public String getJobType() { return jobType; }
    public void setJobType(String jobType) { this.jobType = jobType; }
    public String getExperienceLevel() { return experienceLevel; }
    public void setExperienceLevel(String experienceLevel) { this.experienceLevel = experienceLevel; }
    public String getJdText() { return jdText; }
    public void setJdText(String jdText) { this.jdText = jdText; }
    public String getRequirements() { return requirements; }
    public void setRequirements(String requirements) { this.requirements = requirements; }
    public String getBenefits() { return benefits; }
    public void setBenefits(String benefits) { this.benefits = benefits; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public Integer getRecruiterId() { return recruiterId; }
    public void setRecruiterId(Integer recruiterId) { this.recruiterId = recruiterId; }
    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }
    public String getCompanyLogo() { return companyLogo; }
    public void setCompanyLogo(String companyLogo) { this.companyLogo = companyLogo; }
    public Integer getCompanyPoint() { return companyPoint; }
    public void setCompanyPoint(Integer companyPoint) { this.companyPoint = companyPoint; }
    public Integer getCategoryId() { return categoryId; }
    public void setCategoryId(Integer categoryId) { this.categoryId = categoryId; }
    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }
    public long getApplicationCount() { return applicationCount; }
    public void setApplicationCount(long applicationCount) { this.applicationCount = applicationCount; }
    public String getJobCode() { return jobCode; }
    public void setJobCode(String jobCode) { this.jobCode = jobCode; }

    public boolean isVip() { return vip; }
    public void setVip(boolean vip) { this.vip = vip; }
}
