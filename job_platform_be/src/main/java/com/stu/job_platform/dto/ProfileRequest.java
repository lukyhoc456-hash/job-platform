package com.stu.job_platform.dto;

import java.time.LocalDateTime;

public class ProfileRequest {
    // Tài khoản chung
    private String email;
    private String name;
    private String role;
    private String status;

    // Riêng cho Ứng viên (Candidate)
    private String phone;
    private String address;
    private String cvFileName;

    // Riêng cho Nhà tuyển dụng (Recruiter)
    private String companyName;
    private String companyEmail;
    private String taxCode;
    private String websiteUrl;
    private Integer point;
    private Integer vipStatus;
    private LocalDateTime vipUntil;
    private String companyLogo;

    // --- GETTER & SETTER THUỒN (CHỐNG LỖI LOMBOK) ---
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getCvFileName() { return cvFileName; }
    public void setCvFileName(String cvFileName) { this.cvFileName = cvFileName; }
    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }
    public String getCompanyEmail() { return companyEmail; }
    public void setCompanyEmail(String companyEmail) { this.companyEmail = companyEmail; }
    public String getTaxCode() { return taxCode; }
    public void setTaxCode(String taxCode) { this.taxCode = taxCode; }
    public String getWebsiteUrl() { return websiteUrl; }
    public void setWebsiteUrl(String websiteUrl) { this.websiteUrl = websiteUrl; }
    public Integer getPoint() { return point; }
    public void setPoint(Integer point) { this.point = point; }
    public Integer getVipStatus() { return vipStatus; }
    public void setVipStatus(Integer vipStatus) { this.vipStatus = vipStatus; }
    public LocalDateTime getVipUntil() { return vipUntil; }
    public void setVipUntil(LocalDateTime vipUntil) { this.vipUntil = vipUntil; }
    public String getCompanyLogo() { return companyLogo; }
    public void setCompanyLogo(String companyLogo) { this.companyLogo = companyLogo; }
}