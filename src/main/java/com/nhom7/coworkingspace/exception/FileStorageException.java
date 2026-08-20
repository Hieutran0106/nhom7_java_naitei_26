package com.nhom7.coworkingspace.exception;

import org.springframework.http.HttpStatus;

public class FileStorageException extends AppException {

    public FileStorageException(String messageKey, Throwable cause) {
        super(messageKey, HttpStatus.INTERNAL_SERVER_ERROR);
        initCause(cause);
    }

    public FileStorageException(String messageKey) {
        super(messageKey, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
