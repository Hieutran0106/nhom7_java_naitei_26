package com.nhom7.coworkingspace.controller;

import com.nhom7.coworkingspace.config.JwtProperties;
import com.nhom7.coworkingspace.controller.web.ModeratorBookingWebController;
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
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ModeratorBookingWebController.class)
@EnableMethodSecurity
@Import({JwtAuthenticationFilter.class, JwtProperties.class})
@DisplayName("ModeratorBookingWebController - Thymeleaf Web MVC & Security Tests")
class ModeratorBookingWebControllerTest {

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

    /*
    @Test
    @WithMockUser(username = "moderator@test.com", roles = {"MODERATOR"})
    @DisplayName("Authenticated MODERATOR -> GET /moderator/bookings renders template with model attributes")
    void givenModeratorRole_whenListBookings_thenReturnViewWithModel() throws Exception {
        BookingResponse bookingDto = BookingResponse.builder()
                .id(1L)
                .userName("Nguyen Van A")
                .userEmail("user@test.com")
                .spaceName("Meeting Room 1")
                .startTime(LocalDateTime.of(2026, 9, 1, 9, 0))
                .endTime(LocalDateTime.of(2026, 9, 1, 11, 0))
                .totalPrice(new BigDecimal("200000.00"))
                .status(BookingStatus.PENDING)
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

        mockMvc.perform(get("/moderator/bookings"))
                .andExpect(status().isOk())
                .andExpect(view().name("moderator/bookings"))
                .andExpect(model().attributeExists("bookings"))
                .andExpect(model().attributeExists("statuses"))
                .andExpect(model().attributeExists("searchRequest"));
    }
    */


    @Test
    @WithMockUser(username = "user@test.com", roles = {"USER"})
    @DisplayName("Authenticated USER -> GET /moderator/bookings returns 403 Forbidden")
    void givenUserRole_whenListBookings_thenReturn403() throws Exception {
        mockMvc.perform(get("/moderator/bookings"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Unauthenticated -> GET /moderator/bookings returns 401 Unauthorized")
    void givenUnauthenticated_whenListBookings_thenReturn401() throws Exception {
        mockMvc.perform(get("/moderator/bookings"))
                .andExpect(status().isUnauthorized());
    }
}
