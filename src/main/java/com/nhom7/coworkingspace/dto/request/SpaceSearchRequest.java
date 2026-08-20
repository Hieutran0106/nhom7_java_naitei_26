package com.nhom7.coworkingspace.dto.request;

import lombok.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SpaceSearchRequest {

    private String name;
    private String city;
    private String street;
    private String address;

    /**
     * Space type (e.g. private office, working desk, meeting space)
     */
    private String type;

    private BigDecimal minPrice;
    private BigDecimal maxPrice;

    /**
     * Price unit: hour, day, month (or PER_HOUR, PER_DAY, PER_MONTH)
     */
    private String priceUnit;

    @DateTimeFormat(iso = DateTimeFormat.ISO.TIME)
    private LocalTime openTime;

    @DateTimeFormat(iso = DateTimeFormat.ISO.TIME)
    private LocalTime closeTime;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime bookingStart;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime bookingEnd;

    @Builder.Default
    private int page = 0;

    @Builder.Default
    private int size = 10;

    @Builder.Default
    private String sortBy = "id";

    @Builder.Default
    private String sortDir = "ASC";
}
