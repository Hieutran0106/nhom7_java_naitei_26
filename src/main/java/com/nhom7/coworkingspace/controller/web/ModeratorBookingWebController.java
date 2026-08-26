package com.nhom7.coworkingspace.controller.web;

import com.nhom7.coworkingspace.dto.request.BookingSearchRequest;
import com.nhom7.coworkingspace.dto.response.BookingResponse;
import com.nhom7.coworkingspace.dto.response.PageResponse;
import com.nhom7.coworkingspace.enums.BookingStatus;
import com.nhom7.coworkingspace.service.BookingService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/moderator/bookings")
@RequiredArgsConstructor
public class ModeratorBookingWebController {

    private final BookingService bookingService;

    /**
     * Render HTML page for moderator/admin to view, search, filter, and paginate bookings.
     *
     * @param request search parameters
     * @param model   Spring MVC model
     * @return Thymeleaf template name
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN')")
    public String listBookings(
            @ModelAttribute("searchRequest") BookingSearchRequest request,
            Model model) {
        PageResponse<BookingResponse> bookingPage = bookingService.searchBookings(request);
        model.addAttribute("bookings", bookingPage);
        model.addAttribute("statuses", BookingStatus.values());
        return "moderator/bookings";
    }
}
