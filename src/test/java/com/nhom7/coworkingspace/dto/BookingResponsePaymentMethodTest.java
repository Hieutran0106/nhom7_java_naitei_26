package com.nhom7.coworkingspace.dto;

import com.nhom7.coworkingspace.dto.response.BookingResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName(
        "BookingResponse - Payment method contract"
)
class BookingResponsePaymentMethodTest {

    @Test
    @DisplayName(
            "Booking response should expose payment method"
    )
    void bookingResponseShouldExposePaymentMethod() {

        BookingResponse response =
                BookingResponse.builder()
                        .paymentMethod("VNPAY")
                        .build();

        assertEquals(
                "VNPAY",
                response.getPaymentMethod()
        );
    }
}