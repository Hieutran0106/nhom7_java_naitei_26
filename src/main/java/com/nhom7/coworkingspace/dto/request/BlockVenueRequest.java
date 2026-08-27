package com.nhom7.coworkingspace.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(
        description = "Request body used by Moderator/Admin to block a venue"
)
public class BlockVenueRequest {

    @NotBlank(
            message = "Block reason must not be blank"
    )
    @Schema(
            description = "Reason for blocking the venue",
            example = "Thông tin Venue chưa hợp lệ"
    )
    private String reason;
}