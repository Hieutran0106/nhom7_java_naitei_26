package com.nhom7.coworkingspace.service;

import com.nhom7.coworkingspace.dto.response.VenueResponse;
import com.nhom7.coworkingspace.entity.Venue;
import com.nhom7.coworkingspace.enums.VenueStatus;
import com.nhom7.coworkingspace.mapper.VenueMapper;
import com.nhom7.coworkingspace.repository.AmenityRepository;
import com.nhom7.coworkingspace.repository.SpaceRepository;
import com.nhom7.coworkingspace.repository.UserRepository;
import com.nhom7.coworkingspace.repository.VenueRepository;
import com.nhom7.coworkingspace.service.impl.VenueServiceImpl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName(
        "VenueServiceImpl - Moderator Venue Detail Tests"
)
class VenueModeratorDetailServiceTest {

    @Mock
    private VenueRepository venueRepository;

    @Mock
    private AmenityRepository amenityRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SpaceRepository spaceRepository;

    @Mock
    private VenueMapper venueMapper;

    @InjectMocks
    private VenueServiceImpl venueService;

    @Test
    @DisplayName(
            "Existing non-deleted venue -> return venue detail"
    )
    void givenExistingVenue_whenGetVenueForModerator_thenReturnVenueDetail() {

        Long venueId = 1L;

        Venue venue =
                mock(Venue.class);

        VenueResponse venueResponse =
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
                venueRepository.findByIdAndDeletedFalse(
                        venueId
                )
        ).willReturn(
                Optional.of(venue)
        );

        given(
                venueMapper.toVenueResponse(
                        venue
                )
        ).willReturn(
                venueResponse
        );

        VenueResponse result =
                venueService.getVenueForModerator(
                        venueId
                );

        assertNotNull(result);

        assertEquals(
                venueId,
                result.getId()
        );

        assertEquals(
                "Coworking Space Hanoi",
                result.getName()
        );

        assertEquals(
                VenueStatus.PENDING,
                result.getStatus()
        );

        verify(
                venueRepository
        ).findByIdAndDeletedFalse(
                venueId
        );

        verify(
                venueMapper
        ).toVenueResponse(
                venue
        );
    }
}