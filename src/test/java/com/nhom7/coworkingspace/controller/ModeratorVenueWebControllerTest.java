package com.nhom7.coworkingspace.controller;

import com.nhom7.coworkingspace.config.JwtProperties;
import com.nhom7.coworkingspace.controller.web.ModeratorVenueWebController;
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
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.nullValue;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(ModeratorVenueWebController.class)
@EnableMethodSecurity
@Import({
        JwtAuthenticationFilter.class,
        JwtProperties.class
})
@DisplayName(
        "ModeratorVenueWebController - Thymeleaf Web MVC & Security Tests"
)
class ModeratorVenueWebControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private VenueService venueService;

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
            "MODERATOR -> GET /moderator/venues renders venue list"
    )
    void givenModerator_whenListVenues_thenReturnVenueListView()
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

        PageResponse<VenueResponse> pageResponse =
                PageResponse.<VenueResponse>builder()
                        .content(List.of(venue))
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
        ).willReturn(pageResponse);

        mockMvc.perform(
                        get("/moderator/venues")
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        view().name("moderator/venues")
                )
                .andExpect(
                        model().attributeExists("venues")
                )
                .andExpect(
                        model().attributeExists("statuses")
                )
                .andExpect(
                        model().attribute(
                                "selectedStatus",
                                nullValue()
                        )
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
            username = "admin@test.com",
            roles = {"ADMIN"}
    )
    @DisplayName(
            "ADMIN -> GET /moderator/venues renders venue list"
    )
    void givenAdmin_whenListVenues_thenReturnVenueListView()
            throws Exception {

        given(
                venueService.getVenuesForModerator(
                        null,
                        0,
                        10
                )
        ).willReturn(emptyPage());

        mockMvc.perform(
                        get("/moderator/venues")
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        view().name("moderator/venues")
                );
    }

    @Test
    @WithMockUser(
            username = "user@test.com",
            roles = {"USER"}
    )
    @DisplayName(
            "USER -> GET /moderator/venues returns 403"
    )
    void givenUser_whenListVenues_thenReturn403()
            throws Exception {

        mockMvc.perform(
                        get("/moderator/venues")
                )
                .andExpect(
                        status().isForbidden()
                );
    }

    @Test
    @DisplayName(
            "Unauthenticated -> GET /moderator/venues returns 401"
    )
    void givenUnauthenticated_whenListVenues_thenReturn401()
            throws Exception {

        mockMvc.perform(
                        get("/moderator/venues")
                )
                .andExpect(
                        status().isUnauthorized()
                );
    }

    @Test
    @WithMockUser(
            username = "moderator@test.com",
            roles = {"MODERATOR"}
    )
    @DisplayName(
            "MODERATOR -> filter venues by PENDING"
    )
    void givenPendingStatus_whenListVenues_thenBindPendingStatus()
            throws Exception {

        given(
                venueService.getVenuesForModerator(
                        eq(VenueStatus.PENDING),
                        eq(0),
                        eq(10)
                )
        ).willReturn(emptyPage());

        mockMvc.perform(
                        get("/moderator/venues")
                                .param(
                                        "status",
                                        "PENDING"
                                )
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        model().attribute(
                                "selectedStatus",
                                VenueStatus.PENDING
                        )
                );

        verify(
                venueService
        ).getVenuesForModerator(
                VenueStatus.PENDING,
                0,
                10
        );
    }

    @Test
    @WithMockUser(
            username = "moderator@test.com",
            roles = {"MODERATOR"}
    )
    @DisplayName(
            "MODERATOR -> paginate venue list"
    )
    void givenPageAndSize_whenListVenues_thenUsePagination()
            throws Exception {

        given(
                venueService.getVenuesForModerator(
                        null,
                        2,
                        5
                )
        ).willReturn(emptyPage());

        mockMvc.perform(
                        get("/moderator/venues")
                                .param(
                                        "page",
                                        "2"
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
                null,
                2,
                5
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