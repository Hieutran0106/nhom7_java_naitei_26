package com.nhom7.coworkingspace.specification;

import com.nhom7.coworkingspace.dto.request.BookingSearchRequest;
import com.nhom7.coworkingspace.entity.Booking;
import com.nhom7.coworkingspace.entity.Space;
import com.nhom7.coworkingspace.entity.User;
import com.nhom7.coworkingspace.entity.Venue;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Root;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.domain.Specification;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@DisplayName(
        "BookingSpecification - Venue filter"
)
class BookingSpecificationVenueFilterTest {

    @Test
    @DisplayName(
            "Venue filter should join Space to Venue and filter by Venue ID"
    )
    @SuppressWarnings("unchecked")
    void venueFilterShouldFilterBookingsByVenueId() {

        Root<Booking> root =
                mock(Root.class);

        CriteriaQuery<?> query =
                mock(CriteriaQuery.class);

        CriteriaBuilder criteriaBuilder =
                mock(CriteriaBuilder.class);

        Join<Booking, User> userJoin =
                mock(Join.class);

        Join<Booking, Space> spaceJoin =
                mock(Join.class);

        Join<Space, Venue> venueJoin =
                mock(Join.class);

        Path<Long> venueIdPath =
                mock(Path.class);


        doReturn(userJoin)
                .when(root)
                .join(
                        "user",
                        JoinType.INNER
                );


        doReturn(spaceJoin)
                .when(root)
                .join(
                        "space",
                        JoinType.INNER
                );


        doReturn(venueJoin)
                .when(spaceJoin)
                .join(
                        "venue",
                        JoinType.INNER
                );


        doReturn(venueIdPath)
                .when(venueJoin)
                .get("id");


        BookingSearchRequest request =
                BookingSearchRequest.builder()
                        .venueId(15L)
                        .build();


        Specification<Booking> specification =
                BookingSpecification
                        .buildSearchSpecification(
                                request
                        );


        specification.toPredicate(
                root,
                query,
                criteriaBuilder
        );


        verify(spaceJoin)
                .join(
                        "venue",
                        JoinType.INNER
                );


        verify(venueJoin)
                .get("id");


        verify(criteriaBuilder)
                .equal(
                        venueIdPath,
                        15L
                );
    }
}