package com.nhom7.coworkingspace.dto;

import com.nhom7.coworkingspace.dto.response.BookingResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName(
        "BookingResponse - Venue fields contract"
)
class BookingResponseVenueFieldsTest {

    @Test
    @DisplayName(
            "Booking response should expose venue ID and venue name"
    )
    void bookingResponseShouldExposeVenueInformation() {

        BookingResponse response =
                BookingResponse.builder()
                        .venueId(15L)
                        .venueName("Hanoi Central Workspace")
                        .build();

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