package com.nhom7.coworkingspace.controller.api;

import com.nhom7.coworkingspace.dto.request.LoginRequest;
import com.nhom7.coworkingspace.dto.request.SignupRequest;
import com.nhom7.coworkingspace.dto.response.ApiResponse;
import com.nhom7.coworkingspace.dto.response.LoginResponse;
import com.nhom7.coworkingspace.dto.response.SignupResponse;
import com.nhom7.coworkingspace.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
}
