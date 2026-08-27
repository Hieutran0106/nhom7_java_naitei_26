package com.nhom7.coworkingspace.controller;

import com.nhom7.coworkingspace.config.JwtProperties;
import com.nhom7.coworkingspace.controller.api.ModeratorBookingController;
import com.nhom7.coworkingspace.dto.request.BookingSearchRequest;
import com.nhom7.coworkingspace.dto.response.BookingResponse;
import com.nhom7.coworkingspace.dto.response.PageResponse;
import com.nhom7.coworkingspace.security.CustomUserDetailsService;
import com.nhom7.coworkingspace.security.JwtAuthenticationFilter;
import com.nhom7.coworkingspace.security.JwtTokenProvider;
import com.nhom7.coworkingspace.service.BookingService;
import com.nhom7.coworkingspace.service.TokenBlacklistService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ModeratorBookingController.class)
@EnableMethodSecurity
@Import({
        JwtAuthenticationFilter.class,
        JwtProperties.class
})
@DisplayName(
        "ModeratorBookingController - Venue filter binding"
)
class ModeratorBookingVenueFilterControllerTest {

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
            "GET /api/moderator/bookings should bind venueId into search request"
    )
    void searchBookingsShouldBindVenueId() throws Exception {

        PageResponse<BookingResponse> response =
                PageResponse.<BookingResponse>builder()
                        .content(List.of())
                        .pageNumber(0)
                        .pageSize(20)
                        .totalElements(0)
                        .totalPages(0)
                        .last(true)
                        .build();

        given(
                bookingService.searchBookings(
                        any(BookingSearchRequest.class)
                )
        ).willReturn(response);

        mockMvc.perform(
                        get("/api/moderator/bookings")
                                .param(
                                        "venueId",
                                        "15"
                                )
                )
                .andExpect(
                        status().isOk()
                );

        ArgumentCaptor<BookingSearchRequest> captor =
                ArgumentCaptor.forClass(
                        BookingSearchRequest.class
                );

        verify(
                bookingService
        ).searchBookings(
                captor.capture()
        );

        assertEquals(
                15L,
                captor.getValue()
                        .getVenueId()
        );
    }
}