package com.nhom7.coworkingspace.service.impl;

import com.nhom7.coworkingspace.dto.response.StatisticsOverviewResponse;
import com.nhom7.coworkingspace.repository.BookingRepository;
import com.nhom7.coworkingspace.repository.UserRepository;
import com.nhom7.coworkingspace.repository.VenueRepository;
import com.nhom7.coworkingspace.service.StatisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StatisticsServiceImpl implements StatisticsService {

    private static final String SUCCESSFUL_BOOKING_STATUS = "COMPLETED";
    private static final String ACTIVE_VENUE_STATUS = "ACTIVE";

    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;
    private final VenueRepository venueRepository;

    @Override
    @Transactional(readOnly = true)
    public StatisticsOverviewResponse getOverview() {

        long totalUsers =
                userRepository.count();

        long successfulBookings =
                bookingRepository.countByStatusIgnoreCase(
                        SUCCESSFUL_BOOKING_STATUS
                );

        long activeVenues =
                venueRepository.countByStatusIgnoreCase(
                        ACTIVE_VENUE_STATUS
                );

        return StatisticsOverviewResponse.builder()
                .totalUsers(totalUsers)
                .successfulBookings(successfulBookings)
                .activeVenues(activeVenues)
                .build();
    }
}