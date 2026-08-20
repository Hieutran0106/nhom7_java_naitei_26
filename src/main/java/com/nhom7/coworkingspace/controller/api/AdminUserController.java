package com.nhom7.coworkingspace.controller.api;

import com.nhom7.coworkingspace.dto.request.UpdateUserRoleRequest;
import com.nhom7.coworkingspace.dto.response.UserRoleResponse;
import com.nhom7.coworkingspace.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Các API dành cho Admin quản lý User.
 */
@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    /**
     * Service xử lý nghiệp vụ User.
     */
    private final UserService userService;

    /**
     * API thêm role cho User.
     *
     * Endpoint vẫn giữ đúng task:
     *
     * PUT /api/admin/users/{userId}/role
     *
     * Ví dụ:
     *
     * PUT /api/admin/users/3/role
     *
     * Body:
     * {
     *     "role": "MODERATOR"
     * }
     */
    @PutMapping("/{userId}/role")

    /**
     * Chỉ ADMIN mới được thêm role cho user.
     */
   // @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserRoleResponse> addUserRole(

            /*
             * Lấy userId từ URL.
             *
             * Ví dụ:
             * /users/3/role
             *
             * -> userId = 3
             */
            @PathVariable Long userId,

            /*
             * Nhận JSON body.
             */
            @Valid @RequestBody UpdateUserRoleRequest request) {

        /*
         * Gọi Service để thêm role mới.
         */
        UserRoleResponse response =
                userService.addRole(
                        userId,
                        request.getRole()
                );

        /*
         * Thành công -> HTTP 200 OK.
         */
        return ResponseEntity.ok(response);
    }
}