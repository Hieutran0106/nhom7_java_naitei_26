package com.nhom7.coworkingspace.controller;

import com.nhom7.coworkingspace.config.JwtProperties;
import com.nhom7.coworkingspace.controller.api.ModeratorVenueController;
import com.nhom7.coworkingspace.dto.response.PageResponse;
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

import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ModeratorVenueController.class)
@EnableMethodSecurity
@Import({
        JwtAuthenticationFilter.class,
        JwtProperties.class
})
@DisplayName(
        "ModeratorVenueController - Venue List API Tests"
)
class ModeratorVenueListControllerTest {

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
            "MODERATOR -> GET venue list with PENDING filter"
    )
    void givenModeratorAndPendingStatus_whenGetVenues_thenReturn200()
            throws Exception {

        given(
                venueService.getVenuesForModerator(
                        VenueStatus.PENDING,
                        1,
                        5
                )
        ).willReturn(emptyPage());

        mockMvc.perform(
                        get("/api/moderator/venues")
                                .param(
                                        "status",
                                        "PENDING"
                                )
                                .param(
                                        "page",
                                        "1"
                                )
                                .param(
                                        "size",
                                        "5"
                                )
                )
                .andExpect(
                        status().isOk()
                );

        verify(
                venueService
        ).getVenuesForModerator(
                VenueStatus.PENDING,
                1,
                5
        );
    }

    @Test
    @WithMockUser(
            username = "moderator@test.com",
            roles = {"MODERATOR"}
    )
    @DisplayName(
            "MODERATOR -> GET venue list accepts APPROVE status"
    )
    void givenApproveStatus_whenGetVenues_thenReturn200()
            throws Exception {

        given(
                venueService.getVenuesForModerator(
                        VenueStatus.APPROVE,
                        0,
                        10
                )
        ).willReturn(emptyPage());

        mockMvc.perform(
                        get("/api/moderator/venues")
                                .param(
                                        "status",
                                        "APPROVE"
                                )
                )
                .andExpect(
                        status().isOk()
                );

        verify(
                venueService
        ).getVenuesForModerator(
                VenueStatus.APPROVE,
                0,
                10
        );
    }

    @Test
    @WithMockUser(
            username = "admin@test.com",
            roles = {"ADMIN"}
    )
    @DisplayName(
            "ADMIN -> GET all venues without status filter"
    )
    void givenAdmin_whenGetVenues_thenReturn200()
            throws Exception {

        given(
                venueService.getVenuesForModerator(
                        null,
                        0,
                        10
                )
        ).willReturn(emptyPage());

        mockMvc.perform(
                        get("/api/moderator/venues")
                )
                .andExpect(
                        status().isOk()
                );

        verify(
                venueService
        ).getVenuesForModerator(
                null,
                0,
                10
        );
    }

    @Test
    @WithMockUser(
            username = "user@test.com",
            roles = {"USER"}
    )
    @DisplayName(
            "USER -> GET venue list returns 403"
    )
    void givenUser_whenGetVenues_thenReturn403()
            throws Exception {

        mockMvc.perform(
                        get("/api/moderator/venues")
                )
                .andExpect(
                        status().isForbidden()
                );

        verifyNoInteractions(
                venueService
        );
    }

    @Test
    @DisplayName(
            "Unauthenticated -> GET venue list returns 401"
    )
    void givenUnauthenticated_whenGetVenues_thenReturn401()
            throws Exception {

        mockMvc.perform(
                        get("/api/moderator/venues")
                )
                .andExpect(
                        status().isUnauthorized()
                );

        verifyNoInteractions(
                venueService
        );
    }

    private PageResponse<VenueResponse> emptyPage() {

        return PageResponse
                .<VenueResponse>builder()
                .content(List.of())
                .pageNumber(0)
                .pageSize(10)
                .totalElements(0)
                .totalPages(0)
                .last(true)
                .build();
    }
}