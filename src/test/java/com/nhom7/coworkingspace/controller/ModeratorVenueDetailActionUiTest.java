package com.nhom7.coworkingspace.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName(
        "Moderator Venue Detail - Moderation UI Tests"
)
class ModeratorVenueDetailActionUiTest {

    private static final Path TEMPLATE_PATH =
            Path.of(
                    "src",
                    "main",
                    "resources",
                    "templates",
                    "moderator",
                    "venue-detail.html"
            );

    @Test
    @DisplayName(
            "Venue detail contains Approve and Block actions"
    )
    void venueDetailShouldContainModerationActions()
            throws IOException {

        String html =
                readTemplate();

        assertTrue(
                html.contains(
                        "id=\"approveVenueButton\""
                )
        );

        assertTrue(
                html.contains(
                        "id=\"blockVenueButton\""
                )
        );

        assertTrue(
                html.contains(
                        "PENDING"
                )
        );

        assertTrue(
                html.contains(
                        "APPROVE"
                )
        );
    }

    @Test
    @DisplayName(
            "Block action contains required reason modal"
    )
    void blockActionShouldContainRequiredReasonModal()
            throws IOException {

        String html =
                readTemplate();

        assertTrue(
                html.contains(
                        "id=\"blockVenueModal\""
                )
        );

        assertTrue(
                html.contains(
                        "id=\"blockReason\""
                )
        );

        assertTrue(
                html.contains(
                        "required"
                )
        );
    }

    @Test
    @DisplayName(
            "Blocked venue displays persisted block reason"
    )
    void blockedVenueShouldDisplayBlockReason()
            throws IOException {

        String html =
                readTemplate();

        assertTrue(
                html.contains(
                        "venue.blockReason"
                )
        );

        assertTrue(
                html.contains(
                        "Lý do khóa"
                )
        );
    }

    @Test
    @DisplayName(
            "Moderation actions call dedicated APIs with Bearer token"
    )
    void moderationActionsShouldCallDedicatedApisWithBearerToken()
            throws IOException {

        String html =
                readTemplate();

        assertTrue(
                html.contains(
                        "sessionStorage.getItem"
                )
        );

        assertTrue(
                html.contains(
                        "localStorage.getItem"
                )
        );

        assertTrue(
                html.contains(
                        "\"accessToken\""
                )
        );

        assertTrue(
                html.contains(
                        "Authorization"
                )
        );

        assertTrue(
                html.contains(
                        "Bearer"
                )
        );

        assertTrue(
                html.contains(
                        "/approve"
                )
        );

        assertTrue(
                html.contains(
                        "/block"
                )
        );
    }

    private String readTemplate()
            throws IOException {

        return Files.readString(
                TEMPLATE_PATH,
                StandardCharsets.UTF_8
        );
    }
}