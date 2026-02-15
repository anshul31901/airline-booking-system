package com.indigo.booking.repository;

import com.indigo.booking.model.Airport;
import com.indigo.booking.model.Flight;
import com.indigo.booking.model.SeatInventory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for SeatInventoryRepository.
 * Tests pessimistic locking behavior.
 */
@DataJpaTest
@ActiveProfiles("test")
class SeatInventoryRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private SeatInventoryRepository seatInventoryRepository;

    @Autowired
    private AirportRepository airportRepository;

    @Autowired
    private FlightRepository flightRepository;

    private Flight testFlight;
    private LocalDate testDate;

    @BeforeEach
    void setUp() {
        testDate = LocalDate.now().plusDays(1);

        Airport origin = new Airport();
        origin.setCode("DEL");
        origin.setName("Delhi Airport");
        origin.setCity("Delhi");
        origin.setCountry("India");
        origin = airportRepository.save(origin);

        Airport destination = new Airport();
        destination.setCode("BOM");
        destination.setName("Mumbai Airport");
        destination.setCity("Mumbai");
        destination.setCountry("India");
        destination = airportRepository.save(destination);

        testFlight = new Flight();
        testFlight.setFlightNumber("6E-2001");
        testFlight.setOriginAirport(origin);
        testFlight.setDestinationAirport(destination);
        testFlight.setDepartureTime(LocalTime.of(6, 0));
        testFlight.setArrivalTime(LocalTime.of(8, 0));
        testFlight.setDurationMinutes(120);
        testFlight.setTotalSeats(180);
        testFlight.setActive(true);
        testFlight.setBasePrice(new BigDecimal("4500.00"));
        testFlight = flightRepository.save(testFlight);

        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void findByFlightIdAndDateWithLock_Success() {
        // Arrange
        SeatInventory inventory = new SeatInventory();
        inventory.setFlight(testFlight);
        inventory.setFlightDate(testDate);
        inventory.setTotalSeats(180);
        inventory.setAvailableSeats(180);
        inventory.setBlockedSeats(0);
        inventory.setBookedSeats(0);
        seatInventoryRepository.save(inventory);

        entityManager.flush();
        entityManager.clear();

        // Act
        Optional<SeatInventory> result = seatInventoryRepository
            .findByFlightIdAndDateWithLock(testFlight.getId(), testDate);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(180, result.get().getAvailableSeats());
    }

    @Test
    void findByFlightIdAndDate_Success() {
        // Arrange
        SeatInventory inventory = new SeatInventory();
        inventory.setFlight(testFlight);
        inventory.setFlightDate(testDate);
        inventory.setTotalSeats(180);
        inventory.setAvailableSeats(180);
        inventory.setBlockedSeats(0);
        inventory.setBookedSeats(0);
        seatInventoryRepository.save(inventory);

        entityManager.flush();
        entityManager.clear();

        // Act
        Optional<SeatInventory> result = seatInventoryRepository
            .findByFlightIdAndDate(testFlight.getId(), testDate);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(180, result.get().getAvailableSeats());
    }

    @Test
    void seatInventory_MaintainsInvariant() {
        // Arrange
        SeatInventory inventory = new SeatInventory();
        inventory.setFlight(testFlight);
        inventory.setFlightDate(testDate);
        inventory.setTotalSeats(180);
        inventory.setAvailableSeats(130);
        inventory.setBlockedSeats(30);
        inventory.setBookedSeats(20);

        // Act
        SeatInventory saved = seatInventoryRepository.save(inventory);

        // Assert
        assertTrue(saved.hasInvariant());
        assertEquals(180, saved.getAvailableSeats() + saved.getBlockedSeats() + saved.getBookedSeats());
    }
}
