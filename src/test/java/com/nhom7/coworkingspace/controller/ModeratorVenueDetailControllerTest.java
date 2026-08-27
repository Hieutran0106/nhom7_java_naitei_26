package com.nhom7.coworkingspace.controller;

import com.nhom7.coworkingspace.config.JwtProperties;
import com.nhom7.coworkingspace.controller.api.ModeratorVenueController;
import com.nhom7.coworkingspace.dto.response.VenueResponse;
import com.nhom7.coworkingspace.enums.VenueStatus;
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
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ModeratorVenueController.class)
@EnableMethodSecurity
@Import({
        JwtAuthenticationFilter.class,
        JwtProperties.class
})
@DisplayName(
        "ModeratorVenueController - Venue Detail API Tests"
)
class ModeratorVenueDetailControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private VenueService venueService;

    @MockBean
    private MessageSource messageSource;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @MockBean
    private TokenBlacklistService tokenBlacklistService;

    @Test
    @WithMockUser(
            username = "moderator@test.com",
            roles = {"MODERATOR"}
    )
    @DisplayName(
            "MODERATOR -> GET venue detail returns 200"
    )
    void givenModerator_whenGetVenueDetail_thenReturn200()
            throws Exception {

        Long venueId = 1L;

        VenueResponse venue =
                VenueResponse.builder()
                        .id(venueId)
                        .ownerId(10L)
                        .ownerName("Host A")
                        .name("Coworking Space Hanoi")
                        .description("Venue description")
                        .address("123 Nguyen Trai")
                        .city("Ha Noi")
                        .street("Thanh Xuan")
                        .status(VenueStatus.PENDING)
                        .build();

        given(
                venueService.getVenueForModerator(
                        venueId
                )
        ).willReturn(
                venue
        );

        mockMvc.perform(
                        get(
                                "/api/moderator/venues/{id}",
                                venueId
                        )
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        jsonPath("$.data.id")
                                .value(1)
                )
                .andExpect(
                        jsonPath("$.data.name")
                                .value(
                                        "Coworking Space Hanoi"
                                )
                )
                .andExpect(
                        jsonPath("$.data.status")
                                .value(
                                        "PENDING"
                                )
                );

        verify(
                venueService
        ).getVenueForModerator(
                venueId
        );
    }
}