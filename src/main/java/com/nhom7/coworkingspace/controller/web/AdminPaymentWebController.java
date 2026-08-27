package com.nhom7.coworkingspace.controller.web;

import com.nhom7.coworkingspace.dto.request.PaymentSearchRequest;
import com.nhom7.coworkingspace.dto.response.PageResponse;
import com.nhom7.coworkingspace.dto.response.PaymentResponse;
import com.nhom7.coworkingspace.service.StatisticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Slf4j
@Controller
@RequestMapping("/admin/payments")
@RequiredArgsConstructor
public class AdminPaymentWebController {

    private final StatisticsService statisticsService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public String paymentHistory(
            @ModelAttribute("searchRequest") PaymentSearchRequest request,
            Model model) {
        if (request.getFromDate() != null
                && request.getToDate() != null
                && request.getFromDate().isAfter(request.getToDate())) {
            model.addAttribute("validationError", true);
            model.addAttribute("payments", emptyPage(request));
            return "admin/payments";
        }

        try {
            model.addAttribute("payments", statisticsService.searchPayments(request));
        } catch (RuntimeException exception) {
            log.error("Unable to load admin payment history", exception);
            model.addAttribute("loadError", true);
            model.addAttribute("payments", emptyPage(request));
        }

        return "admin/payments";
    }

    private PageResponse<PaymentResponse> emptyPage(PaymentSearchRequest request) {
        return PageResponse.<PaymentResponse>builder()
                .content(List.of())
                .pageNumber(Math.max(0, request.getPage()))
                .pageSize(Math.max(1, request.getSize()))
                .totalElements(0)
                .totalPages(0)
                .last(true)
                .build();
    }
}
