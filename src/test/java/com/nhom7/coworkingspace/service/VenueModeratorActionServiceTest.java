package com.nhom7.coworkingspace.service;

import com.nhom7.coworkingspace.dto.response.VenueResponse;
import com.nhom7.coworkingspace.entity.Space;
import com.nhom7.coworkingspace.entity.User;
import com.nhom7.coworkingspace.entity.Venue;
import com.nhom7.coworkingspace.enums.SpaceStatus;
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

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName(
        "VenueServiceImpl - Moderator Approve/Block Tests"
)
class VenueModeratorActionServiceTest {

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
            "Block venue -> save reason, set BLOCKED and deactivate spaces"
    )
    void givenPendingVenue_whenBlockVenue_thenSaveReasonAndDeactivateSpaces() {

        Long venueId = 1L;

        String moderatorEmail =
                "moderator@test.com";

        String reason =
                "Thông tin Venue chưa hợp lệ";

        Venue venue =
                mock(Venue.class);

        User owner =
                mock(User.class);

        User moderator =
                mock(User.class);

        Space space =
                mock(Space.class);

        VenueResponse response =
                VenueResponse.builder()
                        .id(venueId)
                        .status(VenueStatus.BLOCKED)
                        .blockReason(reason)
                        .build();

        given(
                venueRepository.findByIdAndDeletedFalse(
                        venueId
                )
        ).willReturn(
                Optional.of(
                        venue
                )
        );

        given(
                userRepository.findByEmail(
                        moderatorEmail
                )
        ).willReturn(
                Optional.of(
                        moderator
                )
        );

        given(
                venue.getOwner()
        ).willReturn(
                owner
        );

        given(
                owner.getId()
        ).willReturn(
                10L
        );

        given(
                moderator.getId()
        ).willReturn(
                20L
        );

        given(
                venueRepository.save(
                        venue
                )
        ).willReturn(
                venue
        );

        given(
                spaceRepository.findByVenueId(
                        venueId
                )
        ).willReturn(
                List.of(
                        space
                )
        );

        given(
                venueMapper.toVenueResponse(
                        venue
                )
        ).willReturn(
                response
        );

        VenueResponse result =
                venueService.blockVenue(
                        venueId,
                        reason,
                        moderatorEmail
                );

        assertNotNull(
                result
        );

        assertEquals(
                VenueStatus.BLOCKED,
                result.getStatus()
        );

        assertEquals(
                reason,
                result.getBlockReason()
        );

        verify(
                venue
        ).setStatus(
                VenueStatus.BLOCKED
        );

        verify(
                venue
        ).setBlockReason(
                reason
        );

        verify(
                venueRepository
        ).save(
                venue
        );

        verify(
                space
        ).setStatus(
                SpaceStatus.INACTIVE
        );

        verify(
                spaceRepository
        ).saveAll(
                List.of(
                        space
                )
        );
    }

    @Test
    @DisplayName(
            "Approve venue -> set APPROVE and clear previous block reason"
    )
    void givenBlockedVenue_whenApproveVenue_thenClearBlockReason() {

        Long venueId = 1L;

        String moderatorEmail =
                "moderator@test.com";

        Venue venue =
                mock(Venue.class);

        User owner =
                mock(User.class);

        User moderator =
                mock(User.class);

        VenueResponse response =
                VenueResponse.builder()
                        .id(venueId)
                        .status(VenueStatus.APPROVE)
                        .blockReason(null)
                        .build();

        given(
                venueRepository.findByIdAndDeletedFalse(
                        venueId
                )
        ).willReturn(
                Optional.of(
                        venue
                )
        );

        given(
                userRepository.findByEmail(
                        moderatorEmail
                )
        ).willReturn(
                Optional.of(
                        moderator
                )
        );

        given(
                venue.getOwner()
        ).willReturn(
                owner
        );

        given(
                owner.getId()
        ).willReturn(
                10L
        );

        given(
                moderator.getId()
        ).willReturn(
                20L
        );

        given(
                venueRepository.save(
                        venue
                )
        ).willReturn(
                venue
        );

        given(
                venueMapper.toVenueResponse(
                        venue
                )
        ).willReturn(
                response
        );

        VenueResponse result =
                venueService.approveVenue(
                        venueId,
                        moderatorEmail
                );

        assertNotNull(
                result
        );

        assertEquals(
                VenueStatus.APPROVE,
                result.getStatus()
        );

        assertNull(
                result.getBlockReason()
        );

        verify(
                venue
        ).setStatus(
                VenueStatus.APPROVE
        );

        verify(
                venue
        ).setBlockReason(
                null
        );

        verify(
                venueRepository
        ).save(
                venue
        );

        verify(
                spaceRepository,
                never()
        ).findByVenueId(
                venueId
        );
    }
}