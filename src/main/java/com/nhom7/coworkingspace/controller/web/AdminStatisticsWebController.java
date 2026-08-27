package com.nhom7.coworkingspace.controller.web;

import com.nhom7.coworkingspace.dto.response.RevenueStatisticsResponse;
import com.nhom7.coworkingspace.dto.response.StatisticsOverviewResponse;
import com.nhom7.coworkingspace.dto.view.RevenueBarView;
import com.nhom7.coworkingspace.service.StatisticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Year;
import java.util.List;

@Slf4j
@Controller
@RequestMapping("/admin/statistics")
@RequiredArgsConstructor
public class AdminStatisticsWebController {

    private final StatisticsService statisticsService;
    private final Clock clock;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public String dashboard(@RequestParam(required = false) Integer year, Model model) {
        int currentYear = Year.now(clock).getValue();
        int selectedYear = year == null ? currentYear : year;

        model.addAttribute("currentYear", currentYear);
        model.addAttribute("selectedYear", selectedYear);

        try {
            StatisticsOverviewResponse overview = statisticsService.getOverview();
            RevenueStatisticsResponse revenue = statisticsService.getRevenueByYear(selectedYear);
            model.addAttribute("overview", overview);
            model.addAttribute("revenue", revenue);
            model.addAttribute("revenueBars", toRevenueBars(revenue.getMonthlyRevenue()));
        } catch (RuntimeException exception) {
            log.error("Unable to load admin statistics dashboard for year {}", selectedYear, exception);
            model.addAttribute("loadError", true);
        }

        return "admin/statistics";
    }

    private List<RevenueBarView> toRevenueBars(
            List<RevenueStatisticsResponse.MonthlyRevenue> monthlyRevenue) {
        BigDecimal maximum = monthlyRevenue.stream()
                .map(RevenueStatisticsResponse.MonthlyRevenue::getRevenue)
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);

        return monthlyRevenue.stream()
                .map(item -> RevenueBarView.builder()
                        .month(item.getMonth())
                        .revenue(item.getRevenue())
                        .percentage(toPercentage(item.getRevenue(), maximum))
                        .build())
                .toList();
    }

    private int toPercentage(BigDecimal value, BigDecimal maximum) {
        if (maximum.signum() == 0) {
            return 0;
        }
        return value.multiply(BigDecimal.valueOf(100))
                .divide(maximum, 0, RoundingMode.HALF_UP)
                .intValue();
    }
}
