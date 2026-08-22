package com.nhom7.coworkingspace.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nhom7.coworkingspace.dto.request.LoginRequest;
import com.nhom7.coworkingspace.entity.Role;
import com.nhom7.coworkingspace.entity.User;
import com.nhom7.coworkingspace.enums.UserStatus;
import com.nhom7.coworkingspace.repository.RoleRepository;
import com.nhom7.coworkingspace.repository.UserRepository;
import com.nhom7.coworkingspace.service.FileStorageService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// End-to-end check (real DB, real @Transactional AOP, real Spring Security filter chain) that a
// business license upload is durably persisted even when the same call goes on to reject the
// HOST upgrade. This is the exact regression that motivated Transactional(noRollbackFor =
// AppException.class) on UserServiceImpl#becomeHost: without it, Spring's default
// rollback-on-RuntimeException silently undoes the license save whenever the subsequent
// eligibility checks throw, leaving business_license_url NULL forever. FileStorageService is
// mocked so the test never talks to the real Supabase bucket.
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class BecomeHostIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private FileStorageService fileStorageService;

    private final String testEmail = "become-host-e2e-" + UUID.randomUUID() + "@coworking.test";
    private static final String TEST_PASSWORD = "Password123@";

    @AfterEach
    void cleanUp() {
        userRepository.findByEmail(testEmail).ifPresent(userRepository::delete);
    }

    private String loginAndGetAccessToken() throws Exception {
        LoginRequest loginRequest = LoginRequest.builder()
                .email(testEmail)
                .password(TEST_PASSWORD)
                .build();

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode loginData = objectMapper.readTree(loginResult.getResponse().getContentAsString()).get("data");
        return loginData.get("accessToken").asText();
    }

    @Test
    void uploadingLicenseMustPersistBusinessLicenseUrlEvenWhenNotYetEligibleForHost() throws Exception {
        Role userRole = roleRepository.findByName("USER")
                .orElseGet(() -> roleRepository.save(Role.builder().name("USER").build()));

        User user = User.builder()
                .name("Become Host E2E")
                .email(testEmail)
                .password(passwordEncoder.encode(TEST_PASSWORD))
                .phone("0912345678")
                .status(UserStatus.ACTIVE)
                .isIdentityVerified(false)
                .isBusinessVerified(false)
                .language("vi")
                .roles(new HashSet<>(Set.of(userRole)))
                .build();
        userRepository.save(user);

        String accessToken = loginAndGetAccessToken();

        given(fileStorageService.storeFile(any(), eq("business-license")))
                .willReturn("business-license/e2e-test-uuid.jpg");

        MockMultipartFile license = new MockMultipartFile(
                "businessLicense", "license.jpg", "image/jpeg", "license-bytes".getBytes());

        // Neither identity nor business is verified yet, so the upgrade itself must be rejected...
        mockMvc.perform(multipart("/api/users/me/roles/host")
                        .file(license)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").isNotEmpty());

        // ...but the uploaded license must still be durably saved - re-read from a fresh
        // transaction, independent of whatever the HTTP request's transaction did.
        Optional<User> persisted = userRepository.findByEmail(testEmail);
        assertThat(persisted).isPresent();
        assertThat(persisted.get().getBusinessLicenseUrl()).isEqualTo("business-license/e2e-test-uuid.jpg");
        assertThat(persisted.get().getIsBusinessVerified()).isFalse();
    }

    @Test
    void activeUserWithBothVerificationsAndLicenseUrlIsUpgradedToHost() throws Exception {
        Role userRole = roleRepository.findByName("USER")
                .orElseGet(() -> roleRepository.save(Role.builder().name("USER").build()));
        roleRepository.findByName("HOST")
                .orElseGet(() -> roleRepository.save(Role.builder().name("HOST").build()));

        User user = User.builder()
                .name("Become Host E2E Success")
                .email(testEmail)
                .password(passwordEncoder.encode(TEST_PASSWORD))
                .phone("0912345679")
                .status(UserStatus.ACTIVE)
                .isIdentityVerified(true)
                .isBusinessVerified(true)
                .businessLicenseUrl("business-license/already-verified.jpg")
                .language("vi")
                .roles(new HashSet<>(Set.of(userRole)))
                .build();
        userRepository.save(user);

        String accessToken = loginAndGetAccessToken();

        mockMvc.perform(multipart("/api/users/me/roles/host")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.roles", org.hamcrest.Matchers.hasItem("HOST")));

        Optional<User> persisted = userRepository.findByEmail(testEmail);
        assertThat(persisted).isPresent();
        assertThat(persisted.get().getRoles())
                .extracting(Role::getName)
                .contains("HOST");
    }
}
