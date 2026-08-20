package com.nhom7.coworkingspace.service;

import com.nhom7.coworkingspace.dto.request.LoginRequest;
import com.nhom7.coworkingspace.dto.request.SignupRequest;
import com.nhom7.coworkingspace.dto.response.LoginResponse;
import com.nhom7.coworkingspace.dto.response.SignupResponse;
import com.nhom7.coworkingspace.entity.Role;
import com.nhom7.coworkingspace.entity.User;
import com.nhom7.coworkingspace.enums.UserStatus;
import com.nhom7.coworkingspace.exception.AppException;
import com.nhom7.coworkingspace.repository.RoleRepository;
import com.nhom7.coworkingspace.repository.UserRepository;
import com.nhom7.coworkingspace.security.JwtTokenProvider;
import com.nhom7.coworkingspace.service.impl.AuthServiceImpl;
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
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Date;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthServiceImpl - Unit Tests")
class AuthServiceImplTest {

        @Mock
        private UserRepository userRepository;

        @Mock
        private RoleRepository roleRepository;

        @Mock
        private FileStorageService fileStorageService;

        @Mock
        private PasswordEncoder passwordEncoder;

        @Mock
        private AuthenticationManager authenticationManager;

        @Mock
        private JwtTokenProvider jwtTokenProvider;

        @Mock
        private TokenBlacklistService tokenBlacklistService;

        @InjectMocks
        private AuthServiceImpl authService;

        private Role userRole;
        private MockMultipartFile mockCccdFile;

        @BeforeEach
        void setUp() {
                userRole = Role.builder().id(1L).name("USER").build();
                mockCccdFile = new MockMultipartFile(
                                "cccdImage",
                                "cccd.jpg",
                                "image/jpeg",
                                "test-image-content".getBytes());
        }

        @Nested
        @DisplayName("Signup Tests")
        class SignupTests {

                @Test
                @DisplayName("Should successfully signup a new user with normalized name and hashed password")
                void shouldSignupSuccessfully() {
                        SignupRequest request = SignupRequest.builder()
                                        .name("  nguyen   van   a  ")
                                        .email("TEST@EXAMPLE.COM")
                                        .password("Password123@")
                                        .phone(" 0912345678 ")
                                        .cccdImage(mockCccdFile)
                                        .build();

                        User savedUser = User.builder()
                                        .id(100L)
                                        .name("Nguyen Van A")
                                        .email("test@example.com")
                                        .password("hashed_password")
                                        .phone("0912345678")
                                        .status(UserStatus.INACTIVE)
                                        .isIdentityVerified(false)
                                        .isBusinessVerified(false)
                                        .language("vi")
                                        .cccdUrl("cccd/uuid.jpg")
                                        .roles(new HashSet<>(Set.of(userRole)))
                                        .build();

                        given(userRepository.existsByEmail("TEST@EXAMPLE.COM")).willReturn(false);
                        given(fileStorageService.storeFile(mockCccdFile, "cccd")).willReturn("cccd/uuid.jpg");
                        given(roleRepository.findByName("USER")).willReturn(Optional.of(userRole));
                        given(passwordEncoder.encode("Password123@")).willReturn("hashed_password");
                        given(userRepository.save(any(User.class))).willReturn(savedUser);

                        SignupResponse response = authService.signup(request);

                        assertThat(response).isNotNull();
                        assertThat(response.getId()).isEqualTo(100L);
                        assertThat(response.getName()).isEqualTo("Nguyen Van A");
                        assertThat(response.getEmail()).isEqualTo("test@example.com");
                        assertThat(response.getStatus()).isEqualTo(UserStatus.INACTIVE);
                        assertThat(response.getRoles()).containsExactly("USER");
                        assertThat(response.getCccdUrl()).isEqualTo("cccd/uuid.jpg");

                        verify(userRepository).save(argThat(user -> user.getName().equals("Nguyen Van A") &&
                                        user.getEmail().equals("test@example.com") &&
                                        user.getStatus() == UserStatus.INACTIVE &&
                                        user.getPassword().equals("hashed_password")));
                }

                @Test
                @DisplayName("Should throw AppException when email already exists")
                void shouldThrowExceptionWhenEmailExists() {
                        SignupRequest request = SignupRequest.builder()
                                        .email("existing@example.com")
                                        .build();

                        given(userRepository.existsByEmail("existing@example.com")).willReturn(true);

                        assertThatThrownBy(() -> authService.signup(request))
                                        .isInstanceOf(AppException.class)
                                        .satisfies(ex -> {
                                                AppException appEx = (AppException) ex;
                                                assertThat(appEx.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                                                assertThat(appEx.getMessageKey()).isEqualTo("user.email.exists");
                                        });

                        verify(fileStorageService, never()).storeFile(any(), anyString());
                        verify(userRepository, never()).save(any());
                }

                @Test
                @DisplayName("Should create USER role if it does not exist in repository")
                void shouldCreateRoleWhenNotFound() {
                        SignupRequest request = SignupRequest.builder()
                                        .name("Tran Thi B")
                                        .email("tranb@example.com")
                                        .password("Password123@")
                                        .phone("0987654321")
                                        .cccdImage(mockCccdFile)
                                        .build();

                        User savedUser = User.builder()
                                        .id(101L)
                                        .name("Tran Thi B")
                                        .email("tranb@example.com")
                                        .password("hashed_password")
                                        .phone("0987654321")
                                        .status(UserStatus.INACTIVE)
                                        .roles(new HashSet<>(Set.of(userRole)))
                                        .build();

                        given(userRepository.existsByEmail("tranb@example.com")).willReturn(false);
                        given(fileStorageService.storeFile(mockCccdFile, "cccd")).willReturn("cccd/uuid2.jpg");
                        given(roleRepository.findByName("USER")).willReturn(Optional.empty());
                        given(roleRepository.save(any(Role.class))).willReturn(userRole);
                        given(passwordEncoder.encode("Password123@")).willReturn("hashed_password");
                        given(userRepository.save(any(User.class))).willReturn(savedUser);

                        SignupResponse response = authService.signup(request);

                        assertThat(response).isNotNull();
                        verify(roleRepository).save(any(Role.class));
                }
        }

        @Nested
        @DisplayName("Login Tests")
        class LoginTests {

                @Test
                @DisplayName("Should successfully login and return LoginResponse with JWT tokens")
                void shouldLoginSuccessfully() {
                        LoginRequest request = LoginRequest.builder()
                                        .email("user@example.com")
                                        .password("Password123@")
                                        .build();

                        Authentication auth = mock(Authentication.class);
                        User user = User.builder()
                                        .id(1L)
                                        .name("Nguyen Van A")
                                        .email("user@example.com")
                                        .phone("0912345678")
                                        .status(UserStatus.ACTIVE)
                                        .isIdentityVerified(true)
                                        .language("vi")
                                        .roles(new HashSet<>(Set.of(userRole)))
                                        .build();

                        given(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                                        .willReturn(auth);
                        given(jwtTokenProvider.generateAccessToken(auth)).willReturn("mock.access.token");
                        given(jwtTokenProvider.generateRefreshToken(auth)).willReturn("mock.refresh.token");
                        given(userRepository.findByEmail("user@example.com")).willReturn(Optional.of(user));

                        LoginResponse response = authService.login(request);

                        assertThat(response).isNotNull();
                        assertThat(response.getAccessToken()).isEqualTo("mock.access.token");
                        assertThat(response.getRefreshToken()).isEqualTo("mock.refresh.token");
                        assertThat(response.getTokenType()).isEqualTo("Bearer");
                        assertThat(response.getEmail()).isEqualTo("user@example.com");
                        assertThat(response.getRoles()).containsExactly("USER");
                }

                @Test
                @DisplayName("Should throw BadCredentialsException when authentication fails")
                void shouldThrowExceptionWhenBadCredentials() {
                        LoginRequest request = LoginRequest.builder()
                                        .email("user@example.com")
                                        .password("wrongpassword")
                                        .build();

                        given(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                                        .willThrow(new BadCredentialsException("Bad credentials"));

                        assertThatThrownBy(() -> authService.login(request))
                                        .isInstanceOf(BadCredentialsException.class);

                        verify(jwtTokenProvider, never()).generateAccessToken(any());
                }

                @Test
                @DisplayName("Should throw AppException when user is not found in database after authentication")
                void shouldThrowExceptionWhenUserNotFoundAfterAuth() {
                        LoginRequest request = LoginRequest.builder()
                                        .email("user@example.com")
                                        .password("Password123@")
                                        .build();

                        Authentication auth = mock(Authentication.class);

                        given(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                                        .willReturn(auth);
                        given(jwtTokenProvider.generateAccessToken(auth)).willReturn("mock.access.token");
                        given(jwtTokenProvider.generateRefreshToken(auth)).willReturn("mock.refresh.token");
                        given(userRepository.findByEmail("user@example.com")).willReturn(Optional.empty());

                        assertThatThrownBy(() -> authService.login(request))
                                        .isInstanceOf(AppException.class)
                                        .satisfies(ex -> {
                                                AppException appEx = (AppException) ex;
                                                assertThat(appEx.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
                                                assertThat(appEx.getMessageKey()).isEqualTo("auth.invalid.credentials");
                                        });
                }

                @Test
                @DisplayName("Should throw AppException with FORBIDDEN when user account is BLOCKED")
                void shouldThrowExceptionWhenUserIsBlocked() {
                        LoginRequest request = LoginRequest.builder()
                                        .email("blocked@example.com")
                                        .password("Password123@")
                                        .build();

                        Authentication auth = mock(Authentication.class);
                        User blockedUser = User.builder()
                                        .id(2L)
                                        .name("Blocked User")
                                        .email("blocked@example.com")
                                        .status(UserStatus.BLOCKED)
                                        .roles(new HashSet<>(Set.of(userRole)))
                                        .build();

                        given(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                                        .willReturn(auth);
                        given(jwtTokenProvider.generateAccessToken(auth)).willReturn("mock.access.token");
                        given(jwtTokenProvider.generateRefreshToken(auth)).willReturn("mock.refresh.token");
                        given(userRepository.findByEmail("blocked@example.com")).willReturn(Optional.of(blockedUser));

                        assertThatThrownBy(() -> authService.login(request))
                                        .isInstanceOf(AppException.class)
                                        .satisfies(ex -> {
                                                AppException appEx = (AppException) ex;
                                                assertThat(appEx.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
                                                assertThat(appEx.getMessageKey()).isEqualTo("auth.account.blocked");
                                        });
                }
        }

        @Nested
        @DisplayName("Logout Tests")
        class LogoutTests {

                @Test
                @DisplayName("Should successfully blacklist valid Bearer token upon logout")
                void shouldLogoutSuccessfullyWithValidToken() {
                        String authHeader = "Bearer valid.jwt.token";
                        Date mockExpiry = new Date(System.currentTimeMillis() + 3600000);

                        given(jwtTokenProvider.validateToken("valid.jwt.token")).willReturn(true);
                        given(jwtTokenProvider.extractExpiration("valid.jwt.token")).willReturn(mockExpiry);

                        authService.logout(authHeader);

                        verify(tokenBlacklistService).blacklistToken("valid.jwt.token", mockExpiry);
                }

                @Test
                @DisplayName("Should not blacklist token if token is invalid")
                void shouldNotBlacklistInvalidToken() {
                        String authHeader = "Bearer invalid.jwt.token";

                        given(jwtTokenProvider.validateToken("invalid.jwt.token")).willReturn(false);

                        authService.logout(authHeader);

                        verify(tokenBlacklistService, never()).blacklistToken(anyString(), any());
                }

                @Test
                @DisplayName("Should handle null or non-Bearer auth header gracefully")
                void shouldHandleNullAuthHeader() {
                        authService.logout(null);
                        authService.logout("Basic dXNlcjpwYXNz");

                        verify(tokenBlacklistService, never()).blacklistToken(anyString(), any());
                        verify(jwtTokenProvider, never()).validateToken(anyString());
                }
        }
}
