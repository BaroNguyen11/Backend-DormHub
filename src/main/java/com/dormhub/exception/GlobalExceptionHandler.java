package com.dormhub.exception;

import com.dormhub.dto.response.ApiResponse;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.web.bind.MissingServletRequestParameterException;

import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // 1. Lỗi nghiệp vụ do ứng dụng chủ động ném ra
    @ExceptionHandler(AppException.class)
    public ResponseEntity<ApiResponse<Void>> handleAppException(AppException ex) {
        ErrorCode errorCode = ex.getErrorCode();

        return ResponseEntity.status(errorCode.getStatus()).body(
                        ApiResponse.<Void>error(
                                errorCode.name(),
                                ex.getMessage()
                        )
                );
    }

    // 2. Lỗi validation của DTO, ví dụ @NotBlank, @Email
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new LinkedHashMap<>();

        ex.getBindingResult()
                .getFieldErrors()
                .forEach(error ->
                        errors.putIfAbsent(
                                error.getField(),
                                error.getDefaultMessage()
                        )
                );

        String firstError = errors.values().stream()
                .filter(message -> message != null && !message.isBlank())
                .findFirst()
                .orElse(ErrorCode.VALIDATION_ERROR.getMessage());

        return ResponseEntity.badRequest().body(
                        ApiResponse.<Map<String, String>>error(
                                "VALIDATION_ERROR",
                                firstError,
                                errors
                        )
                );
    }

    // 3. JSON request sai định dạng hoặc không đọc được
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidJson(HttpMessageNotReadableException ex) {
        return ResponseEntity
                .badRequest()
                .body(
                        ApiResponse.<Void>error(
                                "INVALID_REQUEST",
                                "Dữ liệu JSON không hợp lệ"
                        )
                );
    }

    // 4. Vi phạm ràng buộc database
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataIntegrity(DataIntegrityViolationException ex) {
        log.warn("Database integrity constraint violation");

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(
                        ApiResponse.<Void>error(
                                "DATA_CONFLICT",
                                "Dữ liệu xung đột hoặc vi phạm ràng buộc"
                        )
                );
    }

    // 5. Các lỗi không dự kiến
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpectedException(Exception ex) {
        log.error("Unexpected application error", ex);

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(
                        ApiResponse.<Void>error(
                                "INTERNAL_SERVER_ERROR",
                                "Đã xảy ra lỗi hệ thống"
                        )
                );
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<?>> handleNoResourceFound(
            NoResourceFoundException ex) {

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(
                        "NOT_FOUND",
                        "Không tìm thấy endpoint"
                ));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        return ResponseEntity.badRequest().body(ApiResponse.error("INVALID_REQUEST", "Tham số không hợp lệ"));
    }

    // Vi phạm ràng buộc @Validated trên tham số (@Positive...) → 400 thay vì 500
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolation(ConstraintViolationException ex) {
        String message = ex.getConstraintViolations().stream()
                .map(violation -> violation.getMessage())
                .findFirst()
                .orElse("Tham số không hợp lệ");
        return ResponseEntity.badRequest().body(ApiResponse.error("INVALID_REQUEST", message));
    }

    // Thiếu tham số query bắt buộc → 400 thay vì 500
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingParameter(MissingServletRequestParameterException ex) {
        return ResponseEntity.badRequest().body(ApiResponse.error("INVALID_REQUEST",
                "Thiếu tham số bắt buộc: " + ex.getParameterName()));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(ApiResponse.error("METHOD_NOT_ALLOWED", "HTTP method không được hỗ trợ"));
    }
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<?> handleMaxUploadSizeExceeded(MaxUploadSizeExceededException exc) {
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(Map.of("message", "Kích thước tệp tải lên quá lớn (tối đa 10 MB)"));
    }
}
