package com.indigo.booking.repository;

import com.indigo.booking.model.SeatInventory;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * CRITICAL: Repository for seat inventory management with pessimistic locking.
 * The pessimistic write lock prevents concurrent bookings from causing double-booking.
 */
@Repository
public interface SeatInventoryRepository extends JpaRepository<SeatInventory, Long> {

    /**
     * CRITICAL: Acquires a pessimistic write lock (SELECT FOR UPDATE) on the seat inventory row.
     * This ensures only one transaction can modify the seat inventory at a time.
     * Other transactions will wait up to the configured timeout (10 seconds).
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT si FROM SeatInventory si " +
           "WHERE si.flight.id = :flightId " +
           "AND si.flightDate = :flightDate")
    Optional<SeatInventory> findByFlightIdAndDateWithLock(
        @Param("flightId") Long flightId,
        @Param("flightDate") LocalDate flightDate
    );

    /**
     * Non-locking query for read-only operations (e.g., search results).
     */
    @Query("SELECT si FROM SeatInventory si " +
           "WHERE si.flight.id = :flightId " +
           "AND si.flightDate = :flightDate")
    Optional<SeatInventory> findByFlightIdAndDate(
        @Param("flightId") Long flightId,
        @Param("flightDate") LocalDate flightDate
    );

    /**
     * Batch fetch seat inventory for multiple flights (for search results).
     * Non-locking query.
     */
    @Query("SELECT si FROM SeatInventory si " +
           "WHERE si.flight.id IN :flightIds " +
           "AND si.flightDate = :flightDate")
    List<SeatInventory> findByFlightIdsAndDate(
        @Param("flightIds") List<Long> flightIds,
        @Param("flightDate") LocalDate flightDate
    );
}
