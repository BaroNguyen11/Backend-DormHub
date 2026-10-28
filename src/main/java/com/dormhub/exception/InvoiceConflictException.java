package com.dormhub.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class InvoiceConflictException extends RuntimeException {

    public InvoiceConflictException(String message) {
        super(message);
    }
}
