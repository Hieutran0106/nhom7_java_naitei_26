package com.nhom7.coworkingspace.repository;

import com.nhom7.coworkingspace.entity.Booking;
import com.nhom7.coworkingspace.enums.BookingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long>, JpaSpecificationExecutor<Booking> {

    @Override
    @EntityGraph(attributePaths = { "user", "space" })
    Optional<Booking> findById(Long id);

    @Override
    @EntityGraph(attributePaths = { "user", "space" })
    Page<Booking> findAll(Specification<Booking> spec, Pageable pageable);

    long countByStatus(BookingStatus status);

    @Query("SELECT COUNT(b) FROM Booking b WHERE UPPER(CAST(b.status AS string)) = UPPER(:status)")
    long countByStatusIgnoreCase(@Param("status") String status);

    @EntityGraph(attributePaths = {"user", "space"})
    Page<Booking> findByUserId(Long userId, Pageable pageable);

    @Query("""
            SELECT COUNT(b) > 0
            FROM Booking b
            WHERE b.space.id = :spaceId
              AND b.status NOT IN ('CANCELLED', 'REJECTED')
              AND b.startTime < :endTime
              AND b.endTime > :startTime
            """)
    boolean existsActiveOverlap(
            @Param("spaceId") Long spaceId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    @EntityGraph(attributePaths = { "user", "space" })
    @Query("SELECT b FROM Booking b WHERE b.space.venue.owner.id = :hostId")
    Page<Booking> findByHostId(@Param("hostId") Long hostId, Pageable pageable);
}
