package com.nhom7.coworkingspace.controller;

import com.nhom7.coworkingspace.config.JwtProperties;
import com.nhom7.coworkingspace.controller.web.ModeratorVenueWebController;
import com.nhom7.coworkingspace.dto.response.PageResponse;
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
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.containsString;

import static org.mockito.BDDMockito.given;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(ModeratorVenueWebController.class)
@EnableMethodSecurity
@Import({
        JwtAuthenticationFilter.class,
        JwtProperties.class
})
@DisplayName(
        "Moderator Venue List -> Detail Link Tests"
)
class ModeratorVenueListDetailLinkTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private VenueService venueService;

    @MockBean
    private SpaceService spaceService;

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
            "Venue name links to moderator venue detail page"
    )
    void givenVenueList_whenRendered_thenVenueNameLinksToDetail()
            throws Exception {

        VenueResponse venue =
                VenueResponse.builder()
                        .id(1L)
                        .ownerId(10L)
                        .ownerName("Host A")
                        .name("Coworking Space Hanoi")
                        .address("123 Nguyen Trai")
                        .city("Ha Noi")
                        .status(VenueStatus.PENDING)
                        .build();

        PageResponse<VenueResponse> venues =
                PageResponse.<VenueResponse>builder()
                        .content(
                                List.of(
                                        venue
                                )
                        )
                        .pageNumber(0)
                        .pageSize(10)
                        .totalElements(1)
                        .totalPages(1)
                        .last(true)
                        .build();

        given(
                venueService.getVenuesForModerator(
                        null,
                        0,
                        10
                )
        ).willReturn(
                venues
        );

        mockMvc.perform(
                        get(
                                "/moderator/venues"
                        )
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        view().name(
                                "moderator/venues"
                        )
                )
                .andExpect(
                        content().string(
                                containsString(
                                        "Coworking Space Hanoi"
                                )
                        )
                )
                .andExpect(
                        content().string(
                                containsString(
                                        "href=\"/moderator/venues/1\""
                                )
                        )
                );
    }
}