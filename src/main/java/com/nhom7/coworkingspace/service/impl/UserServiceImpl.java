package com.nhom7.coworkingspace.service.impl;

import com.nhom7.coworkingspace.dto.request.UpdateUserRequest;
import com.nhom7.coworkingspace.dto.response.HostUpgradeResponse;
import com.nhom7.coworkingspace.dto.response.UpdateUserRoleResponse;
import com.nhom7.coworkingspace.dto.response.UserProfileResponse;
import com.nhom7.coworkingspace.entity.Role;
import com.nhom7.coworkingspace.entity.User;
import com.nhom7.coworkingspace.enums.UserStatus;
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

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private static final int IMAGE_SIGNED_URL_EXPIRES_IN_SECONDS = 3600;
    private static final String HOST_ROLE_NAME = "HOST";
    private static final String BUSINESS_LICENSE_SUBDIRECTORY = "business-license";

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

    // noRollbackFor is essential here: a business license upload must be persisted even when the
    // very same call goes on to reject the upgrade (e.g. verification still pending) - otherwise
    // Spring's default rollback-on-RuntimeException would undo the save() below every time an
    // AppException is thrown afterwards, leaving business_license_url permanently NULL.
    @Override
    @Transactional(noRollbackFor = AppException.class)
    public HostUpgradeResponse becomeHost(String email, MultipartFile businessLicense) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException("user.not.found", HttpStatus.NOT_FOUND));

        boolean alreadyHost = user.getRoles().stream()
                .anyMatch(role -> HOST_ROLE_NAME.equalsIgnoreCase(role.getName()));
        if (alreadyHost) {
            return HostUpgradeResponse.builder()
                    .profile(buildProfileResponse(user))
                    .alreadyHost(true)
                    .build();
        }

        if (businessLicense != null && !businessLicense.isEmpty()) {
            String newHash = sha256Hex(businessLicense);
            boolean isSameFileAlreadyOnFile = StringUtils.hasText(user.getBusinessLicenseUrl())
                    && newHash.equals(user.getBusinessLicenseHash());

            // A resubmission of the exact same file (e.g. a client retrying this call with the
            // same attachment still selected) must NOT wipe out a verification that was already
            // granted in the meantime - only a genuinely different file resets it.
            if (!isSameFileAlreadyOnFile) {
                String licensePath = fileStorageService.storeFile(businessLicense, BUSINESS_LICENSE_SUBDIRECTORY);
                user.setBusinessLicenseUrl(licensePath);
                user.setBusinessLicenseHash(newHash);
                user.setIsBusinessVerified(false);
                userRepository.save(user);
            }
        }

        boolean isActive = user.getStatus() == UserStatus.ACTIVE;
        boolean hasLicense = StringUtils.hasText(user.getBusinessLicenseUrl());
        boolean identityVerified = Boolean.TRUE.equals(user.getIsIdentityVerified());
        boolean businessVerified = Boolean.TRUE.equals(user.getIsBusinessVerified());

        if (!isActive) {
            throw new AppException("host.status.not.active", HttpStatus.FORBIDDEN);
        }
        if (!hasLicense) {
            throw new AppException("host.license.required", HttpStatus.BAD_REQUEST);
        }
        if (!identityVerified) {
            throw new AppException("host.identity.required", HttpStatus.FORBIDDEN);
        }
        if (!businessVerified) {
            throw new AppException("host.business.pending", HttpStatus.FORBIDDEN);
        }

        Role hostRole = roleRepository.findByName(HOST_ROLE_NAME)
                .orElseThrow(() -> new AppException("role.not.found", HttpStatus.NOT_FOUND));
        user.getRoles().add(hostRole);
        User updatedUser = userRepository.save(user);

        return HostUpgradeResponse.builder()
                .profile(buildProfileResponse(updatedUser))
                .alreadyHost(false)
                .build();
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

    private String sha256Hex(MultipartFile file) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(file.getBytes());
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException | IOException ex) {
            throw new AppException("common.error", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}