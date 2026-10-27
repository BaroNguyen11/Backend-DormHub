package com.dormhub.service;

import com.dormhub.dto.request.LoginRequest;
import com.dormhub.dto.request.RegisterRequest;
import com.dormhub.dto.request.ResendOtpRequest;
import com.dormhub.dto.request.VerifyOtpRequest;
import com.dormhub.dto.response.LoginResponse;
import com.dormhub.dto.response.LoginResult;
import com.dormhub.dto.response.RegisterResponse;
import com.dormhub.dto.response.VerifyOtpResponse;
import com.dormhub.entity.EmailVerification;
import com.dormhub.entity.Student;
import com.dormhub.entity.User;
import com.dormhub.entity.enums.OtpPurpose;
import com.dormhub.entity.enums.Role;
import com.dormhub.entity.enums.UserStatus;
import com.dormhub.exception.AppException;
import com.dormhub.exception.ErrorCode;
import com.dormhub.repository.EmailVerificationRepository;
import com.dormhub.repository.StudentRepository;
import com.dormhub.repository.UserRepository;

import com.dormhub.security.JwtService;
import lombok.RequiredArgsConstructor;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final PasswordEncoder passwordEncoder;
    private final OtpService otpService;
    private final EmailService emailService;
    private final EmailVerificationRepository emailVerificationRepository;
    private static final int MAX_OTP_ATTEMPTS = 5;
    private final JwtService jwtService;
    @Transactional
    public RegisterResponse register(RegisterRequest request) {

        String email = request.getEmail().trim().toLowerCase(Locale.ROOT);

        String username = request.getUsername().trim();

        if (userRepository.existsByEmail(email)) {
            throw new AppException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }

        if (userRepository.existsByUsername(username)) {
            throw new AppException(ErrorCode.USERNAME_ALREADY_EXISTS);
        }

        User user = new User();

        user.setUsername(username);
        user.setEmail(email);

        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));

        user.setFullName(request.getFullName().trim());

        String phone = request.getPhone();
        user.setPhone(phone == null || phone.isBlank() ? null : phone.trim());


        user.setRole(Role.STUDENT);
        user.setStatus(UserStatus.PENDING_VERIFICATION);

        User savedUser = userRepository.save(user);

        Student student = new Student();
        student.setUser(savedUser);
        studentRepository.save(student);
        // Tạo OTP và lưu hash vào database
        String otp = otpService.createOtp(
                savedUser,
                OtpPurpose.REGISTER
        );

        // Gửi OTP đến email đăng ký
        emailService.sendVerificationOtp(
                savedUser.getEmail(),
                otp
        );
        return new RegisterResponse(
                savedUser.getId(),
                savedUser.getUsername(),
                savedUser.getEmail(),
                savedUser.getFullName(),
                savedUser.getStatus()
        );
    }
    @Transactional(noRollbackFor = AppException.class)
    public VerifyOtpResponse verifyEmail(VerifyOtpRequest request) {

        // 1. Chuẩn hóa email
        String email = request.getEmail()
                .trim()
                .toLowerCase(Locale.ROOT);

        // 2. Tìm tài khoản
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(
                        ErrorCode.USER_NOT_FOUND,
                        "Không tìm thấy tài khoản với email này"
                ));

        // 3. Kiểm tra trạng thái tài khoản
        if (user.getStatus() == UserStatus.ACTIVE) {
            throw new AppException(
                    ErrorCode.INVALID_REQUEST,
                    "Email này đã được xác minh"
            );
        }

        if (user.getStatus() != UserStatus.PENDING_VERIFICATION) {
            throw new AppException(ErrorCode.ACCOUNT_NOT_ACTIVE);
        }

        // 4. Tìm OTP REGISTER mới nhất chưa sử dụng
        EmailVerification verification =
                emailVerificationRepository.findTopByUserIdAndPurposeAndUsedAtIsNullOrderByCreatedAtDesc(user.getId(), OtpPurpose.REGISTER)
                        .orElseThrow(() -> new AppException(
                                ErrorCode.OTP_INVALID,
                                "Không tìm thấy OTP hợp lệ"
                        ));

        int attempts = verification.getAttempts() == null
                ? 0
                : verification.getAttempts();

        // 5. Kiểm tra số lần nhập sai
        if (attempts >= MAX_OTP_ATTEMPTS) {
            throw new AppException(ErrorCode.OTP_MAX_ATTEMPTS);
        }

        LocalDateTime now = LocalDateTime.now();

        // 6. Kiểm tra thời hạn OTP
        if (verification.getExpiresAt() == null
                || !verification.getExpiresAt().isAfter(now)) {

            verification.setUsedAt(now);
            emailVerificationRepository.save(verification);

            throw new AppException(ErrorCode.OTP_EXPIRED);
        }

        // 7. So sánh OTP người dùng nhập với hash đã lưu
        boolean otpMatches = passwordEncoder.matches(
                request.getOtp(),
                verification.getOtpHash()
        );

        if (!otpMatches) {
            attempts++;

            verification.setAttempts(attempts);
            emailVerificationRepository.save(verification);

            if (attempts >= MAX_OTP_ATTEMPTS) {
                throw new AppException(ErrorCode.OTP_MAX_ATTEMPTS);
            }

            throw new AppException(
                    ErrorCode.OTP_INVALID,
                    "OTP không chính xác. Bạn còn "
                            + (MAX_OTP_ATTEMPTS - attempts)
                            + " lần thử"
            );
        }

        // 8. OTP hợp lệ: đánh dấu đã sử dụng
        verification.setUsedAt(now);
        emailVerificationRepository.save(verification);

        // 9. Kích hoạt tài khoản
        user.setStatus(UserStatus.ACTIVE);
        userRepository.save(user);

        // 10. Trả kết quả
        return new VerifyOtpResponse(
                user.getEmail(),
                user.getStatus()
        );
    }
    @Transactional
    public void resendVerificationOtp(ResendOtpRequest request) {

        // 1. Chuẩn hóa email
        String email = request.getEmail()
                .trim()
                .toLowerCase(Locale.ROOT);

        // 2. Tìm tài khoản
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(
                        ErrorCode.USER_NOT_FOUND
                ));

        // 3. Chỉ cho phép tài khoản đang chờ xác minh
        if (user.getStatus() == UserStatus.ACTIVE) {
            throw new AppException(
                    ErrorCode.INVALID_REQUEST,
                    "Tài khoản đã được xác minh email"
            );
        }

        if (user.getStatus() != UserStatus.PENDING_VERIFICATION) {
            throw new AppException(ErrorCode.ACCOUNT_NOT_ACTIVE);
        }

        // 4. Kiểm tra thời điểm tạo OTP gần nhất
        LocalDateTime now = LocalDateTime.now();

        EmailVerification latestVerification = emailVerificationRepository.findTopByUserIdAndPurposeOrderByCreatedAtDesc(user.getId(), OtpPurpose.REGISTER).orElse(null);

        if (latestVerification != null && latestVerification.getCreatedAt() != null && latestVerification.getCreatedAt().isAfter(now.minusSeconds(60))) {
            throw new AppException(ErrorCode.OTP_RESEND_TOO_SOON);
        }

        // 5. Tạo OTP mới và lưu hash
        String otp = otpService.createOtp(
                user,
                OtpPurpose.REGISTER
        );

        // 6. Gửi OTP mới qua email
        emailService.sendVerificationOtp(
                user.getEmail(),
                otp
        );
    }

    @Transactional(readOnly = true)
    public LoginResult login(LoginRequest request) {

        String email = request.getEmail()
                .trim()
                .toLowerCase(Locale.ROOT);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new AppException(ErrorCode.INVALID_CREDENTIALS));

        boolean passwordMatches = passwordEncoder.matches(
                request.getPassword(),
                user.getPasswordHash()
        );

        if (!passwordMatches) {
            throw new AppException(ErrorCode.INVALID_CREDENTIALS);
        }

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new AppException(ErrorCode.ACCOUNT_NOT_ACTIVE);
        }

        LoginResponse userResponse = new LoginResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFullName(),
                user.getRole(),
                user.getStatus()
        );

        String accessToken = jwtService.generateAccessToken(user);

        return new LoginResult(userResponse, accessToken);
    }

}