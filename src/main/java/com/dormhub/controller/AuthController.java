package com.dormhub.controller;

import com.dormhub.dto.request.*;
import com.dormhub.dto.response.*;
import com.dormhub.entity.User;
import com.dormhub.entity.enums.Role;
import com.dormhub.service.AuthService;
import com.dormhub.service.SepayPaymentSyncService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final SepayPaymentSyncService sepayPaymentSyncService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<RegisterResponse>> register(
            @Valid @RequestBody RegisterRequest request) {

        RegisterResponse response = authService.register(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Cấp tài khoản thành công. Sinh viên có thể yêu cầu OTP để kích hoạt", response));
    }
    @PostMapping("/verify-email")
    public ResponseEntity<ApiResponse<VerifyOtpResponse>> verifyEmail(
            @Valid @RequestBody VerifyOtpRequest request) {

        VerifyOtpResponse response =
                authService.verifyEmail(request);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Xác minh email thành công",
                        response
                )
        );
    }
    @PostMapping("/resend-otp")
    public ResponseEntity<ApiResponse<Void>> resendOtp(
            @Valid @RequestBody ResendOtpRequest request) {

        authService.resendVerificationOtp(request);

        return ResponseEntity.ok(
                ApiResponse.<Void>success(
                        "OTP mới đã được gửi đến email của bạn",
                        null
                )
        );
    }
    @Value("${dormhub.jwt.cookie-secure:true}")
    private boolean cookieSecure;

    @Value("${dormhub.jwt.access-token-minutes}")
    private long accessTokenMinutes;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest request) {

        LoginResult result = authService.login(request);

        log.info("Login successful for: {}", result.getUser().getEmail());

        // Đối soát bù giao dịch SePay ngay sau khi sinh viên đăng nhập (chạy nền,
        // không chặn login): nếu webhook bị lỡ, tiền đã chuyển vẫn được ghép vào
        // phiên thanh toán PENDING -> payments chuyển SUCCESS và sinh viên vào được phòng.
        if (result.getUser().getRole() == Role.STUDENT) {
            sepayPaymentSyncService.syncInBackground(result.getUser().getUserId());
        }

        ResponseCookie accessCookie = ResponseCookie
                .from("access_token", result.getAccessToken())
                .httpOnly(true)
                .sameSite("None")
                .secure(cookieSecure)
                .path("/")
                .maxAge(Duration.ofMinutes(accessTokenMinutes))
                .build();
        ResponseCookie refreshCookie = ResponseCookie
                .from("refresh_token", result.getRefreshToken())
                .httpOnly(true)
                .sameSite("None")
                .secure(cookieSecure)
                .path("/api/v1/auth")
                .maxAge(Duration.ofDays(7))
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, accessCookie.toString())
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .body(
                        ApiResponse.success(
                                "Đăng nhập thành công",
                                result.getUser()
                        )
                );
    }

    @GetMapping("/csrf")
    public ResponseEntity<ApiResponse<Map<String, String>>> getCsrfToken(CsrfToken csrfToken) {

        return ResponseEntity.ok(
                ApiResponse.success("Lấy CSRF token thành công", Map.of("token", csrfToken.getToken()))
        );
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<LoginResponse>> getCurrentUser(
            @AuthenticationPrincipal User user) {

        LoginResponse response = new LoginResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFullName(),
                user.getRole(),
                user.getStatus()
        );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Lấy thông tin tài khoản thành công",
                        response
                )
        );
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<?>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request) {

        authService.forgotPassword(request);

        return ResponseEntity.ok(
                ApiResponse.success("Nếu email tồn tại trong hệ thống, hướng dẫn đặt lại mật khẩu sẽ được gửi đến email của bạn.", null
                )
        );
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<?>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {

        authService.resetPassword(request);

        return ResponseEntity.ok(ApiResponse.success("Đặt lại mật khẩu thành công", null));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<?>> logout(
            @CookieValue(value = "refresh_token", required = false)
            String refreshToken
    ) {

        authService.logout(refreshToken);

        // Xóa access token cookie
        ResponseCookie clearedAccessCookie = ResponseCookie
                .from("access_token", "")
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite("None")
                .path("/")
                .maxAge(Duration.ZERO)
                .build();

        // Xóa refresh token cookie
        ResponseCookie clearedRefreshCookie = ResponseCookie
                .from("refresh_token", "")
                .httpOnly(true)
                .sameSite("None")
                .secure(cookieSecure)
                .path("/api/v1/auth")
                .maxAge(Duration.ZERO)
                .build();

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.SET_COOKIE,
                        clearedAccessCookie.toString(),
                        clearedRefreshCookie.toString()
                )
                .body(
                        ApiResponse.success(
                                "Đăng xuất thành công",
                                null
                        )
                );
    }
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<LoginResponse>> refresh(
            @CookieValue(
                    name = "refresh_token",
                    required = false
            ) String refreshToken
    ) {
        LoginResult result = authService.refresh(refreshToken);

        ResponseCookie accessCookie = ResponseCookie
                .from("access_token", result.getAccessToken())
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite("None")
                .path("/")
                .maxAge(Duration.ofMinutes(accessTokenMinutes))
                .build();

        ResponseCookie refreshCookie = ResponseCookie
                .from("refresh_token", result.getRefreshToken())
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite("None")
                .path("/api/v1/auth")
                .maxAge(Duration.ofDays(7))
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, accessCookie.toString())
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .body(
                        ApiResponse.success(
                                "Làm mới token thành công",
                                result.getUser()
                        )
                );
    }
    @PostMapping("/change-password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody ChangePasswordRequest request) {

        authService.changePassword(user.getId(), request);

        return ResponseEntity.ok(
                ApiResponse.success("Đổi mật khẩu thành công", null)
        );
    }
}
