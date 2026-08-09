package com.stu.job_platform.entity;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "recruiters")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class Recruiter {
    @Id
    private Integer id;


    @Column(name = "company_name")
    private String companyName;

    @Column(name = "tax_code", unique = true)
    private String taxCode;

    @Column(name = "company_email", unique = true)
    private String companyEmail;

    @Column(name = "website_url")
    private String websiteUrl; // ◄ Khớp chuẩn website_url

    private String logo;
    private Integer point;

    @Column(name = "status_trust")
    private String statusTrust; // ◄ Khớp chuẩn status_trust ('pending', 'verified', 'banned')

    @Column(name = "vip_status")
    private Integer vipStatus = 0;

    @Column(name = "vip_until")
    private LocalDateTime vipUntil;

    @OneToOne
    @MapsId
    @JoinColumn(name = "id")
    private User user;
}