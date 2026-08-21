package com.nhom7.coworkingspace.controller;

import com.nhom7.coworkingspace.controller.api.UserController;
import com.nhom7.coworkingspace.dto.response.UserProfileResponse;
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

import static org.mockito.ArgumentMatchers.any;
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
}
