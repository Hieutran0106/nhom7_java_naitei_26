package com.nhom7.coworkingspace.repository;

import com.nhom7.coworkingspace.entity.Booking;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository interface for {@link Booking} entity.
 */
@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    @Override
    @EntityGraph(attributePaths = {"user", "space"})
    Optional<Booking> findById(Long id);
}
