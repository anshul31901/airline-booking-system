package com.indigo.booking.repository;

import com.indigo.booking.model.Airport;
import com.indigo.booking.model.FlightRoute;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FlightRouteRepository extends JpaRepository<FlightRoute, Long> {

    Optional<FlightRoute> findByOriginAirportAndDestinationAirport(Airport origin, Airport destination);

    boolean existsByOriginAirportAndDestinationAirport(Airport origin, Airport destination);
}
