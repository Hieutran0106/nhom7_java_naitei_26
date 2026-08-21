package com.nhom7.coworkingspace.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nhom7.coworkingspace.config.JwtProperties;
import com.nhom7.coworkingspace.controller.api.BookingController;
import com.nhom7.coworkingspace.dto.request.BookingRequest;
import com.nhom7.coworkingspace.dto.response.BookingResponse;
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
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BookingController.class)
@Import({JwtAuthenticationFilter.class, JwtProperties.class})
@DisplayName("BookingController - WebMvc & Security Tests")
class BookingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private BookingService bookingService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @MockBean
    private TokenBlacklistService tokenBlacklistService;

    @Test
    @WithMockUser(username = "user@test.com", roles = {"USER"})
    @DisplayName("Authenticated USER -> POST /api/bookings returns 201 Created")
    void givenUserRole_whenCreateBooking_thenReturn201() throws Exception {
        LocalDateTime start = LocalDateTime.now().plusDays(1).withHour(9).withMinute(0);
        LocalDateTime end = start.plusHours(2);

        BookingRequest request = BookingRequest.builder()
                .spaceId(10L)
                .startTime(start)
                .endTime(end)
                .build();

        BookingResponse response = BookingResponse.builder()
                .id(1L)
                .userEmail("user@test.com")
                .spaceId(10L)
                .spaceName("Desk 101")
                .startTime(start)
                .endTime(end)
                .totalPrice(new BigDecimal("200000.00"))
                .status("PENDING")
                .createdAt(LocalDateTime.now())
                .build();

        given(bookingService.createBooking(any(BookingRequest.class), eq("user@test.com")))
                .willReturn(response);

        mockMvc.perform(post("/api/bookings")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value(201))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.totalPrice").value(200000.00));
    }

    @Test
    @DisplayName("Unauthenticated request -> POST /api/bookings returns 401 Unauthorized")
    void givenUnauthenticated_whenCreateBooking_thenReturn401() throws Exception {
        BookingRequest request = BookingRequest.builder()
                .spaceId(10L)
                .startTime(LocalDateTime.now().plusDays(1))
                .endTime(LocalDateTime.now().plusDays(1).plusHours(2))
                .build();

        mockMvc.perform(post("/api/bookings")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }
}
