package com.tradeball.exception;

import org.springframework.http.HttpStatus;

public class ApiException extends RuntimeException {
    private final ApiErrorCode errorCode;
    private final HttpStatus status;

    public ApiException(ApiErrorCode errorCode, HttpStatus status, String message) {
        super(message);
        this.errorCode = errorCode;
        this.status = status;
    }

    public ApiErrorCode getErrorCode() { return errorCode; }
    public HttpStatus getStatus() { return status; }
}
