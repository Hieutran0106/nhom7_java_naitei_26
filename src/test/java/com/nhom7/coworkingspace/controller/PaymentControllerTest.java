package com.nhom7.coworkingspace.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nhom7.coworkingspace.config.JwtProperties;
import com.nhom7.coworkingspace.controller.api.PaymentController;
import com.nhom7.coworkingspace.dto.response.PaymentResponse;
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
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.nhom7.coworkingspace.exception.AppException;
import com.nhom7.coworkingspace.exception.BookingNotFoundException;
import org.springframework.http.HttpStatus;

@WebMvcTest(PaymentController.class)
@Import({JwtAuthenticationFilter.class, JwtProperties.class})
@DisplayName("PaymentController - WebMvc & Security Tests")
class PaymentControllerTest {

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

    @MockBean
    private MessageSource messageSource;

    @Test
    @WithMockUser(username = "user@test.com", roles = {"USER"})
    @DisplayName("Authenticated USER -> POST /api/payments/mock/bookings/{id}/pay returns 200 OK")
    void givenUserRole_whenPayBooking_thenReturn200() throws Exception {
        PaymentResponse response = PaymentResponse.builder()
                .id(10L)
                .bookingId(1L)
                .amount(new BigDecimal("150000.00"))
                .paymentMethod("MOCK")
                .status("COMPLETED")
                .paidAt(LocalDateTime.now())
                .transactionId("MOCK-1")
                .build();

        given(bookingService.payBooking(eq(1L), eq("user@test.com")))
                .willReturn(response);

        mockMvc.perform(post("/api/payments/mock/bookings/1/pay")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(10))
                .andExpect(jsonPath("$.data.bookingId").value(1))
                .andExpect(jsonPath("$.data.status").value("COMPLETED"));
    }

    @Test
    @DisplayName("Unauthenticated request -> POST /api/payments/mock/bookings/{id}/pay returns 401 Unauthorized")
    void givenUnauthenticated_whenPayBooking_thenReturn401() throws Exception {
        mockMvc.perform(post("/api/payments/mock/bookings/1/pay")
                        .with(csrf()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "user@test.com", roles = {"USER"})
    @DisplayName("Not booking owner -> POST /api/payments/mock/bookings/{id}/pay returns 403 Forbidden")
    void givenNotOwner_whenPayBooking_thenReturn403() throws Exception {
        given(bookingService.payBooking(eq(1L), eq("user@test.com")))
                .willThrow(new AppException("booking.payment.not.owner", HttpStatus.FORBIDDEN));

        mockMvc.perform(post("/api/payments/mock/bookings/1/pay")
                        .with(csrf()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));
    }

    @Test
    @WithMockUser(username = "user@test.com", roles = {"USER"})
    @DisplayName("Booking not found -> POST /api/payments/mock/bookings/{id}/pay returns 404 Not Found")
    void givenBookingNotFound_whenPayBooking_thenReturn404() throws Exception {
        given(bookingService.payBooking(eq(999L), eq("user@test.com")))
                .willThrow(new BookingNotFoundException(999L));

        mockMvc.perform(post("/api/payments/mock/bookings/999/pay")
                        .with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404));
    }

    @Test
    @WithMockUser(username = "user@test.com", roles = {"USER"})
    @DisplayName("Booking not in APPROVED status -> POST /api/payments/mock/bookings/{id}/pay returns 400 Bad Request")
    void givenBookingNotApproved_whenPayBooking_thenReturn400() throws Exception {
        given(bookingService.payBooking(eq(1L), eq("user@test.com")))
                .willThrow(new AppException("booking.payment.not.approved", HttpStatus.BAD_REQUEST));

        mockMvc.perform(post("/api/payments/mock/bookings/1/pay")
                        .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }
}
