package com.nhom7.coworkingspace.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Request parameters for paginating the current user's booking history")
public class BookingHistoryRequest {

    @Schema(description = "Page number (0-indexed)", example = "0", defaultValue = "0")
    @Builder.Default
    private int page = 0;

    @Schema(description = "Number of items per page (1 - 100)", example = "10", defaultValue = "10")
    @Builder.Default
    private int size = 10;

    @Schema(description = "Field to sort by", example = "createdAt", defaultValue = "createdAt", allowableValues = {
            "id", "startTime", "endTime", "status", "totalPrice", "createdAt" })
    @Builder.Default
    private String sortBy = "createdAt";

    @Schema(description = "Sort direction", example = "DESC", defaultValue = "DESC", allowableValues = { "ASC",
            "DESC" })
    @Builder.Default
    private String sortDir = "DESC";
}
