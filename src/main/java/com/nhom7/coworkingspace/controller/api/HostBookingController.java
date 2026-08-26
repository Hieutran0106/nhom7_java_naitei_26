package com.nhom7.coworkingspace.controller.api;

import com.nhom7.coworkingspace.dto.response.ApiResponse;
import com.nhom7.coworkingspace.dto.response.BookingResponse;
import com.nhom7.coworkingspace.dto.response.PageResponse;
import com.nhom7.coworkingspace.exception.AppException;
import com.nhom7.coworkingspace.service.BookingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Locale;

@RestController
@RequestMapping("/api/host/bookings")
@RequiredArgsConstructor
@Tag(name = "View spaces booking state", description = "Endpoints for HOST to view bookings made on their own Spaces and approve/reject bookings")
@SecurityRequirement(name = "BearerAuth")
public class HostBookingController {

    private final BookingService bookingService;
    private final MessageSource messageSource;

    /**
     * List all bookings made on Spaces owned by the currently authenticated HOST.
     *
     * <p>Only bookings for Spaces belonging to Venues owned by the caller are returned;
     * a HOST can never see bookings belonging to another HOST's Spaces.</p>
     */
    @GetMapping
    @PreAuthorize("hasRole('HOST')")
    @Operation(
            summary = "View all booking of host's spaces",
            description = "Allows an authenticated HOST to retrieve the paginated list of bookings made on Spaces belonging to their own Venues."
    )
    public ResponseEntity<ApiResponse<PageResponse<BookingResponse>>> getMyBookings(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication
    ) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AppException("auth.token.missing", HttpStatus.UNAUTHORIZED);
        }

        PageResponse<BookingResponse> response =
                bookingService.getBookingsForHost(authentication.getName(), page, size);
        Locale locale = LocaleContextHolder.getLocale();
        String messageKey = response.getContent().isEmpty() ? "booking.list.empty" : "booking.list.fetched";
        String message = messageSource.getMessage(messageKey, null, locale);
        return ResponseEntity.ok(ApiResponse.success(response, message));
    }
}
