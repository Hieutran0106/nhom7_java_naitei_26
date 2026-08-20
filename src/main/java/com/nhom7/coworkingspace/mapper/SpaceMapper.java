package com.nhom7.coworkingspace.mapper;

import com.nhom7.coworkingspace.dto.response.SpaceResponse;
import com.nhom7.coworkingspace.entity.Space;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface SpaceMapper {

    @Mapping(target = "venueId", source = "venue.id")
    @Mapping(target = "venueName", source = "venue.name")
    @Mapping(target = "venueAddress", source = "venue.address")
    @Mapping(target = "venueCity", source = "venue.city")
    @Mapping(target = "venueStreet", source = "venue.street")
    @Mapping(target = "venueLatitude", source = "venue.latitude")
    @Mapping(target = "venueLongitude", source = "venue.longitude")
    SpaceResponse toSpaceResponse(Space space);
}
