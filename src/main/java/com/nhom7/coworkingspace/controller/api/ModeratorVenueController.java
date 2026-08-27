package com.nhom7.coworkingspace.controller.api;

import com.nhom7.coworkingspace.dto.request.UpdateVenueStatusRequest;
import com.nhom7.coworkingspace.dto.response.ApiResponse;
import com.nhom7.coworkingspace.dto.response.PageResponse;
import com.nhom7.coworkingspace.dto.response.VenueResponse;
import com.nhom7.coworkingspace.enums.VenueStatus;
import com.nhom7.coworkingspace.service.VenueService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Locale;

@RestController
@RequestMapping("/api/moderator/venues")
@RequiredArgsConstructor
@Tag(
        name = "Moderator Venue API",
        description = "Endpoints for Moderator and Admin to manage venues"
)
@SecurityRequirement(name = "BearerAuth")
public class ModeratorVenueController {

    private final VenueService venueService;
    private final MessageSource messageSource;

    @GetMapping
    @PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN')")
    @Operation(
            summary = "Get Venue List",
            description = "Returns paginated non-deleted venues with an optional status filter."
    )
    public ResponseEntity<ApiResponse<PageResponse<VenueResponse>>> getVenues(
            @RequestParam(
                    value = "status",
                    required = false
            ) VenueStatus status,
            @RequestParam(
                    value = "page",
                    defaultValue = "0"
            ) int page,
            @RequestParam(
                    value = "size",
                    defaultValue = "10"
            ) int size
    ) {
        PageResponse<VenueResponse> response =
                venueService.getVenuesForModerator(
                        status,
                        page,
                        size
                );

        Locale locale =
                LocaleContextHolder.getLocale();

        String message =
                messageSource.getMessage(
                        "venue.list.success",
                        null,
                        locale
                );

        return ResponseEntity.ok(
                ApiResponse.success(
                        response,
                        message
                )
        );
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN')")
    @Operation(
            summary = "Get Venue Detail",
            description = "Returns details of one non-deleted venue for Moderator or Admin."
    )
    public ResponseEntity<ApiResponse<VenueResponse>> getVenueDetail(
            @PathVariable Long id
    ) {
        VenueResponse response =
                venueService.getVenueForModerator(
                        id
                );

        Locale locale =
                LocaleContextHolder.getLocale();

        String message =
                messageSource.getMessage(
                        "venue.list.success",
                        null,
                        locale
                );

        return ResponseEntity.ok(
                ApiResponse.success(
                        response,
                        message
                )
        );
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN')")
    @Operation(
            summary = "Update Venue Status (Approve/Block)",
            description = "Allows Moderator or Admin to change a venue's moderation status (PENDING, APPROVE, BLOCKED)."
    )
    public ResponseEntity<ApiResponse<VenueResponse>> updateVenueStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateVenueStatusRequest request,
            Authentication authentication
    ) {
        VenueResponse response =
                venueService.updateVenueStatus(
                        id,
                        request.getStatus(),
                        authentication.getName()
                );

        Locale locale =
                LocaleContextHolder.getLocale();

        String message =
                messageSource.getMessage(
                        "venue.status.updated",
                        null,
                        locale
                );

        return ResponseEntity.ok(
                ApiResponse.success(
                        response,
                        message
                )
        );
    }
}