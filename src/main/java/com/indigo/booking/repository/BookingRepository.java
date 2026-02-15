package com.indigo.booking.repository;

import com.indigo.booking.model.Booking;
import com.indigo.booking.model.BookingStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    Optional<Booking> findByBookingReference(String bookingReference);

    /**
     * Finds expired bookings that need to be cleaned up.
     * Used by the scheduled BookingCleanupService.
     */
    @Query("SELECT b FROM Booking b " +
           "WHERE b.status = :status " +
           "AND b.expiresAt < :currentTime " +
           "ORDER BY b.expiresAt ASC")
    List<Booking> findExpiredBookings(
        @Param("status") BookingStatus status,
        @Param("currentTime") LocalDateTime currentTime,
        Pageable pageable
    );

    /**
     * Finds all pending bookings for a specific flight and date.
     */
    @Query("SELECT b FROM Booking b " +
           "WHERE b.flight.id = :flightId " +
           "AND b.flightDate = :flightDate " +
           "AND b.status = :status")
    List<Booking> findByFlightAndDateAndStatus(
        @Param("flightId") Long flightId,
        @Param("flightDate") java.time.LocalDate flightDate,
        @Param("status") BookingStatus status
    );
}
