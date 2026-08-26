package com.nhom7.coworkingspace.service;

import com.nhom7.coworkingspace.dto.request.BookingHistoryRequest;
import com.nhom7.coworkingspace.dto.request.BookingRequest;
import com.nhom7.coworkingspace.dto.request.BookingSearchRequest;
import com.nhom7.coworkingspace.dto.response.BookingResponse;
import com.nhom7.coworkingspace.dto.response.PageResponse;
import com.nhom7.coworkingspace.entity.Booking;

public interface BookingService {

    Booking changeStatus(Long bookingId, String newStatus);

    BookingResponse createBooking(BookingRequest request, String userEmail);

    PageResponse<BookingResponse> getMyBookingHistory(String userEmail, BookingHistoryRequest request);

    BookingResponse cancelBooking(Long bookingId, String userEmail);

    PageResponse<BookingResponse> searchBookings(BookingSearchRequest request);

    BookingResponse getBookingById(Long bookingId);
}



