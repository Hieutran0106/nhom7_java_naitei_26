package com.nhom7.coworkingspace.mapper;

import com.nhom7.coworkingspace.dto.response.BookingResponse;
import com.nhom7.coworkingspace.entity.Booking;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface BookingMapper {

    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "userEmail", source = "user.email")
    @Mapping(target = "userName", source = "user.name")

    @Mapping(target = "venueId", source = "space.venue.id")
    @Mapping(target = "venueName", source = "space.venue.name")

    @Mapping(target = "spaceId", source = "space.id")
    @Mapping(target = "spaceName", source = "space.name")

    @Mapping(target = "paymentMethod", ignore = true)
    BookingResponse toBookingResponse(Booking booking);
}