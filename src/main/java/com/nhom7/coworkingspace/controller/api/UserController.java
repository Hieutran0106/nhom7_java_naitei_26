package com.nhom7.coworkingspace.controller.api;

import com.nhom7.coworkingspace.dto.request.BecomeHostRequest;
import com.nhom7.coworkingspace.dto.request.UpdateUserRequest;
import com.nhom7.coworkingspace.dto.response.ApiResponse;
import com.nhom7.coworkingspace.dto.response.HostUpgradeResponse;
import com.nhom7.coworkingspace.dto.response.UserProfileResponse;
import com.nhom7.coworkingspace.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Locale;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "User", description = "Endpoints for the currently authenticated user")
public class UserController {

    private final UserService userService;
    private final MessageSource messageSource;

    @Operation(
            summary = "Get current user profile",
            description = "Get the current user's profile information based on the access token that has been authenticated by JwtAuthenticationFilter. Click the Authorize button and enter the accessToken received after a successful login. Then click the Execute button."
    )
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getMyProfile(
            @AuthenticationPrincipal UserDetails userDetails) {
        UserProfileResponse response = userService.getMyProfile(userDetails.getUsername());
        Locale locale = LocaleContextHolder.getLocale();
        String message = messageSource.getMessage("user.profile.fetched", null, locale);
        return ResponseEntity.ok(ApiResponse.success(response, message));
    }

    @Operation(
            summary = "Update current user profile",
            description = "Update the currently authenticated user's own name, phone, and/or CCCD image, based on the access token authenticated by JwtAuthenticationFilter. All fields are optional (partial update) - only the fields provided in the request are changed, the rest keep their current value. Click the Authorize button and enter the accessToken received after a successful login before calling this endpoint."
    )
    @PutMapping(value = "/me", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<UserProfileResponse>> updateMyProfile(
            @AuthenticationPrincipal UserDetails userDetails,
            @ModelAttribute @Valid UpdateUserRequest request) {
        UserProfileResponse response = userService.updateMyProfile(userDetails.getUsername(), request);
        Locale locale = LocaleContextHolder.getLocale();
        String message = messageSource.getMessage("user.updated", null, locale);
        return ResponseEntity.ok(ApiResponse.success(response, message));
    }

    @Operation(
            summary = "Register/upgrade to HOST",
            description = "Allows the currently authenticated USER to upload a business license and, once both "
                    + "identity and business verification have been approved (currently set manually by a moderator), "
                    + "upgrade their account to the HOST role. The businessLicense file is optional - omit it if one "
                    + "was already uploaded in a previous call. The user is always resolved from the access token "
                    + "(SecurityContext), never from client input. Click the Authorize button and enter the accessToken "
                    + "received after a successful login before calling this endpoint."
    )
    @PostMapping(value = "/me/roles/host", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<UserProfileResponse>> becomeHost(
            @AuthenticationPrincipal UserDetails userDetails,
            @ModelAttribute @Valid BecomeHostRequest request) {
        HostUpgradeResponse result = userService.becomeHost(userDetails.getUsername(), request.getBusinessLicense());
        String messageKey = result.isAlreadyHost() ? "host.already" : "host.upgrade.success";
        Locale locale = LocaleContextHolder.getLocale();
        String message = messageSource.getMessage(messageKey, null, locale);
        return ResponseEntity.ok(ApiResponse.success(result.getProfile(), message));
    }
}
