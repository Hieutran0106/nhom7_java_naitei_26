package com.nhom7.coworkingspace.dto.request;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingRequest {

    @NotNull(message = "{validation.booking.space.required}")
    private Long spaceId;

    @NotNull(message = "{validation.booking.start.time.required}")
    @Future(message = "{validation.booking.start.time.future}")
    private LocalDateTime startTime;

    @NotNull(message = "{validation.booking.end.time.required}")
    @Future(message = "{validation.booking.end.time.future}")
    private LocalDateTime endTime;
}
