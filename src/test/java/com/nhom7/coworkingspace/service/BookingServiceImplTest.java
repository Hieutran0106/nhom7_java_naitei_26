package com.nhom7.coworkingspace.service;

import com.nhom7.coworkingspace.dto.request.BookingRequest;
import com.nhom7.coworkingspace.dto.response.BookingResponse;
import com.nhom7.coworkingspace.entity.Booking;
import com.nhom7.coworkingspace.entity.Space;
import com.nhom7.coworkingspace.entity.User;
import com.nhom7.coworkingspace.exception.AppException;
import com.nhom7.coworkingspace.mapper.BookingMapper;
import com.nhom7.coworkingspace.repository.BookingRepository;
import com.nhom7.coworkingspace.repository.SpaceRepository;
import com.nhom7.coworkingspace.repository.UserRepository;
import com.nhom7.coworkingspace.service.impl.BookingServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Locale;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.time.Clock;

@ExtendWith(MockitoExtension.class)
@DisplayName("BookingServiceImpl Unit Tests")
class BookingServiceImplTest {

        @Mock
        private BookingRepository bookingRepository;

        @Mock
        private SpaceRepository spaceRepository;

        @Mock
        private UserRepository userRepository;

        @Mock
        private BookingMapper bookingMapper;

        @Mock
        private EmailService emailService;

        @Mock
        private EmailTemplateService emailTemplateService;

        @Mock
        private MessageSource messageSource;

        private BookingServiceImpl bookingService;

        @BeforeEach
        void setUp() {
                bookingService = new BookingServiceImpl(
                                bookingRepository,
                                spaceRepository,
                                userRepository,
                                bookingMapper,
                                emailService,
                                emailTemplateService,
                                messageSource,
                                Clock.systemUTC());
        }

        @Nested
        @DisplayName("createBooking Tests")
        class CreateBookingTests {

                @Test
                @DisplayName("Should create booking successfully with PENDING status and calculated price")
                void createBooking_Success() {
                        String email = "customer@coworking.test";
                        LocalDateTime start = LocalDateTime.now().plusDays(1).withHour(9).withMinute(0);
                        LocalDateTime end = start.plusHours(2);

                        User user = User.builder().id(1L).email(email).name("Nguyen Van A").build();
                        Space space = Space.builder()
                                        .id(10L)
                                        .name("Desk 1")
                                        .status("ACTIVE")
                                        .price(new BigDecimal("100000.00"))
                                        .priceUnit("HOUR")
                                        .build();

                        BookingRequest request = BookingRequest.builder()
                                        .spaceId(10L)
                                        .startTime(start)
                                        .endTime(end)
                                        .build();

                        Booking savedBooking = Booking.builder()
                                        .id(100L)
                                        .user(user)
                                        .space(space)
                                        .startTime(start)
                                        .endTime(end)
                                        .totalPrice(new BigDecimal("200000.00"))
                                        .status("PENDING")
                                        .build();

                        BookingResponse expectedResponse = BookingResponse.builder()
                                        .id(100L)
                                        .userEmail(email)
                                        .spaceId(10L)
                                        .status("PENDING")
                                        .totalPrice(new BigDecimal("200000.00"))
                                        .build();

                        given(userRepository.findByEmail(email)).willReturn(Optional.of(user));
                        given(spaceRepository.findById(10L)).willReturn(Optional.of(space));
                        given(bookingRepository.existsActiveOverlap(10L, start, end)).willReturn(false);
                        given(bookingRepository.save(any(Booking.class))).willReturn(savedBooking);
                        given(bookingMapper.toBookingResponse(savedBooking)).willReturn(expectedResponse);

                        BookingResponse response = bookingService.createBooking(request, email);

                        assertThat(response).isNotNull();
                        assertThat(response.getStatus()).isEqualTo("PENDING");
                        assertThat(response.getTotalPrice()).isEqualByComparingTo("200000.00");
                        verify(bookingRepository).save(any(Booking.class));
                }

                @Test
                @DisplayName("Should throw AppException when active booking overlap exists (PENDING/APPROVED)")
                void createBooking_OverlapError() {
                        String email = "customer@coworking.test";
                        LocalDateTime start = LocalDateTime.now().plusDays(1).withHour(9).withMinute(0);
                        LocalDateTime end = start.plusHours(2);

                        User user = User.builder().id(1L).email(email).build();
                        Space space = Space.builder().id(10L).status("ACTIVE").build();

                        BookingRequest request = BookingRequest.builder()
                                        .spaceId(10L)
                                        .startTime(start)
                                        .endTime(end)
                                        .build();

                        given(userRepository.findByEmail(email)).willReturn(Optional.of(user));
                        given(spaceRepository.findById(10L)).willReturn(Optional.of(space));
                        given(bookingRepository.existsActiveOverlap(10L, start, end)).willReturn(true);

                        assertThatThrownBy(() -> bookingService.createBooking(request, email))
                                        .isInstanceOf(AppException.class)
                                        .hasMessage("booking.overlap.error")
                                        .extracting("status")
                                        .isEqualTo(HttpStatus.BAD_REQUEST);
                }

                @Test
                @DisplayName("Should throw AppException when startTime is after endTime")
                void createBooking_InvalidTimeRange() {
                        String email = "customer@coworking.test";
                        LocalDateTime start = LocalDateTime.now().plusDays(1).withHour(11).withMinute(0);
                        LocalDateTime end = start.minusHours(2);

                        BookingRequest request = BookingRequest.builder()
                                        .spaceId(10L)
                                        .startTime(start)
                                        .endTime(end)
                                        .build();

                        assertThatThrownBy(() -> bookingService.createBooking(request, email))
                                        .isInstanceOf(AppException.class)
                                        .hasMessage("booking.time.invalid")
                                        .extracting("status")
                                        .isEqualTo(HttpStatus.BAD_REQUEST);
                }

                @Test
                @DisplayName("Should throw AppException when startTime is in the past")
                void createBooking_PastStartTime() {
                        String email = "customer@coworking.test";
                        LocalDateTime start = LocalDateTime.now().minusDays(1);
                        LocalDateTime end = start.plusHours(2);

                        BookingRequest request = BookingRequest.builder()
                                        .spaceId(10L)
                                        .startTime(start)
                                        .endTime(end)
                                        .build();

                        assertThatThrownBy(() -> bookingService.createBooking(request, email))
                                        .isInstanceOf(AppException.class)
                                        .hasMessage("booking.time.past")
                                        .extracting("status")
                                        .isEqualTo(HttpStatus.BAD_REQUEST);
                }

                @Test
                @DisplayName("Should throw AppException when booking outside space operating hours")
                void createBooking_OutsideOperatingHours() {
                        String email = "customer@coworking.test";
                        LocalDateTime start = LocalDateTime.now().plusDays(1).withHour(7).withMinute(0); // 07:00
                        LocalDateTime end = start.plusHours(2); // 09:00

                        User user = User.builder().id(1L).email(email).build();
                        Space space = Space.builder()
                                        .id(10L)
                                        .status("ACTIVE")
                                        .openTime(LocalTime.of(8, 0)) // Opens at 08:00
                                        .closeTime(LocalTime.of(20, 0))
                                        .build();

                        BookingRequest request = BookingRequest.builder()
                                        .spaceId(10L)
                                        .startTime(start)
                                        .endTime(end)
                                        .build();

                        given(userRepository.findByEmail(email)).willReturn(Optional.of(user));
                        given(spaceRepository.findById(10L)).willReturn(Optional.of(space));

                        assertThatThrownBy(() -> bookingService.createBooking(request, email))
                                        .isInstanceOf(AppException.class)
                                        .hasMessage("booking.operating.hours.invalid")
                                        .extracting("status")
                                        .isEqualTo(HttpStatus.BAD_REQUEST);
                }
        }

        @Nested
        @DisplayName("calculateTotalPrice Tests")
        class PriceCalculationTests {

                @Test
                @DisplayName("Calculate price by HOUR: 2 hours 30 mins at 100,000 -> 250,000.00")
                void calculatePrice_Hour() {
                        Space space = Space.builder()
                                        .price(new BigDecimal("100000.00"))
                                        .priceUnit("HOUR")
                                        .build();

                        LocalDateTime start = LocalDateTime.of(2026, 8, 25, 9, 0);
                        LocalDateTime end = LocalDateTime.of(2026, 8, 25, 11, 30);

                        BigDecimal totalPrice = bookingService.calculateTotalPrice(space, start, end);

                        assertThat(totalPrice).isEqualByComparingTo("250000.00");
                }

                @Test
                @DisplayName("Calculate price by DAY: 1.5 days at 500,000 -> 2 days = 1,000,000.00")
                void calculatePrice_Day() {
                        Space space = Space.builder()
                                        .price(new BigDecimal("500000.00"))
                                        .priceUnit("DAY")
                                        .build();

                        LocalDateTime start = LocalDateTime.of(2026, 8, 25, 9, 0);
                        LocalDateTime end = LocalDateTime.of(2026, 8, 26, 15, 0); // 30 hours = 2 days ceiling

                        BigDecimal totalPrice = bookingService.calculateTotalPrice(space, start, end);

                        assertThat(totalPrice).isEqualByComparingTo("1000000.00");
                }

                @Test
                @DisplayName("Calculate price by MONTH: 45 days at 3,000,000 -> 2 months = 6,000,000.00")
                void calculatePrice_Month() {
                        Space space = Space.builder()
                                        .price(new BigDecimal("3000000.00"))
                                        .priceUnit("MONTH")
                                        .build();

                        LocalDateTime start = LocalDateTime.of(2026, 8, 1, 9, 0);
                        LocalDateTime end = LocalDateTime.of(2026, 9, 15, 9, 0); // 45 days = 2 months ceiling

                        BigDecimal totalPrice = bookingService.calculateTotalPrice(space, start, end);

                        assertThat(totalPrice).isEqualByComparingTo("6000000.00");
                }
        }

        @Test
        @DisplayName("changeStatus should persist and send booking status email")
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
                                "email.booking.status.subject", new Object[] { 42L }, locale))
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
        @DisplayName("changeStatus should not persist or send email when status is unchanged")
        void changeStatusShouldNotPersistOrSendEmailWhenStatusIsUnchanged() {
                Booking booking = Booking.builder().id(42L).status("APPROVED").build();
                given(bookingRepository.findById(42L)).willReturn(Optional.of(booking));

                Booking unchanged = bookingService.changeStatus(42L, " approved ");

                assertThat(unchanged).isSameAs(booking);
                verifyNoInteractions(emailService, emailTemplateService);
                verify(bookingRepository, org.mockito.Mockito.never()).saveAndFlush(booking);
        }

        @Nested
        @DisplayName("cancelBooking Tests")
        class CancelBookingTests {

                @Test
                @DisplayName("Should cancel PENDING booking successfully when requested by owner")
                void cancelBooking_Success_PendingStatus() {
                        String email = "owner@test.com";
                        User user = User.builder().id(1L).email(email).build();
                        Space space = Space.builder().id(10L).name("Space A").build();
                        Booking booking = Booking.builder()
                                        .id(100L)
                                        .user(user)
                                        .space(space)
                                        .status("PENDING")
                                        .build();

                        Booking cancelledBooking = Booking.builder()
                                        .id(100L)
                                        .user(user)
                                        .space(space)
                                        .status("CANCELLED")
                                        .build();

                        BookingResponse expectedResponse = BookingResponse.builder()
                                        .id(100L)
                                        .userEmail(email)
                                        .spaceId(10L)
                                        .status("CANCELLED")
                                        .build();

                        given(userRepository.findByEmail(email)).willReturn(Optional.of(user));
                        given(bookingRepository.findById(100L)).willReturn(Optional.of(booking));
                        given(bookingRepository.save(booking)).willReturn(cancelledBooking);
                        given(bookingMapper.toBookingResponse(cancelledBooking)).willReturn(expectedResponse);

                        BookingResponse response = bookingService.cancelBooking(100L, email);

                        assertThat(response).isNotNull();
                        assertThat(response.getStatus()).isEqualTo("CANCELLED");
                        verify(bookingRepository).save(booking);
                        verifyNoInteractions(emailService, emailTemplateService);
                }

                @Test
                @DisplayName("Should cancel APPROVED booking successfully when requested by owner")
                void cancelBooking_Success_ApprovedStatus() {
                        String email = "owner@test.com";
                        User user = User.builder().id(1L).email(email).build();
                        Space space = Space.builder().id(10L).name("Space A").build();
                        Booking booking = Booking.builder()
                                        .id(101L)
                                        .user(user)
                                        .space(space)
                                        .status("APPROVED")
                                        .build();

                        Booking cancelledBooking = Booking.builder()
                                        .id(101L)
                                        .user(user)
                                        .space(space)
                                        .status("CANCELLED")
                                        .build();

                        BookingResponse expectedResponse = BookingResponse.builder()
                                        .id(101L)
                                        .userEmail(email)
                                        .spaceId(10L)
                                        .status("CANCELLED")
                                        .build();

                        given(userRepository.findByEmail(email)).willReturn(Optional.of(user));
                        given(bookingRepository.findById(101L)).willReturn(Optional.of(booking));
                        given(bookingRepository.save(booking)).willReturn(cancelledBooking);
                        given(bookingMapper.toBookingResponse(cancelledBooking)).willReturn(expectedResponse);

                        BookingResponse response = bookingService.cancelBooking(101L, email);

                        assertThat(response).isNotNull();
                        assertThat(response.getStatus()).isEqualTo("CANCELLED");
                        verify(bookingRepository).save(booking);
                }

                @Test
                @DisplayName("Should throw 403 Forbidden when user is not the owner of the booking")
                void cancelBooking_Forbidden_NotOwner() {
                        String currentUserEmail = "userB@test.com";
                        User owner = User.builder().id(1L).email("owner@test.com").build();
                        User currentUser = User.builder().id(2L).email(currentUserEmail).build();
                        Booking booking = Booking.builder()
                                        .id(102L)
                                        .user(owner)
                                        .status("PENDING")
                                        .build();

                        given(userRepository.findByEmail(currentUserEmail)).willReturn(Optional.of(currentUser));
                        given(bookingRepository.findById(102L)).willReturn(Optional.of(booking));

                        assertThatThrownBy(() -> bookingService.cancelBooking(102L, currentUserEmail))
                                        .isInstanceOf(AppException.class)
                                        .hasMessage("booking.cannot.cancel.not.owner")
                                        .extracting("status")
                                        .isEqualTo(HttpStatus.FORBIDDEN);
                }

                @Test
                @DisplayName("Should throw 400 Bad Request when booking status is CONFIRMED (PAID)")
                void cancelBooking_BadRequest_ConfirmedPaid() {
                        String email = "owner@test.com";
                        User user = User.builder().id(1L).email(email).build();
                        Booking booking = Booking.builder()
                                        .id(103L)
                                        .user(user)
                                        .status("CONFIRMED")
                                        .build();

                        given(userRepository.findByEmail(email)).willReturn(Optional.of(user));
                        given(bookingRepository.findById(103L)).willReturn(Optional.of(booking));

                        assertThatThrownBy(() -> bookingService.cancelBooking(103L, email))
                                        .isInstanceOf(AppException.class)
                                        .hasMessage("booking.cannot.cancel.invalid.status")
                                        .extracting("status")
                                        .isEqualTo(HttpStatus.BAD_REQUEST);
                }

                @Test
                @DisplayName("Should throw 400 Bad Request when booking status is already CANCELLED")
                void cancelBooking_BadRequest_AlreadyCancelled() {
                        String email = "owner@test.com";
                        User user = User.builder().id(1L).email(email).build();
                        Booking booking = Booking.builder()
                                        .id(104L)
                                        .user(user)
                                        .status("CANCELLED")
                                        .build();

                        given(userRepository.findByEmail(email)).willReturn(Optional.of(user));
                        given(bookingRepository.findById(104L)).willReturn(Optional.of(booking));

                        assertThatThrownBy(() -> bookingService.cancelBooking(104L, email))
                                        .isInstanceOf(AppException.class)
                                        .hasMessage("booking.cannot.cancel.invalid.status")
                                        .extracting("status")
                                        .isEqualTo(HttpStatus.BAD_REQUEST);
                }
        }
}
