package com.nhom7.coworkingspace.service;

import com.nhom7.coworkingspace.dto.response.BookingResponse;
import com.nhom7.coworkingspace.entity.Booking;
import com.nhom7.coworkingspace.entity.Payment;
import com.nhom7.coworkingspace.mapper.BookingMapper;
import com.nhom7.coworkingspace.mapper.PaymentMapper;
import com.nhom7.coworkingspace.repository.BookingRepository;
import com.nhom7.coworkingspace.repository.PaymentRepository;
import com.nhom7.coworkingspace.repository.SpaceRepository;
import com.nhom7.coworkingspace.repository.UserRepository;
import com.nhom7.coworkingspace.service.impl.BookingServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;

import java.time.Clock;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName(
        "Booking detail - Payment method"
)
class BookingDetailPaymentMethodServiceTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private SpaceRepository spaceRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private BookingMapper bookingMapper;

    @Mock
    private PaymentMapper paymentMapper;

    @Mock
    private EmailService emailService;

    @Mock
    private EmailTemplateService emailTemplateService;

    @Mock
    private MessageSource messageSource;

    @Mock
    private Clock clock;

    @InjectMocks
    private BookingServiceImpl bookingService;

    @Test
    @DisplayName(
            "Get booking detail should include payment method when payment exists"
    )
    void getBookingDetailShouldIncludePaymentMethod() {

        Long bookingId = 1L;

        Booking booking =
                Booking.builder()
                        .id(bookingId)
                        .build();

        BookingResponse mappedResponse =
                BookingResponse.builder()
                        .id(bookingId)
                        .build();

        Payment payment =
                Payment.builder()
                        .id(10L)
                        .booking(booking)
                        .paymentMethod("VNPAY")
                        .build();

        given(
                bookingRepository.findById(
                        bookingId
                )
        ).willReturn(
                Optional.of(booking)
        );

        given(
                bookingMapper.toBookingResponse(
                        booking
                )
        ).willReturn(
                mappedResponse
        );

        given(
                paymentRepository.findByBookingId(
                        bookingId
                )
        ).willReturn(
                Optional.of(payment)
        );

        BookingResponse response =
                bookingService.getBookingById(
                        bookingId
                );

        assertEquals(
                "VNPAY",
                response.getPaymentMethod()
        );

        verify(
                paymentRepository
        ).findByBookingId(
                bookingId
        );
    }

    @Test
    @DisplayName(
            "Get booking detail should keep payment method null when payment does not exist"
    )
    void getBookingDetailShouldAllowMissingPayment() {

        Long bookingId = 2L;

        Booking booking =
                Booking.builder()
                        .id(bookingId)
                        .build();

        BookingResponse mappedResponse =
                BookingResponse.builder()
                        .id(bookingId)
                        .build();

        given(
                bookingRepository.findById(
                        bookingId
                )
        ).willReturn(
                Optional.of(booking)
        );

        given(
                bookingMapper.toBookingResponse(
                        booking
                )
        ).willReturn(
                mappedResponse
        );

        given(
                paymentRepository.findByBookingId(
                        bookingId
                )
        ).willReturn(
                Optional.empty()
        );

        BookingResponse response =
                bookingService.getBookingById(
                        bookingId
                );

        assertNull(
                response.getPaymentMethod()
        );

        verify(
                paymentRepository
        ).findByBookingId(
                bookingId
        );
    }
}