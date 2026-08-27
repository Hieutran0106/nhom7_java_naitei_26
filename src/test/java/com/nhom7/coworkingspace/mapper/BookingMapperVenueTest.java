package com.nhom7.coworkingspace.mapper;

import com.nhom7.coworkingspace.dto.response.BookingResponse;
import com.nhom7.coworkingspace.entity.Booking;
import com.nhom7.coworkingspace.entity.Space;
import com.nhom7.coworkingspace.entity.Venue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName(
        "BookingMapper - Venue mapping"
)
class BookingMapperVenueTest {

    private final BookingMapper bookingMapper =
            Mappers.getMapper(
                    BookingMapper.class
            );

    @Test
    @DisplayName(
            "Booking mapper should map venue ID and venue name from booking space"
    )
    void shouldMapVenueInformation() {

        Venue venue =
                Venue.builder()
                        .id(15L)
                        .name("Hanoi Central Workspace")
                        .build();

        Space space =
                Space.builder()
                        .id(5L)
                        .name("Meeting Room 1")
                        .venue(venue)
                        .build();

        Booking booking =
                Booking.builder()
                        .id(100L)
                        .space(space)
                        .build();

        BookingResponse response =
                bookingMapper.toBookingResponse(
                        booking
                );

        assertEquals(
                15L,
                response.getVenueId()
        );

        assertEquals(
                "Hanoi Central Workspace",
                response.getVenueName()
        );
    }
}