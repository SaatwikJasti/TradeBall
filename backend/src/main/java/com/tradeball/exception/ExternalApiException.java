package com.tradeball.exception;

import org.springframework.http.HttpStatus;

public class ExternalApiException extends ApiException {
    public ExternalApiException(String message) {
        super(ApiErrorCode.EXTERNAL_API_ERROR, HttpStatus.BAD_GATEWAY, message);
    }

    public ExternalApiException(String message, Throwable cause) {
        super(ApiErrorCode.EXTERNAL_API_ERROR, HttpStatus.BAD_GATEWAY, message);
        initCause(cause);
    }
}
