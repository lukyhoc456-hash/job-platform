package com.stu.job_platform.service;

import com.stu.job_platform.dto.VipHistoryItemDTO;
import com.stu.job_platform.entity.Recruiter;
import com.stu.job_platform.entity.VipTransaction;
import com.stu.job_platform.repository.RecruiterRepository;
import com.stu.job_platform.repository.VipTransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class PaymentService {

    @Autowired private VnPayService vnPayService;
    @Autowired private VipTransactionRepository vipTransactionRepository;
    @Autowired private RecruiterRepository recruiterRepository;

    public enum ConfirmResult {
        SUCCESS,            // xác nhận + set VIP thành công
        ALREADY_CONFIRMED,  // giao dịch đã xử lý trước đó (idempotent)
        INVALID_SIGNATURE,  // chữ ký sai -> nghi giả mạo
        ORDER_NOT_FOUND,    // không tìm thấy txnRef
        INVALID_AMOUNT,     // số tiền không khớp
        PAYMENT_FAILED      // VNPay báo giao dịch thất bại (không phải lỗi hệ thống)
    }

    /** Xác nhận 1 giao dịch VNPay gửi về (dùng chung cho cả /ipn và /return) */
    public ConfirmResult confirmTransaction(Map<String, String> params) {
        if (!vnPayService.verifySignature(params)) {
            return ConfirmResult.INVALID_SIGNATURE;
        }

        String txnRef = params.get("vnp_TxnRef");
        VipTransaction txn = vipTransactionRepository.findByTxnRef(txnRef).orElse(null);
        if (txn == null) {
            return ConfirmResult.ORDER_NOT_FOUND;
        }

        if (!"PENDING".equals(txn.getStatus())) {
            // Đã SUCCESS hoặc FAILED trước đó rồi -> không xử lý lại (chống double-processing)
            return "SUCCESS".equals(txn.getStatus())
                    ? ConfirmResult.ALREADY_CONFIRMED
                    : ConfirmResult.PAYMENT_FAILED;
        }

        long expectedAmount = txn.getAmount() * 100;
        long receivedAmount = Long.parseLong(params.getOrDefault("vnp_Amount", "0"));
        if (expectedAmount != receivedAmount) {
            return ConfirmResult.INVALID_AMOUNT;
        }

        String responseCode = params.get("vnp_ResponseCode");
        boolean isSuccess = "00".equals(responseCode);

        if (isSuccess) {
            txn.setStatus("SUCCESS");
            txn.setPaidAt(LocalDateTime.now());
            vipTransactionRepository.save(txn);
            upgradeToVip(txn);
            return ConfirmResult.SUCCESS;
        } else {
            txn.setStatus("FAILED");
            vipTransactionRepository.save(txn);
            return ConfirmResult.PAYMENT_FAILED;
        }
    }

    private void upgradeToVip(VipTransaction txn) {
        Recruiter rec = recruiterRepository.findById(txn.getRecruiterId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy hồ sơ doanh nghiệp!"));

        LocalDateTime base = (rec.getVipStatus() != null && rec.getVipStatus() == 1
                && rec.getVipUntil() != null && rec.getVipUntil().isAfter(LocalDateTime.now()))
                ? rec.getVipUntil() : LocalDateTime.now();
        LocalDateTime newExpireAt = base.plusDays(txn.getDays());
        rec.setVipStatus(1);
        rec.setVipUntil(newExpireAt);
        recruiterRepository.save(rec);

        txn.setExpireAt(newExpireAt);
        vipTransactionRepository.save(txn);
    }


    public List<VipHistoryItemDTO> getVipHistory(Integer recruiterId) {
        return vipTransactionRepository.findByRecruiterIdAndStatusOrderByPaidAtDesc(recruiterId, "SUCCESS")
                .stream()
                .map(t -> new VipHistoryItemDTO(t.getPaidAt(), t.getDays(), t.getExpireAt(), t.getAmount()))
                .toList();
    }
}