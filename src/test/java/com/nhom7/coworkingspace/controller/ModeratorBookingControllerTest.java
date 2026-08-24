package com.nhom7.coworkingspace.controller;

import com.nhom7.coworkingspace.config.JwtProperties;
import com.nhom7.coworkingspace.controller.api.ModeratorBookingController;
import com.nhom7.coworkingspace.dto.request.BookingSearchRequest;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ModeratorBookingController.class)
@EnableMethodSecurity
@Import({JwtAuthenticationFilter.class, JwtProperties.class})
@DisplayName("ModeratorBookingController - REST API & Security Tests")
class ModeratorBookingControllerTest {

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
    @WithMockUser(username = "moderator@test.com", roles = {"MODERATOR"})
    @DisplayName("Authenticated MODERATOR -> GET /api/moderator/bookings returns 200 OK with page data")
    void givenModeratorRole_whenSearchBookings_thenReturn200() throws Exception {
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
                .pageSize(20)
                .totalElements(1)
                .totalPages(1)
                .last(true)
                .build();

        given(bookingService.searchBookings(any(BookingSearchRequest.class))).willReturn(pageResponse);

        mockMvc.perform(get("/api/moderator/bookings")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.content[0].id").value(1L))
                .andExpect(jsonPath("$.data.content[0].userName").value("Nguyen Van A"))
                .andExpect(jsonPath("$.data.content[0].spaceName").value("Meeting Room 1"))
                .andExpect(jsonPath("$.data.content[0].status").value("PENDING"));
    }

    @Test
    @WithMockUser(username = "admin@test.com", roles = {"ADMIN"})
    @DisplayName("Authenticated ADMIN -> GET /api/moderator/bookings returns 200 OK")
    void givenAdminRole_whenSearchBookings_thenReturn200() throws Exception {
        PageResponse<BookingResponse> pageResponse = PageResponse.<BookingResponse>builder()
                .content(List.of())
                .pageNumber(0)
                .pageSize(20)
                .totalElements(0)
                .totalPages(0)
                .last(true)
                .build();

        given(bookingService.searchBookings(any(BookingSearchRequest.class))).willReturn(pageResponse);

        mockMvc.perform(get("/api/moderator/bookings")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @WithMockUser(username = "user@test.com", roles = {"USER"})
    @DisplayName("Authenticated regular USER -> GET /api/moderator/bookings returns 403 Forbidden")
    void givenUserRole_whenSearchBookings_thenReturn403() throws Exception {
        mockMvc.perform(get("/api/moderator/bookings")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Unauthenticated -> GET /api/moderator/bookings returns 401 Unauthorized")
    void givenUnauthenticated_whenSearchBookings_thenReturn401() throws Exception {
        mockMvc.perform(get("/api/moderator/bookings")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "moderator@test.com", roles = {"MODERATOR"})
    @DisplayName("Authenticated MODERATOR -> GET /api/moderator/bookings/{bookingId} returns 200 OK with booking details")
    void givenModeratorRole_whenGetBookingById_thenReturn200() throws Exception {
        Long bookingId = 1L;
        BookingResponse bookingDto = BookingResponse.builder()
                .id(bookingId)
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

        given(bookingService.getBookingById(bookingId)).willReturn(bookingDto);

        mockMvc.perform(get("/api/moderator/bookings/{bookingId}", bookingId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(bookingId))
                .andExpect(jsonPath("$.data.userName").value("Nguyen Van A"))
                .andExpect(jsonPath("$.data.spaceName").value("Meeting Room 1"))
                .andExpect(jsonPath("$.data.status").value("PENDING"));
    }

    @Test
    @WithMockUser(username = "moderator@test.com", roles = {"MODERATOR"})
    @DisplayName("Authenticated MODERATOR -> GET /api/moderator/bookings/{bookingId} returns 404 when not found")
    void givenModeratorRole_whenGetBookingById_notFound_thenReturn404() throws Exception {
        Long bookingId = 999L;
        given(bookingService.getBookingById(bookingId))
                .willThrow(new com.nhom7.coworkingspace.exception.BookingNotFoundException(bookingId));

        mockMvc.perform(get("/api/moderator/bookings/{bookingId}", bookingId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "user@test.com", roles = {"USER"})
    @DisplayName("Authenticated USER -> GET /api/moderator/bookings/{bookingId} returns 403 Forbidden")
    void givenUserRole_whenGetBookingById_thenReturn403() throws Exception {
        mockMvc.perform(get("/api/moderator/bookings/1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Unauthenticated -> GET /api/moderator/bookings/{bookingId} returns 401 Unauthorized")
    void givenUnauthenticated_whenGetBookingById_thenReturn401() throws Exception {
        mockMvc.perform(get("/api/moderator/bookings/1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }
}

