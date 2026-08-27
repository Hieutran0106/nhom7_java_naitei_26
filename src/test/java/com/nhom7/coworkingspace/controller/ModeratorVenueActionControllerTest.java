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
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ModeratorVenueController.class)
@EnableMethodSecurity
@Import({
        JwtAuthenticationFilter.class,
        JwtProperties.class
})
@DisplayName(
        "ModeratorVenueController - Approve/Block API Tests"
)
class ModeratorVenueActionControllerTest {

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
            "MODERATOR -> PUT /api/moderator/venues/{id}/approve approves venue"
    )
    void givenModerator_whenApproveVenue_thenReturnApprovedVenue()
            throws Exception {

        VenueResponse response =
                VenueResponse.builder()
                        .id(1L)
                        .name("Coworking Space Hanoi")
                        .status(VenueStatus.APPROVE)
                        .blockReason(null)
                        .build();

        given(
                venueService.approveVenue(
                        1L,
                        "moderator@test.com"
                )
        ).willReturn(
                response
        );

        mockMvc.perform(
                        put(
                                "/api/moderator/venues/{id}/approve",
                                1L
                        )
                                .with(
                                        csrf()
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
                        jsonPath("$.data.status")
                                .value("APPROVE")
                );

        verify(
                venueService
        ).approveVenue(
                1L,
                "moderator@test.com"
        );
    }

    @Test
    @WithMockUser(
            username = "moderator@test.com",
            roles = {"MODERATOR"}
    )
    @DisplayName(
            "MODERATOR -> PUT /api/moderator/venues/{id}/block blocks venue with reason"
    )
    void givenModeratorAndReason_whenBlockVenue_thenReturnBlockedVenue()
            throws Exception {

        String reason =
                "Thông tin Venue chưa hợp lệ";

        VenueResponse response =
                VenueResponse.builder()
                        .id(1L)
                        .name("Coworking Space Hanoi")
                        .status(VenueStatus.BLOCKED)
                        .blockReason(reason)
                        .build();

        given(
                venueService.blockVenue(
                        1L,
                        reason,
                        "moderator@test.com"
                )
        ).willReturn(
                response
        );

        mockMvc.perform(
                        put(
                                "/api/moderator/venues/{id}/block",
                                1L
                        )
                                .with(
                                        csrf()
                                )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        """
                                        {
                                          "reason": "Thông tin Venue chưa hợp lệ"
                                        }
                                        """
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
                        jsonPath("$.data.status")
                                .value("BLOCKED")
                )
                .andExpect(
                        jsonPath("$.data.blockReason")
                                .value(reason)
                );

        verify(
                venueService
        ).blockVenue(
                1L,
                reason,
                "moderator@test.com"
        );
    }

    @Test
    @WithMockUser(
            username = "moderator@test.com",
            roles = {"MODERATOR"}
    )
    @DisplayName(
            "MODERATOR -> Block venue with blank reason returns 400"
    )
    void givenBlankReason_whenBlockVenue_thenReturnBadRequest()
            throws Exception {

        mockMvc.perform(
                        put(
                                "/api/moderator/venues/{id}/block",
                                1L
                        )
                                .with(
                                        csrf()
                                )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        """
                                        {
                                          "reason": "   "
                                        }
                                        """
                                )
                )
                .andExpect(
                        status().isBadRequest()
                );

        verifyNoInteractions(
                venueService
        );
    }
}