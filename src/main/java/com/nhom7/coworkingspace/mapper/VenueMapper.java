package com.nhom7.coworkingspace.mapper;

import com.nhom7.coworkingspace.dto.response.AmenityResponse;
import com.nhom7.coworkingspace.dto.response.VenueResponse;
import com.nhom7.coworkingspace.entity.Amenity;
import com.nhom7.coworkingspace.entity.Venue;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface VenueMapper {

    @Mapping(target = "ownerId", source = "owner.id")
    @Mapping(target = "ownerName", source = "owner.name")
    VenueResponse toVenueResponse(Venue venue);

    AmenityResponse toAmenityResponse(Amenity amenity);
}
