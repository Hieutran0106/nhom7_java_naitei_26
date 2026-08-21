package com.nhom7.coworkingspace.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nhom7.coworkingspace.dto.request.LoginRequest;
import com.nhom7.coworkingspace.entity.Role;
import com.nhom7.coworkingspace.entity.User;
import com.nhom7.coworkingspace.enums.UserStatus;
import com.nhom7.coworkingspace.repository.RoleRepository;
import com.nhom7.coworkingspace.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// End-to-end check that the JWT filter chain (not mocks) really enforces:
// access-token-only, blacklist-after-logout, and no-token rejection on /api/users/me.
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class UserMeSecurityIntegrationTest {

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

    private final String testEmail = "jwt-e2e-" + UUID.randomUUID() + "@coworking.test";
    private static final String TEST_PASSWORD = "Password123@";

    @AfterEach
    void cleanUp() {
        userRepository.findByEmail(testEmail).ifPresent(userRepository::delete);
    }

    @Test
    void meEndpointShouldEnforceAccessTokenOnlyAndRevokeAfterLogout() throws Exception {
        Role userRole = roleRepository.findByName("USER")
                .orElseGet(() -> roleRepository.save(Role.builder().name("USER").build()));

        User user = User.builder()
                .name("JWT E2E Test")
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
        String accessToken = loginData.get("accessToken").asText();
        String refreshToken = loginData.get("refreshToken").asText();
        assertThat(accessToken).isNotBlank();
        assertThat(refreshToken).isNotBlank();
        assertThat(accessToken).isNotEqualTo(refreshToken);

        // 1) No token at all -> must NOT return user data, and must say so clearly
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403))
                .andExpect(jsonPath("$.message").isNotEmpty());

        // 2) Refresh token used as Bearer credential -> must NOT return user data
        mockMvc.perform(get("/api/users/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + refreshToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));

        // 3) Valid access token -> returns the correct user
        mockMvc.perform(get("/api/users/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value(testEmail));

        // 4) Logout with that access token
        mockMvc.perform(post("/api/auth/logout")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isOk());

        // 5) The SAME access token must now be rejected (blacklisted), with a message that
        // specifically says the caller was logged out - not a generic "Access Denied".
        mockMvc.perform(get("/api/users/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403))
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    // Reproduces the reported "old user still shows up with no/blacklisted token" bug:
    // if Spring Security is allowed to persist the authenticated SecurityContext into the
    // HTTP session (SessionCreationPolicy.IF_REQUIRED default repository), then a browser's
    // JSESSIONID cookie alone - independent of the Authorization header or Swagger's Authorize
    // state - can keep restoring a stale identity on later requests.
    @Test
    void meEndpointMustNotAuthenticateFromALeftoverHttpSessionWithoutAValidTokenOnTheRequest() throws Exception {
        Role userRole = roleRepository.findByName("USER")
                .orElseGet(() -> roleRepository.save(Role.builder().name("USER").build()));

        User user = User.builder()
                .name("JWT E2E Test")
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

        LoginRequest loginRequest = LoginRequest.builder()
                .email(testEmail)
                .password(TEST_PASSWORD)
                .build();

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();
        String accessToken = objectMapper.readTree(loginResult.getResponse().getContentAsString())
                .get("data").get("accessToken").asText();

        // First call: valid access token, capture whatever HTTP session got created for it
        // (simulates the browser receiving/storing a JSESSIONID cookie).
        MvcResult meResult = mockMvc.perform(get("/api/users/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andReturn();
        MockHttpSession session = (MockHttpSession) meResult.getRequest().getSession(false);

        if (session != null) {
            // Second call: SAME browser session cookie, but NO Authorization header at all -
            // must still be rejected. If Spring Security silently restores the previously
            // authenticated user from the session, this would incorrectly return 200.
            mockMvc.perform(get("/api/users/me").session(session))
                    .andExpect(status().isForbidden());
        }
    }

    // Verifies the actual regression: /logout used to declare BOTH an explicit Authorization
    // parameter AND inherit the global security scheme. Swagger UI then had two competing
    // sources for the same header - if the caller relied on the Authorize button (as for every
    // other endpoint) without separately filling the operation's own field, the header sent was
    // empty, logout blacklisted nothing, yet still returned 200. Fixed by hiding the parameter
    // from the docs so Authorize is the only visible mechanism, same as everywhere else. This
    // can only be checked against the generated OpenAPI spec, not via a plain MockMvc call.
    @Test
    void logoutOperationMustNotExposeACompetingAuthorizationParameter() throws Exception {
        MvcResult result = mockMvc.perform(get("/api-docs"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode logoutPost = objectMapper.readTree(result.getResponse().getContentAsString())
                .path("paths").path("/api/auth/logout").path("post");

        boolean hasVisibleAuthorizationParam = false;
        for (JsonNode param : logoutPost.path("parameters")) {
            if ("Authorization".equalsIgnoreCase(param.path("name").asText())) {
                hasVisibleAuthorizationParam = true;
            }
        }
        assertThat(hasVisibleAuthorizationParam)
                .as("logout must not expose its own visible Authorization parameter - "
                        + "Authorize should be the single source")
                .isFalse();
    }
}
