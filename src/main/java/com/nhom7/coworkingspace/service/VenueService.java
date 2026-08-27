package com.nhom7.coworkingspace.service;

import com.nhom7.coworkingspace.dto.request.VenueRequest;
import com.nhom7.coworkingspace.dto.response.PageResponse;
import com.nhom7.coworkingspace.dto.response.VenueResponse;
import com.nhom7.coworkingspace.enums.VenueStatus;

public interface VenueService {

    /**
     * Create a venue owned by the currently authenticated HOST.
     *
     * @param request   venue creation payload
     * @param hostEmail email of the authenticated user
     * @return created venue details
     */
    VenueResponse createVenue(
            VenueRequest request,
            String hostEmail
    );

    /**
     * List all non-deleted venues owned by the currently authenticated HOST.
     *
     * @param hostEmail email of the authenticated user
     * @param page      zero-based page index
     * @param size      page size
     * @return paginated venue responses
     */
    PageResponse<VenueResponse> getMyVenues(
            String hostEmail,
            int page,
            int size
    );

    /**
     * List non-deleted venues for Moderator/Admin.
     *
     * @param status optional moderation status filter
     * @param page   zero-based page index
     * @param size   page size
     * @return paginated venue responses
     */
    PageResponse<VenueResponse> getVenuesForModerator(
            VenueStatus status,
            int page,
            int size
    );

    /**
     * Get one non-deleted venue for Moderator/Admin.
     *
     * @param venueId venue id
     * @return venue detail
     */
    VenueResponse getVenueForModerator(
            Long venueId
    );

    /**
     * Update a venue owned by the currently authenticated HOST.
     *
     * @param venueId   id of the venue to update
     * @param request   updated venue payload
     * @param hostEmail email of the authenticated user
     * @return updated venue details
     */
    VenueResponse updateVenue(
            Long venueId,
            VenueRequest request,
            String hostEmail
    );

    /**
     * Approve or block a venue.
     *
     * @param venueId        id of the venue
     * @param newStatus      new moderation status
     * @param moderatorEmail email of authenticated moderator/admin
     * @return updated venue details
     */
    VenueResponse updateVenueStatus(
            Long venueId,
            VenueStatus newStatus,
            String moderatorEmail
    );

    /**
     * Soft delete a venue owned by the currently authenticated HOST.
     *
     * @param venueId   venue id
     * @param hostEmail authenticated HOST email
     */
    void deleteVenue(
            Long venueId,
            String hostEmail
    );
}