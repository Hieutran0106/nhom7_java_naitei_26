package com.nhom7.coworkingspace.dto.request;

import com.nhom7.coworkingspace.enums.BookingStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Request body for a Host to approve or reject a booking")
public class UpdateBookingStatusRequest {

    @NotNull(message = "{booking.status.required}")
    @Schema(description = "New booking status", example = "APPROVED", allowableValues = {"APPROVED", "REJECTED"})
    private BookingStatus status;
}
