package com.nhom7.coworkingspace.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nhom7.coworkingspace.controller.api.AuthController;
import com.nhom7.coworkingspace.dto.request.LoginRequest;
import com.nhom7.coworkingspace.dto.response.LoginResponse;
import com.nhom7.coworkingspace.dto.response.SignupResponse;
import com.nhom7.coworkingspace.enums.UserStatus;
import com.nhom7.coworkingspace.exception.GlobalExceptionHandler;
import com.nhom7.coworkingspace.service.AuthService;
import com.nhom7.coworkingspace.service.OtpService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Locale;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthController - Unit Tests")
class AuthControllerTest {

        @Mock
        private AuthService authService;

        @Mock
        private MessageSource messageSource;

        @Mock
        private OtpService otpService;

        @InjectMocks
        private AuthController authController;

        private MockMvc mockMvc;
        private ObjectMapper objectMapper;

        @BeforeEach
        void setUp() {
                GlobalExceptionHandler exceptionHandler = new GlobalExceptionHandler(messageSource);
                mockMvc = MockMvcBuilders.standaloneSetup(authController)
                                .setControllerAdvice(exceptionHandler)
                                .build();
                objectMapper = new ObjectMapper();
        }

        @Nested
        @DisplayName("POST /api/auth/signup")
        class SignupEndpointTests {

                @Test
                @DisplayName("Should return 201 CREATED when signup request is valid")
                void shouldReturn201WhenSignupValid() throws Exception {
                        MockMultipartFile mockFile = new MockMultipartFile(
                                        "cccdImage",
                                        "cccd.jpg",
                                        "image/jpeg",
                                        "sample-image-content".getBytes());

                        SignupResponse signupResponse = SignupResponse.builder()
                                        .id(1L)
                                        .name("Nguyen Van A")
                                        .email("user@example.com")
                                        .phone("0912345678")
                                        .status(UserStatus.INACTIVE)
                                        .roles(Set.of("USER"))
                                        .cccdUrl("cccd/uuid.jpg")
                                        .build();

                        given(authService.signup(any())).willReturn(signupResponse);
                        given(messageSource.getMessage(eq("user.created"), any(), any(Locale.class)))
                                        .willReturn("User registered successfully");

                        mockMvc.perform(multipart("/api/auth/signup")
                                        .file(mockFile)
                                        .param("name", "Nguyen Van A")
                                        .param("email", "user@example.com")
                                        .param("password", "Password123@")
                                        .param("phone", "0912345678"))
                                        .andExpect(status().isCreated())
                                        .andExpect(jsonPath("$.code").value(201))
                                        .andExpect(jsonPath("$.message").value("User registered successfully"))
                                        .andExpect(jsonPath("$.data.email").value("user@example.com"))
                                        .andExpect(jsonPath("$.data.name").value("Nguyen Van A"));
                }

                @Test
                @DisplayName("Should return 400 BAD REQUEST when signup input is invalid")
                void shouldReturn400WhenSignupInvalid() throws Exception {
                        given(messageSource.getMessage(any(), any(Locale.class)))
                                        .willReturn("Validation error");

                        mockMvc.perform(multipart("/api/auth/signup")
                                        .param("name", "") // Blank name
                                        .param("email", "not-an-email")
                                        .param("password", "short")
                                        .param("phone", "123"))
                                        .andExpect(status().isBadRequest())
                                        .andExpect(jsonPath("$.code").value(400));
                }
        }

        @Nested
        @DisplayName("POST /api/auth/login")
        class LoginEndpointTests {

                @Test
                @DisplayName("Should return 200 OK with LoginResponse when credentials are valid")
                void shouldReturn200WhenLoginValid() throws Exception {
                        LoginRequest request = LoginRequest.builder()
                                        .email("user@example.com")
                                        .password("Password123@")
                                        .build();

                        LoginResponse loginResponse = LoginResponse.builder()
                                        .accessToken("access.token.sample")
                                        .refreshToken("refresh.token.sample")
                                        .tokenType("Bearer")
                                        .id(1L)
                                        .name("Nguyen Van A")
                                        .email("user@example.com")
                                        .roles(Set.of("USER"))
                                        .build();

                        given(authService.login(any(LoginRequest.class))).willReturn(loginResponse);
                        given(messageSource.getMessage(eq("auth.login.success"), any(), any(Locale.class)))
                                        .willReturn("Login successful");

                        mockMvc.perform(post("/api/auth/login")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(request)))
                                        .andExpect(status().isOk())
                                        .andExpect(jsonPath("$.code").value(200))
                                        .andExpect(jsonPath("$.message").value("Login successful"))
                                        .andExpect(jsonPath("$.data.accessToken").value("access.token.sample"))
                                        .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                                        .andExpect(jsonPath("$.data.email").value("user@example.com"));
                }

                @Test
                @DisplayName("Should return 400 BAD REQUEST when login input is invalid")
                void shouldReturn400WhenLoginInvalid() throws Exception {
                        LoginRequest request = LoginRequest.builder()
                                        .email("invalid-email")
                                        .password("")
                                        .build();

                        given(messageSource.getMessage(any(), any(Locale.class)))
                                        .willReturn("Validation error");

                        mockMvc.perform(post("/api/auth/login")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(request)))
                                        .andExpect(status().isBadRequest())
                                        .andExpect(jsonPath("$.code").value(400));
                }
        }

        @Nested
        @DisplayName("POST /api/auth/logout")
        class LogoutEndpointTests {

                @Test
                @DisplayName("Should return 200 OK when logout is called with Authorization header")
                void shouldReturn200WhenLogoutCalled() throws Exception {
                        given(messageSource.getMessage(eq("auth.logout.success"), any(), any(Locale.class)))
                                        .willReturn("Logout successful");

                        mockMvc.perform(post("/api/auth/logout")
                                        .header(HttpHeaders.AUTHORIZATION, "Bearer valid.jwt.token"))
                                        .andExpect(status().isOk())
                                        .andExpect(jsonPath("$.code").value(200))
                                        .andExpect(jsonPath("$.message").value("Logout successful"));

                        verify(authService).logout("Bearer valid.jwt.token");
                }
        }

        @Nested
        @DisplayName("OTP email endpoints")
        class OtpEmailEndpointTests {

                @Test
                void sendConfirmationShouldReturnAccepted() throws Exception {
                        mockMvc.perform(post("/api/auth/send-confirm")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content("{\"email\":\"user@coworking.test\"}"))
                                        .andExpect(status().isAccepted());

                        verify(otpService).sendConfirmationOtp("user@coworking.test");
                }

                @Test
                void sendConfirmationShouldRejectInvalidEmail() throws Exception {
                        mockMvc.perform(post("/api/auth/send-confirm")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content("{\"email\":\"not-an-email\"}"))
                                        .andExpect(status().isBadRequest());

                        verifyNoInteractions(otpService);
                }

                @Test
                void forgotPasswordShouldReturnAccepted() throws Exception {
                        mockMvc.perform(post("/api/auth/forgot-password")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content("{\"email\":\"active@coworking.test\"}"))
                                        .andExpect(status().isAccepted());

                        verify(otpService).sendPasswordResetOtp("active@coworking.test");
                }
        }
}
