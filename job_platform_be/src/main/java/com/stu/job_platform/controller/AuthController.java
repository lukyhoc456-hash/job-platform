package com.stu.job_platform.controller;

import com.stu.job_platform.dto.ApiResponse;
import com.stu.job_platform.dto.LoginRequest;
import com.stu.job_platform.dto.LoginResponse;
import com.stu.job_platform.dto.RefreshTokenRequest;
import com.stu.job_platform.entity.RefreshToken;
import com.stu.job_platform.entity.User;
import com.stu.job_platform.repository.UserRepository;
import com.stu.job_platform.service.RefreshTokenService;
import com.stu.job_platform.util.JwtUtil;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;


@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private RefreshTokenService refreshTokenService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        // 1. Tìm user theo email
        User user = userRepository.findByEmail(request.getEmail());
        if (user == null) {
            return ResponseEntity.badRequest().body(com.stu.job_platform.dto.ApiResponse.error("Email không tồn tại!"));
        }

        

        // 2. Kiểm tra mật khẩu đã hash dưới DB bằng BCrypt
        if (!BCrypt.checkpw(request.getPassword(), user.getPassword())) {
            return ResponseEntity.badRequest().body(com.stu.job_platform.dto.ApiResponse.error("Sai mật khẩu!"));
        }

        if (user.getStatus() == null || user.getStatus() != 1) {
        return ResponseEntity.badRequest()
                .body(ApiResponse.error("Tài khoản đã bị khóa hoặc chưa được kích hoạt!"));
        }

        // 3. Đăng nhập đúng -> Khạc ra Access Token (7 phút)
        String accessToken = jwtUtil.generateAccessToken(user);

        // 4. Khạc tiếp Refresh Token (5 ngày) và lưu xuống DB bảng refresh_tokens
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user.getId());

        // 5. Trả cặp đôi này về cho React
        return ResponseEntity.ok(new LoginResponse(accessToken, refreshToken.getToken()));
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@RequestBody RefreshTokenRequest request) {
        String requestRefreshToken = request.getToken();

        // Tìm cục token dưới DB, kiểm tra hết hạn 5 ngày chưa, nếu ổn thì cấp Access Token mới
        return refreshTokenService.findByToken(requestRefreshToken)
                .map(refreshTokenService::verifyExpiration)
                .map(RefreshToken::getUser)
                .map(user -> {
                    String newAccessToken = jwtUtil.generateAccessToken(user);
                    return ResponseEntity.ok(Map.of("accessToken", newAccessToken));
                })
                .orElseThrow(() -> new RuntimeException("Refresh Token không hợp lệ dưới DB!"));
    }


    // Xóa cục token thô dưới DB khi người dùng bấm nút Logout
    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestBody RefreshTokenRequest request) {
        String requestRefreshToken = request.getToken();

        // Xóa cục token thô dưới DB
        if(requestRefreshToken != null && !requestRefreshToken.isEmpty()) {
            refreshTokenService.deleteByToken(requestRefreshToken);
        }

        return ResponseEntity.ok(com.stu.job_platform.dto.ApiResponse.success("Đăng xuất thành công!"));
    }
    
}