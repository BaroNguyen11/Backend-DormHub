package com.dormhub.service;

import com.dormhub.dto.request.*;
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
    private final RefreshTokenService refreshTokenService;
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

    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {

        // 1. Chuẩn hóa email
        String email = request.getEmail().trim().toLowerCase(Locale.ROOT);

        // 2. Tìm tài khoản
        User user = userRepository.findByEmail(email).orElse(null);

        // 3. Không xử lý nếu tài khoản không tồn tại
        // hoặc chưa ở trạng thái ACTIVE
        if (user == null || user.getStatus() != UserStatus.ACTIVE) {
            return;
        }

        // 4. Giới hạn gửi OTP: tối đa một lần mỗi 60 giây
        LocalDateTime now = LocalDateTime.now();

        EmailVerification latestVerification = emailVerificationRepository.findTopByUserIdAndPurposeOrderByCreatedAtDesc(
                                user.getId(),
                                OtpPurpose.RESET_PASSWORD
                        ).orElse(null);

        if (latestVerification != null
                && latestVerification.getCreatedAt() != null
                && latestVerification.getCreatedAt()
                .isAfter(now.minusSeconds(60))) {
            return;
        }

        // 5. Tạo OTP và lưu hash vào database
        String otp = otpService.createOtp(user, OtpPurpose.RESET_PASSWORD);

        // 6. Gửi OTP đặt lại mật khẩu
        emailService.sendPasswordResetOtp(user.getEmail(), otp);
    }

    @Transactional(noRollbackFor = AppException.class)
    public void resetPassword(ResetPasswordRequest request) {
        String email = request.getEmail().trim().toLowerCase(Locale.ROOT);

        // 1. Tìm tài khoản
        User user = userRepository.findByEmail(email).orElseThrow(() -> new AppException(ErrorCode.OTP_INVALID));

        // 2. Chỉ tài khoản đang hoạt động mới được đổi mật khẩu
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new AppException(ErrorCode.ACCOUNT_NOT_ACTIVE);
        }

        // 3. Lấy OTP mới nhất của mục đích RESET_PASSWORD
        EmailVerification verification = emailVerificationRepository.findTopByUserIdAndPurposeOrderByCreatedAtDesc(user.getId(), OtpPurpose.RESET_PASSWORD)
                        .orElseThrow(() -> new AppException(ErrorCode.OTP_INVALID));

        // 4. Kiểm tra OTP đã được sử dụng chưa
        if (verification.getUsedAt() != null) {
            throw new AppException(ErrorCode.OTP_ALREADY_USED);
        }

        // 5. Kiểm tra số lần nhập sai
        int attempts = verification.getAttempts() == null ? 0 : verification.getAttempts();
        if (attempts >= MAX_OTP_ATTEMPTS) {
            throw new AppException(ErrorCode.OTP_MAX_ATTEMPTS);
        }

        // 6. Kiểm tra thời hạn OTP
        LocalDateTime now = LocalDateTime.now();

        if (!verification.getExpiresAt().isAfter(now)) {
            throw new AppException(ErrorCode.OTP_EXPIRED);
        }

        // 7. So sánh OTP người dùng nhập với OTP đã được hash
        boolean otpMatches = passwordEncoder.matches(request.getOtp(), verification.getOtpHash());

        if (!otpMatches) {
            verification.setAttempts(attempts + 1);
            emailVerificationRepository.save(verification);

            throw new AppException(ErrorCode.OTP_INVALID);
        }

        // 8. Đánh dấu OTP đã sử dụng
        verification.setUsedAt(now);
        emailVerificationRepository.save(verification);

        // 9. Hash mật khẩu mới rồi cập nhật tài khoản
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    @Transactional()
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

        String refreshToken = refreshTokenService.createRefreshToken(user);

        return new LoginResult(
                userResponse,
                accessToken,
                refreshToken
        );
    }
    @Transactional(readOnly = false)
    public LoginResult refresh(String rawRefreshToken) {
        // Kiểm tra, thu hồi token cũ và tạo refresh token mới
        RefreshTokenService.RotationResult rotation = refreshTokenService.rotateRefreshToken(rawRefreshToken);

        User user = rotation.user();

        // Tạo access token mới
        String accessToken = jwtService.generateAccessToken(user);

        // Tạo lại thông tin người dùng theo cách method login() đang làm
        LoginResponse userResponse = new LoginResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFullName(),
                user.getRole(),
                user.getStatus()
        );

        return new LoginResult(
                userResponse,
                accessToken,
                rotation.refreshToken()
        );
    }
    @Transactional
    public void logout(String rawRefreshToken) {
        refreshTokenService.revokeRefreshToken(rawRefreshToken);
    }
    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        // 1. Kiểm tra xác nhận mật khẩu mới khớp nhau
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new AppException(ErrorCode.PASSWORD_CONFIRM_NOT_MATCH);
        }

        // 2. Tìm người dùng
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new AppException(ErrorCode.ACCOUNT_NOT_ACTIVE);
        }

        // 3. Kiểm tra mật khẩu cũ đúng không
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPasswordHash())) {
            throw new AppException(ErrorCode.OLD_PASSWORD_INCORRECT);
        }

        // 4. Mật khẩu mới không được trùng mật khẩu cũ
        if (passwordEncoder.matches(request.getNewPassword(), user.getPasswordHash())) {
            throw new AppException(ErrorCode.NEW_PASSWORD_SAME_AS_OLD);
        }

        // 5. Cập nhật mật khẩu mới
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }
}