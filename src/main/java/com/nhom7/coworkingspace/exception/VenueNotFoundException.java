package com.nhom7.coworkingspace.exception;

import org.springframework.http.HttpStatus;

public class VenueNotFoundException extends AppException {

    public VenueNotFoundException() {
        super("venue.not.found", HttpStatus.NOT_FOUND);
    }
}
