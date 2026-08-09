package com.stu.job_platform.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "vip_transactions")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class VipTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "recruiter_id", nullable = false)
    private Integer recruiterId;

    @Column(name = "txn_ref", unique = true, nullable = false)
    private String txnRef; // Mã giao dịch gửi cho VNPay, dùng để đối chiếu khi IPN gọi về

    @Column(name = "amount", nullable = false)
    private Long amount; // Số tiền VND (không nhân 100, lưu số thật)

    @Column(name = "days", nullable = false)
    private Integer days; // Số ngày VIP tương ứng gói đã mua

    @Column(name = "status", nullable = false)
    private String status; // PENDING | SUCCESS | FAILED

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "paid_at")
    private LocalDateTime paidAt;


    private LocalDateTime expireAt;
}