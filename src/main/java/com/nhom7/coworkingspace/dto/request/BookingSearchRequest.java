package com.nhom7.coworkingspace.dto.request;

import com.nhom7.coworkingspace.enums.BookingStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Request parameters for searching and filtering bookings (Moderator/Admin)")
public class BookingSearchRequest {

    @Schema(
            description = "Keyword to search by user name, user email, or space name",
            example = "hanoi"
    )
    private String keyword;

    @Schema(
            description = "Filter by booking status",
            example = "PENDING",
            allowableValues = {
                    "PENDING",
                    "APPROVED",
                    "PAID",
                    "CONFIRMED",
                    "REJECTED",
                    "CANCELLED",
                    "COMPLETED"
            }
    )
    private BookingStatus status;

    @Schema(
            description = "Filter by user ID",
            example = "1"
    )
    private Long userId;

    @Schema(
            description = "Filter by space ID",
            example = "2"
    )
    private Long spaceId;

    @Schema(
            description = "Filter by venue ID",
            example = "3"
    )
    private Long venueId;

    @DateTimeFormat(
            iso = DateTimeFormat.ISO.DATE_TIME
    )
    @Schema(
            description = "Filter bookings starting from this date/time (ISO-8601)",
            example = "2026-08-01T00:00:00"
    )
    private LocalDateTime fromDate;

    @DateTimeFormat(
            iso = DateTimeFormat.ISO.DATE_TIME
    )
    @Schema(
            description = "Filter bookings ending before this date/time (ISO-8601)",
            example = "2026-08-31T23:59:59"
    )
    private LocalDateTime toDate;

    @Schema(
            description = "Page number (0-indexed)",
            example = "0",
            defaultValue = "0"
    )
    @Builder.Default
    private int page = 0;

    @Schema(
            description = "Number of items per page (1 - 100)",
            example = "20",
            defaultValue = "20"
    )
    @Builder.Default
    private int size = 20;

    @Schema(
            description = "Field to sort by",
            example = "id",
            defaultValue = "id",
            allowableValues = {
                    "id",
                    "startTime",
                    "endTime",
                    "totalPrice",
                    "status",
                    "createdAt"
            }
    )
    @Builder.Default
    private String sortBy = "id";

    @Schema(
            description = "Sort direction",
            example = "ASC",
            defaultValue = "ASC",
            allowableValues = {
                    "ASC",
                    "DESC"
            }
    )
    @Builder.Default
    private String sortDir = "ASC";
}