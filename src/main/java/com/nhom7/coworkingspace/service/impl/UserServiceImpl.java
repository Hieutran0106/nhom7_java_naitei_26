package com.nhom7.coworkingspace.service.impl;

import com.nhom7.coworkingspace.dto.request.UpdateUserRequest;
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
import org.springframework.web.multipart.MultipartFile;
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

        return buildProfileResponse(user);
    }

    @Override
    @Transactional
    public UserProfileResponse updateMyProfile(String email, UpdateUserRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException("user.not.found", HttpStatus.NOT_FOUND));

        if (request.getName() != null) {
            String trimmedName = request.getName().trim();
            if (trimmedName.isEmpty()) {
                throw new AppException("validation.name.required", HttpStatus.BAD_REQUEST);
            }
            user.setName(trimmedName);
        }

        if (request.getPhone() != null) {
            String trimmedPhone = request.getPhone().trim();
            if (userRepository.existsByPhoneAndIdNot(trimmedPhone, user.getId())) {
                throw new AppException("user.phone.exists", HttpStatus.CONFLICT);
            }
            user.setPhone(trimmedPhone);
        }

        MultipartFile cccdImage = request.getCccdImage();
        if (cccdImage != null && !cccdImage.isEmpty()) {
            String cccdPath = fileStorageService.storeFile(cccdImage, "cccd");
            user.setCccdUrl(cccdPath);
        }

        User updatedUser = userRepository.save(user);

        return buildProfileResponse(updatedUser);
    }

    private UserProfileResponse buildProfileResponse(User user) {
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