package com.nhom7.coworkingspace.service.impl;

import com.nhom7.coworkingspace.dto.response.UpdateUserRoleResponse;
import com.nhom7.coworkingspace.dto.response.UserProfileResponse;
import com.nhom7.coworkingspace.entity.Role;
import com.nhom7.coworkingspace.entity.User;
import com.nhom7.coworkingspace.exception.AppException;
import com.nhom7.coworkingspace.repository.RoleRepository;
import com.nhom7.coworkingspace.repository.UserRepository;
import com.nhom7.coworkingspace.service.FileStorageService;
import com.nhom7.coworkingspace.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private static final int IMAGE_SIGNED_URL_EXPIRES_IN_SECONDS = 3600;

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final FileStorageService fileStorageService;

    @Override
    @Transactional
    public UpdateUserRoleResponse addRole(Long userId, String roleName) {

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "User not found with id: " + userId
                        )
                );

        String normalizedRoleName =
                roleName.trim().toUpperCase();

        Role role = roleRepository.findByName(normalizedRoleName)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Role not found: " + normalizedRoleName
                        )
                );

        user.getRoles().add(role);

        User updatedUser = userRepository.save(user);

        Set<String> roleNames = updatedUser.getRoles()
                .stream()
                .map(Role::getName)
                .collect(Collectors.toSet());

        return UpdateUserRoleResponse.builder()
                .id(updatedUser.getId())
                .name(updatedUser.getName())
                .email(updatedUser.getEmail())
                .roles(roleNames)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public UserProfileResponse getMyProfile(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException("user.not.found", HttpStatus.NOT_FOUND));

        Set<String> roleNames = user.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toSet());

        return UserProfileResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .status(user.getStatus())
                .isIdentityVerified(user.getIsIdentityVerified())
                .isBusinessVerified(user.getIsBusinessVerified())
                .language(user.getLanguage())
                .cccdUrl(resolveSignedUrl(user.getCccdUrl()))
                .businessLicenseUrl(resolveSignedUrl(user.getBusinessLicenseUrl()))
                .roles(roleNames)
                .build();
    }

    private String resolveSignedUrl(String filePath) {
        if (!StringUtils.hasText(filePath)) {
            return null;
        }
        return fileStorageService.createSignedUrl(filePath, IMAGE_SIGNED_URL_EXPIRES_IN_SECONDS);
    }
}