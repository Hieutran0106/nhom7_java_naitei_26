package com.nhom7.coworkingspace.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName(
        "Moderator Booking Detail Modal Style"
)
class ModeratorBookingDetailModalStyleTest {

    private static final Path CSS_PATH =
            Path.of(
                    "src/main/resources/static/css/moderator-bookings.css"
            );

    @Test
    @DisplayName(
            "Booking detail modal should provide required styles"
    )
    void bookingDetailModalShouldProvideRequiredStyles()
            throws IOException {

        assertTrue(
                Files.exists(CSS_PATH),
                "moderator-bookings.css must exist"
        );

        String css =
                Files.readString(CSS_PATH);

        assertAll(
                () -> assertTrue(
                        css.contains(
                                ".detail-button"
                        ),
                        "Detail button style is required"
                ),

                () -> assertTrue(
                        css.contains(
                                ".modal {"
                        ),
                        "Modal container style is required"
                ),

                () -> assertTrue(
                        css.contains(
                                ".modal[hidden]"
                        ),
                        "Hidden modal style is required"
                ),

                () -> assertTrue(
                        css.contains(
                                ".modal-backdrop"
                        ),
                        "Modal backdrop style is required"
                ),

                () -> assertTrue(
                        css.contains(
                                ".modal-dialog"
                        ),
                        "Modal dialog style is required"
                ),

                () -> assertTrue(
                        css.contains(
                                ".modal-header"
                        ),
                        "Modal header style is required"
                ),

                () -> assertTrue(
                        css.contains(
                                ".modal-close"
                        ),
                        "Modal close button style is required"
                ),

                () -> assertTrue(
                        css.contains(
                                ".detail-grid"
                        ),
                        "Modal detail grid style is required"
                ),

                () -> assertTrue(
                        css.contains(
                                ".detail-item"
                        ),
                        "Modal detail item style is required"
                ),

                () -> assertTrue(
                        css.contains(
                                ".modal-error"
                        ),
                        "Modal error state style is required"
                )
        );
    }
}