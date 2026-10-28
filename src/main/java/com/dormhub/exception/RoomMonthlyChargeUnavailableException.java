package com.dormhub.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class RoomMonthlyChargeUnavailableException extends RuntimeException {

    public RoomMonthlyChargeUnavailableException(String message) {
        super(message);
    }
}
