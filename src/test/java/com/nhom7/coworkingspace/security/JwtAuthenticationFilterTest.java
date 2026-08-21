package com.nhom7.coworkingspace.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import com.nhom7.coworkingspace.service.TokenBlacklistService;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

// Unit tests for {@link JwtAuthenticationFilter}.

@ExtendWith(MockitoExtension.class)
@DisplayName("JwtAuthenticationFilter - Unit Tests")
class JwtAuthenticationFilterTest {

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private CustomUserDetailsService userDetailsService;

    @Mock
    private TokenBlacklistService tokenBlacklistService;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private JwtAuthenticationFilter filter;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    private static final String VALID_TOKEN = "valid.jwt.token";
    private static final String INVALID_TOKEN = "invalid.jwt.token";
    private static final String USER_EMAIL = "user@coworking.test";

    private UserDetails buildUserDetails() {
        return User.withUsername(USER_EMAIL)
                .password("{noop}secret")
                .authorities(new SimpleGrantedAuthority("ROLE_USER"))
                .build();
    }

    @BeforeEach
    void setUp() {
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
    }

    // Ensure SecurityContext is clean between tests to avoid state leakage.
    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // ============================================================
    // Group 1: No Authorization header
    // ============================================================

    @Nested
    @DisplayName("Request without Authorization header")
    class NoAuthorizationHeader {

        @Test
        @DisplayName("Should NOT set authentication in SecurityContext")
        void givenNoHeader_whenFilter_thenSecurityContextIsEmpty()
                throws ServletException, IOException {
            filter.doFilter(request, response, filterChain);
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        }

        @Test
        @DisplayName("Should always call filterChain.doFilter()")
        void givenNoHeader_whenFilter_thenFilterChainIsCalled()
                throws ServletException, IOException {
            filter.doFilter(request, response, filterChain);
            verify(filterChain).doFilter(request, response);
        }

        @Test
        @DisplayName("Should NOT call validateToken() when header is absent")
        void givenNoHeader_whenFilter_thenValidateTokenIsNeverCalled()
                throws ServletException, IOException {
            filter.doFilter(request, response, filterChain);
            verify(jwtTokenProvider, never()).validateToken(anyString());
        }
    }

    // ============================================================
    // Group 2: Malformed / wrong-prefix header
    // ============================================================

    @Nested
    @DisplayName("Request with malformed / wrong-prefix Authorization header")
    class MalformedAuthorizationHeader {

        @Test
        @DisplayName("Header 'Token abc' (no Bearer prefix) -> no authentication")
        void givenWrongPrefix_whenFilter_thenSecurityContextIsEmpty()
                throws ServletException, IOException {
            request.addHeader("Authorization", "Token " + VALID_TOKEN);
            filter.doFilter(request, response, filterChain);
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
            verify(jwtTokenProvider, never()).validateToken(anyString());
        }

        @Test
        @DisplayName("Header 'Bearer' with empty value -> no authentication")
        void givenBearerWithoutToken_whenFilter_thenSecurityContextIsEmpty()
                throws ServletException, IOException {
            request.addHeader("Authorization", "Bearer ");
            filter.doFilter(request, response, filterChain);
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        }
    }

    // ============================================================
    // Group 3: Invalid / expired token
    // ============================================================

    @Nested
    @DisplayName("Request with invalid / expired token")
    class InvalidToken {

        @Test
        @DisplayName("validateToken() returns false -> SecurityContext stays empty")
        void givenInvalidToken_whenFilter_thenSecurityContextIsEmpty()
                throws ServletException, IOException {
            request.addHeader("Authorization", "Bearer " + INVALID_TOKEN);
            given(jwtTokenProvider.validateToken(INVALID_TOKEN)).willReturn(false);
            filter.doFilter(request, response, filterChain);
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        }

        @Test
        @DisplayName("validateToken() returns false -> loadUserByUsername is NEVER called")
        void givenInvalidToken_whenFilter_thenUserDetailsServiceIsNeverCalled()
                throws ServletException, IOException {
            request.addHeader("Authorization", "Bearer " + INVALID_TOKEN);
            given(jwtTokenProvider.validateToken(INVALID_TOKEN)).willReturn(false);
            filter.doFilter(request, response, filterChain);
            verify(userDetailsService, never()).loadUserByUsername(anyString());
        }

        @Test
        @DisplayName("Even with invalid token, filterChain.doFilter() is still called")
        void givenInvalidToken_whenFilter_thenFilterChainIsCalled()
                throws ServletException, IOException {
            request.addHeader("Authorization", "Bearer " + INVALID_TOKEN);
            given(jwtTokenProvider.validateToken(INVALID_TOKEN)).willReturn(false);
            filter.doFilter(request, response, filterChain);
            verify(filterChain).doFilter(request, response);
        }
    }

    // ============================================================
    // Group 4: Valid token
    // ============================================================

    @Nested
    @DisplayName("Request with a valid token")
    class ValidToken {

        @Test
        @DisplayName("Should set Authentication in SecurityContext")
        void givenValidToken_whenFilter_thenAuthenticationIsSet()
                throws ServletException, IOException {
            request.addHeader("Authorization", "Bearer " + VALID_TOKEN);
            given(jwtTokenProvider.validateToken(VALID_TOKEN)).willReturn(true);
            given(jwtTokenProvider.extractUsername(VALID_TOKEN)).willReturn(USER_EMAIL);
            given(userDetailsService.loadUserByUsername(USER_EMAIL)).willReturn(buildUserDetails());

            filter.doFilter(request, response, filterChain);

            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            assertThat(auth).isNotNull();
            assertThat(auth.isAuthenticated()).isTrue();
        }

        @Test
        @DisplayName("Authentication principal should be the UserDetails object")
        void givenValidToken_whenFilter_thenPrincipalIsCorrect()
                throws ServletException, IOException {
            UserDetails userDetails = buildUserDetails();
            request.addHeader("Authorization", "Bearer " + VALID_TOKEN);
            given(jwtTokenProvider.validateToken(VALID_TOKEN)).willReturn(true);
            given(jwtTokenProvider.extractUsername(VALID_TOKEN)).willReturn(USER_EMAIL);
            given(userDetailsService.loadUserByUsername(USER_EMAIL)).willReturn(userDetails);

            filter.doFilter(request, response, filterChain);

            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            assertThat(auth.getPrincipal()).isEqualTo(userDetails);
        }

        @Test
        @DisplayName("Authentication should carry the roles from UserDetails")
        void givenValidToken_whenFilter_thenAuthoritiesArePresent()
                throws ServletException, IOException {
            request.addHeader("Authorization", "Bearer " + VALID_TOKEN);
            given(jwtTokenProvider.validateToken(VALID_TOKEN)).willReturn(true);
            given(jwtTokenProvider.extractUsername(VALID_TOKEN)).willReturn(USER_EMAIL);
            given(userDetailsService.loadUserByUsername(USER_EMAIL)).willReturn(buildUserDetails());

            filter.doFilter(request, response, filterChain);

            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            assertThat(auth.getAuthorities())
                    .extracting("authority")
                    .containsExactly("ROLE_USER");
        }

        @Test
        @DisplayName("Credentials in Authentication should be null (JWT - no password needed)")
        void givenValidToken_whenFilter_thenCredentialsAreNull()
                throws ServletException, IOException {
            request.addHeader("Authorization", "Bearer " + VALID_TOKEN);
            given(jwtTokenProvider.validateToken(VALID_TOKEN)).willReturn(true);
            given(jwtTokenProvider.extractUsername(VALID_TOKEN)).willReturn(USER_EMAIL);
            given(userDetailsService.loadUserByUsername(USER_EMAIL)).willReturn(buildUserDetails());

            filter.doFilter(request, response, filterChain);

            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            assertThat(auth.getCredentials()).isNull();
        }

        @Test
        @DisplayName("filterChain.doFilter() is still called after successful authentication")
        void givenValidToken_whenFilter_thenFilterChainIsCalled()
                throws ServletException, IOException {
            request.addHeader("Authorization", "Bearer " + VALID_TOKEN);
            given(jwtTokenProvider.validateToken(VALID_TOKEN)).willReturn(true);
            given(jwtTokenProvider.extractUsername(VALID_TOKEN)).willReturn(USER_EMAIL);
            given(userDetailsService.loadUserByUsername(USER_EMAIL)).willReturn(buildUserDetails());

            filter.doFilter(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
        }
    }

    // ============================================================
    // Group 5: Multiple roles
    // ============================================================

    @Nested
    @DisplayName("User with multiple roles")
    class MultipleRoles {

        @Test
        @DisplayName("All roles from UserDetails should appear in Authentication")
        void givenValidToken_withMultipleRoles_thenAllAuthoritiesSet()
                throws ServletException, IOException {
            UserDetails adminUser = User.withUsername(USER_EMAIL)
                    .password("{noop}secret")
                    .authorities(
                            new SimpleGrantedAuthority("ROLE_USER"),
                            new SimpleGrantedAuthority("ROLE_ADMIN"))
                    .build();

            request.addHeader("Authorization", "Bearer " + VALID_TOKEN);
            given(jwtTokenProvider.validateToken(VALID_TOKEN)).willReturn(true);
            given(jwtTokenProvider.extractUsername(VALID_TOKEN)).willReturn(USER_EMAIL);
            given(userDetailsService.loadUserByUsername(USER_EMAIL)).willReturn(adminUser);

            filter.doFilter(request, response, filterChain);

            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            assertThat(auth.getAuthorities())
                    .extracting("authority")
                    .containsExactlyInAnyOrder("ROLE_USER", "ROLE_ADMIN");
        }
    }

    // ============================================================
    // Group 6: Blacklisted and Revoked Tokens
    // ============================================================

    @Nested
    @DisplayName("Request with blacklisted or revoked token")
    class BlacklistedAndRevokedTokens {

        @Test
        @DisplayName("Token in blacklist -> SecurityContext stays empty")
        void givenBlacklistedToken_whenFilter_thenSecurityContextIsEmpty()
                throws ServletException, IOException {
            request.addHeader("Authorization", "Bearer " + VALID_TOKEN);
            given(jwtTokenProvider.validateToken(VALID_TOKEN)).willReturn(true);
            given(tokenBlacklistService.isBlacklisted(VALID_TOKEN)).willReturn(true);

            filter.doFilter(request, response, filterChain);

            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
            verify(filterChain).doFilter(request, response);
            verify(userDetailsService, never()).loadUserByUsername(anyString());
        }

        @Test
        @DisplayName("Token revoked due to password reset -> SecurityContext stays empty")
        void givenRevokedUserToken_whenFilter_thenSecurityContextIsEmpty()
                throws ServletException, IOException {
            java.util.Date issuedAt = new java.util.Date(1000L);
            request.addHeader("Authorization", "Bearer " + VALID_TOKEN);
            given(jwtTokenProvider.validateToken(VALID_TOKEN)).willReturn(true);
            given(tokenBlacklistService.isBlacklisted(VALID_TOKEN)).willReturn(false);
            given(jwtTokenProvider.extractUsername(VALID_TOKEN)).willReturn(USER_EMAIL);
            given(jwtTokenProvider.extractIssuedAt(VALID_TOKEN)).willReturn(issuedAt);
            given(tokenBlacklistService.isUserTokenRevoked(USER_EMAIL, issuedAt)).willReturn(true);

            filter.doFilter(request, response, filterChain);

            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
            verify(filterChain).doFilter(request, response);
            verify(userDetailsService, never()).loadUserByUsername(anyString());
        }
    }
}