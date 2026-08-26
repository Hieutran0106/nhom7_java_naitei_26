package com.nhom7.coworkingspace.controller.api;

import com.nhom7.coworkingspace.dto.request.BookingSearchRequest;
import com.nhom7.coworkingspace.dto.response.ApiResponse;
import com.nhom7.coworkingspace.dto.response.BookingResponse;
import com.nhom7.coworkingspace.dto.response.PageResponse;
import com.nhom7.coworkingspace.service.BookingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Locale;

@RestController
@RequestMapping("/api/moderator/bookings")
@RequiredArgsConstructor
@Tag(name = "Moderator Booking API", description = "Endpoints for Moderator and Admin to search and manage all bookings")
@SecurityRequirement(name = "BearerAuth")
public class ModeratorBookingController {

    private final BookingService bookingService;
    private final MessageSource messageSource;

    @GetMapping
    @PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN')")
    @Operation(
            summary = "Search & Filter All Bookings (Moderator/Admin)",
            description = "Allows Moderator or Admin to search all bookings by keyword (user name, email, space name), filter by status/user/space/dates, and paginate results."
    )
    public ResponseEntity<ApiResponse<PageResponse<BookingResponse>>> searchBookings(
            @ParameterObject @ModelAttribute BookingSearchRequest request) {
        PageResponse<BookingResponse> result = bookingService.searchBookings(request);
        Locale locale = LocaleContextHolder.getLocale();
        String message = messageSource.getMessage("booking.list.fetched", null, locale);
        return ResponseEntity.ok(ApiResponse.success(result, message));
    }

    @GetMapping("/{bookingId}")
    @PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN')")
    @Operation(
            summary = "Get Booking Details by ID (Moderator/Admin)",
            description = "Allows Moderator or Admin to view full booking details including user and space information."
    )
    public ResponseEntity<ApiResponse<BookingResponse>> getBookingById(@PathVariable Long bookingId) {
        BookingResponse response = bookingService.getBookingById(bookingId);
        Locale locale = LocaleContextHolder.getLocale();
        String message = messageSource.getMessage("booking.detail.fetched", null, locale);
        return ResponseEntity.ok(ApiResponse.success(response, message));
    }
}

