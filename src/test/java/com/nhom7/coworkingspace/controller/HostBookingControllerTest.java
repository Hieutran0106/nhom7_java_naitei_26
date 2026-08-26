package com.nhom7.coworkingspace.controller;

import com.nhom7.coworkingspace.config.JwtProperties;
import com.nhom7.coworkingspace.controller.api.HostBookingController;
import com.nhom7.coworkingspace.dto.response.BookingResponse;
import com.nhom7.coworkingspace.enums.BookingStatus;
import com.nhom7.coworkingspace.exception.AppException;
import com.nhom7.coworkingspace.exception.BookingNotFoundException;
import com.nhom7.coworkingspace.security.CustomUserDetailsService;
import com.nhom7.coworkingspace.security.JwtAuthenticationFilter;
import com.nhom7.coworkingspace.security.JwtTokenProvider;
import com.nhom7.coworkingspace.service.BookingService;
import com.nhom7.coworkingspace.service.TokenBlacklistService;
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

@WebMvcTest(HostBookingController.class)
@EnableMethodSecurity
@Import({JwtAuthenticationFilter.class, JwtProperties.class})
@DisplayName("HostBookingController - WebMvc & Security Tests")
class HostBookingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BookingService bookingService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @MockBean
    private TokenBlacklistService tokenBlacklistService;

    @Test
    @WithMockUser(username = "host@test.com", roles = {"HOST"})
    @DisplayName("Authenticated HOST -> PUT /api/host/bookings/{id}/status APPROVED returns 200 OK")
    void givenHostRole_whenApprove_thenReturn200() throws Exception {
        BookingResponse response = BookingResponse.builder().id(1L).spaceId(10L).status(BookingStatus.APPROVED).build();

        given(bookingService.updateBookingStatusByHost(eq(1L), eq(BookingStatus.APPROVED), eq("host@test.com")))
                .willReturn(response);

        mockMvc.perform(put("/api/host/bookings/1/status")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"APPROVED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.status").value("APPROVED"));
    }

    @Test
    @WithMockUser(username = "host@test.com", roles = {"HOST"})
    @DisplayName("Authenticated HOST -> PUT /api/host/bookings/{id}/status REJECTED returns 200 OK")
    void givenHostRole_whenReject_thenReturn200() throws Exception {
        BookingResponse response = BookingResponse.builder().id(1L).spaceId(10L).status(BookingStatus.REJECTED).build();

        given(bookingService.updateBookingStatusByHost(eq(1L), eq(BookingStatus.REJECTED), eq("host@test.com")))
                .willReturn(response);

        mockMvc.perform(put("/api/host/bookings/1/status")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"REJECTED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("REJECTED"));
    }

    @Test
    @WithMockUser(username = "user@test.com", roles = {"USER"})
    @DisplayName("@PreAuthorize blocks non-HOST roles before the service is ever called")
    void givenNonHostRole_whenUpdateBookingStatus_thenServiceNeverInvoked() throws Exception {
        mockMvc.perform(put("/api/host/bookings/1/status")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"APPROVED\"}"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(bookingService);
    }

    @Test
    @DisplayName("Unauthenticated request -> PUT /api/host/bookings/{id}/status returns 401 Unauthorized")
    void givenUnauthenticated_whenUpdateBookingStatus_thenReturn401() throws Exception {
        mockMvc.perform(put("/api/host/bookings/1/status")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"APPROVED\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "host@test.com", roles = {"HOST"})
    @DisplayName("Null status in body -> PUT /api/host/bookings/{id}/status returns 400 Bad Request")
    void givenNullStatus_whenUpdateBookingStatus_thenReturn400() throws Exception {
        mockMvc.perform(put("/api/host/bookings/1/status")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": null}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(bookingService);
    }

    @Test
    @WithMockUser(username = "host@test.com", roles = {"HOST"})
    @DisplayName("Booking not found -> PUT /api/host/bookings/{id}/status returns 404 Not Found")
    void givenBookingNotFound_whenUpdateBookingStatus_thenReturn404() throws Exception {
        given(bookingService.updateBookingStatusByHost(eq(999L), eq(BookingStatus.APPROVED), eq("host@test.com")))
                .willThrow(new BookingNotFoundException(999L));

        mockMvc.perform(put("/api/host/bookings/999/status")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"APPROVED\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404));
    }

    @Test
    @WithMockUser(username = "otherhost@test.com", roles = {"HOST"})
    @DisplayName("Host does not own the Space -> PUT /api/host/bookings/{id}/status returns 403 Forbidden")
    void givenHostNotOwner_whenUpdateBookingStatus_thenReturn403() throws Exception {
        given(bookingService.updateBookingStatusByHost(eq(1L), eq(BookingStatus.APPROVED), eq("otherhost@test.com")))
                .willThrow(new AppException("booking.access.denied", HttpStatus.FORBIDDEN));

        mockMvc.perform(put("/api/host/bookings/1/status")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"APPROVED\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));
    }

    @Test
    @WithMockUser(username = "host@test.com", roles = {"HOST"})
    @DisplayName("Booking not PENDING -> PUT /api/host/bookings/{id}/status returns 400 Bad Request")
    void givenBookingNotPending_whenUpdateBookingStatus_thenReturn400() throws Exception {
        given(bookingService.updateBookingStatusByHost(eq(1L), eq(BookingStatus.APPROVED), eq("host@test.com")))
                .willThrow(new AppException("booking.status.transition.invalid", HttpStatus.BAD_REQUEST));

        mockMvc.perform(put("/api/host/bookings/1/status")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"APPROVED\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }
}
