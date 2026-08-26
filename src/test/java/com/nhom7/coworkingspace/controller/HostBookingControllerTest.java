package com.nhom7.coworkingspace.controller;

import com.nhom7.coworkingspace.config.JwtProperties;
import com.nhom7.coworkingspace.controller.api.HostBookingController;
import com.nhom7.coworkingspace.dto.response.BookingResponse;
import com.nhom7.coworkingspace.dto.response.PageResponse;
import com.nhom7.coworkingspace.enums.BookingStatus;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
    @DisplayName("@PreAuthorize blocks non-HOST before the service is ever called")
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
}
