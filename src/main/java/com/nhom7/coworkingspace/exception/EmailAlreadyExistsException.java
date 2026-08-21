package com.nhom7.coworkingspace.exception;

import org.springframework.http.HttpStatus;

public class EmailAlreadyExistsException extends AppException {

    public EmailAlreadyExistsException() {
        super("user.email.exists", HttpStatus.CONFLICT);
    }
}
