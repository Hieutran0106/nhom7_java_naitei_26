package com.nhom7.coworkingspace.exception;

import org.springframework.http.HttpStatus;

public class BookingNotFoundException extends AppException {

    public BookingNotFoundException(Long bookingId) {
        super("booking.not.found", HttpStatus.NOT_FOUND);
    }
}
