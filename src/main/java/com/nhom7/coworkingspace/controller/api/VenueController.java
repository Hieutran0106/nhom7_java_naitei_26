package com.nhom7.coworkingspace.controller.api;

import com.nhom7.coworkingspace.dto.request.VenueRequest;
import com.nhom7.coworkingspace.dto.response.ApiResponse;
import com.nhom7.coworkingspace.dto.response.PageResponse;
import com.nhom7.coworkingspace.dto.response.VenueResponse;
import com.nhom7.coworkingspace.service.VenueService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Locale;

@RestController
@RequestMapping("/api/venues")
@RequiredArgsConstructor
@Tag(name = "Venue API", description = "Endpoints for HOST venue management")
@SecurityRequirement(name = "BearerAuth")
public class VenueController {

    private final VenueService venueService;
    private final MessageSource messageSource;

    /**
     * Create a venue owned by the currently authenticated HOST.
     *
     * <p>Requires authenticated user with HOST role. The owner is always resolved
     * from the SecurityContext; it is never accepted from the client.</p>
     */
    @PostMapping
    @Operation(
            summary = "Create Venue",
            description = "Allows an authenticated HOST to create a new venue (with amenities) owned by them."
    )
    public ResponseEntity<ApiResponse<VenueResponse>> createVenue(
            @Valid @RequestBody VenueRequest request,
            Authentication authentication
    ) {
        VenueResponse response = venueService.createVenue(request, authentication.getName());
        String message = resolveMessage("venue.created");
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(HttpStatus.CREATED.value(), message, response));
    }

    /**
     * List all non-deleted venues owned by the currently authenticated HOST.
     */
    @GetMapping("/my-venues")
    @Operation(
            summary = "List My Venues",
            description = "Allows an authenticated HOST to retrieve the paginated list of their own (non-deleted) venues."
    )
    public ResponseEntity<ApiResponse<PageResponse<VenueResponse>>> getMyVenues(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication
    ) {
        PageResponse<VenueResponse> response = venueService.getMyVenues(authentication.getName(), page, size);
        String message = resolveMessage("venue.list.success");
        return ResponseEntity.ok(ApiResponse.success(response, message));
    }

    /**
     * Update a venue owned by the currently authenticated HOST.
     */
    @PutMapping("/{id}")
    @Operation(
            summary = "Update Venue",
            description = "Allows an authenticated HOST to update their own venue (with amenities). Fails with 404 if the venue does not exist, or 403 if it belongs to another HOST."
    )
    public ResponseEntity<ApiResponse<VenueResponse>> updateVenue(
            @PathVariable Long id,
            @Valid @RequestBody VenueRequest request,
            Authentication authentication
    ) {
        VenueResponse response = venueService.updateVenue(id, request, authentication.getName());
        String message = resolveMessage("venue.updated");
        return ResponseEntity.ok(ApiResponse.success(response, message));
    }

    /**
     * Soft delete a venue owned by the currently authenticated HOST.
     */
    @DeleteMapping("/{id}")
    @Operation(
            summary = "Delete Venue",
            description = "Allows an authenticated HOST to soft delete their own venue. Fails with 404 if the venue does not exist, or 403 if it belongs to another HOST."
    )
    public ResponseEntity<ApiResponse<Void>> deleteVenue(
            @PathVariable Long id,
            Authentication authentication
    ) {
        venueService.deleteVenue(id, authentication.getName());
        String message = resolveMessage("venue.deleted");
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), message, null));
    }

    private String resolveMessage(String key) {
        Locale locale = LocaleContextHolder.getLocale();
        return messageSource.getMessage(key, null, locale);
    }
}
