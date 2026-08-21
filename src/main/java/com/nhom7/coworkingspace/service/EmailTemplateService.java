package com.nhom7.coworkingspace.service;

import com.nhom7.coworkingspace.entity.Booking;

import java.util.Locale;

public interface EmailTemplateService {

    String renderAccountConfirmation(String code, Locale locale);

    String renderPasswordReset(String code, Locale locale);

    String renderBookingStatusChanged(Booking booking, String previousStatus, Locale locale);
}
