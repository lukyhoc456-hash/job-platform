package com.stu.job_platform.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Cấu hình Spring Security:
 * - Stateless (JWT, không dùng session)
 * - Phân quyền role-based cho từng nhóm API
 * - Tích hợp JWT Filter tự chế
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // Bật CORS tích hợp sẵn để nhận diện cấu hình toàn cục (CorsConfig)
            .cors(org.springframework.security.config.Customizer.withDefaults())
            // Tắt CSRF vì dùng JWT stateless
            .csrf(csrf -> csrf.disable())

            // Không tạo session, mỗi request phải tự mang theo JWT
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // Cấu hình phân quyền từng nhóm API
            .authorizeHttpRequests(auth -> auth
                // API công khai (không cần đăng nhập)
                .requestMatchers("/api/v1/auth/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/jobs/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/categories/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/industries/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/companies/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/profile/public/**").permitAll()
                .requestMatchers("/uploads/**").permitAll()
                .requestMatchers("/error").permitAll()

                // API chỉ dành cho ADMIN
                .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                // API chỉ dành cho RECRUITER
                .requestMatchers("/api/v1/payment/vnpay/ipn", "/api/v1/payment/vnpay/return").permitAll()

                // Tất cả API còn lại phải đăng nhập
                .anyRequest().authenticated()
            )

            // Gắn JWT Filter chạy trước filter mặc định của Spring Security
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public org.springframework.web.cors.CorsConfigurationSource corsConfigurationSource() {
        org.springframework.web.cors.CorsConfiguration configuration = new org.springframework.web.cors.CorsConfiguration();
        
        // Chỉ định đích danh duy nhất Domain Frontend của mày, cấm tuyệt đối dùng dấu "*"
        configuration.setAllowedOrigins(java.util.List.of("http://localhost:5173"));
        
        // Cho phép nhận diện đầy đủ các phương thức và mọi Header truyền lên từ Axios Frontend
        configuration.setAllowedMethods(java.util.List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(java.util.List.of("*"));
        configuration.setExposedHeaders(java.util.List.of("Authorization")); // Bộc lộ thêm header token nếu cần
        configuration.setAllowCredentials(true);
        
        org.springframework.web.cors.UrlBasedCorsConfigurationSource source = new org.springframework.web.cors.UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
