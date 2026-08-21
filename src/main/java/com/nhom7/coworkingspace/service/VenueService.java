package com.nhom7.coworkingspace.service;

import com.nhom7.coworkingspace.dto.request.VenueRequest;
import com.nhom7.coworkingspace.dto.response.PageResponse;
import com.nhom7.coworkingspace.dto.response.VenueResponse;

public interface VenueService {

    /**
     * Create a venue owned by the currently authenticated HOST.
     *
     * @param request   venue creation payload
     * @param hostEmail email of the authenticated user (from SecurityContext)
     * @return created venue details
     */
    VenueResponse createVenue(VenueRequest request, String hostEmail);

    /**
     * List all non-deleted venues owned by the currently authenticated HOST.
     *
     * @param hostEmail email of the authenticated user (from SecurityContext)
     * @param page      zero-based page index
     * @param size      page size
     * @return paginated venue responses
     */
    PageResponse<VenueResponse> getMyVenues(String hostEmail, int page, int size);

    /**
     * Update a venue owned by the currently authenticated HOST.
     *
     * @param venueId   id of the venue to update
     * @param request   updated venue payload
     * @param hostEmail email of the authenticated user (from SecurityContext)
     * @return updated venue details
     */
    VenueResponse updateVenue(Long venueId, VenueRequest request, String hostEmail);

    /**
     * Soft delete a venue owned by the currently authenticated HOST.
     *
     * @param venueId   id of the venue to delete
     * @param hostEmail email of the authenticated user (from SecurityContext)
     */
    void deleteVenue(Long venueId, String hostEmail);
}
