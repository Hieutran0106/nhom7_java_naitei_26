package com.nhom7.coworkingspace.service;

import com.nhom7.coworkingspace.dto.response.HostUpgradeResponse;
import com.nhom7.coworkingspace.dto.response.UpdateUserRoleResponse;
import com.nhom7.coworkingspace.entity.Role;
import com.nhom7.coworkingspace.entity.User;
import com.nhom7.coworkingspace.enums.UserStatus;
import com.nhom7.coworkingspace.exception.AppException;
import com.nhom7.coworkingspace.repository.RoleRepository;
import com.nhom7.coworkingspace.repository.UserRepository;
import com.nhom7.coworkingspace.service.impl.UserServiceImpl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private FileStorageService fileStorageService;

    @InjectMocks
    private UserServiceImpl userService;

    private User user;
    private Role userRole;
    private Role moderatorRole;

    private static final String HOST_EMAIL = "user@test.com";

    @BeforeEach
    void setUp() {
        userRole = Role.builder()
                .id(1L)
                .name("USER")
                .build();

        moderatorRole = Role.builder()
                .id(3L)
                .name("MODERATOR")
                .build();

        Set<Role> roles = new HashSet<>();
        roles.add(userRole);

        user = User.builder()
                .id(3L)
                .name("Test User")
                .email("user@test.com")
                .status(UserStatus.ACTIVE)
                .roles(roles)
                .build();
    }

    @Test
    void addRole_shouldAddModeratorAndKeepExistingUserRole() {
        when(userRepository.findById(3L))
                .thenReturn(Optional.of(user));

        when(roleRepository.findByName("MODERATOR"))
                .thenReturn(Optional.of(moderatorRole));

        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        UpdateUserRoleResponse response =
                userService.addRole(3L, "MODERATOR");

        assertNotNull(response);

        assertEquals(3L, response.getId());

        assertEquals(
                "user@test.com",
                response.getEmail()
        );

        assertTrue(
                response.getRoles().contains("USER")
        );

        assertTrue(
                response.getRoles().contains("MODERATOR")
        );

        assertEquals(
                2,
                response.getRoles().size()
        );

        verify(userRepository, times(1))
                .findById(3L);

        verify(roleRepository, times(1))
                .findByName("MODERATOR");

        verify(userRepository, times(1))
                .save(user);
    }

    @Test
    void addRole_shouldThrowNotFound_whenUserNotFound() {
        when(userRepository.findById(999L))
                .thenReturn(Optional.empty());

        ResponseStatusException exception =
                assertThrows(
                        ResponseStatusException.class,
                        () -> userService.addRole(
                                999L,
                                "MODERATOR"
                        )
                );

        assertEquals(
                HttpStatus.NOT_FOUND,
                exception.getStatusCode()
        );

        assertTrue(
                exception.getReason()
                        .contains("User not found")
        );

        verify(userRepository, times(1))
                .findById(999L);

        verify(roleRepository, never())
                .findByName(anyString());

        verify(userRepository, never())
                .save(any(User.class));
    }

    @Test
    void addRole_shouldThrowNotFound_whenRoleNotFound() {
        when(userRepository.findById(3L))
                .thenReturn(Optional.of(user));

        when(roleRepository.findByName("ABC"))
                .thenReturn(Optional.empty());

        ResponseStatusException exception =
                assertThrows(
                        ResponseStatusException.class,
                        () -> userService.addRole(
                                3L,
                                "ABC"
                        )
                );

        assertEquals(
                HttpStatus.NOT_FOUND,
                exception.getStatusCode()
        );

        assertTrue(
                exception.getReason()
                        .contains("Role not found")
        );

        verify(userRepository, times(1))
                .findById(3L);

        verify(roleRepository, times(1))
                .findByName("ABC");

        verify(userRepository, never())
                .save(any(User.class));
    }

    @Test
    void addRole_shouldNormalizeRoleNameToUpperCase() {
        when(userRepository.findById(3L))
                .thenReturn(Optional.of(user));

        when(roleRepository.findByName("MODERATOR"))
                .thenReturn(Optional.of(moderatorRole));

        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        userService.addRole(
                3L,
                "moderator"
        );

        verify(roleRepository, times(1))
                .findByName("MODERATOR");

        verify(userRepository, times(1))
                .save(user);
    }

    @Nested
    @DisplayName("becomeHost")
    class BecomeHostTests {

        private final MultipartFile validLicense =
                new MockMultipartFile("businessLicense", "license.jpg", "image/jpeg", "license-bytes".getBytes());

        @Test
        @DisplayName("Account status is not ACTIVE -> not upgraded to HOST, regardless of verification flags")
        void becomeHost_StatusNotActive_ThrowsForbidden() {
            user.setStatus(UserStatus.INACTIVE);
            user.setBusinessLicenseUrl("business-license/existing.jpg");
            user.setIsBusinessVerified(true);
            user.setIsIdentityVerified(true);

            given(userRepository.findByEmail(HOST_EMAIL)).willReturn(Optional.of(user));

            assertThatThrownBy(() -> userService.becomeHost(HOST_EMAIL, null))
                    .isInstanceOf(AppException.class)
                    .hasMessage("host.status.not.active")
                    .extracting("status")
                    .isEqualTo(HttpStatus.FORBIDDEN);

            verify(roleRepository, never()).findByName(anyString());
            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        @DisplayName("Uploads license successfully but neither is verified yet -> not upgraded to HOST, license URL still persisted")
        void becomeHost_UploadedButNotVerified_ThrowsForbidden() {
            user.setIsBusinessVerified(false);
            user.setIsIdentityVerified(false);

            given(userRepository.findByEmail(HOST_EMAIL)).willReturn(Optional.of(user));
            given(fileStorageService.storeFile(validLicense, "business-license")).willReturn("business-license/uuid.jpg");

            assertThatThrownBy(() -> userService.becomeHost(HOST_EMAIL, validLicense))
                    .isInstanceOf(AppException.class)
                    .hasMessage("host.identity.required")
                    .extracting("status")
                    .isEqualTo(HttpStatus.FORBIDDEN);

            // The upload must be persisted even though the overall call ends up throwing -
            // this is the exact regression the noRollbackFor fix protects against.
            assertThat(user.getBusinessLicenseUrl()).isEqualTo("business-license/uuid.jpg");
            assertThat(user.getIsBusinessVerified()).isFalse();
            verify(userRepository).save(user);
            verify(roleRepository, never()).findByName(anyString());
        }

        @Test
        @DisplayName("Business verified but identity not verified -> not upgraded to HOST")
        void becomeHost_BusinessVerifiedIdentityNot_ThrowsForbidden() {
            user.setBusinessLicenseUrl("business-license/existing.jpg");
            user.setIsBusinessVerified(true);
            user.setIsIdentityVerified(false);

            given(userRepository.findByEmail(HOST_EMAIL)).willReturn(Optional.of(user));

            assertThatThrownBy(() -> userService.becomeHost(HOST_EMAIL, null))
                    .isInstanceOf(AppException.class)
                    .hasMessage("host.identity.required")
                    .extracting("status")
                    .isEqualTo(HttpStatus.FORBIDDEN);

            verify(roleRepository, never()).findByName(anyString());
            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        @DisplayName("Identity verified but business not verified -> not upgraded to HOST")
        void becomeHost_IdentityVerifiedBusinessNot_ThrowsForbidden() {
            user.setBusinessLicenseUrl("business-license/existing.jpg");
            user.setIsBusinessVerified(false);
            user.setIsIdentityVerified(true);

            given(userRepository.findByEmail(HOST_EMAIL)).willReturn(Optional.of(user));

            assertThatThrownBy(() -> userService.becomeHost(HOST_EMAIL, null))
                    .isInstanceOf(AppException.class)
                    .hasMessage("host.business.pending")
                    .extracting("status")
                    .isEqualTo(HttpStatus.FORBIDDEN);

            verify(roleRepository, never()).findByName(anyString());
            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        @DisplayName("No business license at all -> Business license is required")
        void becomeHost_NoLicense_ThrowsBadRequest() {
            user.setBusinessLicenseUrl(null);

            given(userRepository.findByEmail(HOST_EMAIL)).willReturn(Optional.of(user));

            assertThatThrownBy(() -> userService.becomeHost(HOST_EMAIL, null))
                    .isInstanceOf(AppException.class)
                    .hasMessage("host.license.required")
                    .extracting("status")
                    .isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("Both verified=true but business_license_url is NULL -> still Business license is required")
        void becomeHost_BothVerifiedButNoLicenseUrl_ThrowsBadRequest() {
            user.setBusinessLicenseUrl(null);
            user.setIsBusinessVerified(true);
            user.setIsIdentityVerified(true);

            given(userRepository.findByEmail(HOST_EMAIL)).willReturn(Optional.of(user));

            assertThatThrownBy(() -> userService.becomeHost(HOST_EMAIL, null))
                    .isInstanceOf(AppException.class)
                    .hasMessage("host.license.required")
                    .extracting("status")
                    .isEqualTo(HttpStatus.BAD_REQUEST);

            verify(roleRepository, never()).findByName(anyString());
            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        @DisplayName("ACTIVE + identity verified + business verified + license URL present -> upgraded to HOST successfully")
        void becomeHost_BothVerified_UpgradesToHost() {
            user.setBusinessLicenseUrl("business-license/existing.jpg");
            user.setIsBusinessVerified(true);
            user.setIsIdentityVerified(true);

            Role hostRole = Role.builder().id(4L).name("HOST").build();

            given(userRepository.findByEmail(HOST_EMAIL)).willReturn(Optional.of(user));
            given(roleRepository.findByName("HOST")).willReturn(Optional.of(hostRole));
            given(userRepository.save(any(User.class))).willAnswer(invocation -> invocation.getArgument(0));

            HostUpgradeResponse response = userService.becomeHost(HOST_EMAIL, null);

            assertThat(response.isAlreadyHost()).isFalse();
            assertThat(response.getProfile().getRoles()).contains("HOST", "USER");
            assertThat(user.getRoles()).contains(hostRole);
            verify(userRepository).save(user);
        }

        @Test
        @DisplayName("User already HOST -> not duplicated, returns already-host response without touching storage")
        void becomeHost_AlreadyHost_DoesNotDuplicateRole() {
            Role hostRole = Role.builder().id(4L).name("HOST").build();
            user.getRoles().add(hostRole);

            given(userRepository.findByEmail(HOST_EMAIL)).willReturn(Optional.of(user));

            HostUpgradeResponse response = userService.becomeHost(HOST_EMAIL, validLicense);

            assertThat(response.isAlreadyHost()).isTrue();
            assertThat(user.getRoles()).hasSize(2);
            verifyNoInteractions(fileStorageService);
            verify(userRepository, never()).save(any(User.class));
            verify(roleRepository, never()).findByName(anyString());
        }

        @Test
        @DisplayName("Resubmitting the exact same already-verified file does NOT reset verification -> still upgrades to HOST")
        void becomeHost_ResubmitSameFileAfterVerification_StillUpgradesToHost() {
            user.setBusinessLicenseUrl("business-license/existing.jpg");
            user.setBusinessLicenseHash(sha256Hex(validLicense));
            user.setIsBusinessVerified(true);
            user.setIsIdentityVerified(true);

            Role hostRole = Role.builder().id(4L).name("HOST").build();

            given(userRepository.findByEmail(HOST_EMAIL)).willReturn(Optional.of(user));
            given(roleRepository.findByName("HOST")).willReturn(Optional.of(hostRole));
            given(userRepository.save(any(User.class))).willAnswer(invocation -> invocation.getArgument(0));

            HostUpgradeResponse response = userService.becomeHost(HOST_EMAIL, validLicense);

            assertThat(response.isAlreadyHost()).isFalse();
            assertThat(response.getProfile().getRoles()).contains("HOST");
            assertThat(user.getIsBusinessVerified()).isTrue();
            verify(fileStorageService, never()).storeFile(any(), anyString());
        }

        @Test
        @DisplayName("Uploading a genuinely different file after verification resets isBusinessVerified again")
        void becomeHost_UploadDifferentFileAfterVerification_ResetsVerification() {
            user.setBusinessLicenseUrl("business-license/existing.jpg");
            user.setBusinessLicenseHash("different-hash-value");
            user.setIsBusinessVerified(true);
            user.setIsIdentityVerified(true);

            given(userRepository.findByEmail(HOST_EMAIL)).willReturn(Optional.of(user));
            given(fileStorageService.storeFile(validLicense, "business-license"))
                    .willReturn("business-license/new-uuid.jpg");

            assertThatThrownBy(() -> userService.becomeHost(HOST_EMAIL, validLicense))
                    .isInstanceOf(AppException.class)
                    .hasMessage("host.business.pending")
                    .extracting("status")
                    .isEqualTo(HttpStatus.FORBIDDEN);

            assertThat(user.getBusinessLicenseUrl()).isEqualTo("business-license/new-uuid.jpg");
            assertThat(user.getIsBusinessVerified()).isFalse();
        }

        private String sha256Hex(MultipartFile file) {
            try {
                java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
                byte[] hash = digest.digest(file.getBytes());
                return java.util.HexFormat.of().formatHex(hash);
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }
    }
}