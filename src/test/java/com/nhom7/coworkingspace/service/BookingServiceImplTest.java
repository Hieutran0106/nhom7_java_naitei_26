package com.nhom7.coworkingspace.service;

import com.nhom7.coworkingspace.dto.request.BookingRequest;
import com.nhom7.coworkingspace.dto.request.BookingSearchRequest;
import com.nhom7.coworkingspace.dto.response.BookingResponse;
import com.nhom7.coworkingspace.dto.response.PageResponse;
import com.nhom7.coworkingspace.entity.Booking;
import com.nhom7.coworkingspace.entity.Space;
import com.nhom7.coworkingspace.entity.User;
import com.nhom7.coworkingspace.enums.BookingStatus;
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
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

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

        private Clock clock;
        private BookingServiceImpl bookingService;

        @BeforeEach
        void setUp() {
                Instant fixedInstant = Instant.parse("2026-08-20T00:00:00Z");
                clock = Clock.fixed(fixedInstant, ZoneId.of("UTC"));
                bookingService = new BookingServiceImpl(
                                bookingRepository,
                                spaceRepository,
                                userRepository,
                                bookingMapper,
                                emailService,
                                emailTemplateService,
                                messageSource,
                                clock);
        }

        @Nested
        @DisplayName("Create Booking Tests")
        class CreateBookingTests {

                @Test
                @DisplayName("Should create booking successfully with valid inputs")
                void createBooking_Success() {
                        String email = "customer@coworking.test";
                        LocalDateTime start = LocalDateTime.now(clock).plusDays(1).withHour(9).withMinute(0);
                        LocalDateTime end = start.plusHours(2);

                        User user = User.builder().id(1L).email(email).build();
                        Space space = Space.builder()
                                        .id(10L)
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
                                        .status(BookingStatus.PENDING)
                                        .build();

                        BookingResponse expectedResponse = BookingResponse.builder()
                                        .id(100L)
                                        .userEmail(email)
                                        .spaceId(10L)
                                        .status(BookingStatus.PENDING)
                                        .totalPrice(new BigDecimal("200000.00"))
                                        .build();

                        given(userRepository.findByEmail(email)).willReturn(Optional.of(user));
                        given(spaceRepository.findByIdForUpdate(10L)).willReturn(Optional.of(space));
                        given(bookingRepository.existsActiveOverlap(10L, start, end)).willReturn(false);
                        given(bookingRepository.save(any(Booking.class))).willReturn(savedBooking);
                        given(bookingMapper.toBookingResponse(savedBooking)).willReturn(expectedResponse);

                        BookingResponse response = bookingService.createBooking(request, email);

                        assertThat(response).isNotNull();
                        assertThat(response.getStatus()).isEqualTo(BookingStatus.PENDING);
                        assertThat(response.getTotalPrice()).isEqualByComparingTo("200000.00");
                        verify(bookingRepository).save(any(Booking.class));
                }

                @Test
                @DisplayName("Should throw AppException when active booking overlap exists (PENDING/APPROVED)")
                void createBooking_OverlapError() {
                        String email = "customer@coworking.test";
                        LocalDateTime start = LocalDateTime.now(clock).plusDays(1).withHour(9).withMinute(0);
                        LocalDateTime end = start.plusHours(2);

                        User user = User.builder().id(1L).email(email).build();
                        Space space = Space.builder().id(10L).status("ACTIVE").build();

                        BookingRequest request = BookingRequest.builder()
                                        .spaceId(10L)
                                        .startTime(start)
                                        .endTime(end)
                                        .build();

                        given(userRepository.findByEmail(email)).willReturn(Optional.of(user));
                        given(spaceRepository.findByIdForUpdate(10L)).willReturn(Optional.of(space));
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
                        LocalDateTime start = LocalDateTime.now(clock).plusDays(1).withHour(11).withMinute(0);
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
                void createBooking_PastTime() {
                        String email = "customer@coworking.test";
                        LocalDateTime start = LocalDateTime.now(clock).minusDays(1);
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
                        LocalDateTime start = LocalDateTime.now(clock).plusDays(1).withHour(7).withMinute(0);
                        LocalDateTime end = start.plusHours(2);

                        User user = User.builder().id(1L).email(email).build();
                        Space space = Space.builder()
                                        .id(10L)
                                        .status("ACTIVE")
                                        .openTime(LocalTime.of(8, 0))
                                        .closeTime(LocalTime.of(20, 0))
                                        .build();

                        BookingRequest request = BookingRequest.builder()
                                        .spaceId(10L)
                                        .startTime(start)
                                        .endTime(end)
                                        .build();

                        given(userRepository.findByEmail(email)).willReturn(Optional.of(user));
                        given(spaceRepository.findByIdForUpdate(10L)).willReturn(Optional.of(space));

                        assertThatThrownBy(() -> bookingService.createBooking(request, email))
                                        .isInstanceOf(AppException.class)
                                        .hasMessage("booking.operating.hours.invalid")
                                        .extracting("status")
                                        .isEqualTo(HttpStatus.BAD_REQUEST);
                }
        }

        @Nested
        @DisplayName("Search Bookings Tests")
        class SearchBookingsTests {

                @Test
                @DisplayName("searchBookings should return paged booking responses")
                void searchBookings_Success() {
                        BookingSearchRequest request = BookingSearchRequest.builder()
                                        .keyword("test")
                                        .status(BookingStatus.PENDING)
                                        .page(0)
                                        .size(10)
                                        .sortBy("id")
                                        .sortDir("DESC")
                                        .build();

                        Booking booking = Booking.builder()
                                        .id(1L)
                                        .status(BookingStatus.PENDING)
                                        .totalPrice(new BigDecimal("100000.00"))
                                        .build();

                        BookingResponse bookingResponse = BookingResponse.builder()
                                        .id(1L)
                                        .status(BookingStatus.PENDING)
                                        .totalPrice(new BigDecimal("100000.00"))
                                        .build();

                        Page<Booking> bookingPage = new PageImpl<>(List.of(booking));

                        given(bookingRepository.findAll(ArgumentMatchers.<Specification<Booking>>any(), any(Pageable.class)))
                                        .willReturn(bookingPage);
                        given(bookingMapper.toBookingResponse(booking)).willReturn(bookingResponse);

                        PageResponse<BookingResponse> result = bookingService.searchBookings(request);

                        assertThat(result).isNotNull();
                        assertThat(result.getContent()).hasSize(1);
                        assertThat(result.getContent().get(0).getId()).isEqualTo(1L);
                        assertThat(result.getContent().get(0).getStatus()).isEqualTo(BookingStatus.PENDING);
                }
        }

        @Nested
        @DisplayName("Get Booking By ID Tests")
        class GetBookingByIdTests {

                @Test
                @DisplayName("Should return BookingResponse when booking exists")
                void getBookingById_Success() {
                        Long bookingId = 1L;
                        Booking booking = Booking.builder()
                                        .id(bookingId)
                                        .status(BookingStatus.PENDING)
                                        .totalPrice(new BigDecimal("100000.00"))
                                        .build();

                        BookingResponse expectedResponse = BookingResponse.builder()
                                        .id(bookingId)
                                        .status(BookingStatus.PENDING)
                                        .totalPrice(new BigDecimal("100000.00"))
                                        .build();

                        given(bookingRepository.findById(bookingId)).willReturn(Optional.of(booking));
                        given(bookingMapper.toBookingResponse(booking)).willReturn(expectedResponse);

                        BookingResponse response = bookingService.getBookingById(bookingId);

                        assertThat(response).isNotNull();
                        assertThat(response.getId()).isEqualTo(bookingId);
                        assertThat(response.getStatus()).isEqualTo(BookingStatus.PENDING);
                }

                @Test
                @DisplayName("Should throw BookingNotFoundException when booking does not exist")
                void getBookingById_NotFound() {
                        Long bookingId = 999L;
                        given(bookingRepository.findById(bookingId)).willReturn(Optional.empty());

                        assertThatThrownBy(() -> bookingService.getBookingById(bookingId))
                                        .isInstanceOf(com.nhom7.coworkingspace.exception.BookingNotFoundException.class)
                                        .hasMessage("booking.not.found");
                }
        }

        @Nested
        @DisplayName("Price Calculation Tests")
        class PriceCalculationTests {


                @Test
                @DisplayName("Calculate price by HOUR: 2 hours at 100,000 = 200,000.00")
                void calculatePrice_Hour() {
                        Space space = Space.builder()
                                        .price(new BigDecimal("100000.00"))
                                        .priceUnit("HOUR")
                                        .build();

                        LocalDateTime start = LocalDateTime.of(2026, 8, 25, 9, 0);
                        LocalDateTime end = LocalDateTime.of(2026, 8, 25, 11, 0);

                        BigDecimal totalPrice = bookingService.calculateTotalPrice(space, start, end);

                        assertThat(totalPrice).isEqualByComparingTo("200000.00");
                }

                @Test
                @DisplayName("Calculate price by HOUR: 2.5 hours (150 mins) at 100,000 = 250,000.00")
                void calculatePrice_HourFractional() {
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
                        LocalDateTime end = LocalDateTime.of(2026, 8, 26, 15, 0);

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
                        LocalDateTime end = LocalDateTime.of(2026, 9, 15, 9, 0);

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
                                .status(BookingStatus.PENDING)
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

                assertThat(updated.getStatus()).isEqualTo(BookingStatus.APPROVED);
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
                Booking booking = Booking.builder().id(42L).status(BookingStatus.APPROVED).build();
                given(bookingRepository.findById(42L)).willReturn(Optional.of(booking));

                Booking unchanged = bookingService.changeStatus(42L, " approved ");

                assertThat(unchanged).isSameAs(booking);
                verifyNoInteractions(emailService, emailTemplateService);
                verify(bookingRepository, org.mockito.Mockito.never()).saveAndFlush(booking);
        }
}
