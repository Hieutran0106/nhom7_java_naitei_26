package com.nhom7.coworkingspace.controller;

import com.nhom7.coworkingspace.config.JwtProperties;
import com.nhom7.coworkingspace.controller.web.ModeratorVenueWebController;
import com.nhom7.coworkingspace.dto.response.AmenityResponse;
import com.nhom7.coworkingspace.dto.response.PageResponse;
import com.nhom7.coworkingspace.dto.response.SpaceResponse;
import com.nhom7.coworkingspace.dto.response.VenueResponse;
import com.nhom7.coworkingspace.enums.SpaceStatus;
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

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;

import static org.hamcrest.Matchers.containsString;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
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
        "ModeratorVenueWebController - Venue Detail Tests"
)
class ModeratorVenueDetailWebControllerTest {

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
            "MODERATOR -> GET venue detail renders complete venue information"
    )
    void givenModerator_whenGetVenueDetail_thenRenderCompleteDetailPage()
            throws Exception {

        Long venueId = 1L;

        AmenityResponse wifi =
                AmenityResponse.builder()
                        .id(1L)
                        .name("WiFi")
                        .build();

        AmenityResponse parking =
                AmenityResponse.builder()
                        .id(2L)
                        .name("Parking")
                        .build();

        VenueResponse venue =
                VenueResponse.builder()
                        .id(venueId)
                        .ownerId(10L)
                        .ownerName("Host A")
                        .name("Coworking Space Hanoi")
                        .description(
                                "Không gian làm việc hiện đại"
                        )
                        .address("123 Nguyen Trai")
                        .city("Ha Noi")
                        .street("Thanh Xuan")
                        .latitude(
                                new BigDecimal(
                                        "21.028511"
                                )
                        )
                        .longitude(
                                new BigDecimal(
                                        "105.804817"
                                )
                        )
                        .status(VenueStatus.PENDING)
                        .amenities(
                                List.of(
                                        wifi,
                                        parking
                                )
                        )
                        .build();

        SpaceResponse space =
                SpaceResponse.builder()
                        .id(100L)
                        .venueId(venueId)
                        .venueName(
                                "Coworking Space Hanoi"
                        )
                        .name("Meeting Room A")
                        .type("MEETING_ROOM")
                        .capacity(8)
                        .description(
                                "Phòng họp riêng"
                        )
                        .price(
                                new BigDecimal(
                                        "200000"
                                )
                        )
                        .priceUnit("HOUR")
                        .openTime(
                                LocalTime.of(
                                        8,
                                        0
                                )
                        )
                        .closeTime(
                                LocalTime.of(
                                        18,
                                        0
                                )
                        )
                        .status(
                                SpaceStatus.ACTIVE
                        )
                        .build();

        PageResponse<SpaceResponse> spaces =
                PageResponse.<SpaceResponse>builder()
                        .content(
                                List.of(
                                        space
                                )
                        )
                        .pageNumber(0)
                        .pageSize(100)
                        .totalElements(1)
                        .totalPages(1)
                        .last(true)
                        .build();

        given(
                venueService.getVenueForModerator(
                        venueId
                )
        ).willReturn(
                venue
        );

        given(
                spaceService.getSpacesByVenue(
                        venueId,
                        0,
                        100
                )
        ).willReturn(
                spaces
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
                )
                .andExpect(
                        model().attribute(
                                "venue",
                                venue
                        )
                )
                .andExpect(
                        model().attribute(
                                "spaces",
                                spaces
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
                                        "Không gian làm việc hiện đại"
                                )
                        )
                )
                .andExpect(
                        content().string(
                                containsString(
                                        "123 Nguyen Trai"
                                )
                        )
                )
                .andExpect(
                        content().string(
                                containsString(
                                        "Host A"
                                )
                        )
                )
                .andExpect(
                        content().string(
                                containsString(
                                        "WiFi"
                                )
                        )
                )
                .andExpect(
                        content().string(
                                containsString(
                                        "Parking"
                                )
                        )
                )
                .andExpect(
                        content().string(
                                containsString(
                                        "Meeting Room A"
                                )
                        )
                )
                .andExpect(
                        content().string(
                                containsString(
                                        "MEETING_ROOM"
                                )
                        )
                )
                .andExpect(
                        content().string(
                                containsString(
                                        "Chưa có hình ảnh"
                                )
                        )
                );

        verify(
                venueService
        ).getVenueForModerator(
                venueId
        );

        verify(
                spaceService
        ).getSpacesByVenue(
                venueId,
                0,
                100
        );
    }
}