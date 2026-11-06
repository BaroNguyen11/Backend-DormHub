package com.dormhub.exception;

public class AiSearchUnavailableException extends RuntimeException {
    public AiSearchUnavailableException(Throwable cause) {
        super("Không thể kết nối dịch vụ tìm kiếm ngữ nghĩa", cause);
    }
}
