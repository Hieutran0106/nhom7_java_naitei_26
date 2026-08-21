package com.nhom7.coworkingspace.service;

import com.nhom7.coworkingspace.dto.request.SpaceSearchRequest;
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
}
