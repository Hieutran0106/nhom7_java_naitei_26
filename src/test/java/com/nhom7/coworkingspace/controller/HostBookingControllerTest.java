package com.nhom7.coworkingspace.controller;

import com.nhom7.coworkingspace.config.JwtProperties;
import com.nhom7.coworkingspace.controller.api.HostBookingController;
import com.nhom7.coworkingspace.dto.response.BookingResponse;
import com.nhom7.coworkingspace.dto.response.PageResponse;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(HostBookingController.class)
@EnableMethodSecurity
@Import({JwtAuthenticationFilter.class, JwtProperties.class})
@DisplayName("HostBookingController - REST API & Security Tests")
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
    @DisplayName("Authenticated HOST -> GET /api/host/bookings returns 200 OK with bookings for own spaces")
    void givenHostWithBookings_whenGetMyBookings_thenReturn200() throws Exception {
        BookingResponse bookingDto = BookingResponse.builder()
                .id(1L)
                .userId(10L)
                .userName("Nguyen Van A")
                .userEmail("user@test.com")
                .spaceId(5L)
                .spaceName("Meeting Room 1")
                .startTime(LocalDateTime.of(2026, 9, 1, 9, 0))
                .endTime(LocalDateTime.of(2026, 9, 1, 11, 0))
                .totalPrice(new BigDecimal("200000.00"))
                .status(BookingStatus.PENDING)
                .createdAt(LocalDateTime.of(2026, 8, 20, 10, 0))
                .build();

        PageResponse<BookingResponse> pageResponse = PageResponse.<BookingResponse>builder()
                .content(List.of(bookingDto))
                .pageNumber(0)
                .pageSize(10)
                .totalElements(1)
                .totalPages(1)
                .last(true)
                .build();

        given(bookingService.getBookingsForHost(eq("host@test.com"), eq(0), eq(10))).willReturn(pageResponse);

        mockMvc.perform(get("/api/host/bookings")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("Lấy danh sách đặt chỗ thành công"))
                .andExpect(jsonPath("$.data.content[0].id").value(1L))
                .andExpect(jsonPath("$.data.content[0].spaceName").value("Meeting Room 1"))
                .andExpect(jsonPath("$.data.content[0].status").value("PENDING"));
    }

    @Test
    @WithMockUser(username = "host@test.com", roles = {"HOST"})
    @DisplayName("Authenticated HOST with no bookings -> GET /api/host/bookings returns 200 OK with empty content")
    void givenHostWithNoBookings_whenGetMyBookings_thenReturnEmptyPage() throws Exception {
        PageResponse<BookingResponse> emptyPage = PageResponse.<BookingResponse>builder()
                .content(List.of())
                .pageNumber(0)
                .pageSize(10)
                .totalElements(0)
                .totalPages(0)
                .last(true)
                .build();

        given(bookingService.getBookingsForHost(eq("host@test.com"), eq(0), eq(10))).willReturn(emptyPage);

        mockMvc.perform(get("/api/host/bookings")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("Bạn chưa có lượt đặt chỗ cho space của mình"))
                .andExpect(jsonPath("$.data.content").isEmpty())
                .andExpect(jsonPath("$.data.totalElements").value(0));
    }

    @Test
    @WithMockUser(username = "user@test.com", roles = {"USER"})
    @DisplayName("@PreAuthorize blocks non-HOST before the service is ever called (GET listing)")
    void givenNonHostRole_whenGetMyBookings_thenReturn403AndServiceNeverInvoked() throws Exception {
        mockMvc.perform(get("/api/host/bookings")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());

        verifyNoInteractions(bookingService);
    }

    @Test
    @WithMockUser(username = "moderator@test.com", roles = {"MODERATOR"})
    @DisplayName("Authenticated MODERATOR (not HOST) -> GET /api/host/bookings returns 403 Forbidden")
    void givenModeratorRole_whenGetMyBookings_thenReturn403() throws Exception {
        mockMvc.perform(get("/api/host/bookings")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());

        verifyNoInteractions(bookingService);
    }

    @Test
    @DisplayName("Unauthenticated -> GET /api/host/bookings returns 401 Unauthorized")
    void givenUnauthenticated_whenGetMyBookings_thenReturn401() throws Exception {
        mockMvc.perform(get("/api/host/bookings")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(bookingService);
    }

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
    @DisplayName("@PreAuthorize blocks non-HOST roles before the service is ever called (status update)")
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
