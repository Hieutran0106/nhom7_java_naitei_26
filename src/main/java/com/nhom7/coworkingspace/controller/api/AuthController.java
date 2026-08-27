package com.nhom7.coworkingspace.controller.api;

import com.nhom7.coworkingspace.dto.request.ConfirmAccountRequest;
import com.nhom7.coworkingspace.dto.request.LoginRequest;
import com.nhom7.coworkingspace.dto.request.ResetPasswordRequest;
import com.nhom7.coworkingspace.dto.request.SendConfirmationRequest;
import com.nhom7.coworkingspace.dto.request.SignupRequest;
import com.nhom7.coworkingspace.dto.response.ApiResponse;
import com.nhom7.coworkingspace.dto.response.LoginResponse;
import com.nhom7.coworkingspace.dto.response.SignupResponse;
import com.nhom7.coworkingspace.security.JwtAuthenticationFilter;
import com.nhom7.coworkingspace.service.AuthService;
import com.nhom7.coworkingspace.service.OtpService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Locale;
import java.util.Set;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(
        name = "Authentication",
        description = "Endpoints for user authentication and registration"
)
public class AuthController {

    private static final String ROLE_ADMIN = "ADMIN";
    private static final String ROLE_MODERATOR = "MODERATOR";

    private final AuthService authService;
    private final MessageSource messageSource;
    private final OtpService otpService;


    /**
     * =========================================================
     * SIGNUP
     * =========================================================
     *
     * Public signup.
     *
     * Role của user mới vẫn được quyết định bởi AuthService.
     * Controller không tự cấp ADMIN/MODERATOR.
     */
    @Operation(
            summary = "User Signup",
            description = "Register a new user account with personal info and CCCD image"
    )
    @PostMapping(
            value = "/signup",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<ApiResponse<SignupResponse>> signup(
            @ModelAttribute
            @Valid
            SignupRequest request
    ) {

        SignupResponse response =
                authService.signup(request);

        Locale locale =
                LocaleContextHolder.getLocale();

        String message =
                messageSource.getMessage(
                        "user.created",
                        null,
                        locale
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        ApiResponse.success(
                                HttpStatus.CREATED.value(),
                                message,
                                response
                        )
                );
    }


    /**
     * =========================================================
     * LOGIN
     * =========================================================
     *
     * Đây là login chung của toàn hệ thống.
     *
     * USER / HOST / MODERATOR / ADMIN đều login qua API này.
     *
     * Sau khi login:
     *
     * 1. accessToken + refreshToken vẫn được trả trong JSON.
     *
     * 2. Frontend lưu accessToken trong sessionStorage để gọi
     *    REST API bằng:
     *
     *    Authorization: Bearer <accessToken>
     *
     * 3. Chỉ ADMIN / MODERATOR được tạo thêm HttpOnly cookie
     *    moderator_access_token.
     *
     *    Cookie này chỉ phục vụ browser navigation tới:
     *
     *    /moderator/**
     *    /admin/**
     *
     * 4. USER / HOST vẫn login bình thường, nhưng không được
     *    cấp management cookie.
     */
    @Operation(
            summary = "User Login",
            description = "Authenticate with email and password to receive JWT access and refresh tokens"
    )
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @RequestBody
            @Valid
            LoginRequest request,

            HttpServletRequest httpRequest,

            HttpServletResponse httpResponse
    ) {

        /*
         * Authenticate user bằng logic hiện tại trong service.
         */
        LoginResponse response =
                authService.login(request);


        /*
         * Quan trọng:
         *
         * Xóa management cookie cũ trước.
         *
         * Ví dụ:
         *
         * 1. Browser login ADMIN
         * 2. Có moderator_access_token
         * 3. Sau đó login USER
         *
         * Nếu không xóa cookie cũ, browser vẫn có thể tiếp tục
         * gửi JWT của ADMIN cho /moderator/**.
         */
        clearManagementCookie(
                httpRequest,
                httpResponse
        );


        /*
         * Chỉ ADMIN/MODERATOR được dùng Management Web UI.
         */
        if (
                hasManagementRole(
                        response.getRoles()
                )
        ) {

            addManagementCookie(
                    response.getAccessToken(),
                    httpRequest,
                    httpResponse
            );
        }


        Locale locale =
                LocaleContextHolder.getLocale();

        String message =
                messageSource.getMessage(
                        "auth.login.success",
                        null,
                        locale
                );


        /*
         * LoginResponse vẫn được trả nguyên vẹn.
         *
         * Frontend sử dụng:
         *
         * response.data.accessToken
         * response.data.refreshToken
         * response.data.roles
         * response.data.id
         * response.data.name
         * response.data.email
         */
        return ResponseEntity.ok(
                ApiResponse.success(
                        response,
                        message
                )
        );
    }


    /**
     * =========================================================
     * LOGOUT
     * =========================================================
     *
     * 1. Blacklist JWT như logic cũ.
     * 2. Xóa management cookie.
     */
    @Operation(
            summary = "User Logout",
            description = "Invalidate JWT token on server by adding it to blacklist"
    )
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(

            /*
             * Không hiển thị Authorization thành parameter riêng
             * trên Swagger.
             *
             * Token được lấy từ global Authorize button.
             */
            @Parameter(hidden = true)
            @RequestHeader(
                    value = HttpHeaders.AUTHORIZATION,
                    required = false
            )
            String authHeader,

            HttpServletRequest httpRequest,

            HttpServletResponse httpResponse
    ) {

        /*
         * Blacklist token.
         */
        authService.logout(
                authHeader
        );


        /*
         * Xóa cookie Web Management.
         */
        clearManagementCookie(
                httpRequest,
                httpResponse
        );


        Locale locale =
                LocaleContextHolder.getLocale();

        String message =
                messageSource.getMessage(
                        "auth.logout.success",
                        null,
                        locale
                );


        return ResponseEntity.ok(
                ApiResponse.success(
                        null,
                        message
                )
        );
    }


    /**
     * =========================================================
     * SEND ACCOUNT CONFIRMATION OTP
     * =========================================================
     */
    @Operation(
            summary = "Send account confirmation OTP"
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "202",
                    description = "Confirmation request accepted",
                    content = @Content(
                            schema = @Schema(
                                    implementation = ApiResponse.class
                            )
                    )
            )
    })
    @PostMapping("/send-confirm")
    public ResponseEntity<ApiResponse<Void>> sendConfirmation(
            @Valid
            @RequestBody
            SendConfirmationRequest request
    ) {

        otpService.sendConfirmationOtp(
                request.email()
        );


        String message =
                messageSource.getMessage(
                        "auth.confirmation.sent",
                        null,
                        LocaleContextHolder.getLocale()
                );


        return ResponseEntity
                .status(HttpStatus.ACCEPTED)
                .body(
                        ApiResponse.success(
                                HttpStatus.ACCEPTED.value(),
                                message,
                                null
                        )
                );
    }


    /**
     * =========================================================
     * FORGOT PASSWORD
     * =========================================================
     */
    @Operation(
            summary = "Send password reset OTP"
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "202",
                    description = "Password reset request accepted",
                    content = @Content(
                            schema = @Schema(
                                    implementation = ApiResponse.class
                            )
                    )
            )
    })
    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(
            @Valid
            @RequestBody
            SendConfirmationRequest request
    ) {

        otpService.sendPasswordResetOtp(
                request.email()
        );


        String message =
                messageSource.getMessage(
                        "auth.password.reset.sent",
                        null,
                        LocaleContextHolder.getLocale()
                );


        return ResponseEntity
                .status(HttpStatus.ACCEPTED)
                .body(
                        ApiResponse.success(
                                HttpStatus.ACCEPTED.value(),
                                message,
                                null
                        )
                );
    }


    /**
     * =========================================================
     * CONFIRM ACCOUNT
     * =========================================================
     */
    @Operation(
            summary = "Confirm account registration",
            description = "Verify 6-digit OTP code sent via email to activate user account from INACTIVE to ACTIVE"
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Account confirmed and activated successfully",
                    content = @Content(
                            schema = @Schema(
                                    implementation = ApiResponse.class
                            )
                    )
            ),

            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Invalid or expired OTP code, or validation error",
                    content = @Content(
                            schema = @Schema(
                                    implementation = ApiResponse.class
                            )
                    )
            )
    })
    @PostMapping("/confirm")
    public ResponseEntity<ApiResponse<Void>> confirmAccount(
            @Valid
            @RequestBody
            ConfirmAccountRequest request
    ) {

        otpService.confirmAccount(
                request.email(),
                request.otp()
        );


        String message =
                messageSource.getMessage(
                        "auth.confirmation.success",
                        null,
                        LocaleContextHolder.getLocale()
                );


        return ResponseEntity.ok(
                ApiResponse.success(
                        null,
                        message
                )
        );
    }


    /**
     * =========================================================
     * RESET PASSWORD
     * =========================================================
     */
    @Operation(
            summary = "Reset password with OTP",
            description = "Verify 6-digit OTP code and set new password for active user account"
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Password reset successfully",
                    content = @Content(
                            schema = @Schema(
                                    implementation = ApiResponse.class
                            )
                    )
            ),

            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Invalid or expired OTP code, or validation error",
                    content = @Content(
                            schema = @Schema(
                                    implementation = ApiResponse.class
                            )
                    )
            )
    })
    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @Valid
            @RequestBody
            ResetPasswordRequest request
    ) {

        otpService.resetPassword(
                request.email(),
                request.otp(),
                request.newPassword()
        );


        String message =
                messageSource.getMessage(
                        "auth.password.reset.success",
                        null,
                        LocaleContextHolder.getLocale()
                );


        return ResponseEntity.ok(
                ApiResponse.success(
                        null,
                        message
                )
        );
    }


    /**
     * =========================================================
     * MANAGEMENT ROLE CHECK
     * =========================================================
     *
     * Login là chung cho mọi role.
     *
     * Nhưng chỉ ADMIN/MODERATOR cần cookie dùng cho
     * Management Web UI.
     */
    private boolean hasManagementRole(
            Set<String> roles
    ) {

        if (
                roles == null
                || roles.isEmpty()
        ) {

            return false;
        }


        return (
                roles.contains(
                        ROLE_ADMIN
                )
                ||
                roles.contains(
                        ROLE_MODERATOR
                )
        );
    }


    /**
     * =========================================================
     * ADD MANAGEMENT COOKIE
     * =========================================================
     *
     * Cookie này:
     *
     * - HttpOnly
     * - SameSite=Lax
     * - Session cookie
     * - Secure khi app chạy HTTPS
     *
     * JavaScript không cần và không thể đọc cookie này.
     */
    private void addManagementCookie(
            String accessToken,
            HttpServletRequest request,
            HttpServletResponse response
    ) {

        if (
                accessToken == null
                || accessToken.isBlank()
        ) {

            return;
        }


        Cookie cookie =
                new Cookie(
                        JwtAuthenticationFilter
                                .MODERATOR_ACCESS_TOKEN_COOKIE,
                        accessToken
                );


        /*
         * Không cho JavaScript đọc JWT cookie.
         */
        cookie.setHttpOnly(
                true
        );


        /*
         * Localhost HTTP -> false
         * Production HTTPS -> true
         */
        cookie.setSecure(
                request.isSecure()
        );


        /*
         * Cần "/" vì management web có thể nằm ở:
         *
         * /moderator/**
         * /admin/**
         */
        cookie.setPath(
                "/"
        );


        /*
         * -1 = Session Cookie.
         *
         * Browser đóng thì cookie mất.
         */
        cookie.setMaxAge(
                -1
        );


        /*
         * Hạn chế cookie trong cross-site requests.
         */
        cookie.setAttribute(
                "SameSite",
                "Lax"
        );


        response.addCookie(
                cookie
        );
    }


    /**
     * =========================================================
     * CLEAR MANAGEMENT COOKIE
     * =========================================================
     *
     * Dùng khi:
     *
     * - logout
     * - login bằng USER/HOST sau ADMIN/MODERATOR
     * - login management account mới
     *
     * Max-Age = 0 => browser xóa ngay.
     */
    private void clearManagementCookie(
            HttpServletRequest request,
            HttpServletResponse response
    ) {

        Cookie cookie =
                new Cookie(
                        JwtAuthenticationFilter
                                .MODERATOR_ACCESS_TOKEN_COOKIE,
                        ""
                );


        cookie.setHttpOnly(
                true
        );


        cookie.setSecure(
                request.isSecure()
        );


        /*
         * Path khi xóa phải giống path khi tạo.
         */
        cookie.setPath(
                "/"
        );


        /*
         * Xóa ngay.
         */
        cookie.setMaxAge(
                0
        );


        cookie.setAttribute(
                "SameSite",
                "Lax"
        );


        response.addCookie(
                cookie
        );
    }
}