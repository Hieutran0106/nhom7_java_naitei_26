package com.nhom7.coworkingspace.controller;

import com.nhom7.coworkingspace.config.JwtProperties;
import com.nhom7.coworkingspace.controller.api.ModeratorVenueController;
import com.nhom7.coworkingspace.controller.web.ModeratorVenueWebController;
import com.nhom7.coworkingspace.dto.response.PageResponse;
import com.nhom7.coworkingspace.dto.response.SpaceResponse;
import com.nhom7.coworkingspace.dto.response.VenueResponse;
import com.nhom7.coworkingspace.enums.VenueStatus;
import com.nhom7.coworkingspace.security.CustomUserDetailsService;
import com.nhom7.coworkingspace.security.JwtAuthenticationFilter;
import com.nhom7.coworkingspace.security.JwtTokenProvider;
import com.nhom7.coworkingspace.service.SpaceService;
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

import java.util.List;

import static org.mockito.BDDMockito.given;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest({
        ModeratorVenueController.class,
        ModeratorVenueWebController.class
})
@EnableMethodSecurity
@Import({
        JwtAuthenticationFilter.class,
        JwtProperties.class
})
@DisplayName(
        "Moderator Venue Detail - Security Tests"
)
class ModeratorVenueDetailSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private VenueService venueService;

    @MockBean
    private SpaceService spaceService;

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
            username = "admin@test.com",
            roles = {"ADMIN"}
    )
    @DisplayName(
            "ADMIN -> GET detail API returns 200"
    )
    void givenAdmin_whenGetVenueDetailApi_thenReturn200()
            throws Exception {

        Long venueId = 1L;

        given(
                venueService.getVenueForModerator(
                        venueId
                )
        ).willReturn(
                venueResponse(
                        venueId
                )
        );

        mockMvc.perform(
                        get(
                                "/api/moderator/venues/{id}",
                                venueId
                        )
                )
                .andExpect(
                        status().isOk()
                );
    }

    @Test
    @WithMockUser(
            username = "user@test.com",
            roles = {"USER"}
    )
    @DisplayName(
            "USER -> GET detail API returns 403"
    )
    void givenUser_whenGetVenueDetailApi_thenReturn403()
            throws Exception {

        mockMvc.perform(
                        get(
                                "/api/moderator/venues/{id}",
                                1L
                        )
                )
                .andExpect(
                        status().isForbidden()
                );
    }

    @Test
    @DisplayName(
            "Unauthenticated -> GET detail API returns 401"
    )
    void givenUnauthenticated_whenGetVenueDetailApi_thenReturn401()
            throws Exception {

        mockMvc.perform(
                        get(
                                "/api/moderator/venues/{id}",
                                1L
                        )
                )
                .andExpect(
                        status().isUnauthorized()
                );
    }

    @Test
    @WithMockUser(
            username = "admin@test.com",
            roles = {"ADMIN"}
    )
    @DisplayName(
            "ADMIN -> GET detail web page returns 200"
    )
    void givenAdmin_whenGetVenueDetailWeb_thenReturn200()
            throws Exception {

        Long venueId = 1L;

        given(
                venueService.getVenueForModerator(
                        venueId
                )
        ).willReturn(
                venueResponse(
                        venueId
                )
        );

        given(
                spaceService.getSpacesByVenue(
                        venueId,
                        0,
                        100
                )
        ).willReturn(
                emptySpaces()
        );

        mockMvc.perform(
                        get(
                                "/moderator/venues/{id}",
                                venueId
                        )
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        view().name(
                                "moderator/venue-detail"
                        )
                );
    }

    @Test
    @WithMockUser(
            username = "user@test.com",
            roles = {"USER"}
    )
    @DisplayName(
            "USER -> GET detail web page returns 403"
    )
    void givenUser_whenGetVenueDetailWeb_thenReturn403()
            throws Exception {

        mockMvc.perform(
                        get(
                                "/moderator/venues/{id}",
                                1L
                        )
                )
                .andExpect(
                        status().isForbidden()
                );
    }

    @Test
    @DisplayName(
            "Unauthenticated -> GET detail web page returns 401"
    )
    void givenUnauthenticated_whenGetVenueDetailWeb_thenReturn401()
            throws Exception {

        mockMvc.perform(
                        get(
                                "/moderator/venues/{id}",
                                1L
                        )
                )
                .andExpect(
                        status().isUnauthorized()
                );
    }

    private VenueResponse venueResponse(
            Long venueId
    ) {
        return VenueResponse.builder()
                .id(venueId)
                .ownerId(10L)
                .ownerName("Host A")
                .name("Coworking Space Hanoi")
                .address("123 Nguyen Trai")
                .city("Ha Noi")
                .status(VenueStatus.PENDING)
                .build();
    }

    private PageResponse<SpaceResponse> emptySpaces() {

        return PageResponse
                .<SpaceResponse>builder()
                .content(
                        List.of()
                )
                .pageNumber(0)
                .pageSize(100)
                .totalElements(0)
                .totalPages(0)
                .last(true)
                .build();
    }
}