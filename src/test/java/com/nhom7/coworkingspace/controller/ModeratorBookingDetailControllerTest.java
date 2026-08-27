package com.nhom7.coworkingspace.controller;

import com.nhom7.coworkingspace.config.JwtProperties;
import com.nhom7.coworkingspace.controller.api.ModeratorBookingController;
import com.nhom7.coworkingspace.dto.response.BookingResponse;
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
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ModeratorBookingController.class)
@EnableMethodSecurity
@Import({
        JwtAuthenticationFilter.class,
        JwtProperties.class
})
@DisplayName(
        "Moderator booking detail API"
)
class ModeratorBookingDetailControllerTest {

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
    @WithMockUser(
            username = "moderator@test.com",
            roles = {
                    "MODERATOR"
            }
    )
    @DisplayName(
            "GET booking detail should expose payment method"
    )
    void getBookingDetailShouldExposePaymentMethod() throws Exception {

        Long bookingId = 10L;

        BookingResponse response =
                BookingResponse.builder()
                        .id(bookingId)
                        .userName("Nguyen Van A")
                        .userEmail("user@test.com")
                        .venueName("Hanoi Workspace")
                        .spaceName("Meeting Room 1")
                        .totalPrice(
                                new BigDecimal("250000.00")
                        )
                        .status(
                                BookingStatus.PAID
                        )
                        .paymentMethod("VNPAY")
                        .build();

        given(
                bookingService.getBookingById(
                        bookingId
                )
        ).willReturn(
                response
        );

        mockMvc.perform(
                        get(
                                "/api/moderator/bookings/{bookingId}",
                                bookingId
                        )
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        jsonPath("$.data.id")
                                .value(10)
                )
                .andExpect(
                        jsonPath("$.data.userName")
                                .value("Nguyen Van A")
                )
                .andExpect(
                        jsonPath("$.data.totalPrice")
                                .value(250000.00)
                )
                .andExpect(
                        jsonPath("$.data.paymentMethod")
                                .value("VNPAY")
                );
    }
}