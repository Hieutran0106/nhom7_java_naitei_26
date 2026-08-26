package com.nhom7.coworkingspace.dto.response;

import com.nhom7.coworkingspace.enums.VenueStatus;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VenueResponse {

    private Long id;

    // Owner (HOST) information
    private Long ownerId;
    private String ownerName;

    private String name;
    private String description;
    private String address;
    private String city;
    private String street;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private VenueStatus status;

    private List<AmenityResponse> amenities;
}
