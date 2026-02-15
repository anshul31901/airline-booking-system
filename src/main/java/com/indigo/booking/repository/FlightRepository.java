package com.indigo.booking.repository;

import com.indigo.booking.model.Airport;
import com.indigo.booking.model.Flight;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FlightRepository extends JpaRepository<Flight, Long> {

    Optional<Flight> findByFlightNumber(String flightNumber);

    /**
     * Finds direct flights between origin and destination airports.
     * Used for direct flight search.
     */
    @Query("SELECT f FROM Flight f " +
           "WHERE f.originAirport.id = :originId " +
           "AND f.destinationAirport.id = :destinationId " +
           "AND f.isActive = true " +
           "ORDER BY f.departureTime")
    List<Flight> findDirectFlights(
        @Param("originId") Long originId,
        @Param("destinationId") Long destinationId
    );

    /**
     * Finds all flights departing from a specific airport.
     * Used for indirect flight search (first leg).
     */
    @Query("SELECT f FROM Flight f " +
           "WHERE f.originAirport.id = :originId " +
           "AND f.isActive = true " +
           "ORDER BY f.departureTime")
    List<Flight> findFlightsByOrigin(@Param("originId") Long originId);

    /**
     * Finds all flights arriving at a specific airport.
     * Used for indirect flight search (finding layover airports).
     */
    @Query("SELECT DISTINCT f.destinationAirport.id FROM Flight f " +
           "WHERE f.originAirport.id = :originId " +
           "AND f.isActive = true")
    List<Long> findDestinationAirportIdsByOrigin(@Param("originId") Long originId);

    /**
     * Finds connecting flights from layover to destination.
     * Used for indirect flight search (second leg).
     */
    @Query("SELECT f FROM Flight f " +
           "WHERE f.originAirport.id = :layoverAirportId " +
           "AND f.destinationAirport.id = :destinationId " +
           "AND f.isActive = true " +
           "ORDER BY f.departureTime")
    List<Flight> findConnectingFlights(
        @Param("layoverAirportId") Long layoverAirportId,
        @Param("destinationId") Long destinationId
    );
}
