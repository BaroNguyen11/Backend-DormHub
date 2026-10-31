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
    STUDENT_PROFILE_INCOMPLETE(HttpStatus.BAD_REQUEST, "Hồ sơ sinh viên chưa đủ thông tin để đăng ký"),

    // Building
    BUILDING_NOT_FOUND(HttpStatus.NOT_FOUND, "Không tìm thấy tòa nhà"),
    BUILDING_HAS_ROOMS(HttpStatus.CONFLICT, "Không thể xóa tòa nhà đang có phòng"),
    BUILDING_GENDER_POLICY_IN_USE(HttpStatus.CONFLICT, "Không thể đổi chính sách giới tính khi tòa đang có đơn hoặc phân phòng"),

    // Room
    ROOM_ALREADY_EXISTS(HttpStatus.CONFLICT, "Số phòng đã tồn tại trong tòa nhà này"),
    ROOM_CAPACITY_BELOW_OCCUPANCY(HttpStatus.CONFLICT, "Sức chứa mới thấp hơn số sinh viên đang được phân phòng"),
    ROOM_GENDER_POLICY_REQUIRED(HttpStatus.BAD_REQUEST, "Cần thiết lập chính sách giới tính cho tòa nhà"),
    ROOM_GENDER_POLICY_MISMATCH(HttpStatus.BAD_REQUEST, "Phòng không phù hợp với chính sách giới tính của tòa"),
    ROOM_FULL(HttpStatus.CONFLICT, "Phòng đã đủ sức chứa"),
    ROOM_HAS_ACTIVE_ASSIGNMENTS(HttpStatus.CONFLICT, "Không thể thay đổi phòng đang có sinh viên được phân phòng"),
    ROOM_HAS_ACTIVE_APPLICATIONS(HttpStatus.CONFLICT, "Không thể thay đổi phòng đang được chọn trong đơn đăng ký"),
    ROOM_HAS_HISTORY(HttpStatus.CONFLICT, "Không thể xóa phòng đã có lịch sử đơn hoặc phân phòng"),
    ROOM_NOT_AVAILABLE(HttpStatus.CONFLICT, "Phòng không ở trạng thái có thể đăng ký"),

    // Dorm application
    DORM_APPLICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "Không tìm thấy đơn đăng ký"),
    DORM_APPLICATION_ALREADY_ACTIVE(HttpStatus.CONFLICT, "Sinh viên đã có đơn đăng ký đang được xử lý"),
    DORM_APPLICATION_NOT_PENDING(HttpStatus.CONFLICT, "Chỉ có thể thực hiện thao tác với đơn đang chờ duyệt"),
    DORM_APPLICATION_INVALID_REVIEW_STATUS(HttpStatus.BAD_REQUEST, "Trạng thái xử lý chỉ được là APPROVED hoặc REJECTED"),
    DORM_APPLICATION_REJECTION_NOTE_REQUIRED(HttpStatus.BAD_REQUEST, "Cần ghi rõ lý do khi từ chối đơn đăng ký"),
    DORM_APPLICATION_ROOM_REQUIRED(HttpStatus.BAD_REQUEST, "Cần chọn phòng trước khi nộp đơn"),
    DORM_APPLICATION_GENDER_UNSUPPORTED(HttpStatus.BAD_REQUEST, "Giới tính hiện chưa có chính sách tòa phù hợp để chọn phòng"),

    // Room assignment
    DORM_APPLICATION_NOT_APPROVED(HttpStatus.CONFLICT, "Chỉ có thể phân phòng cho đơn đã được duyệt"),
    STUDENT_ALREADY_ASSIGNED(HttpStatus.CONFLICT, "Sinh viên đã được phân phòng"),
    DORM_APPLICATION_ALREADY_ASSIGNED(HttpStatus.CONFLICT, "Đơn đăng ký đã có phân phòng đang hiệu lực"),
    ROOM_ASSIGNMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "Không tìm thấy phân phòng"),

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

    // Billing
    BILLING_NOT_FOUND(HttpStatus.NOT_FOUND, "Không tìm thấy dữ liệu hóa đơn"),
    BILLING_CONFLICT(HttpStatus.CONFLICT, "Dữ liệu Billing xung đột"),
    DEPOSIT_NOT_SUPPORTED(HttpStatus.NOT_IMPLEMENTED, "Chưa hỗ trợ tạo hóa đơn DEPOSIT"),

    // Lỗi không xác định
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Đã xảy ra lỗi hệ thống");

    private final HttpStatus status;
    private final String message;
}
