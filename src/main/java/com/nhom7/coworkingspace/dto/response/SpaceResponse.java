package com.nhom7.coworkingspace.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SpaceResponse {

    private Long id;

    // Venue information
    private Long venueId;
    private String venueName;
    private String venueAddress;
    private String venueCity;
    private String venueStreet;
    private BigDecimal venueLatitude;
    private BigDecimal venueLongitude;

    // Space information
    private String name;
    private String type;
    private Integer capacity;
    private String description;
    private BigDecimal price;
    private String priceUnit;
    private LocalTime openTime;
    private LocalTime closeTime;
    private String status;
}
