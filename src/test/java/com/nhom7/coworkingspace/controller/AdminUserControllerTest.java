package com.nhom7.coworkingspace.controller;

import com.nhom7.coworkingspace.config.JwtProperties;
import com.nhom7.coworkingspace.controller.api.AdminUserController;
import com.nhom7.coworkingspace.dto.response.UpdateUserRoleResponse;
import com.nhom7.coworkingspace.security.CustomUserDetailsService;
import com.nhom7.coworkingspace.security.JwtAuthenticationFilter;
import com.nhom7.coworkingspace.security.JwtTokenProvider;
import com.nhom7.coworkingspace.service.TokenBlacklistService;
import com.nhom7.coworkingspace.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Set;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminUserController.class)
@EnableMethodSecurity
@Import({
        JwtAuthenticationFilter.class,
        JwtProperties.class
})
@DisplayName("AdminUserController - WebMvc & Security Tests")
class AdminUserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @MockBean
    private TokenBlacklistService tokenBlacklistService;

    @Test
    @DisplayName("Unauthenticated -> PUT role returns 401")
    void givenUnauthenticated_whenChangeRole_thenReturn401()
            throws Exception {

        mockMvc.perform(
                        put("/api/admin/users/5/role")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "role": "MODERATOR"
                                        }
                                        """)
                )
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(userService);
    }

    @Test
    @WithMockUser(
            username = "user@test.com",
            roles = {"USER"}
    )
    @DisplayName("USER -> PUT role returns 403")
    void givenUserRole_whenChangeRole_thenReturn403()
            throws Exception {

        mockMvc.perform(
                        put("/api/admin/users/5/role")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "role": "MODERATOR"
                                        }
                                        """)
                )
                .andExpect(status().isForbidden());

        verifyNoInteractions(userService);
    }

    @Test
    @WithMockUser(
            username = "moderator@test.com",
            roles = {"MODERATOR"}
    )
    @DisplayName("MODERATOR -> PUT role returns 403")
    void givenModeratorRole_whenChangeRole_thenReturn403()
            throws Exception {

        mockMvc.perform(
                        put("/api/admin/users/5/role")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "role": "MODERATOR"
                                        }
                                        """)
                )
                .andExpect(status().isForbidden());

        verifyNoInteractions(userService);
    }

    @Test
    @WithMockUser(
            username = "admin@test.com",
            roles = {"ADMIN"}
    )
    @DisplayName("Blank role -> returns 400")
    void givenBlankRole_whenChangeRole_thenReturn400()
            throws Exception {

        mockMvc.perform(
                        put("/api/admin/users/5/role")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "role": ""
                                        }
                                        """)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.data.role").exists());

        verifyNoInteractions(userService);
    }

    @Test
    @WithMockUser(
            username = "admin@test.com",
            roles = {"ADMIN"}
    )
    @DisplayName("ADMIN -> change role returns standard ApiResponse")
    void givenAdminRole_whenChangeRole_thenReturn200()
            throws Exception {

        UpdateUserRoleResponse response =
                UpdateUserRoleResponse.builder()
                        .id(5L)
                        .name("Target User")
                        .email("target@test.com")
                        .roles(
                                Set.of(
                                        "USER",
                                        "MODERATOR"
                                )
                        )
                        .build();

        given(
                userService.changeRole(
                        5L,
                        "MODERATOR"
                )
        ).willReturn(response);

        mockMvc.perform(
                        put("/api/admin/users/5/role")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "role": "MODERATOR"
                                        }
                                        """)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.data.id").value(5))
                .andExpect(
                        jsonPath("$.data.email")
                                .value("target@test.com")
                )
                .andExpect(jsonPath("$.data.roles").isArray())
                .andExpect(jsonPath("$.timestamp").exists());

        verify(userService)
                .changeRole(
                        5L,
                        "MODERATOR"
                );
    }
}