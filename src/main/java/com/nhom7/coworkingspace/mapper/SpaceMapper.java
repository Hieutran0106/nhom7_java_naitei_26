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
    @Mapping(target = "managerIds", expression = "java(mapHostsToManagerIds(space.getHosts()))")
    SpaceResponse toSpaceResponse(Space space);

    default java.util.Set<Long> mapHostsToManagerIds(java.util.Set<com.nhom7.coworkingspace.entity.User> hosts) {
        if (hosts == null) {
            return java.util.Collections.emptySet();
        }
        return hosts.stream()
                .map(com.nhom7.coworkingspace.entity.User::getId)
                .collect(java.util.stream.Collectors.toSet());
    }
}
