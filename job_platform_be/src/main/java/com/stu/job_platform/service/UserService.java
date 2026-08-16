package com.stu.job_platform.service;

import com.stu.job_platform.dto.RecruiterTrustResponse;
import com.stu.job_platform.dto.RegisterRequest;
import com.stu.job_platform.entity.User;
import com.stu.job_platform.entity.Candidate;
import com.stu.job_platform.entity.Recruiter;
import com.stu.job_platform.repository.RecruiterRepository;
import com.stu.job_platform.repository.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.List;

import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder; // Sử dụng PasswordEncoder của Spring Security để mã hóa mật khẩu

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private EmailService emailService;

    @Autowired
    private RecruiterRepository recruiterRepository;

    private final Map<String, Long> otpCoolDownStore = new ConcurrentHashMap<>();
    private static final long OTP_COUNTDOWN = 30*1000;

    // Lưu tạm thông tin đăng ký + OTP cho tài khoản (email đăng nhập)
    private final Map<String, PendingRegistration> otpStore = new ConcurrentHashMap<>();

    // Lưu tạm OTP xác thực email công ty (chỉ dùng cho recruiter)
    private final Map<String, CompanyEmailOtp> companyOtpStore = new ConcurrentHashMap<>();

    // ================= BƯỚC 1: NHẬN FORM ĐĂNG KÝ -> GỬI OTP TÀI KHOẢN =================
    public String registerUser(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            return "Email này đã được sử dụng!";
        }

        //check thời gian otp(hạn 30s/1 lần)
        long now = System.currentTimeMillis();
        Long nextAllowedTime = otpCoolDownStore.get(request.getEmail());
        if(nextAllowedTime!=null && now<nextAllowedTime){
            long remainingTime = (nextAllowedTime-now)/1000+1;
            return "Bạn vừa yêu cầu OTP, vui lòng chờ "+ remainingTime+" giây để thử lại!";
        }
        otpCoolDownStore.put(request.getEmail(), now+OTP_COUNTDOWN);

        // Recruiter bắt buộc phải xác thực email công ty trước khi được phép đăng ký
        if ("recruiter".equalsIgnoreCase(request.getRole())
                && !isCompanyEmailVerified(request.getCompanyEmail())) {
            return "Vui lòng xác thực email công ty trước khi đăng ký!";
        }

        String otpCode = String.valueOf((int) (Math.random() * 900000) + 100000);
        long expireTime = System.currentTimeMillis() + 5 * 60 * 1000;

        otpStore.put(request.getEmail(), new PendingRegistration(request, otpCode, expireTime));

        try {
            emailService.sendOtpEmail(request.getEmail(), otpCode);
        } catch (Exception e) {
            System.out.println("Lỗi gửi mail SMTP trên Railway chặn SMTP. DEMO OTP: " + otpCode);
            return "Server đang chặn gửi Email. (DEMO OTP của bạn là: " + otpCode + ")";
        }

        return "Mã OTP đã được gửi về Gmail của bạn, vui lòng check mail để xác thực!";
    }

    // ================= BƯỚC 2: XÁC THỰC OTP TÀI KHOẢN -> TẠO USER THẬT =================
    @Transactional
    public String verifyOtp(String email, String otpInput) {
        PendingRegistration pending = otpStore.get(email);

        if (pending == null) {
            return "Phiên làm việc hết hạn!";
        }
        if (!pending.getOtpCode().equals(otpInput)) {
            return "Mã OTP nhập sai!";
        }
        if (System.currentTimeMillis() > pending.getExpireTime()) {
            otpStore.remove(email);
            return "Mã OTP đã hết hạn, vui lòng đăng ký lại!";
        }

        RegisterRequest request = pending.getRequest();

        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(request.getRole().toLowerCase());
        user.setStatus(1);

        User savedUser = userRepository.save(user);
        userRepository.flush();

        if ("candidate".equalsIgnoreCase(savedUser.getRole())) {
            Candidate candidate = new Candidate();
            candidate.setUser(savedUser);
            candidate.setId(savedUser.getId());
            candidate.setPhone(request.getPhone());
            candidate.setAddress(request.getAddress());
            entityManager.persist(candidate);

        } else if ("recruiter".equalsIgnoreCase(savedUser.getRole())) {
            Recruiter recruiter = new Recruiter();
            recruiter.setUser(savedUser);
            recruiter.setId(savedUser.getId());
            recruiter.setCompanyName(request.getCompanyName());

            String compEmail = request.getCompanyEmail();
            recruiter.setCompanyEmail(compEmail != null && !compEmail.isEmpty() ? compEmail : request.getEmail());

            int initialPoint = 30; // Đăng ký thành công: +30 điểm
            if (isCompanyEmail(recruiter.getCompanyEmail())) {
                initialPoint += 30; // Đuôi mail công ty chính chủ: +30 điểm
            }
            recruiter.setPoint(initialPoint);
            recruiter.setStatusTrust("pending");
            entityManager.persist(recruiter);
        }

        otpStore.remove(email);
        // Dọn luôn OTP email công ty sau khi tài khoản đã tạo xong
        if (request.getCompanyEmail() != null) {
            companyOtpStore.remove(request.getCompanyEmail());
        }

        return "Xác thực thành công! Tài khoản của bạn đã được kích hoạt.";
    }

    // ================= XÁC THỰC EMAIL CÔNG TY (RIÊNG, TRƯỚC KHI ĐĂNG KÝ) =================

    // Gửi OTP tới email công ty
    public String sendCompanyEmailOtp(String companyEmail) {
        if (companyEmail == null || companyEmail.isBlank()) {
            return "Thiếu email công ty";
        }

        //check thời gian otp(hạn 30s/1 lần)
        long now = System.currentTimeMillis();
        Long nextAllowedTime = otpCoolDownStore.get(companyEmail);
        if(nextAllowedTime!=null && now<nextAllowedTime){
            long remainingTime = (nextAllowedTime-now)/1000+1;
            return "Bạn vừa yêu cầu OTP, vui lòng chờ "+ remainingTime+" giây để thử lại!";
        }
        otpCoolDownStore.put(companyEmail, now+OTP_COUNTDOWN);

        String otpCode = String.valueOf((int) (Math.random() * 900000) + 100000);
        long expireTime = System.currentTimeMillis() + 5 * 60 * 1000;

        companyOtpStore.put(companyEmail, new CompanyEmailOtp(otpCode, expireTime));

        try {
            emailService.sendOtpEmail(companyEmail, otpCode);
        } catch (Exception e) {
            System.out.println("Lỗi gửi mail SMTP trên Railway chặn SMTP. DEMO OTP công ty: " + otpCode);
            return "Server đang chặn gửi Email. (DEMO OTP là: " + otpCode + ")";
        }

        return "Đã gửi mã OTP tới email công ty!";
    }

    // Xác thực OTP email công ty
    public String verifyCompanyEmailOtp(String companyEmail, String otpInput) {
        CompanyEmailOtp entry = companyOtpStore.get(companyEmail);

        if (entry == null) {
            return "Vui lòng bấm 'Lấy OTP' trước!";
        }
        if (System.currentTimeMillis() > entry.getExpireTime()) {
            companyOtpStore.remove(companyEmail);
            return "Mã OTP đã hết hạn, vui lòng lấy lại!";
        }
        if (!entry.getOtpCode().equals(otpInput)) {
            return "Mã OTP không đúng!";
        }

        entry.setVerified(true);
        return "Xác thực email công ty thành công!";
    }

    private boolean isCompanyEmailVerified(String companyEmail) {
        if (companyEmail == null) return false;
        CompanyEmailOtp entry = companyOtpStore.get(companyEmail);
        if (System.currentTimeMillis() > entry.getExpireTime()) {
            companyOtpStore.remove(companyEmail);
            return false;
        }
        return entry != null && entry.isVerified();
    }

    /**
     * Hàm Helper kiểm tra xem Email có phải thuộc Doanh nghiệp hay không
     * Bằng cách lọc bỏ các Domain mail cá nhân công cộng miễn phí
     */
    private boolean isCompanyEmail(String email) {
        if (email == null || !email.contains("@")) return false;

        String domain = email.substring(email.indexOf("@") + 1).toLowerCase().trim();

        String[] publicDomains = {
            "gmail.com", "yahoo.com", "hotmail.com", "outlook.com",
            "live.com", "icloud.com", "mail.ru", "yandex.com"
        };

        for (String publicDomain : publicDomains) {
            if (domain.equals(publicDomain)) {
                return false;
            }
        }
        return true;
    }

    // ================= QUÊN MẬT KHẨU =================
    private final Map<String, PasswordOtp> resetPasswordStore = new ConcurrentHashMap<>();
    public  String forgotPassword(String email){
        if(email == null || email.isBlank()){
            return "Vui lòng nhập email";
        }
        //check thời gian otp(hạn 30s/1 lần)
        long now = System.currentTimeMillis();
        Long nextAllowedTime = otpCoolDownStore.get(email);
        if(nextAllowedTime!=null && now<nextAllowedTime){
            long remainingTime = (nextAllowedTime-now)/1000+1;
            return "Bạn vừa yêu cầu OTP, vui lòng chờ "+ remainingTime+" giây để thử lại!";
        }
        otpCoolDownStore.put(email, now+OTP_COUNTDOWN);


        if(!userRepository.existsByEmail(email)){
            return "Email không tồn tại";
        }

        String otpCode = String.valueOf((int) (Math.random()*900000) + 100000);
        Long expireTime = System.currentTimeMillis() + 5 * 60 * 1000;// 5 phút

        resetPasswordStore.put(email, new PasswordOtp(otpCode, expireTime));

        try{
            emailService.sendOtpEmail(email, otpCode);
        }catch (Exception e){
            System.out.println("Lỗi gửi mail SMTP. DEMO OTP quên mật khẩu: " + otpCode);
            return "Server đang chặn gửi Email. (DEMO OTP là: " + otpCode + ")";
        }
        return "Đã gửi mã OTP tới email của bạn!";
    }

    // Xác thực OTP và đổi mật khẩu
    @Transactional
    public String resetPassword(String email, String otpInput, String newPassword){
        PasswordOtp pending = resetPasswordStore.get(email);

        // Kiểm tra OTP
        if(pending == null){
            return "Vui lòng nhập mã OTP trước!";
        }
        if(!pending.getOtpCode().equals(otpInput)){
            return "Mã OTP nhập sai!";
        }
        if(System.currentTimeMillis() > pending.getExpireTime()){
            resetPasswordStore.remove(email);
            return "Mã OTP đã hết hạn, vui lòng thử lại!";
        }
        if(newPassword == null || newPassword.length() < 6){
            return "Mật khẩu mới phải có ít nhất 6 ký tự!";
        }

        User user = userRepository.findByEmail(email);
        if(user == null){
            return "Email không tồn tại!";
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        resetPasswordStore.remove(email);

        return "Đổi mật khẩu thành công!";
    }



    @Transactional(readOnly = true)
    public List<RecruiterTrustResponse> getTrustedRecruiters() {
        return recruiterRepository.findByPointAndStatusTrust(100, "verified")
                .stream()
                .map(RecruiterTrustResponse::new)
                .collect(Collectors.toList());
    }

}