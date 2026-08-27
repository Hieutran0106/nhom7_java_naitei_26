package com.nhom7.coworkingspace.service;

import com.nhom7.coworkingspace.dto.request.AddSpaceManagerRequest;
import com.nhom7.coworkingspace.dto.request.SpaceCreateRequest;
import com.nhom7.coworkingspace.dto.request.SpaceSearchRequest;
import com.nhom7.coworkingspace.dto.request.SpaceUpdateRequest;
import com.nhom7.coworkingspace.dto.response.PageResponse;
import com.nhom7.coworkingspace.dto.response.SpaceResponse;

public interface SpaceService {

    /**
     * Search and filter co-working spaces dynamically.
     *
     * @param request search parameters
     * @return paginated space responses
     */
    PageResponse<SpaceResponse> searchSpaces(SpaceSearchRequest request);

    /**
     * Create a new space under a specific venue owned by the host.
     */
    SpaceResponse createSpace(Long venueId, SpaceCreateRequest request, String hostEmail);

    /**
     * Retrieve paginated list of spaces owned or managed by the authenticated host.
     */
    PageResponse<SpaceResponse> getMySpaces(String hostEmail, int page, int size);

    /**
     * Retrieve paginated list of spaces inside a specific venue.
     */
    PageResponse<SpaceResponse> getSpacesByVenue(Long venueId, int page, int size);

    /**
     * Update space details.
     */
    SpaceResponse updateSpace(Long id, SpaceUpdateRequest request, String hostEmail);

    /**
     * Delete space.
     */
    void deleteSpace(Long id, String hostEmail);

    /**
     * Add a manager (host) to manage a space.
     */
    SpaceResponse addManagerToSpace(Long id, AddSpaceManagerRequest request, String hostEmail);
}
