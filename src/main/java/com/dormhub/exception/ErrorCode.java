
package com.dormhub.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // Request và validation
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "Yêu cầu không hợp lệ"),
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "Dữ liệu đầu vào không hợp lệ"),

    // Authentication
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "Bạn cần đăng nhập để thực hiện thao tác này"),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "Bạn không có quyền thực hiện thao tác này"),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "Email hoặc mật khẩu không chính xác"),
    ACCOUNT_NOT_ACTIVE(HttpStatus.FORBIDDEN, "Tài khoản chưa được kích hoạt hoặc đã bị khóa"),
    REFRESH_TOKEN_INVALID(HttpStatus.UNAUTHORIZED, "Refresh token không hợp lệ"),
    OLD_PASSWORD_INCORRECT(HttpStatus.BAD_REQUEST, "Mật khẩu hiện tại không chính xác"),
    PASSWORD_CONFIRM_NOT_MATCH(HttpStatus.BAD_REQUEST, "Mật khẩu xác nhận không khớp"),
    NEW_PASSWORD_SAME_AS_OLD(HttpStatus.BAD_REQUEST, "Mật khẩu mới không được trùng với mật khẩu cũ"),

    // User
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "Không tìm thấy người dùng"),
    EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT, "Email đã được sử dụng"),
    USERNAME_ALREADY_EXISTS(HttpStatus.CONFLICT, "Username đã được sử dụng"),

    // Student
    STUDENT_NOT_FOUND(HttpStatus.NOT_FOUND, "Không tìm thấy hồ sơ sinh viên"),
    // OTP
    OTP_INVALID(HttpStatus.BAD_REQUEST, "Mã OTP không chính xác"),
    OTP_EXPIRED(HttpStatus.BAD_REQUEST, "Mã OTP đã hết hạn"),
    OTP_ALREADY_USED(HttpStatus.BAD_REQUEST, "Mã OTP đã được sử dụng"),
    OTP_MAX_ATTEMPTS(HttpStatus.TOO_MANY_REQUESTS, "Bạn đã nhập sai OTP quá số lần cho phép"),
    OTP_RESEND_TOO_SOON(HttpStatus.TOO_MANY_REQUESTS, "Vui lòng chờ ít nhất 60 giây trước khi yêu cầu OTP mới"),

    // Maintenance và Notification
    MAINTENANCE_NOT_FOUND(HttpStatus.NOT_FOUND, "Không tìm thấy yêu cầu sửa chữa"),
    NOTIFICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "Không tìm thấy thông báo"),
    ROOM_NOT_FOUND(HttpStatus.NOT_FOUND, "Không tìm thấy phòng"),
    MAINTENANCE_INVALID_STATUS(HttpStatus.BAD_REQUEST, "Yêu cầu không hợp lệ"),

    // Database
    DATA_CONFLICT(HttpStatus.CONFLICT, "Dữ liệu xung đột hoặc vi phạm ràng buộc"),

    // Lỗi không xác định
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Đã xảy ra lỗi hệ thống");

    private final HttpStatus status;
    private final String message;
}
