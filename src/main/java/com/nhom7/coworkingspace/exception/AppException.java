package com.nhom7.coworkingspace.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class AppException extends RuntimeException {

    private final HttpStatus status;
    private final String messageKey;

    public AppException(String messageKey, HttpStatus status) {
        super(messageKey);
        this.messageKey = messageKey;
        this.status = status;
    }

    public AppException(String messageKey) {
        this(messageKey, HttpStatus.BAD_REQUEST);
    }
}
