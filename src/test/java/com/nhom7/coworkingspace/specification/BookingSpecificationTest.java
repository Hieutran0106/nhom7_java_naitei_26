package com.nhom7.coworkingspace.specification;

import com.nhom7.coworkingspace.dto.request.BookingSearchRequest;
import com.nhom7.coworkingspace.entity.Booking;
import com.nhom7.coworkingspace.entity.Space;
import com.nhom7.coworkingspace.entity.User;
import com.nhom7.coworkingspace.enums.BookingStatus;
import jakarta.persistence.criteria.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("BookingSpecification - Unit Tests for Filtering Logic")
class BookingSpecificationTest {

    @Mock
    private Root<Booking> root;

    @Mock
    private CriteriaQuery<?> query;

    @Mock
    private CriteriaBuilder cb;

    @Mock
    private Join<Booking, User> userJoin;

    @Mock
    private Join<Booking, Space> spaceJoin;

    @Mock
    private Path<Object> statusPath;

    @Mock
    private Path<Object> userIdPath;

    @Mock
    private Path<Object> spaceIdPath;

    @Mock
    private Path<LocalDateTime> startTimePath;

    @Mock
    private Path<LocalDateTime> endTimePath;

    @Mock
    private Path<String> userNamePath;

    @Mock
    private Path<String> userEmailPath;

    @Mock
    private Path<String> spaceNamePath;

    @Mock
    private Expression<String> lowerUserName;

    @Mock
    private Expression<String> lowerUserEmail;

    @Mock
    private Expression<String> lowerSpaceName;

    @Mock
    private Predicate dummyPredicate;

    @Mock
    private Predicate orPredicate;

    @BeforeEach
    void setUp() {
        given(cb.and(any(Predicate[].class))).willReturn(dummyPredicate);
    }

    @Test
    @DisplayName("Should return empty predicate when request is null")
    void givenNullRequest_whenBuildSearchSpecification_thenReturnEmptyPredicates() {
        Specification<Booking> spec = BookingSpecification.buildSearchSpecification(null);
        Predicate result = spec.toPredicate(root, query, cb);

        assertThat(result).isNotNull();
        ArgumentCaptor<Predicate[]> captor = ArgumentCaptor.forClass(Predicate[].class);
        verify(cb).and(captor.capture());
        assertThat(captor.getValue()).isEmpty();
    }

    @Test
    @DisplayName("Should build LIKE predicates for keyword search on userName, userEmail, spaceName")
    void givenKeyword_whenBuildSearchSpecification_thenAddLikePredicates() {
        given(root.<Booking, User>join(eq("user"), eq(JoinType.INNER))).willReturn(userJoin);
        given(root.<Booking, Space>join(eq("space"), eq(JoinType.INNER))).willReturn(spaceJoin);

        given(userJoin.<String>get("name")).willReturn(userNamePath);
        given(userJoin.<String>get("email")).willReturn(userEmailPath);
        given(spaceJoin.<String>get("name")).willReturn(spaceNamePath);

        given(cb.lower(userNamePath)).willReturn(lowerUserName);
        given(cb.lower(userEmailPath)).willReturn(lowerUserEmail);
        given(cb.lower(spaceNamePath)).willReturn(lowerSpaceName);

        given(cb.like(eq(lowerUserName), eq("%john%"))).willReturn(dummyPredicate);
        given(cb.like(eq(lowerUserEmail), eq("%john%"))).willReturn(dummyPredicate);
        given(cb.like(eq(lowerSpaceName), eq("%john%"))).willReturn(dummyPredicate);
        given(cb.or(any(Predicate.class), any(Predicate.class), any(Predicate.class))).willReturn(orPredicate);

        BookingSearchRequest request = BookingSearchRequest.builder()
                .keyword("John")
                .build();

        Specification<Booking> spec = BookingSpecification.buildSearchSpecification(request);
        Predicate result = spec.toPredicate(root, query, cb);

        assertThat(result).isNotNull();
        verify(cb).like(lowerUserName, "%john%");
        verify(cb).like(lowerUserEmail, "%john%");
        verify(cb).like(lowerSpaceName, "%john%");

        ArgumentCaptor<Predicate[]> captor = ArgumentCaptor.forClass(Predicate[].class);
        verify(cb).and(captor.capture());
        assertThat(captor.getValue()).hasSize(1);
    }

    @Test
    @DisplayName("Should build EQUAL predicate for status filter")
    void givenStatus_whenBuildSearchSpecification_thenAddStatusPredicate() {
        given(root.<Booking, User>join(eq("user"), eq(JoinType.INNER))).willReturn(userJoin);
        given(root.<Booking, Space>join(eq("space"), eq(JoinType.INNER))).willReturn(spaceJoin);
        given(root.get("status")).willReturn(statusPath);
        given(cb.equal(statusPath, BookingStatus.CONFIRMED)).willReturn(dummyPredicate);

        BookingSearchRequest request = BookingSearchRequest.builder()
                .status(BookingStatus.CONFIRMED)
                .build();

        Specification<Booking> spec = BookingSpecification.buildSearchSpecification(request);
        Predicate result = spec.toPredicate(root, query, cb);

        assertThat(result).isNotNull();
        verify(cb).equal(statusPath, BookingStatus.CONFIRMED);

        ArgumentCaptor<Predicate[]> captor = ArgumentCaptor.forClass(Predicate[].class);
        verify(cb).and(captor.capture());
        assertThat(captor.getValue()).hasSize(1);
    }

    @Test
    @DisplayName("Should build EQUAL predicates for userId and spaceId")
    void givenUserAndSpaceId_whenBuildSearchSpecification_thenAddIdPredicates() {
        given(root.<Booking, User>join(eq("user"), eq(JoinType.INNER))).willReturn(userJoin);
        given(root.<Booking, Space>join(eq("space"), eq(JoinType.INNER))).willReturn(spaceJoin);
        given(userJoin.get("id")).willReturn(userIdPath);
        given(spaceJoin.get("id")).willReturn(spaceIdPath);

        given(cb.equal(userIdPath, 10L)).willReturn(dummyPredicate);
        given(cb.equal(spaceIdPath, 20L)).willReturn(dummyPredicate);

        BookingSearchRequest request = BookingSearchRequest.builder()
                .userId(10L)
                .spaceId(20L)
                .build();

        Specification<Booking> spec = BookingSpecification.buildSearchSpecification(request);
        Predicate result = spec.toPredicate(root, query, cb);

        assertThat(result).isNotNull();
        verify(cb).equal(userIdPath, 10L);
        verify(cb).equal(spaceIdPath, 20L);

        ArgumentCaptor<Predicate[]> captor = ArgumentCaptor.forClass(Predicate[].class);
        verify(cb).and(captor.capture());
        assertThat(captor.getValue()).hasSize(2);
    }

    @Test
    @DisplayName("Should build date range predicates for fromDate and toDate")
    void givenDateRange_whenBuildSearchSpecification_thenAddDatePredicates() {
        LocalDateTime fromDate = LocalDateTime.of(2026, 8, 1, 0, 0);
        LocalDateTime toDate = LocalDateTime.of(2026, 8, 31, 23, 59);

        given(root.<Booking, User>join(eq("user"), eq(JoinType.INNER))).willReturn(userJoin);
        given(root.<Booking, Space>join(eq("space"), eq(JoinType.INNER))).willReturn(spaceJoin);
        given(root.<LocalDateTime>get("startTime")).willReturn(startTimePath);
        given(root.<LocalDateTime>get("endTime")).willReturn(endTimePath);

        given(cb.greaterThanOrEqualTo(startTimePath, fromDate)).willReturn(dummyPredicate);
        given(cb.lessThanOrEqualTo(endTimePath, toDate)).willReturn(dummyPredicate);

        BookingSearchRequest request = BookingSearchRequest.builder()
                .fromDate(fromDate)
                .toDate(toDate)
                .build();

        Specification<Booking> spec = BookingSpecification.buildSearchSpecification(request);
        Predicate result = spec.toPredicate(root, query, cb);

        assertThat(result).isNotNull();
        verify(cb).greaterThanOrEqualTo(startTimePath, fromDate);
        verify(cb).lessThanOrEqualTo(endTimePath, toDate);

        ArgumentCaptor<Predicate[]> captor = ArgumentCaptor.forClass(Predicate[].class);
        verify(cb).and(captor.capture());
        assertThat(captor.getValue()).hasSize(2);
    }
}
