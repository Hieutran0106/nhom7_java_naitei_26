package com.nhom7.coworkingspace.controller.api;

import com.nhom7.coworkingspace.dto.request.UpdateBookingStatusRequest;
import com.nhom7.coworkingspace.dto.response.ApiResponse;
import com.nhom7.coworkingspace.dto.response.BookingResponse;
import com.nhom7.coworkingspace.service.BookingService;
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
import org.springframework.web.bind.annotation.*;

import java.util.Locale;

@RestController
@RequestMapping("/api/host/bookings")
@RequiredArgsConstructor
@Tag(name = "Host Booking API", description = "Endpoints for Host to manage bookings on the spaces they own")
@SecurityRequirement(name = "BearerAuth")
public class HostBookingController {

    private final BookingService bookingService;
    private final MessageSource messageSource;

    // Only the Host who owns the Space behind this booking may approve/reject it, enforced in
    // BookingServiceImpl (booking must also still be PENDING).
    @PutMapping("/{bookingId}/status")
    @PreAuthorize("hasRole('HOST')")
    @Operation(
            summary = "Approve or Reject a Booking",
            description = "Allows the Host who owns the Space to approve or reject a PENDING booking made on it."
    )
    public ResponseEntity<ApiResponse<BookingResponse>> updateBookingStatus(
            @PathVariable Long bookingId,
            @Valid @RequestBody UpdateBookingStatusRequest request,
            Authentication authentication) {
        BookingResponse response = bookingService.updateBookingStatusByHost(
                bookingId, request.getStatus(), authentication.getName());
        Locale locale = LocaleContextHolder.getLocale();
        String message = messageSource.getMessage("booking.status.updated", null, locale);
        return ResponseEntity.ok(ApiResponse.success(response, message));
    }
}
