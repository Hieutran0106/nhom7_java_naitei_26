package com.nhom7.coworkingspace.controller.web;

import com.nhom7.coworkingspace.dto.response.PageResponse;
import com.nhom7.coworkingspace.dto.response.VenueResponse;
import com.nhom7.coworkingspace.enums.VenueStatus;
import com.nhom7.coworkingspace.service.VenueService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/moderator/venues")
@RequiredArgsConstructor
public class ModeratorVenueWebController {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 10;

    private final VenueService venueService;

    @GetMapping
    @PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN')")
    public String listVenues(
            @RequestParam(
                    value = "status",
                    required = false
            ) VenueStatus status,
            @RequestParam(
                    value = "page",
                    defaultValue = "0"
            ) int page,
            @RequestParam(
                    value = "size",
                    defaultValue = "10"
            ) int size,
            Model model
    ) {
        int resolvedPage =
                Math.max(
                        DEFAULT_PAGE,
                        page
                );

        int resolvedSize =
                Math.max(
                        1,
                        size
                );

        PageResponse<VenueResponse> venuePage =
                venueService.getVenuesForModerator(
                        status,
                        resolvedPage,
                        resolvedSize
                );

        model.addAttribute(
                "venues",
                venuePage
        );

        model.addAttribute(
                "statuses",
                VenueStatus.values()
        );

        model.addAttribute(
                "selectedStatus",
                status
        );

        return "moderator/venues";
    }
}