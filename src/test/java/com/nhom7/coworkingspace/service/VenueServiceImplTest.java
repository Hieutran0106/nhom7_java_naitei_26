package com.nhom7.coworkingspace.service;

import com.nhom7.coworkingspace.dto.request.VenueRequest;
import com.nhom7.coworkingspace.dto.response.PageResponse;
import com.nhom7.coworkingspace.dto.response.VenueResponse;
import com.nhom7.coworkingspace.entity.Amenity;
import com.nhom7.coworkingspace.entity.Role;
import com.nhom7.coworkingspace.entity.User;
import com.nhom7.coworkingspace.entity.Venue;
import com.nhom7.coworkingspace.exception.AppException;
import com.nhom7.coworkingspace.exception.VenueNotFoundException;
import com.nhom7.coworkingspace.mapper.VenueMapper;
import com.nhom7.coworkingspace.repository.AmenityRepository;
import com.nhom7.coworkingspace.repository.UserRepository;
import com.nhom7.coworkingspace.repository.VenueRepository;
import com.nhom7.coworkingspace.service.impl.VenueServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("VenueServiceImpl - Unit Tests")
class VenueServiceImplTest {

    @Mock
    private VenueRepository venueRepository;

    @Mock
    private AmenityRepository amenityRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private VenueMapper venueMapper;

    private VenueServiceImpl venueService;

    private static final String HOST_EMAIL = "host@coworking.test";

    @BeforeEach
    void setUp() {
        venueService = new VenueServiceImpl(venueRepository, amenityRepository, userRepository, venueMapper);
    }

    private User hostUser(Long id) {
        return User.builder()
                .id(id)
                .email(HOST_EMAIL)
                .name("Host User")
                .roles(Set.of(Role.builder().id(1L).name("HOST").build()))
                .build();
    }

    private User nonHostUser(Long id) {
        return User.builder()
                .id(id)
                .email(HOST_EMAIL)
                .name("Regular User")
                .roles(Set.of(Role.builder().id(2L).name("USER").build()))
                .build();
    }

    @Nested
    @DisplayName("createVenue")
    class CreateVenueTests {

        @Test
        @DisplayName("HOST creates venue successfully, owner resolved from SecurityContext (never from request)")
        void createVenue_Success() {
            User host = hostUser(1L);
            VenueRequest request = VenueRequest.builder()
                    .name("Innovation Hub")
                    .city("Hanoi")
                    .amenityIds(Set.of(10L))
                    .build();

            Amenity amenity = Amenity.builder().id(10L).name("Wifi").build();
            Venue savedVenue = Venue.builder().id(100L).owner(host).name("Innovation Hub").deleted(false).build();
            VenueResponse expectedResponse = VenueResponse.builder().id(100L).ownerId(1L).name("Innovation Hub").build();

            given(userRepository.findByEmail(HOST_EMAIL)).willReturn(Optional.of(host));
            given(amenityRepository.findAllById(Set.of(10L))).willReturn(List.of(amenity));
            given(venueRepository.save(any(Venue.class))).willReturn(savedVenue);
            given(venueMapper.toVenueResponse(savedVenue)).willReturn(expectedResponse);

            VenueResponse response = venueService.createVenue(request, HOST_EMAIL);

            assertThat(response.getId()).isEqualTo(100L);
            assertThat(response.getOwnerId()).isEqualTo(1L);

            org.mockito.ArgumentCaptor<Venue> captor = org.mockito.ArgumentCaptor.forClass(Venue.class);
            verify(venueRepository).save(captor.capture());
            assertThat(captor.getValue().getOwner()).isEqualTo(host);
            assertThat(captor.getValue().getDeleted()).isFalse();
        }

        @Test
        @DisplayName("Non-HOST user creates venue -> 403 with venue.host.required message")
        void createVenue_NonHost_Forbidden() {
            User user = nonHostUser(2L);
            VenueRequest request = VenueRequest.builder().name("Some Venue").build();

            given(userRepository.findByEmail(HOST_EMAIL)).willReturn(Optional.of(user));

            assertThatThrownBy(() -> venueService.createVenue(request, HOST_EMAIL))
                    .isInstanceOf(AppException.class)
                    .hasMessage("venue.host.required")
                    .extracting("status")
                    .isEqualTo(HttpStatus.FORBIDDEN);

            verify(venueRepository, never()).save(any(Venue.class));
        }

        @Test
        @DisplayName("Invalid amenity id -> 400 with amenity.not.found message")
        void createVenue_InvalidAmenity_BadRequest() {
            User host = hostUser(1L);
            VenueRequest request = VenueRequest.builder()
                    .name("Innovation Hub")
                    .amenityIds(Set.of(10L, 20L))
                    .build();

            given(userRepository.findByEmail(HOST_EMAIL)).willReturn(Optional.of(host));
            given(amenityRepository.findAllById(Set.of(10L, 20L)))
                    .willReturn(List.of(Amenity.builder().id(10L).name("Wifi").build()));

            assertThatThrownBy(() -> venueService.createVenue(request, HOST_EMAIL))
                    .isInstanceOf(AppException.class)
                    .hasMessage("amenity.not.found")
                    .extracting("status")
                    .isEqualTo(HttpStatus.BAD_REQUEST);

            verify(venueRepository, never()).save(any(Venue.class));
        }
    }

    @Nested
    @DisplayName("getMyVenues")
    class GetMyVenuesTests {

        @Test
        @DisplayName("Returns only the authenticated HOST's non-deleted venues (paginated)")
        void getMyVenues_ReturnsOwnNonDeletedVenues() {
            User host = hostUser(1L);
            Venue venue = Venue.builder().id(100L).owner(host).name("Innovation Hub").deleted(false).build();
            VenueResponse response = VenueResponse.builder().id(100L).ownerId(1L).name("Innovation Hub").build();

            given(userRepository.findByEmail(HOST_EMAIL)).willReturn(Optional.of(host));
            given(venueRepository.findByOwnerIdAndDeletedFalse(eq(1L), any(Pageable.class)))
                    .willReturn(new PageImpl<>(List.of(venue)));
            given(venueMapper.toVenueResponse(venue)).willReturn(response);

            PageResponse<VenueResponse> result = venueService.getMyVenues(HOST_EMAIL, 0, 10);

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getId()).isEqualTo(100L);
            verify(venueRepository).findByOwnerIdAndDeletedFalse(eq(1L), any(Pageable.class));
        }

        @Test
        @DisplayName("Non-HOST user cannot list venues -> 403")
        void getMyVenues_NonHost_Forbidden() {
            given(userRepository.findByEmail(HOST_EMAIL)).willReturn(Optional.of(nonHostUser(2L)));

            assertThatThrownBy(() -> venueService.getMyVenues(HOST_EMAIL, 0, 10))
                    .isInstanceOf(AppException.class)
                    .hasMessage("venue.host.required")
                    .extracting("status")
                    .isEqualTo(HttpStatus.FORBIDDEN);
        }
    }

    @Nested
    @DisplayName("updateVenue")
    class UpdateVenueTests {

        @Test
        @DisplayName("Owner HOST updates own venue successfully")
        void updateVenue_Owner_Success() {
            User host = hostUser(1L);
            Venue existingVenue = Venue.builder().id(100L).owner(host).name("Old Name").deleted(false).build();
            VenueRequest request = VenueRequest.builder().name("New Name").city("Hanoi").build();
            VenueResponse response = VenueResponse.builder().id(100L).ownerId(1L).name("New Name").build();

            given(userRepository.findByEmail(HOST_EMAIL)).willReturn(Optional.of(host));
            given(venueRepository.findByIdAndDeletedFalse(100L)).willReturn(Optional.of(existingVenue));
            given(venueRepository.save(existingVenue)).willReturn(existingVenue);
            given(venueMapper.toVenueResponse(existingVenue)).willReturn(response);

            VenueResponse result = venueService.updateVenue(100L, request, HOST_EMAIL);

            assertThat(result.getName()).isEqualTo("New Name");
            assertThat(existingVenue.getName()).isEqualTo("New Name");
            assertThat(existingVenue.getCity()).isEqualTo("Hanoi");
        }

        @Test
        @DisplayName("HOST updates a venue owned by another HOST -> 403 with venue.access.denied message")
        void updateVenue_NotOwner_Forbidden() {
            User otherHost = hostUser(1L);
            User currentHost = hostUser(2L);
            Venue existingVenue = Venue.builder().id(100L).owner(otherHost).name("Old Name").deleted(false).build();
            VenueRequest request = VenueRequest.builder().name("New Name").build();

            given(userRepository.findByEmail(HOST_EMAIL)).willReturn(Optional.of(currentHost));
            given(venueRepository.findByIdAndDeletedFalse(100L)).willReturn(Optional.of(existingVenue));

            assertThatThrownBy(() -> venueService.updateVenue(100L, request, HOST_EMAIL))
                    .isInstanceOf(AppException.class)
                    .hasMessage("venue.access.denied")
                    .extracting("status")
                    .isEqualTo(HttpStatus.FORBIDDEN);

            verify(venueRepository, never()).save(any(Venue.class));
        }

        @Test
        @DisplayName("Updating a non-existent (or soft-deleted) venue -> 404 with venue.not.found message")
        void updateVenue_NotFound() {
            User host = hostUser(1L);
            VenueRequest request = VenueRequest.builder().name("New Name").build();

            given(userRepository.findByEmail(HOST_EMAIL)).willReturn(Optional.of(host));
            given(venueRepository.findByIdAndDeletedFalse(999L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> venueService.updateVenue(999L, request, HOST_EMAIL))
                    .isInstanceOf(VenueNotFoundException.class)
                    .hasMessage("venue.not.found")
                    .extracting("status")
                    .isEqualTo(HttpStatus.NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("deleteVenue")
    class DeleteVenueTests {

        @Test
        @DisplayName("Owner HOST soft deletes own venue (flag set, not physically removed)")
        void deleteVenue_Owner_SoftDeletes() {
            User host = hostUser(1L);
            Venue existingVenue = Venue.builder().id(100L).owner(host).name("Venue").deleted(false).build();

            given(userRepository.findByEmail(HOST_EMAIL)).willReturn(Optional.of(host));
            given(venueRepository.findByIdAndDeletedFalse(100L)).willReturn(Optional.of(existingVenue));

            venueService.deleteVenue(100L, HOST_EMAIL);

            assertThat(existingVenue.getDeleted()).isTrue();
            verify(venueRepository).save(existingVenue);
            verify(venueRepository, never()).delete(any(Venue.class));
            verify(venueRepository, never()).deleteById(any(Long.class));
        }

        @Test
        @DisplayName("HOST deletes a venue owned by another HOST -> 403 with venue.access.denied message")
        void deleteVenue_NotOwner_Forbidden() {
            User otherHost = hostUser(1L);
            User currentHost = hostUser(2L);
            Venue existingVenue = Venue.builder().id(100L).owner(otherHost).name("Venue").deleted(false).build();

            given(userRepository.findByEmail(HOST_EMAIL)).willReturn(Optional.of(currentHost));
            given(venueRepository.findByIdAndDeletedFalse(100L)).willReturn(Optional.of(existingVenue));

            assertThatThrownBy(() -> venueService.deleteVenue(100L, HOST_EMAIL))
                    .isInstanceOf(AppException.class)
                    .hasMessage("venue.access.denied")
                    .extracting("status")
                    .isEqualTo(HttpStatus.FORBIDDEN);

            verify(venueRepository, never()).save(any(Venue.class));
        }

        @Test
        @DisplayName("Deleting a non-existent (or already soft-deleted) venue -> 404 with venue.not.found message")
        void deleteVenue_NotFound() {
            User host = hostUser(1L);

            given(userRepository.findByEmail(HOST_EMAIL)).willReturn(Optional.of(host));
            given(venueRepository.findByIdAndDeletedFalse(999L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> venueService.deleteVenue(999L, HOST_EMAIL))
                    .isInstanceOf(VenueNotFoundException.class)
                    .hasMessage("venue.not.found")
                    .extracting("status")
                    .isEqualTo(HttpStatus.NOT_FOUND);
        }
    }
}
