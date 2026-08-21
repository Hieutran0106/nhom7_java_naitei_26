package com.nhom7.coworkingspace.controller.api;

import com.nhom7.coworkingspace.dto.request.ConfirmAccountRequest;
import com.nhom7.coworkingspace.dto.request.LoginRequest;
import com.nhom7.coworkingspace.dto.request.ResetPasswordRequest;
import com.nhom7.coworkingspace.dto.request.SendConfirmationRequest;
import com.nhom7.coworkingspace.dto.request.SignupRequest;
import com.nhom7.coworkingspace.dto.response.ApiResponse;
import com.nhom7.coworkingspace.dto.response.LoginResponse;
import com.nhom7.coworkingspace.dto.response.SignupResponse;
import com.nhom7.coworkingspace.service.AuthService;
import com.nhom7.coworkingspace.service.OtpService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Locale;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Endpoints for user authentication and registration")
public class AuthController {

    private final AuthService authService;
    private final MessageSource messageSource;
    private final OtpService otpService;

    @Operation(summary = "User Signup", description = "Register a new user account with personal info and CCCD image")
    @PostMapping(value = "/signup", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<SignupResponse>> signup(@ModelAttribute @Valid SignupRequest request) {
        SignupResponse response = authService.signup(request);
        Locale locale = LocaleContextHolder.getLocale();
        String message = messageSource.getMessage("user.created", null, locale);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(HttpStatus.CREATED.value(), message, response));
    }

    @Operation(summary = "User Login", description = "Authenticate with email and password to receive JWT access and refresh tokens")
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@RequestBody @Valid LoginRequest request) {
        LoginResponse response = authService.login(request);
        Locale locale = LocaleContextHolder.getLocale();
        String message = messageSource.getMessage("auth.login.success", null, locale);
        return ResponseEntity.ok(ApiResponse.success(response, message));
    }

    @Operation(
            summary = "User Logout",
            description = "Invalidate JWT token on server by adding it to blacklist. Header Authorization cần nhập accessToken nhận được sau khi đăng nhập thành công theo format: Bearer <accessToken>"
    )
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @Parameter(description = "Access token nhận được sau khi đăng nhập thành công. Định dạng: Bearer <accessToken>", example = "Bearer eyJhbGciOi...")
            @RequestHeader(value = org.springframework.http.HttpHeaders.AUTHORIZATION, required = false) String authHeader) {
        authService.logout(authHeader);
        Locale locale = LocaleContextHolder.getLocale();
        String message = messageSource.getMessage("auth.logout.success", null, locale);
        return ResponseEntity.ok(ApiResponse.success(null, message));
    }

    @Operation(summary = "Send account confirmation OTP")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "202",
                    description = "Confirmation request accepted",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    })
    @PostMapping("/send-confirm")
    public ResponseEntity<ApiResponse<Void>> sendConfirmation(
            @Valid @RequestBody SendConfirmationRequest request) {
        otpService.sendConfirmationOtp(request.email());
        String message = messageSource.getMessage(
                "auth.confirmation.sent", null, LocaleContextHolder.getLocale());
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(ApiResponse.success(HttpStatus.ACCEPTED.value(), message, null));
    }

    @Operation(summary = "Send password reset OTP")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "202",
                    description = "Password reset request accepted",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    })
    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(
            @Valid @RequestBody SendConfirmationRequest request) {
        otpService.sendPasswordResetOtp(request.email());
        String message = messageSource.getMessage(
                "auth.password.reset.sent", null, LocaleContextHolder.getLocale());
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(ApiResponse.success(HttpStatus.ACCEPTED.value(), message, null));
    }

    @Operation(
            summary = "Confirm account registration",
            description = "Verify 6-digit OTP code sent via email to activate user account from INACTIVE to ACTIVE"
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Account confirmed and activated successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Invalid or expired OTP code, or validation error",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    })
    @PostMapping("/confirm")
    public ResponseEntity<ApiResponse<Void>> confirmAccount(
            @Valid @RequestBody ConfirmAccountRequest request) {
        otpService.confirmAccount(request.email(), request.otp());
        String message = messageSource.getMessage(
                "auth.confirmation.success", null, LocaleContextHolder.getLocale());
        return ResponseEntity.ok(ApiResponse.success(null, message));
    }

    @Operation(
            summary = "Reset password with OTP",
            description = "Verify 6-digit OTP code and set new password for active user account"
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Password reset successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Invalid or expired OTP code, or validation error",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    })
    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {
        otpService.resetPassword(request.email(), request.otp(), request.newPassword());
        String message = messageSource.getMessage(
                "auth.password.reset.success", null, LocaleContextHolder.getLocale());
        return ResponseEntity.ok(ApiResponse.success(null, message));
    }
}
