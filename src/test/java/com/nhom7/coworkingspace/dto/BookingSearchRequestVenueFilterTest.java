package com.nhom7.coworkingspace.dto;

import com.nhom7.coworkingspace.dto.request.BookingSearchRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName(
        "BookingSearchRequest - Venue filter contract"
)
class BookingSearchRequestVenueFilterTest {

    @Test
    @DisplayName(
            "Booking search request should support venueId filter"
    )
    void bookingSearchRequestShouldSupportVenueIdFilter() {

        BookingSearchRequest request =
                BookingSearchRequest.builder()
                        .venueId(15L)
                        .build();

        assertEquals(
                15L,
                request.getVenueId()
        );
    }
}