package com.nhom7.coworkingspace.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nhom7.coworkingspace.config.JwtProperties;
import com.nhom7.coworkingspace.controller.api.VenueController;
import com.nhom7.coworkingspace.dto.request.VenueRequest;
import com.nhom7.coworkingspace.dto.response.PageResponse;
import com.nhom7.coworkingspace.dto.response.VenueResponse;
import com.nhom7.coworkingspace.exception.AppException;
import com.nhom7.coworkingspace.exception.VenueNotFoundException;
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
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(VenueController.class)
@Import({JwtAuthenticationFilter.class, JwtProperties.class})
@DisplayName("VenueController - WebMvc & Security Tests")
class VenueControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private VenueService venueService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @MockBean
    private TokenBlacklistService tokenBlacklistService;

    @Test
    @WithMockUser(username = "host@test.com", roles = {"HOST"})
    @DisplayName("Authenticated HOST -> POST /api/venues returns 201 Created")
    void givenHost_whenCreateVenue_thenReturn201() throws Exception {
        VenueRequest request = VenueRequest.builder().name("Innovation Hub").city("Hanoi").build();
        VenueResponse response = VenueResponse.builder().id(1L).ownerId(10L).name("Innovation Hub").build();

        given(venueService.createVenue(any(VenueRequest.class), eq("host@test.com"))).willReturn(response);

        mockMvc.perform(post("/api/venues")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value(201))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.ownerId").value(10));
    }

    @Test
    @DisplayName("Unauthenticated request -> POST /api/venues returns 401 Unauthorized")
    void givenUnauthenticated_whenCreateVenue_thenReturn401() throws Exception {
        VenueRequest request = VenueRequest.builder().name("Innovation Hub").build();

        mockMvc.perform(post("/api/venues")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "user@test.com", roles = {"USER"})
    @DisplayName("Non-HOST user -> POST /api/venues returns 403 with error body")
    void givenNonHost_whenCreateVenue_thenReturn403() throws Exception {
        VenueRequest request = VenueRequest.builder().name("Innovation Hub").build();

        given(venueService.createVenue(any(VenueRequest.class), eq("user@test.com")))
                .willThrow(new AppException("venue.host.required", HttpStatus.FORBIDDEN));

        mockMvc.perform(post("/api/venues")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    @WithMockUser(username = "host@test.com", roles = {"HOST"})
    @DisplayName("Authenticated HOST -> GET /api/venues/my-venues returns 200 with only own venues")
    void givenHost_whenGetMyVenues_thenReturn200() throws Exception {
        VenueResponse venue = VenueResponse.builder().id(1L).ownerId(10L).name("Innovation Hub").build();
        PageResponse<VenueResponse> page = PageResponse.<VenueResponse>builder()
                .content(List.of(venue))
                .pageNumber(0)
                .pageSize(10)
                .totalElements(1)
                .totalPages(1)
                .last(true)
                .build();

        given(venueService.getMyVenues(eq("host@test.com"), eq(0), eq(10))).willReturn(page);

        mockMvc.perform(get("/api/venues/my-venues")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", org.hamcrest.Matchers.hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].ownerId").value(10));
    }

    @Test
    @WithMockUser(username = "host@test.com", roles = {"HOST"})
    @DisplayName("HOST updates own venue -> 200 OK")
    void givenOwnerHost_whenUpdateVenue_thenReturn200() throws Exception {
        VenueRequest request = VenueRequest.builder().name("Updated Name").build();
        VenueResponse response = VenueResponse.builder().id(1L).ownerId(10L).name("Updated Name").build();

        given(venueService.updateVenue(eq(1L), any(VenueRequest.class), eq("host@test.com"))).willReturn(response);

        mockMvc.perform(put("/api/venues/{id}", 1L)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Updated Name"));
    }

    @Test
    @WithMockUser(username = "other-host@test.com", roles = {"HOST"})
    @DisplayName("HOST updates a venue owned by another HOST -> 403 with error body")
    void givenNonOwnerHost_whenUpdateVenue_thenReturn403() throws Exception {
        VenueRequest request = VenueRequest.builder().name("Updated Name").build();

        given(venueService.updateVenue(eq(1L), any(VenueRequest.class), eq("other-host@test.com")))
                .willThrow(new AppException("venue.access.denied", HttpStatus.FORBIDDEN));

        mockMvc.perform(put("/api/venues/{id}", 1L)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @WithMockUser(username = "host@test.com", roles = {"HOST"})
    @DisplayName("Updating a non-existent venue -> 404 with error body")
    void givenMissingVenue_whenUpdateVenue_thenReturn404() throws Exception {
        VenueRequest request = VenueRequest.builder().name("Updated Name").build();

        given(venueService.updateVenue(eq(999L), any(VenueRequest.class), eq("host@test.com")))
                .willThrow(new VenueNotFoundException());

        mockMvc.perform(put("/api/venues/{id}", 999L)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    @WithMockUser(username = "host@test.com", roles = {"HOST"})
    @DisplayName("HOST deletes own venue -> 200 OK (soft delete)")
    void givenOwnerHost_whenDeleteVenue_thenReturn200() throws Exception {
        mockMvc.perform(delete("/api/venues/{id}", 1L)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @WithMockUser(username = "other-host@test.com", roles = {"HOST"})
    @DisplayName("HOST deletes a venue owned by another HOST -> 403 with error body")
    void givenNonOwnerHost_whenDeleteVenue_thenReturn403() throws Exception {
        org.mockito.Mockito.doThrow(new AppException("venue.access.denied", HttpStatus.FORBIDDEN))
                .when(venueService).deleteVenue(eq(1L), eq("other-host@test.com"));

        mockMvc.perform(delete("/api/venues/{id}", 1L)
                        .with(csrf()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @WithMockUser(username = "host@test.com", roles = {"HOST"})
    @DisplayName("Deleting a non-existent venue -> 404 with error body")
    void givenMissingVenue_whenDeleteVenue_thenReturn404() throws Exception {
        org.mockito.Mockito.doThrow(new VenueNotFoundException())
                .when(venueService).deleteVenue(eq(999L), eq("host@test.com"));

        mockMvc.perform(delete("/api/venues/{id}", 999L)
                        .with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.message").exists());
    }
}
