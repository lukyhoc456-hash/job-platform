package com.stu.job_platform.service;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
public class VnPayService {

    private final String vnpTmnCode;
    private final String vnpHashSecret;
    private final String vnpUrl;

    public VnPayService() {
        Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
        this.vnpTmnCode = dotenv.get("vnp_TmnCode");
        this.vnpHashSecret = dotenv.get("vnp_HashSecret");
        String url = dotenv.get("vnp_Url");
        this.vnpUrl = (url != null) ? url : "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html";

        if (vnpTmnCode == null || vnpHashSecret == null) {
            throw new IllegalStateException("Thiếu cấu hình vnp_TmnCode/vnp_HashSecret trong .env!");
        }
    }

    /**
     * Tạo URL thanh toán VNPay. amount tính bằng VND (số thật, hàm tự nhân 100 theo yêu cầu VNPay).
     */
    public String createPaymentUrl(String txnRef, long amount, String orderInfo,
                                    String ipAddress, String returnUrl) {
        Map<String, String> params = new HashMap<>();
        params.put("vnp_Version", "2.1.0");
        params.put("vnp_Command", "pay");
        params.put("vnp_TmnCode", vnpTmnCode);
        params.put("vnp_Amount", String.valueOf(amount * 100));
        params.put("vnp_CurrCode", "VND");
        params.put("vnp_TxnRef", txnRef);
        params.put("vnp_OrderInfo", orderInfo);
        params.put("vnp_OrderType", "other");
        params.put("vnp_Locale", "vn");
        params.put("vnp_ReturnUrl", returnUrl);
        params.put("vnp_IpAddr", ipAddress);

        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("Etc/GMT+7"));
        params.put("vnp_CreateDate", formatter.format(cal.getTime()));
        cal.add(Calendar.MINUTE, 15); // hết hạn link thanh toán sau 15 phút
        params.put("vnp_ExpireDate", formatter.format(cal.getTime()));

        List<String> fieldNames = new ArrayList<>(params.keySet());
        Collections.sort(fieldNames);

        StringBuilder hashData = new StringBuilder();
        StringBuilder query = new StringBuilder();

        for (Iterator<String> it = fieldNames.iterator(); it.hasNext(); ) {
            String fieldName = it.next();
            String fieldValue = params.get(fieldName);
            if (fieldValue != null && !fieldValue.isEmpty()) {
                hashData.append(fieldName).append('=')
                        .append(urlEncode(fieldValue));
                query.append(urlEncode(fieldName)).append('=')
                        .append(urlEncode(fieldValue));
                if (it.hasNext()) {
                    hashData.append('&');
                    query.append('&');
                }
            }
        }

        String secureHash = hmacSHA512(vnpHashSecret, hashData.toString());
        query.append("&vnp_SecureHash=").append(secureHash);

        return vnpUrl + "?" + query;
    }

    /**
     * Xác minh chữ ký của request VNPay gửi về (dùng cho cả IPN và Return URL).
     * @param params toàn bộ query param VNPay gửi lên (đã bao gồm vnp_SecureHash)
     */
    public boolean verifySignature(Map<String, String> params) {
        String receivedHash = params.get("vnp_SecureHash");
        if (receivedHash == null) return false;

        Map<String, String> filtered = new HashMap<>(params);
        filtered.remove("vnp_SecureHash");
        filtered.remove("vnp_SecureHashType");

        List<String> fieldNames = new ArrayList<>(filtered.keySet());
        Collections.sort(fieldNames);

        StringBuilder hashData = new StringBuilder();
        for (Iterator<String> it = fieldNames.iterator(); it.hasNext(); ) {
            String fieldName = it.next();
            String fieldValue = filtered.get(fieldName);
            if (fieldValue != null && !fieldValue.isEmpty()) {
                hashData.append(fieldName).append('=').append(urlEncode(fieldValue));
                if (it.hasNext()) hashData.append('&');
            }
        }

        String calculatedHash = hmacSHA512(vnpHashSecret, hashData.toString());
        return calculatedHash.equalsIgnoreCase(receivedHash);
    }

    private String urlEncode(String value) {
        try {
            return URLEncoder.encode(value, StandardCharsets.US_ASCII.toString());
        } catch (Exception e) {
            return value;
        }
    }

    private String hmacSHA512(String key, String data) {
        try {
            Mac hmac512 = Mac.getInstance("HmacSHA512");
            SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA512");
            hmac512.init(secretKey);
            byte[] result = hmac512.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(2 * result.length);
            for (byte b : result) sb.append(String.format("%02x", b & 0xff));
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("Lỗi tạo chữ ký thanh toán!", e);
        }
    }
}