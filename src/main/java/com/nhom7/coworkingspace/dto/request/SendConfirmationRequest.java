package com.nhom7.coworkingspace.dto.request;

import com.nhom7.coworkingspace.util.ValidEmail;

public record SendConfirmationRequest(
        @ValidEmail
        String email) {
}
