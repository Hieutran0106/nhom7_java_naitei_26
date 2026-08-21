package com.nhom7.coworkingspace.service;

import com.nhom7.coworkingspace.entity.Booking;
import com.nhom7.coworkingspace.entity.Space;
import com.nhom7.coworkingspace.entity.User;
import com.nhom7.coworkingspace.repository.BookingRepository;
import com.nhom7.coworkingspace.service.impl.BookingServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class BookingServiceImplTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private EmailService emailService;

    @Mock
    private EmailTemplateService emailTemplateService;

    @Mock
    private MessageSource messageSource;

    private BookingService bookingService;

    @BeforeEach
    void setUp() {
        bookingService = new BookingServiceImpl(
                bookingRepository, emailService, emailTemplateService, messageSource);
    }

    @Test
    void changeStatusShouldPersistAndSendBookingStatusEmail() {
        User user = User.builder()
                .name("Nguyen Van A")
                .email("customer@coworking.test")
                .language("vi")
                .build();
        Space space = Space.builder().name("Meeting Room A").build();
        Booking booking = Booking.builder()
                .id(42L)
                .user(user)
                .space(space)
                .startTime(LocalDateTime.of(2026, 8, 22, 9, 0))
                .endTime(LocalDateTime.of(2026, 8, 22, 11, 0))
                .totalPrice(new BigDecimal("250000.00"))
                .status("PENDING")
                .build();
        given(bookingRepository.findById(42L)).willReturn(Optional.of(booking));
        given(bookingRepository.saveAndFlush(booking)).willReturn(booking);
        Locale locale = Locale.forLanguageTag("vi");
        given(emailTemplateService.renderBookingStatusChanged(booking, "PENDING", locale))
                .willReturn("<p>Booking updated</p>");
        given(messageSource.getMessage(
                "email.booking.status.subject", new Object[]{42L}, locale))
                .willReturn("Trạng thái đặt chỗ #42 đã được cập nhật");

        Booking updated = bookingService.changeStatus(42L, " approved ");

        assertThat(updated.getStatus()).isEqualTo("APPROVED");
        verify(bookingRepository).saveAndFlush(booking);
        verify(emailTemplateService).renderBookingStatusChanged(booking, "PENDING", locale);
        verify(emailService).sendHtmlEmail(
                "customer@coworking.test",
                "Trạng thái đặt chỗ #42 đã được cập nhật",
                "<p>Booking updated</p>");
    }

    @Test
    void changeStatusShouldNotPersistOrSendEmailWhenStatusIsUnchanged() {
        Booking booking = Booking.builder().id(42L).status("APPROVED").build();
        given(bookingRepository.findById(42L)).willReturn(Optional.of(booking));

        Booking unchanged = bookingService.changeStatus(42L, " approved ");

        assertThat(unchanged).isSameAs(booking);
        verifyNoInteractions(emailService, emailTemplateService);
        verify(bookingRepository, org.mockito.Mockito.never()).saveAndFlush(booking);
    }
}
