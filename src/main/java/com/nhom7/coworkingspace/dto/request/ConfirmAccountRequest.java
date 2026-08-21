package com.nhom7.coworkingspace.dto.request;

import com.nhom7.coworkingspace.util.ValidEmail;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Schema(description = "Request body to confirm account with OTP")
public record ConfirmAccountRequest(
        @ValidEmail
        @Schema(description = "User registered email", example = "user@example.com")
        String email,

        @NotBlank(message = "{validation.otp.required}")
        @Pattern(regexp = "^\\d{6}$", message = "{validation.otp.invalid}")
        @Schema(description = "OTP code", example = "123456")
        String otp
) {}

