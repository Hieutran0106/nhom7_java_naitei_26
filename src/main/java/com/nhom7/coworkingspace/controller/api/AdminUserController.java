package com.nhom7.coworkingspace.controller.api;

import com.nhom7.coworkingspace.dto.request.UpdateUserRoleRequest;
import com.nhom7.coworkingspace.dto.response.UserRoleResponse;
import com.nhom7.coworkingspace.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final UserService userService;

    @PutMapping("/{userId}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserRoleResponse> addUserRole(
            @PathVariable Long userId,
            @Valid @RequestBody UpdateUserRoleRequest request) {

        UserRoleResponse response =
                userService.addRole(
                        userId,
                        request.getRole()
                );

        return ResponseEntity.ok(response);
    }
}