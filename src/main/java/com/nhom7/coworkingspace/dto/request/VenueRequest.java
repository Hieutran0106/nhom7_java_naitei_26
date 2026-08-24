package com.nhom7.coworkingspace.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VenueRequest {

    @NotBlank(message = "{validation.venue.name.required}")
    @Size(max = 200, message = "{validation.venue.name.size}")
    private String name;

    private String description;

    @Size(max = 255, message = "{validation.venue.address.size}")
    private String address;

    @Size(max = 100, message = "{validation.venue.city.size}")
    private String city;

    @Size(max = 150, message = "{validation.venue.street.size}")
    private String street;

    @DecimalMin(value = "-90.0", message = "{validation.venue.latitude.range}")
    @DecimalMax(value = "90.0", message = "{validation.venue.latitude.range}")
    private BigDecimal latitude;

    @DecimalMin(value = "-180.0", message = "{validation.venue.longitude.range}")
    @DecimalMax(value = "180.0", message = "{validation.venue.longitude.range}")
    private BigDecimal longitude;

    @Builder.Default
    private Set<Long> amenityIds = new HashSet<>();
}
