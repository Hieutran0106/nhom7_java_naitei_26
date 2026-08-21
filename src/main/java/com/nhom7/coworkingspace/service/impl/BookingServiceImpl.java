package com.nhom7.coworkingspace.service.impl;

import com.nhom7.coworkingspace.entity.Booking;
import com.nhom7.coworkingspace.exception.AppException;
import com.nhom7.coworkingspace.exception.BookingNotFoundException;
import com.nhom7.coworkingspace.repository.BookingRepository;
import com.nhom7.coworkingspace.service.BookingService;
import com.nhom7.coworkingspace.service.EmailService;
import com.nhom7.coworkingspace.service.EmailTemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepository;
    private final EmailService emailService;
    private final EmailTemplateService emailTemplateService;
    private final MessageSource messageSource;

    @Override
    @Transactional
    public Booking changeStatus(Long bookingId, String newStatus) {
        String normalizedStatus = normalizeStatus(newStatus);
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BookingNotFoundException(bookingId));
        String previousStatus = booking.getStatus();

        if (normalizedStatus.equalsIgnoreCase(previousStatus)) {
            return booking;
        }

        booking.setStatus(normalizedStatus);
        Booking savedBooking = bookingRepository.saveAndFlush(booking);

        Locale locale = toLocale(savedBooking.getUser().getLanguage());
        String html = emailTemplateService.renderBookingStatusChanged(
                savedBooking, previousStatus, locale);
        String subject = messageSource.getMessage(
                "email.booking.status.subject",
                new Object[]{savedBooking.getId()},
                locale);
        emailService.sendHtmlEmail(
                savedBooking.getUser().getEmail(),
                subject,
                html);
        return savedBooking;
    }

    private String normalizeStatus(String status) {
        if (status == null || status.isBlank()) {
            throw new AppException("booking.status.required", HttpStatus.BAD_REQUEST);
        }
        return status.trim().toUpperCase(Locale.ROOT);
    }

    private Locale toLocale(String language) {
        if (!StringUtils.hasText(language)) {
            return Locale.ENGLISH;
        }
        return Locale.forLanguageTag(language.replace('_', '-'));
    }
}
