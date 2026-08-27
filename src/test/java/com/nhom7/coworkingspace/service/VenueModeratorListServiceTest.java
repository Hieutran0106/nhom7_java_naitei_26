package com.nhom7.coworkingspace.service;

import com.nhom7.coworkingspace.dto.response.PageResponse;
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

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("VenueServiceImpl - Moderator Venue List Tests")
class VenueModeratorListServiceTest {

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
            "No status filter -> return all non-deleted venues"
    )
    void givenNoStatus_whenGetVenuesForModerator_thenReturnAllNonDeletedVenues() {

        Venue venue = mock(Venue.class);

        VenueResponse venueResponse =
                VenueResponse.builder()
                        .id(1L)
                        .name("Coworking Space Hanoi")
                        .ownerId(10L)
                        .ownerName("Host A")
                        .address("123 Nguyen Trai")
                        .city("Ha Noi")
                        .build();

        Page<Venue> venuePage =
                new PageImpl<>(
                        List.of(venue),
                        PageRequest.of(0, 10),
                        1
                );

        given(
                venueRepository.findByDeletedFalse(
                        any(Pageable.class)
                )
        ).willReturn(venuePage);

        given(
                venueMapper.toVenueResponse(venue)
        ).willReturn(venueResponse);

        PageResponse<VenueResponse> result =
                venueService.getVenuesForModerator(
                        null,
                        0,
                        10
                );

        assertNotNull(result);

        assertEquals(
                1,
                result.getContent().size()
        );

        assertEquals(
                1L,
                result.getContent().get(0).getId()
        );

        assertEquals(
                "Coworking Space Hanoi",
                result.getContent().get(0).getName()
        );

        verify(
                venueRepository
        ).findByDeletedFalse(
                any(Pageable.class)
        );
    }

    @Test
    @DisplayName(
            "PENDING status filter -> return only pending non-deleted venues"
    )
    void givenPendingStatus_whenGetVenuesForModerator_thenFilterByPendingStatus() {

        Venue venue = mock(Venue.class);

        VenueResponse venueResponse =
                VenueResponse.builder()
                        .id(2L)
                        .name("Pending Venue")
                        .ownerId(20L)
                        .ownerName("Host B")
                        .address("456 Tran Duy Hung")
                        .city("Ha Noi")
                        .status(VenueStatus.PENDING)
                        .build();

        Page<Venue> venuePage =
                new PageImpl<>(
                        List.of(venue),
                        PageRequest.of(0, 10),
                        1
                );

        given(
                venueRepository.findByStatusAndDeletedFalse(
                        eq(VenueStatus.PENDING),
                        any(Pageable.class)
                )
        ).willReturn(venuePage);

        given(
                venueMapper.toVenueResponse(venue)
        ).willReturn(venueResponse);

        PageResponse<VenueResponse> result =
                venueService.getVenuesForModerator(
                        VenueStatus.PENDING,
                        0,
                        10
                );

        assertNotNull(result);

        assertEquals(
                1,
                result.getContent().size()
        );

        assertEquals(
                VenueStatus.PENDING,
                result.getContent().get(0).getStatus()
        );

        verify(
                venueRepository
        ).findByStatusAndDeletedFalse(
                eq(VenueStatus.PENDING),
                any(Pageable.class)
        );
    }
}