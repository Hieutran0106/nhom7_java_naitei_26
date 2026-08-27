package com.nhom7.coworkingspace.service.impl;

import com.nhom7.coworkingspace.dto.request.VenueRequest;
import com.nhom7.coworkingspace.dto.response.PageResponse;
import com.nhom7.coworkingspace.dto.response.VenueResponse;
import com.nhom7.coworkingspace.entity.Amenity;
import com.nhom7.coworkingspace.entity.Space;
import com.nhom7.coworkingspace.entity.User;
import com.nhom7.coworkingspace.entity.Venue;
import com.nhom7.coworkingspace.enums.SpaceStatus;
import com.nhom7.coworkingspace.enums.VenueStatus;
import com.nhom7.coworkingspace.exception.AppException;
import com.nhom7.coworkingspace.exception.VenueNotFoundException;
import com.nhom7.coworkingspace.mapper.VenueMapper;
import com.nhom7.coworkingspace.repository.AmenityRepository;
import com.nhom7.coworkingspace.repository.SpaceRepository;
import com.nhom7.coworkingspace.repository.UserRepository;
import com.nhom7.coworkingspace.repository.VenueRepository;
import com.nhom7.coworkingspace.service.VenueService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class VenueServiceImpl implements VenueService {

    private static final String HOST_ROLE = "HOST";

    private final VenueRepository venueRepository;
    private final AmenityRepository amenityRepository;
    private final UserRepository userRepository;
    private final SpaceRepository spaceRepository;
    private final VenueMapper venueMapper;

    @Override
    @Transactional
    public VenueResponse createVenue(
            VenueRequest request,
            String hostEmail
    ) {
        User host = resolveHostUser(hostEmail);
        Set<Amenity> amenities =
                resolveAmenities(request.getAmenityIds());

        Venue venue =
                Venue.builder()
                        .owner(host)
                        .name(request.getName())
                        .description(request.getDescription())
                        .address(request.getAddress())
                        .city(request.getCity())
                        .street(request.getStreet())
                        .latitude(request.getLatitude())
                        .longitude(request.getLongitude())
                        .status(VenueStatus.PENDING)
                        .amenities(amenities)
                        .deleted(false)
                        .build();

        Venue savedVenue =
                venueRepository.save(venue);

        return venueMapper.toVenueResponse(savedVenue);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<VenueResponse> getMyVenues(
            String hostEmail,
            int page,
            int size
    ) {
        User host =
                resolveHostUser(hostEmail);

        Pageable pageable =
                createPageable(page, size);

        Page<Venue> venuePage =
                venueRepository.findByOwnerIdAndDeletedFalse(
                        host.getId(),
                        pageable
                );

        return PageResponse.fromPage(
                venuePage.map(venueMapper::toVenueResponse)
        );
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<VenueResponse> getVenuesForModerator(
            VenueStatus status,
            int page,
            int size
    ) {
        Pageable pageable =
                createPageable(page, size);

        Page<Venue> venuePage;

        if (status == null) {
            venuePage =
                    venueRepository.findByDeletedFalse(
                            pageable
                    );
        } else {
            venuePage =
                    venueRepository.findByStatusAndDeletedFalse(
                            status,
                            pageable
                    );
        }

        return PageResponse.fromPage(
                venuePage.map(venueMapper::toVenueResponse)
        );
    }

    @Override
    @Transactional
    public VenueResponse updateVenue(
            Long venueId,
            VenueRequest request,
            String hostEmail
    ) {
        User host =
                resolveHostUser(hostEmail);

        Venue venue =
                getActiveVenueOrThrow(venueId);

        assertOwnership(venue, host);

        Set<Amenity> amenities =
                resolveAmenities(request.getAmenityIds());

        venue.setName(request.getName());
        venue.setDescription(request.getDescription());
        venue.setAddress(request.getAddress());
        venue.setCity(request.getCity());
        venue.setStreet(request.getStreet());
        venue.setLatitude(request.getLatitude());
        venue.setLongitude(request.getLongitude());
        venue.setAmenities(amenities);

        Venue savedVenue =
                venueRepository.save(venue);

        return venueMapper.toVenueResponse(savedVenue);
    }

    @Override
    @Transactional
    public VenueResponse updateVenueStatus(
            Long venueId,
            VenueStatus newStatus,
            String moderatorEmail
    ) {
        Venue venue =
                getActiveVenueOrThrow(venueId);

        User moderator =
                userRepository.findByEmail(moderatorEmail)
                        .orElseThrow(
                                () -> new AppException(
                                        "user.not.found",
                                        HttpStatus.NOT_FOUND
                                )
                        );

        if (venue.getOwner()
                .getId()
                .equals(moderator.getId())) {

            throw new AppException(
                    "venue.cannot.moderate.self",
                    HttpStatus.FORBIDDEN
            );
        }

        if (venue.getStatus() == newStatus) {
            return venueMapper.toVenueResponse(venue);
        }

        venue.setStatus(newStatus);

        Venue savedVenue =
                venueRepository.save(venue);

        if (newStatus == VenueStatus.BLOCKED) {
            deactivateSpaces(venueId);
        }

        return venueMapper.toVenueResponse(savedVenue);
    }

    @Override
    @Transactional
    public void deleteVenue(
            Long venueId,
            String hostEmail
    ) {
        User host =
                resolveHostUser(hostEmail);

        Venue venue =
                getActiveVenueOrThrow(venueId);

        assertOwnership(venue, host);

        venue.setDeleted(true);

        venueRepository.save(venue);

        deactivateSpaces(venueId);
    }

    private Pageable createPageable(
            int page,
            int size
    ) {
        return PageRequest.of(
                Math.max(0, page),
                Math.max(1, size),
                Sort.by(
                        Sort.Direction.DESC,
                        "id"
                )
        );
    }

    private void deactivateSpaces(
            Long venueId
    ) {
        List<Space> spaces =
                spaceRepository.findByVenueId(venueId);

        spaces.forEach(
                space -> space.setStatus(
                        SpaceStatus.INACTIVE
                )
        );

        spaceRepository.saveAll(spaces);
    }

    private User resolveHostUser(
            String email
    ) {
        User user =
                userRepository.findByEmail(email)
                        .orElseThrow(
                                () -> new AppException(
                                        "user.not.found",
                                        HttpStatus.NOT_FOUND
                                )
                        );

        boolean isHost =
                user.getRoles()
                        .stream()
                        .anyMatch(
                                role -> HOST_ROLE.equalsIgnoreCase(
                                        role.getName()
                                )
                        );

        if (!isHost) {
            throw new AppException(
                    "venue.host.required",
                    HttpStatus.FORBIDDEN
            );
        }

        return user;
    }

    private Venue getActiveVenueOrThrow(
            Long venueId
    ) {
        return venueRepository
                .findByIdAndDeletedFalse(venueId)
                .orElseThrow(
                        VenueNotFoundException::new
                );
    }

    private void assertOwnership(
            Venue venue,
            User host
    ) {
        if (!venue.getOwner()
                .getId()
                .equals(host.getId())) {

            throw new AppException(
                    "venue.access.denied",
                    HttpStatus.FORBIDDEN
            );
        }
    }

    private Set<Amenity> resolveAmenities(
            Set<Long> amenityIds
    ) {
        if (amenityIds == null
                || amenityIds.isEmpty()) {

            return new HashSet<>();
        }

        List<Amenity> foundAmenities =
                amenityRepository.findAllById(
                        amenityIds
                );

        if (foundAmenities.size()
                != amenityIds.size()) {

            throw new AppException(
                    "amenity.not.found",
                    HttpStatus.BAD_REQUEST
            );
        }

        return new HashSet<>(
                foundAmenities
        );
    }
}