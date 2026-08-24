package com.nhom7.coworkingspace.service.impl;

import com.nhom7.coworkingspace.dto.request.SpaceSearchRequest;
import com.nhom7.coworkingspace.dto.response.PageResponse;
import com.nhom7.coworkingspace.dto.response.SpaceResponse;
import com.nhom7.coworkingspace.entity.Space;
import com.nhom7.coworkingspace.mapper.SpaceMapper;
import com.nhom7.coworkingspace.repository.SpaceRepository;
import com.nhom7.coworkingspace.service.SpaceService;
import com.nhom7.coworkingspace.specification.SpaceSpecification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class SpaceServiceImpl implements SpaceService {

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "id", "name", "price", "createdAt", "type", "priceUnit");

    private final SpaceRepository spaceRepository;
    private final SpaceMapper spaceMapper;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<SpaceResponse> searchSpaces(SpaceSearchRequest request) {
        log.debug("[SpaceService] Searching spaces with params: name={}, city={}, type={}, priceUnit={}",
                request.getName(), request.getCity(), request.getType(), request.getPriceUnit());

        Sort.Direction direction = "DESC".equalsIgnoreCase(request.getSortDir())
                ? Sort.Direction.DESC
                : Sort.Direction.ASC;

        String rawSortBy = (request.getSortBy() != null) ? request.getSortBy().trim() : "id";
        String sortBy = ALLOWED_SORT_FIELDS.contains(rawSortBy) ? rawSortBy : "id";

        int page = Math.max(0, request.getPage());
        int size = Math.min(Math.max(1, request.getSize()), 100);

        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

        Specification<Space> spec = SpaceSpecification.buildSearchSpecification(request);
        Page<Space> spacePage = spaceRepository.findAll(spec, pageable);

        Page<SpaceResponse> dtoPage = spacePage.map(spaceMapper::toSpaceResponse);
        return PageResponse.fromPage(dtoPage);
    }
}

