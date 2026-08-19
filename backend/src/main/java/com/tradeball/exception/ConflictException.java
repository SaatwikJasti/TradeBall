package com.tradeball.exception;

import org.springframework.http.HttpStatus;

public class ConflictException extends ApiException {
    public ConflictException(String message) {
        super(ApiErrorCode.CONFLICT, HttpStatus.CONFLICT, message);
    }
}
