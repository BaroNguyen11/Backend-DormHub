package com.dormhub.controller;

import com.dormhub.dto.request.LoginRequest;
import com.dormhub.dto.request.RegisterRequest;
import com.dormhub.dto.request.ResendOtpRequest;
import com.dormhub.dto.request.VerifyOtpRequest;
import com.dormhub.dto.response.*;
import com.dormhub.service.AuthService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<RegisterResponse>> register(
            @Valid @RequestBody RegisterRequest request) {

        RegisterResponse response = authService.register(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Đăng ký thành công", response));
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
    @Value("${dormhub.jwt.cookie-secure:false}")
    private boolean cookieSecure;

    @Value("${dormhub.jwt.access-token-minutes}")
    private long accessTokenMinutes;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest request) {

        LoginResult result = authService.login(request);

        ResponseCookie cookie = ResponseCookie
                .from("access_token", result.getAccessToken())
                .httpOnly(true)
                .secure(cookieSecure)
                .path("/")
                .maxAge(Duration.ofMinutes(accessTokenMinutes))
                .sameSite("Lax")
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(ApiResponse.success(
                        "Đăng nhập thành công",
                        result.getUser()
                ));
    }


}