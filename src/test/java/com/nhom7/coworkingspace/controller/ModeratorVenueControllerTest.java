package com.nhom7.coworkingspace.controller;

import com.nhom7.coworkingspace.config.JwtProperties;
import com.nhom7.coworkingspace.controller.api.ModeratorVenueController;
import com.nhom7.coworkingspace.dto.response.VenueResponse;
import com.nhom7.coworkingspace.enums.VenueStatus;
import com.nhom7.coworkingspace.exception.AppException;
import com.nhom7.coworkingspace.security.CustomUserDetailsService;
import com.nhom7.coworkingspace.security.JwtAuthenticationFilter;
import com.nhom7.coworkingspace.security.JwtTokenProvider;
import com.nhom7.coworkingspace.service.TokenBlacklistService;
import com.nhom7.coworkingspace.service.VenueService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ModeratorVenueController.class)
@EnableMethodSecurity
@Import({JwtAuthenticationFilter.class, JwtProperties.class})
@DisplayName("ModeratorVenueController - WebMvc & Security Tests")
class ModeratorVenueControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private VenueService venueService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @MockBean
    private TokenBlacklistService tokenBlacklistService;

    @Test
    @WithMockUser(username = "moderator@test.com", roles = {"MODERATOR"})
    @DisplayName("Authenticated MODERATOR -> PUT /api/moderator/venues/{id}/status returns 200 OK")
    void givenModeratorRole_whenUpdateVenueStatus_thenReturn200() throws Exception {
        VenueResponse response = VenueResponse.builder().id(1L).ownerId(10L).status(VenueStatus.APPROVE).build();

        given(venueService.updateVenueStatus(eq(1L), eq(VenueStatus.APPROVE), eq("moderator@test.com")))
                .willReturn(response);

        mockMvc.perform(put("/api/moderator/venues/1/status")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"APPROVE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.status").value("APPROVE"));
    }

    @Test
    @WithMockUser(username = "admin@test.com", roles = {"ADMIN"})
    @DisplayName("Authenticated ADMIN -> PUT /api/moderator/venues/{id}/status returns 200 OK")
    void givenAdminRole_whenUpdateVenueStatus_thenReturn200() throws Exception {
        VenueResponse response = VenueResponse.builder().id(1L).ownerId(10L).status(VenueStatus.BLOCKED).build();

        given(venueService.updateVenueStatus(eq(1L), eq(VenueStatus.BLOCKED), eq("admin@test.com")))
                .willReturn(response);

        mockMvc.perform(put("/api/moderator/venues/1/status")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"BLOCKED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("BLOCKED"));
    }

    @Test
    @WithMockUser(username = "host@test.com", roles = {"HOST"})
    @DisplayName("@PreAuthorize blocks HOST before the service is ever called (cannot self-moderate)")
    void givenHostRole_whenUpdateVenueStatus_thenServiceNeverInvoked() throws Exception {
        mockMvc.perform(put("/api/moderator/venues/1/status")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"APPROVE\"}"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(venueService);
    }

    @Test
    @DisplayName("Unauthenticated request -> PUT /api/moderator/venues/{id}/status returns 401 Unauthorized")
    void givenUnauthenticated_whenUpdateVenueStatus_thenReturn401() throws Exception {
        mockMvc.perform(put("/api/moderator/venues/1/status")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"APPROVE\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "moderator@test.com", roles = {"MODERATOR"})
    @DisplayName("Invalid status in body -> PUT /api/moderator/venues/{id}/status returns 400 Bad Request")
    void givenInvalidStatus_whenUpdateVenueStatus_thenReturn400() throws Exception {
        mockMvc.perform(put("/api/moderator/venues/1/status")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": null}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "moderator@test.com", roles = {"MODERATOR"})
    @DisplayName("Moderator moderating their own venue -> 403 with venue.cannot.moderate.self message")
    void givenModeratorOwnsVenue_whenUpdateVenueStatus_thenReturn403() throws Exception {
        given(venueService.updateVenueStatus(eq(1L), eq(VenueStatus.APPROVE), eq("moderator@test.com")))
                .willThrow(new AppException("venue.cannot.moderate.self", HttpStatus.FORBIDDEN));

        mockMvc.perform(put("/api/moderator/venues/1/status")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"APPROVE\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));
    }
}
