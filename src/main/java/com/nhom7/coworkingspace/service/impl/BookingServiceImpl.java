package com.nhom7.coworkingspace.service.impl;

import com.nhom7.coworkingspace.dto.request.BookingRequest;
import com.nhom7.coworkingspace.dto.response.BookingResponse;
import com.nhom7.coworkingspace.entity.Booking;
import com.nhom7.coworkingspace.entity.Space;
import com.nhom7.coworkingspace.entity.User;
import com.nhom7.coworkingspace.enums.BookingStatus;
import com.nhom7.coworkingspace.enums.PriceUnit;
import com.nhom7.coworkingspace.enums.SpaceStatus;
import com.nhom7.coworkingspace.exception.AppException;
import com.nhom7.coworkingspace.exception.BookingNotFoundException;
import com.nhom7.coworkingspace.mapper.BookingMapper;
import com.nhom7.coworkingspace.repository.BookingRepository;
import com.nhom7.coworkingspace.repository.SpaceRepository;
import com.nhom7.coworkingspace.repository.UserRepository;
import com.nhom7.coworkingspace.service.BookingService;
import com.nhom7.coworkingspace.service.EmailService;
import com.nhom7.coworkingspace.service.EmailTemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepository;
    private final SpaceRepository spaceRepository;
    private final UserRepository userRepository;
    private final BookingMapper bookingMapper;
    private final EmailService emailService;
    private final EmailTemplateService emailTemplateService;
    private final MessageSource messageSource;
    private final Clock clock;

    @Override
    @Transactional
    public BookingResponse createBooking(BookingRequest request, String userEmail) {
        if (request.getStartTime() == null || request.getEndTime() == null) {
            throw new AppException("booking.time.required", HttpStatus.BAD_REQUEST);
        }
        if (!request.getStartTime().isBefore(request.getEndTime())) {
            throw new AppException("booking.time.invalid", HttpStatus.BAD_REQUEST);
        }
        if (request.getStartTime().isBefore(LocalDateTime.now(clock))) {
            throw new AppException("booking.time.past", HttpStatus.BAD_REQUEST);
        }

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new AppException("user.not.found", HttpStatus.NOT_FOUND));

        Space space = spaceRepository.findByIdForUpdate(request.getSpaceId())
                .or(() -> spaceRepository.findById(request.getSpaceId()))
                .orElseThrow(() -> new AppException("space.not.found", HttpStatus.NOT_FOUND));

        if (space.getStatus() != null && space.getStatus() != SpaceStatus.ACTIVE) {
            throw new AppException("space.not.available", HttpStatus.BAD_REQUEST);
        }

        // Operating hours validation if openTime & closeTime are defined
        if (space.getOpenTime() != null && space.getCloseTime() != null) {
            LocalTime startLocalTime = request.getStartTime().toLocalTime();
            LocalTime endLocalTime = request.getEndTime().toLocalTime();

            if (startLocalTime.isBefore(space.getOpenTime()) || endLocalTime.isAfter(space.getCloseTime())) {
                throw new AppException("booking.operating.hours.invalid", HttpStatus.BAD_REQUEST);
            }
        }

        // Check for active overlapping bookings (PENDING, APPROVED, etc.)
        boolean hasOverlap = bookingRepository.existsActiveOverlap(
                space.getId(), request.getStartTime(), request.getEndTime());
        if (hasOverlap) {
            throw new AppException("booking.overlap.error", HttpStatus.BAD_REQUEST);
        }

        BigDecimal totalPrice = calculateTotalPrice(space, request.getStartTime(), request.getEndTime());

        Booking booking = Booking.builder()
                .user(user)
                .space(space)
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .totalPrice(totalPrice)
                .status(BookingStatus.PENDING.name())
                .createdAt(LocalDateTime.now(clock))
                .build();

        Booking savedBooking = bookingRepository.save(booking);
        return bookingMapper.toBookingResponse(savedBooking);
    }

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

    public BigDecimal calculateTotalPrice(Space space, LocalDateTime startTime, LocalDateTime endTime) {
        BigDecimal price = space.getPrice() != null ? space.getPrice() : BigDecimal.ZERO;
        PriceUnit priceUnit = PriceUnit.fromString(space.getPriceUnit());

        long minutes = Duration.between(startTime, endTime).toMinutes();

        switch (priceUnit) {
            case HOUR:
                BigDecimal hours = BigDecimal.valueOf(minutes)
                        .divide(BigDecimal.valueOf(60), 4, RoundingMode.HALF_UP);
                return price.multiply(hours).setScale(2, RoundingMode.HALF_UP);

            case DAY:
                long days = (long) Math.ceil(minutes / (24.0 * 60.0));
                days = Math.max(1, days);
                return price.multiply(BigDecimal.valueOf(days)).setScale(2, RoundingMode.HALF_UP);

            case MONTH:
                long months = (long) Math.ceil(minutes / (30.0 * 24.0 * 60.0));
                months = Math.max(1, months);
                return price.multiply(BigDecimal.valueOf(months)).setScale(2, RoundingMode.HALF_UP);

            default:
                BigDecimal defaultHours = BigDecimal.valueOf(minutes)
                        .divide(BigDecimal.valueOf(60), 4, RoundingMode.HALF_UP);
                return price.multiply(defaultHours).setScale(2, RoundingMode.HALF_UP);
        }
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
