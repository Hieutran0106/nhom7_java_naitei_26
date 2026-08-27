package com.nhom7.coworkingspace.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName(
        "Moderator Booking Detail Modal UI"
)
class ModeratorBookingDetailModalUiTest {

    private static final Path TEMPLATE_PATH =
            Path.of(
                    "src/main/resources/templates/moderator/bookings.html"
            );

    @Test
    @DisplayName(
            "Booking list page should provide booking detail modal"
    )
    void bookingListPageShouldProvideBookingDetailModal()
            throws IOException {

        assertTrue(
                Files.exists(TEMPLATE_PATH),
                "moderator/bookings.html must exist"
        );

        String html =
                Files.readString(TEMPLATE_PATH);

        assertAll(
                () -> assertTrue(
                        html.contains(
                                "class=\"detail-button\""
                        ),
                        "Each booking row must provide a detail button"
                ),

                () -> assertTrue(
                        html.contains(
                                "data-booking-id"
                        ),
                        "Detail button must carry booking ID"
                ),

                () -> assertTrue(
                        html.contains(
                                "id=\"booking-detail-modal\""
                        ),
                        "Booking detail modal is required"
                ),

                () -> assertTrue(
                        html.contains(
                                "id=\"detail-user-name\""
                        ),
                        "Modal must display booking user name"
                ),

                () -> assertTrue(
                        html.contains(
                                "id=\"detail-user-email\""
                        ),
                        "Modal must display booking user email"
                ),

                () -> assertTrue(
                        html.contains(
                                "id=\"detail-venue-name\""
                        ),
                        "Modal must display Venue"
                ),

                () -> assertTrue(
                        html.contains(
                                "id=\"detail-space-name\""
                        ),
                        "Modal must display Space"
                ),

                () -> assertTrue(
                        html.contains(
                                "id=\"detail-start-time\""
                        ),
                        "Modal must display booking start time"
                ),

                () -> assertTrue(
                        html.contains(
                                "id=\"detail-end-time\""
                        ),
                        "Modal must display booking end time"
                ),

                () -> assertTrue(
                        html.contains(
                                "id=\"detail-total-price\""
                        ),
                        "Modal must display total price"
                ),

                () -> assertTrue(
                        html.contains(
                                "id=\"detail-payment-method\""
                        ),
                        "Modal must display payment method"
                ),

                () -> assertTrue(
                        html.contains(
                                "id=\"detail-status\""
                        ),
                        "Modal must display booking status"
                ),

                () -> assertTrue(
                        html.contains(
                                "fetch("
                        ),
                        "Booking detail must be loaded from API"
                ),

                () -> assertTrue(
                        html.contains(
                                "/api/moderator/bookings/"
                        ),
                        "Detail modal must call moderator booking detail API"
                )
        );
    }
}