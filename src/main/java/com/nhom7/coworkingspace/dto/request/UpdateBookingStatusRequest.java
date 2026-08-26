package com.nhom7.coworkingspace.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Request body for a Host to approve or reject a booking")
public class UpdateBookingStatusRequest {

    // Kept as a raw String (not the BookingStatus enum) so an unrecognized value fails
    // validation in BookingServiceImpl with a clear "choose APPROVED or REJECTED" message,
    // instead of failing Jackson deserialization with an opaque 500.
    @NotBlank(message = "{booking.status.required}")
    @Schema(description = "New booking status", example = "APPROVED", allowableValues = {"APPROVED", "REJECTED"})
    private String status;
}
