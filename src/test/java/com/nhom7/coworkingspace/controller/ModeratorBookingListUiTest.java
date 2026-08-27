package com.nhom7.coworkingspace.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName(
        "Moderator Booking List UI"
)
class ModeratorBookingListUiTest {

    private static final Path TEMPLATE_PATH =
            Path.of(
                    "src/main/resources/templates/moderator/bookings.html"
            );

    @Test
    @DisplayName(
            "Booking list page should provide required filters, table and pagination"
    )
    void bookingListPageShouldContainRequiredUi() throws IOException {

        assertTrue(
                Files.exists(TEMPLATE_PATH),
                "moderator/bookings.html must exist"
        );

        String html =
                Files.readString(TEMPLATE_PATH);

        assertAll(
                () -> assertTrue(
                        html.contains(
                                "th:field=\"*{venueId}\""
                        ),
                        "Venue filter is required"
                ),

                () -> assertTrue(
                        html.contains(
                                "th:field=\"*{fromDate}\""
                        ),
                        "From date filter is required"
                ),

                () -> assertTrue(
                        html.contains(
                                "th:field=\"*{toDate}\""
                        ),
                        "To date filter is required"
                ),

                () -> assertTrue(
                        html.contains(
                                "th:field=\"*{status}\""
                        ),
                        "Status filter is required"
                ),

                () -> assertTrue(
                        html.contains(
                                "th:each=\"item : ${bookings.content}\""
                        ),
                        "Booking table must render booking content"
                ),

                () -> assertTrue(
                        html.contains(
                                "th:text=\"${item.venueName"
                        ),
                        "Booking table must display Venue"
                ),

                () -> assertTrue(
                        html.contains(
                                "th:text=\"${item.spaceName"
                        ),
                        "Booking table must display Space"
                ),

                () -> assertTrue(
                        html.contains(
                                "th:text=\"${item.userName"
                        ),
                        "Booking table must display booking user"
                ),

                () -> assertTrue(
                        html.contains(
                                "class=\"pagination\""
                        ),
                        "Pagination is required"
                ),

                () -> assertTrue(
                        html.contains(
                                "venueId=${searchRequest.venueId}"
                        ),
                        "Pagination must preserve Venue filter"
                ),

                () -> assertTrue(
                        html.contains(
                                "fromDate=${searchRequest.fromDate}"
                        ),
                        "Pagination must preserve fromDate filter"
                ),

                () -> assertTrue(
                        html.contains(
                                "toDate=${searchRequest.toDate}"
                        ),
                        "Pagination must preserve toDate filter"
                ),

                () -> assertTrue(
                        html.contains(
                                "status=${searchRequest.status}"
                        ),
                        "Pagination must preserve status filter"
                ),

                () -> assertTrue(
                        html.contains(
                                "/css/moderator-bookings.css"
                        ),
                        "Booking page must use its stylesheet"
                ),

                () -> assertTrue(
                        html.contains(
                                "Không tìm thấy booking phù hợp"
                        ),
                        "Booking page must provide an empty state"
                )
        );
    }
}