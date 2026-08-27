package com.nhom7.coworkingspace.dto.response;

import com.nhom7.coworkingspace.enums.BookingStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingResponse {

    private Long id;

    private Long userId;
    private String userEmail;
    private String userName;

    private Long venueId;
    private String venueName;

    private Long spaceId;
    private String spaceName;

    private LocalDateTime startTime;
    private LocalDateTime endTime;

    private BigDecimal totalPrice;

    private BookingStatus status;

    private LocalDateTime createdAt;

    private String paymentMethod;
}