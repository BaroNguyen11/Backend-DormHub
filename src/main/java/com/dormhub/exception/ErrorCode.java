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
    STUDENT_PROFILE_INCOMPLETE(HttpStatus.BAD_REQUEST,
            "Hồ sơ sinh viên cần đủ mã sinh viên, ngày sinh, giới tính và địa chỉ"),
    // Housing
    BUILDING_GENDER_POLICY_CONFIRMATION_REQUIRED(HttpStatus.CONFLICT,
            "Có sinh viên thuộc giới tính khác đang ở tòa nhà; xác nhận để đổi chính sách"),
    ROOM_CAPACITY_BELOW_OCCUPANCY(HttpStatus.CONFLICT,
            "Sức chứa mới thấp hơn số chỗ đang sử dụng hoặc được giữ"),
    ROOM_GENDER_POLICY_REQUIRED(HttpStatus.BAD_REQUEST, "Cần thiết lập giới tính cho tòa nhà"),
    ROOM_GENDER_POLICY_MISMATCH(HttpStatus.BAD_REQUEST, "Phòng không phù hợp với giới tính của sinh viên"),
    ROOM_FULL(HttpStatus.CONFLICT, "Phòng đã đủ sức chứa"),
    ROOM_NOT_AVAILABLE(HttpStatus.CONFLICT, "Phòng không ở trạng thái có thể đăng ký"),
    ROOM_HAS_ACTIVE_ASSIGNMENTS(HttpStatus.CONFLICT, "Không thể thay đổi phòng đang có sinh viên được phân phòng"),
    ROOM_HAS_ACTIVE_APPLICATIONS(HttpStatus.CONFLICT, "Không thể thay đổi phòng đang được chọn trong đơn đăng ký"),
    ROOM_HAS_ACTIVE_CONTRACTS(HttpStatus.CONFLICT, "Không thể bảo trì hoặc ngừng phòng đang được giữ chỗ"),
    ROOM_HAS_HISTORY(HttpStatus.CONFLICT, "Không thể xóa phòng đã có lịch sử đơn hoặc phân phòng"),
    CONTRACT_ALREADY_EXISTS(HttpStatus.CONFLICT, "Đơn đăng ký đã có hợp đồng"),
    CONTRACT_NOT_ACTIVE(HttpStatus.CONFLICT, "Chỉ hợp đồng đang hiệu lực mới được phân phòng"),
    CONTRACT_DEPOSIT_NOT_PAID(HttpStatus.CONFLICT, "Hóa đơn cọc chưa được thanh toán"),
    CONTRACT_RENT_INVALID(HttpStatus.BAD_REQUEST, "Giá phòng chưa hợp lệ để tạo hợp đồng"),
    DORM_APPLICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "Không tìm thấy đơn đăng ký"),
    DORM_APPLICATION_ALREADY_ACTIVE(HttpStatus.CONFLICT, "Sinh viên đã có đơn đăng ký đang được xử lý"),
    DORM_APPLICATION_NOT_PENDING(HttpStatus.CONFLICT, "Chỉ có thể thao tác với đơn đang chờ duyệt"),
    DORM_APPLICATION_NOT_APPROVED(HttpStatus.CONFLICT, "Chỉ có thể phân phòng cho đơn đã được duyệt"),
    DORM_APPLICATION_INVALID_REVIEW_STATUS(HttpStatus.BAD_REQUEST, "Trạng thái xử lý chỉ được là APPROVED hoặc REJECTED"),
    DORM_APPLICATION_REJECTION_NOTE_REQUIRED(HttpStatus.BAD_REQUEST, "Cần ghi rõ lý do khi từ chối đơn đăng ký"),
    DORM_APPLICATION_ROOM_REQUIRED(HttpStatus.BAD_REQUEST, "Cần chọn phòng trước khi nộp đơn"),
    DORM_APPLICATION_GENDER_UNSUPPORTED(HttpStatus.BAD_REQUEST, "Chỉ sinh viên có giới tính MALE hoặc FEMALE mới được đăng ký nội trú"),
    STUDENT_ALREADY_ASSIGNED(HttpStatus.CONFLICT, "Sinh viên đã được phân phòng"),
    DORM_APPLICATION_ALREADY_ASSIGNED(HttpStatus.CONFLICT, "Đơn đăng ký đã có phân phòng đang hiệu lực"),
    ROOM_ASSIGNMENT_APPROVER_REQUIRED(HttpStatus.CONFLICT, "Không xác định được quản lý đã duyệt đơn để ghi nhận người phân phòng"),
    // OTP
    OTP_INVALID(HttpStatus.BAD_REQUEST, "Mã OTP không chính xác"),
    OTP_EXPIRED(HttpStatus.BAD_REQUEST, "Mã OTP đã hết hạn"),
    OTP_ALREADY_USED(HttpStatus.BAD_REQUEST, "Mã OTP đã được sử dụng"),
    OTP_MAX_ATTEMPTS(HttpStatus.TOO_MANY_REQUESTS, "Bạn đã nhập sai OTP quá số lần cho phép"),
    OTP_RESEND_TOO_SOON(HttpStatus.TOO_MANY_REQUESTS, "Vui lòng chờ ít nhất 60 giây trước khi yêu cầu OTP mới"),

    // Building errors
    BUILDING_NOT_FOUND(HttpStatus.NOT_FOUND, "Không tìm thấy tòa nhà"),
    BUILDING_CODE_EXISTS(HttpStatus.CONFLICT, "Mã tòa nhà đã tồn tại"),
    BUILDING_HAS_ROOMS(HttpStatus.CONFLICT, "Tòa nhà còn phòng, không thể xóa"),
    
    // Room errors
    ROOM_NOT_FOUND(HttpStatus.NOT_FOUND, "Không tìm thấy phòng"),
    ROOM_ALREADY_EXISTS(HttpStatus.CONFLICT, "Phòng đã tồn tại trong tòa nhà này"),
    ROOM_OCCUPIED(HttpStatus.CONFLICT, "Phòng đang có người ở"),
    
    // Maintenance và Notification
    MAINTENANCE_NOT_FOUND(HttpStatus.NOT_FOUND, "Không tìm thấy yêu cầu sửa chữa"),
    NOTIFICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "Không tìm thấy thông báo"),
    ROOM_ASSIGNMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "Không tìm thấy phân phòng hiện tại của hợp đồng"),
    ROOM_ASSIGNMENT_INVALID_STATUS(HttpStatus.CONFLICT, "Trạng thái phân phòng không cho phép thao tác này"),
    CONTRACT_HAS_UNPAID_INVOICES(HttpStatus.CONFLICT, "Hợp đồng còn hóa đơn chưa thanh toán"),
    MAINTENANCE_INVALID_STATUS(HttpStatus.BAD_REQUEST, "Yêu cầu không hợp lệ"),
    REJECTION_REASON_REQUIRED(HttpStatus.BAD_REQUEST, "Vui lòng nhập lý do khi từ chối yêu cầu bảo trì"),

    // Database
    DATA_CONFLICT(HttpStatus.CONFLICT, "Dữ liệu xung đột hoặc vi phạm ràng buộc"),

    // Billing
    BILLING_NOT_FOUND(HttpStatus.NOT_FOUND, "Không tìm thấy dữ liệu hóa đơn"),
    BILLING_CONFLICT(HttpStatus.CONFLICT, "Dữ liệu Billing xung đột"),
    DEPOSIT_NOT_SUPPORTED(HttpStatus.NOT_IMPLEMENTED, "Chưa hỗ trợ tạo hóa đơn DEPOSIT"),
    DEPOSIT_INVALID(HttpStatus.BAD_REQUEST, "Thông tin đặt cọc không hợp lệ"),
    DEPOSIT_CONFLICT(HttpStatus.CONFLICT, "Trạng thái đặt cọc không cho phép thao tác này"),

    // Contract
    CONTRACT_NOT_FOUND(HttpStatus.NOT_FOUND, "Không tìm thấy hợp đồng"),
    CONTRACT_CONFLICT(HttpStatus.CONFLICT, "Trạng thái hợp đồng không cho phép thao tác này"),
    CONTRACT_INVALID(HttpStatus.BAD_REQUEST, "Thông tin hợp đồng không hợp lệ"),

    // Room transfer
    TRANSFER_REQUEST_NOT_FOUND(HttpStatus.NOT_FOUND, "Không tìm thấy yêu cầu chuyển phòng"),
    TRANSFER_ALREADY_PENDING(HttpStatus.CONFLICT, "Sinh viên đã có yêu cầu chuyển phòng đang chờ xử lý"),
    TRANSFER_INVALID_STATUS(HttpStatus.CONFLICT, "Trạng thái không cho phép xử lý yêu cầu chuyển phòng"),
    TRANSFER_ROOM_REQUIRED(HttpStatus.BAD_REQUEST, "Cần chọn phòng đích để duyệt yêu cầu"),
    TRANSFER_ROOM_NOT_AVAILABLE(HttpStatus.CONFLICT, "Phòng đích không còn chỗ hoặc không nhận sinh viên"),
    TRANSFER_SAME_ROOM(HttpStatus.BAD_REQUEST, "Phòng đích phải khác phòng hiện tại"),
    TRANSFER_PRICE_MISMATCH(HttpStatus.CONFLICT, "Chỉ được chuyển sang phòng cùng mức giá"),

    // Checkout
    CHECKOUT_REQUEST_NOT_FOUND(HttpStatus.NOT_FOUND, "Không tìm thấy yêu cầu trả phòng"),
    CHECKOUT_REQUEST_ALREADY_OPEN(HttpStatus.CONFLICT, "Hợp đồng đã có yêu cầu trả phòng đang xử lý"),
    CHECKOUT_REQUEST_INVALID_STATUS(HttpStatus.CONFLICT, "Trạng thái yêu cầu trả phòng không cho phép thao tác này"),
    CHECKOUT_REJECTION_NOTE_REQUIRED(HttpStatus.BAD_REQUEST, "Cần ghi rõ lý do khi từ chối yêu cầu trả phòng"),
    CHECKOUT_DEPOSIT_ACTION_REQUIRED(HttpStatus.BAD_REQUEST, "Cần chọn hoàn hoặc tịch thu tiền cọc trước khi xác nhận trả phòng"),
    CHECKOUT_DEPOSIT_ACTION_INVALID(HttpStatus.BAD_REQUEST, "Tiền cọc chỉ được hoàn hoặc tịch thu khi xác nhận trả phòng"),
    CHECKOUT_DEPOSIT_NOT_HELD(HttpStatus.CONFLICT, "Hợp đồng không có tiền cọc đang được giữ để xử lý"),

    // Lỗi không xác định
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Đã xảy ra lỗi hệ thống");

    private final HttpStatus status;
    private final String message;
}
