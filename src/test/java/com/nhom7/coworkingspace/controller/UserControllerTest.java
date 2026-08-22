package com.nhom7.coworkingspace.controller;

import com.nhom7.coworkingspace.controller.api.UserController;
import com.nhom7.coworkingspace.dto.response.HostUpgradeResponse;
import com.nhom7.coworkingspace.dto.response.UserProfileResponse;
import com.nhom7.coworkingspace.exception.AppException;
import com.nhom7.coworkingspace.exception.GlobalExceptionHandler;
import com.nhom7.coworkingspace.service.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Locale;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserController - Unit Tests")
class UserControllerTest {

    @Mock
    private UserService userService;

    @Mock
    private MessageSource messageSource;

    @InjectMocks
    private UserController userController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        GlobalExceptionHandler exceptionHandler = new GlobalExceptionHandler(messageSource);
        mockMvc = MockMvcBuilders.standaloneSetup(userController)
                .setControllerAdvice(exceptionHandler)
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();

        UserDetails principal = new User("user@example.com", "password", List.of());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Nested
    @DisplayName("PUT /api/users/me")
    class UpdateMyProfileTests {

        @Test
        @DisplayName("Should return 400 with a descriptive message when cccdImage is not JPEG/PNG/WEBP")
        void shouldReturn400WhenCccdImageContentTypeIsNotAllowed() throws Exception {
            MockMultipartFile invalidFile = new MockMultipartFile(
                    "cccdImage", "cccd.pdf", "application/pdf", "not-an-image".getBytes());

            given(messageSource.getMessage(any(MessageSourceResolvable.class), any(Locale.class)))
                    .willReturn("Only JPEG, PNG, and WEBP image formats are accepted");

            mockMvc.perform(multipart(HttpMethod.PUT, "/api/users/me").file(invalidFile))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(400))
                    .andExpect(jsonPath("$.data.cccdImage")
                            .value("Only JPEG, PNG, and WEBP image formats are accepted"));

            verifyNoInteractions(userService);
        }

        @Test
        @DisplayName("Should return 200 and the updated profile when cccdImage is a valid image")
        void shouldReturn200WhenUpdateIsValid() throws Exception {
            MockMultipartFile validFile = new MockMultipartFile(
                    "cccdImage", "cccd.jpg", "image/jpeg", "sample-image-content".getBytes());

            UserProfileResponse response = UserProfileResponse.builder()
                    .id(1L)
                    .name("Nguyen Van A")
                    .email("user@example.com")
                    .phone("0912345678")
                    .cccdUrl("https://signed-url/cccd/uuid.jpg")
                    .build();

            given(userService.updateMyProfile(eqEmail(), any())).willReturn(response);
            given(messageSource.getMessage(org.mockito.ArgumentMatchers.eq("user.updated"), any(), any(Locale.class)))
                    .willReturn("User updated successfully");

            mockMvc.perform(multipart(HttpMethod.PUT, "/api/users/me")
                            .file(validFile)
                            .param("name", "Nguyen Van A"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.email").value("user@example.com"))
                    .andExpect(jsonPath("$.data.cccdUrl").value("https://signed-url/cccd/uuid.jpg"));
        }

        private String eqEmail() {
            return org.mockito.ArgumentMatchers.eq("user@example.com");
        }
    }

    @Nested
    @DisplayName("POST /api/users/me/roles/host")
    class BecomeHostEndpointTests {

        @Test
        @DisplayName("Should return 400 with a descriptive message when businessLicense is not JPEG/PNG/WEBP")
        void shouldReturn400WhenBusinessLicenseContentTypeIsNotAllowed() throws Exception {
            MockMultipartFile invalidFile = new MockMultipartFile(
                    "businessLicense", "license.pdf", "application/pdf", "not-an-image".getBytes());

            given(messageSource.getMessage(any(MessageSourceResolvable.class), any(Locale.class)))
                    .willReturn("Only JPEG, PNG, and WEBP image formats are accepted");

            mockMvc.perform(multipart("/api/users/me/roles/host").file(invalidFile))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(400))
                    .andExpect(jsonPath("$.data.businessLicense")
                            .value("Only JPEG, PNG, and WEBP image formats are accepted"));

            verifyNoInteractions(userService);
        }

        @Test
        @DisplayName("Should return 200 with success message when both verifications pass")
        void shouldReturn200WhenBothVerificationsPass() throws Exception {
            UserProfileResponse profile = UserProfileResponse.builder()
                    .id(1L)
                    .email("user@example.com")
                    .roles(Set.of("USER", "HOST"))
                    .isBusinessVerified(true)
                    .isIdentityVerified(true)
                    .build();
            HostUpgradeResponse serviceResponse = HostUpgradeResponse.builder()
                    .profile(profile)
                    .alreadyHost(false)
                    .build();

            given(userService.becomeHost(eq("user@example.com"), isNull())).willReturn(serviceResponse);
            given(messageSource.getMessage(eq("host.upgrade.success"), any(), any(Locale.class)))
                    .willReturn("You have successfully become a Host.");

            mockMvc.perform(multipart("/api/users/me/roles/host"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.message").value("You have successfully become a Host."))
                    .andExpect(jsonPath("$.data.roles", org.hamcrest.Matchers.containsInAnyOrder("USER", "HOST")));
        }

        @Test
        @DisplayName("Should return 200 with already-a-host message when user already has the HOST role")
        void shouldReturn200WhenAlreadyHost() throws Exception {
            UserProfileResponse profile = UserProfileResponse.builder()
                    .id(1L)
                    .email("user@example.com")
                    .roles(Set.of("USER", "HOST"))
                    .build();
            HostUpgradeResponse serviceResponse = HostUpgradeResponse.builder()
                    .profile(profile)
                    .alreadyHost(true)
                    .build();

            given(userService.becomeHost(eq("user@example.com"), isNull())).willReturn(serviceResponse);
            given(messageSource.getMessage(eq("host.already"), any(), any(Locale.class)))
                    .willReturn("You are already a Host.");

            mockMvc.perform(multipart("/api/users/me/roles/host"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.message").value("You are already a Host."));
        }

        @Test
        @DisplayName("Should return 403 with a clear message when business license is still pending verification")
        void shouldReturn403WhenBusinessPendingVerification() throws Exception {
            given(userService.becomeHost(eq("user@example.com"), isNull()))
                    .willThrow(new AppException("host.business.pending", HttpStatus.FORBIDDEN));
            given(messageSource.getMessage(eq("host.business.pending"), any(), any(Locale.class)))
                    .willReturn("Your business license is pending verification.");

            mockMvc.perform(multipart("/api/users/me/roles/host"))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value(403))
                    .andExpect(jsonPath("$.message").value("Your business license is pending verification."));
        }

        @Test
        @DisplayName("Should return 400 with a clear message when no business license has been uploaded")
        void shouldReturn400WhenNoLicenseUploaded() throws Exception {
            given(userService.becomeHost(eq("user@example.com"), isNull()))
                    .willThrow(new AppException("host.license.required", HttpStatus.BAD_REQUEST));
            given(messageSource.getMessage(eq("host.license.required"), any(), any(Locale.class)))
                    .willReturn("Business license is required.");

            mockMvc.perform(multipart("/api/users/me/roles/host"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(400))
                    .andExpect(jsonPath("$.message").value("Business license is required."));
        }
    }
}
