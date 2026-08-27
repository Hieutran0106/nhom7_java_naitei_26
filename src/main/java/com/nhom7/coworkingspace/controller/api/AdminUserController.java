package com.nhom7.coworkingspace.controller.api;

import com.nhom7.coworkingspace.dto.request.UpdateUserRoleRequest;
import com.nhom7.coworkingspace.dto.response.ApiResponse;
import com.nhom7.coworkingspace.dto.response.UpdateUserRoleResponse;
import com.nhom7.coworkingspace.service.UserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Locale;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@Tag(
        name = "User Role Management",
        description = "Endpoints for managing user roles"
)
public class AdminUserController {

    private final UserService userService;
    private final MessageSource messageSource;

    @PutMapping("/{userId}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UpdateUserRoleResponse>> changeUserRole(
            @PathVariable Long userId,
            @Valid @RequestBody UpdateUserRoleRequest request) {

        UpdateUserRoleResponse response =
                userService.changeRole(
                        userId,
                        request.getRole()
                );

        Locale locale =
                LocaleContextHolder.getLocale();

        String message =
                messageSource.getMessage(
                        "user.role.updated",
                        null,
                        locale
                );

        return ResponseEntity.ok(
                ApiResponse.success(
                        response,
                        message
                )
        );
    }

    @DeleteMapping("/{userId}/roles/{roleName}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UpdateUserRoleResponse>> removeUserRole(
            @PathVariable Long userId,
            @PathVariable String roleName) {

        UpdateUserRoleResponse response =
                userService.removeRole(
                        userId,
                        roleName
                );

        Locale locale =
                LocaleContextHolder.getLocale();

        String message =
                messageSource.getMessage(
                        "user.role.updated",
                        null,
                        locale
                );

        return ResponseEntity.ok(
                ApiResponse.success(
                        response,
                        message
                )
        );
    }
}