package com.nhom7.coworkingspace.service.impl;

import com.nhom7.coworkingspace.dto.response.UserRoleResponse;
import com.nhom7.coworkingspace.entity.Role;
import com.nhom7.coworkingspace.entity.User;
import com.nhom7.coworkingspace.repository.RoleRepository;
import com.nhom7.coworkingspace.repository.UserRepository;
import com.nhom7.coworkingspace.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Xử lý các nghiệp vụ liên quan đến User.
 */
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    /**
     * Dùng để tìm và lưu User.
     */
    private final UserRepository userRepository;

    /**
     * Dùng để tìm Role.
     */
    private final RoleRepository roleRepository;

    @Override
    @Transactional
    public UserRoleResponse addRole(Long userId, String roleName) {

        /*
         * 1. Tìm user theo ID.
         *
         * Ví dụ:
         * userId = 3
         */
        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "User not found with id: " + userId
                        )
                );

        /*
         * 2. Chuẩn hóa tên role.
         *
         * Ví dụ:
         * "moderator" -> "MODERATOR"
         */
        String normalizedRoleName =
                roleName.trim().toUpperCase();

        /*
         * 3. Tìm role trong database.
         *
         * Ví dụ:
         * MODERATOR -> id = 3
         */
        Role role = roleRepository.findByName(normalizedRoleName)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Role not found: " + normalizedRoleName
                        )
                );

        /*
         * 4. Thêm role mới.
         *
         * KHÔNG dùng clear()
         *
         * Ví dụ:
         *
         * trước:
         * [USER]
         *
         * thêm:
         * MODERATOR
         *
         * sau:
         * [USER, MODERATOR]
         */
        user.getRoles().add(role);

        /*
         * 5. Lưu xuống database.
         *
         * JPA/Hibernate sẽ tự cập nhật bảng user_roles.
         */
        User updatedUser = userRepository.save(user);

        /*
         * 6. Chuyển Set<Role> thành Set<String>
         * để response dễ đọc.
         *
         * Ví dụ:
         * Role(USER), Role(MODERATOR)
         *
         * ->
         *
         * ["USER", "MODERATOR"]
         */
        Set<String> roleNames = updatedUser.getRoles()
                .stream()
                .map(Role::getName)
                .collect(Collectors.toSet());

        /*
         * 7. Trả thông tin user sau khi thêm role.
         */
        return UserRoleResponse.builder()
                .id(updatedUser.getId())
                .name(updatedUser.getName())
                .email(updatedUser.getEmail())
                .roles(roleNames)
                .build();
    }
}