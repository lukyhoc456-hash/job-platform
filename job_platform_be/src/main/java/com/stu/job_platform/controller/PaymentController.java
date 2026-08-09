package com.stu.job_platform.controller;

import com.stu.job_platform.dto.ApiResponse;
import com.stu.job_platform.entity.Recruiter;
import com.stu.job_platform.entity.VipTransaction;
import com.stu.job_platform.repository.RecruiterRepository;
import com.stu.job_platform.repository.VipTransactionRepository;
import com.stu.job_platform.service.PaymentService;
import com.stu.job_platform.service.VnPayService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payment/vnpay")
public class PaymentController {

    @Autowired private VnPayService vnPayService;
    @Autowired private VipTransactionRepository vipTransactionRepository;
    @Autowired private RecruiterRepository recruiterRepository;
    @Autowired private PaymentService paymentService;

    // Bảng giá cố định, tuỳ chỉnh lại số tiền/số ngày tại đây
    private static final Map<Integer, Long> VIP_PRICING = Map.of(
            7, 50_000L,
            30, 150_000L
    );

    private static final String RETURN_URL = "http://localhost:8090/api/v1/payment/vnpay/return";

    /** Bước 1: Recruiter chọn gói VIP -> tạo giao dịch PENDING + trả về link thanh toán */
    @PostMapping("/create")
    @PreAuthorize("hasRole('RECRUITER')")
    public ResponseEntity<ApiResponse<?>> createPayment(
            Authentication auth, @RequestBody Map<String, Integer> body, HttpServletRequest request) {

        Integer recruiterId = (Integer) auth.getPrincipal();
        Integer days = body.get("days");
        Long amount = VIP_PRICING.get(days);

        if (amount == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Gói VIP không hợp lệ!"));
        }

        String txnRef = "VIP" + recruiterId + System.currentTimeMillis();

        VipTransaction txn = new VipTransaction();
        txn.setRecruiterId(recruiterId);
        txn.setTxnRef(txnRef);
        txn.setAmount(amount);
        txn.setDays(days);
        txn.setStatus("PENDING");
        vipTransactionRepository.save(txn);

        String orderInfo = "Nang cap VIP recruiter " + recruiterId + " " + days + " ngay";
        String ipAddress = request.getRemoteAddr();

        String paymentUrl = vnPayService.createPaymentUrl(txnRef, amount, orderInfo, ipAddress, RETURN_URL);

        return ResponseEntity.ok(ApiResponse.success("Tạo link thanh toán thành công!", Map.of("paymentUrl", paymentUrl)));
    }

    /**
     * VNPay gọi NGẦM (server-to-server) tới đây để xác nhận giao dịch thật.
     * Đây là nơi DUY NHẤT được phép set VIP cho recruiter, không phải Return URL.
     */
    @GetMapping("/ipn")
    public ResponseEntity<Map<String, String>> handleIpn(HttpServletRequest request) {
        Map<String, String> params = extractParams(request);
        PaymentService.ConfirmResult result = paymentService.confirmTransaction(params);

        return switch (result) {
            case SUCCESS, ALREADY_CONFIRMED -> ResponseEntity.ok(Map.of("RspCode", "00", "Message", "Confirm Success"));
            case INVALID_SIGNATURE -> ResponseEntity.ok(Map.of("RspCode", "97", "Message", "Invalid signature"));
            case ORDER_NOT_FOUND -> ResponseEntity.ok(Map.of("RspCode", "01", "Message", "Order not found"));
            case INVALID_AMOUNT -> ResponseEntity.ok(Map.of("RspCode", "04", "Message", "Invalid amount"));
            case PAYMENT_FAILED -> ResponseEntity.ok(Map.of("RspCode", "00", "Message", "Confirm Success")); // vẫn trả 00 để VNPay ngưng gọi lại, giao dịch đã ghi nhận FAILED
        };
    }

    @GetMapping("/return")
    public ResponseEntity<?> handleReturn(HttpServletRequest request) {
        Map<String, String> params = extractParams(request);
        String txnRef = params.get("vnp_TxnRef");

        PaymentService.ConfirmResult result = paymentService.confirmTransaction(params);
        String redirectStatus = (result == PaymentService.ConfirmResult.SUCCESS
                || result == PaymentService.ConfirmResult.ALREADY_CONFIRMED) ? "success" : "failed";

        String feUrl = "http://localhost:5173/profile?payment=" + redirectStatus + "&txnRef=" + txnRef;
        return ResponseEntity.status(302).header("Location", feUrl).build();
    }

    private void upgradeToVip(Integer recruiterId, int days) {
        Recruiter rec = recruiterRepository.findById(recruiterId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy hồ sơ doanh nghiệp!"));

        LocalDateTime base = (rec.getVipStatus() != null && rec.getVipStatus() == 1
                && rec.getVipUntil() != null && rec.getVipUntil().isAfter(LocalDateTime.now()))
                ? rec.getVipUntil() : LocalDateTime.now();

        rec.setVipStatus(1);
        rec.setVipUntil(base.plusDays(days));
        recruiterRepository.save(rec);
    }

    private Map<String, String> extractParams(HttpServletRequest request) {
        Map<String, String> params = new HashMap<>();
        request.getParameterMap().forEach((key, value) -> {
            if (value.length > 0) params.put(key, value[0]);
        });
        return params;
    }

    @GetMapping("/history")
    @PreAuthorize("hasRole('RECRUITER')")
    public ResponseEntity<ApiResponse<?>> getHistory(Authentication auth) {
        Integer recruiterId = (Integer) auth.getPrincipal();
        return ResponseEntity.ok(ApiResponse.success("OK", paymentService.getVipHistory(recruiterId)));
    }
}