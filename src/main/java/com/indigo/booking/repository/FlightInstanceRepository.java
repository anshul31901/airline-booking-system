package com.indigo.booking.repository;

import com.indigo.booking.model.FlightInstance;
import com.indigo.booking.model.FlightInstanceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface FlightInstanceRepository extends JpaRepository<FlightInstance, Long> {

    @Query("SELECT fi FROM FlightInstance fi " +
           "WHERE fi.flight.id = :flightId " +
           "AND fi.flightDate = :flightDate")
    Optional<FlightInstance> findByFlightIdAndDate(
        @Param("flightId") Long flightId,
        @Param("flightDate") LocalDate flightDate
    );

    @Query("SELECT fi FROM FlightInstance fi " +
           "WHERE fi.flight.id IN :flightIds " +
           "AND fi.flightDate = :flightDate " +
           "AND fi.status = :status")
    List<FlightInstance> findByFlightIdsAndDateAndStatus(
        @Param("flightIds") List<Long> flightIds,
        @Param("flightDate") LocalDate flightDate,
        @Param("status") FlightInstanceStatus status
    );

    @Query("SELECT fi FROM FlightInstance fi " +
           "WHERE fi.flightDate = :flightDate " +
           "AND fi.status = :status")
    List<FlightInstance> findByDateAndStatus(
        @Param("flightDate") LocalDate flightDate,
        @Param("status") FlightInstanceStatus status
    );
}
