package com.stu.job_platform.dto;

import java.time.LocalDateTime;

public class VipHistoryItemDTO {
    private LocalDateTime paidAt;
    private Integer days;
    private LocalDateTime expireAt;
    private Long amount;

    public VipHistoryItemDTO(LocalDateTime paidAt, Integer days, LocalDateTime expireAt, Long amount) {
        this.paidAt = paidAt;
        this.days = days;
        this.expireAt = expireAt;
        this.amount = amount;
    }
    // getters
    public LocalDateTime getPaidAt() { return paidAt; }
    public Integer getDays() { return days; }
    public LocalDateTime getExpireAt() { return expireAt; }
    public Long getAmount() { return amount; }
}