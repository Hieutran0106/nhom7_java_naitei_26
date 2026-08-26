package com.nhom7.coworkingspace.dto.request;

import com.nhom7.coworkingspace.enums.VenueStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Request body to update a venue's moderation status")
public class UpdateVenueStatusRequest {

    @NotNull(message = "Status must not be null")
    @Schema(description = "New venue status", example = "APPROVE", allowableValues = {"PENDING", "APPROVE", "BLOCKED"})
    private VenueStatus status;
}
