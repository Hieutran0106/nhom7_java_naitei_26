package com.nhom7.coworkingspace.service;

import com.nhom7.coworkingspace.dto.response.RevenueStatisticsResponse;
import com.nhom7.coworkingspace.dto.response.StatisticsOverviewResponse;

public interface StatisticsService {

    StatisticsOverviewResponse getOverview();

    RevenueStatisticsResponse getRevenueByYear(int year);
}