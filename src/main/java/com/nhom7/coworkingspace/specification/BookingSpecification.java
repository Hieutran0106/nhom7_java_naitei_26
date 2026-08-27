package com.nhom7.coworkingspace.specification;

import com.nhom7.coworkingspace.dto.request.BookingSearchRequest;
import com.nhom7.coworkingspace.entity.Booking;
import com.nhom7.coworkingspace.entity.Space;
import com.nhom7.coworkingspace.entity.User;
import com.nhom7.coworkingspace.entity.Venue;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

public final class BookingSpecification {

    private BookingSpecification() {
        // Private constructor for utility class
    }

    public static Specification<Booking> buildSearchSpecification(
            BookingSearchRequest request
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates =
                    new ArrayList<>();

            if (request == null) {
                return cb.and(
                        predicates.toArray(
                                new Predicate[0]
                        )
                );
            }

            Join<Booking, User> userJoin =
                    root.join(
                            "user",
                            JoinType.INNER
                    );

            Join<Booking, Space> spaceJoin =
                    root.join(
                            "space",
                            JoinType.INNER
                    );

            // Keyword search
            // User name, user email, or space name
            if (StringUtils.hasText(
                    request.getKeyword()
            )) {
                String pattern =
                        "%"
                        + request.getKeyword()
                                .trim()
                                .toLowerCase()
                        + "%";

                Predicate userNameMatch =
                        cb.like(
                                cb.lower(
                                        userJoin.get(
                                                "name"
                                        )
                                ),
                                pattern
                        );

                Predicate userEmailMatch =
                        cb.like(
                                cb.lower(
                                        userJoin.get(
                                                "email"
                                        )
                                ),
                                pattern
                        );

                Predicate spaceNameMatch =
                        cb.like(
                                cb.lower(
                                        spaceJoin.get(
                                                "name"
                                        )
                                ),
                                pattern
                        );

                predicates.add(
                        cb.or(
                                userNameMatch,
                                userEmailMatch,
                                spaceNameMatch
                        )
                );
            }

            // Filter by booking status
            if (request.getStatus() != null) {
                predicates.add(
                        cb.equal(
                                root.get(
                                        "status"
                                ),
                                request.getStatus()
                        )
                );
            }

            // Filter by User ID
            if (request.getUserId() != null) {
                predicates.add(
                        cb.equal(
                                userJoin.get(
                                        "id"
                                ),
                                request.getUserId()
                        )
                );
            }

            // Filter by Space ID
            if (request.getSpaceId() != null) {
                predicates.add(
                        cb.equal(
                                spaceJoin.get(
                                        "id"
                                ),
                                request.getSpaceId()
                        )
                );
            }

            // Filter by Venue ID
            if (request.getVenueId() != null) {
                Join<Space, Venue> venueJoin =
                        spaceJoin.join(
                                "venue",
                                JoinType.INNER
                        );

                predicates.add(
                        cb.equal(
                                venueJoin.get(
                                        "id"
                                ),
                                request.getVenueId()
                        )
                );
            }

            // Filter by Date range
            if (request.getFromDate() != null) {
                predicates.add(
                        cb.greaterThanOrEqualTo(
                                root.get(
                                        "startTime"
                                ),
                                request.getFromDate()
                        )
                );
            }

            if (request.getToDate() != null) {
                predicates.add(
                        cb.lessThanOrEqualTo(
                                root.get(
                                        "endTime"
                                ),
                                request.getToDate()
                        )
                );
            }

            return cb.and(
                    predicates.toArray(
                            new Predicate[0]
                    )
            );
        };
    }
}