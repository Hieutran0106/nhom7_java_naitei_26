package com.nhom7.coworkingspace.dto.request;

import com.nhom7.coworkingspace.util.ValidEmail;
import com.nhom7.coworkingspace.util.ValidPassword;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Schema(description = "Request body to reset password with OTP")
public record ResetPasswordRequest(
        @ValidEmail
        @Schema(description = "User registered email", example = "user@example.com")
        String email,

        @NotBlank(message = "{validation.otp.required}")
        @Pattern(regexp = "^\\d{6}$", message = "{validation.otp.invalid}")
        @Schema(description = "6-digit OTP code received via email", example = "123456")
        String otp,

        @ValidPassword
        @Schema(description = "New password (min 8 chars, 1 uppercase, 1 lowercase, 1 digit, 1 special char)", example = "Password123@")
        String newPassword
) {}
