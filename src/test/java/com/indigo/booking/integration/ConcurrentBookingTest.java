package com.indigo.booking.integration;

import com.indigo.booking.exception.InsufficientSeatsException;
import com.indigo.booking.model.*;
import com.indigo.booking.repository.*;
import com.indigo.booking.service.BookingService;
import com.indigo.booking.service.SeatInventoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CRITICAL: Integration test for concurrent booking scenario.
 * Tests that pessimistic locking prevents double-booking.
 */
@SpringBootTest
@ActiveProfiles("test")
class ConcurrentBookingTest {

    @Autowired
    private BookingService bookingService;

    @Autowired
    private SeatInventoryService seatInventoryService;

    @Autowired
    private FlightRepository flightRepository;

    @Autowired
    private AirportRepository airportRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private SeatInventoryRepository seatInventoryRepository;

    @Autowired
    private FlightInstanceRepository flightInstanceRepository;

    @Autowired
    private FlightRouteRepository flightRouteRepository;

    private Flight testFlight;
    private LocalDate testDate;

    @BeforeEach
    void setUp() {
        // Clean up
        bookingRepository.deleteAll();
        seatInventoryRepository.deleteAll();
        flightInstanceRepository.deleteAll();
        flightRouteRepository.deleteAll();
        flightRepository.deleteAll();
        airportRepository.deleteAll();

        testDate = LocalDate.now().plusDays(1);

        // Create airports
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

        // Create flight with limited seats
        testFlight = new Flight();
        testFlight.setFlightNumber("6E-TEST");
        testFlight.setOriginAirport(origin);
        testFlight.setDestinationAirport(destination);
        testFlight.setDepartureTime(LocalTime.of(6, 0));
        testFlight.setArrivalTime(LocalTime.of(8, 0));
        testFlight.setDurationMinutes(120);
        testFlight.setTotalSeats(10); // Only 10 seats
        testFlight.setActive(true);
        testFlight.setBasePrice(new BigDecimal("4500.00"));
        testFlight = flightRepository.save(testFlight);

        // Create seat inventory
        SeatInventory inventory = new SeatInventory();
        inventory.setFlight(testFlight);
        inventory.setFlightDate(testDate);
        inventory.setTotalSeats(10);
        inventory.setAvailableSeats(10);
        inventory.setBlockedSeats(0);
        inventory.setBookedSeats(0);
        seatInventoryRepository.save(inventory);
    }

    @Test
    void concurrentBooking_PreventDoubleBooking() throws InterruptedException {
        // Arrange
        int numThreads = 5;
        int seatsPerBooking = 3;
        ExecutorService executorService = Executors.newFixedThreadPool(numThreads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(numThreads);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        // Act: Simulate concurrent bookings
        for (int i = 0; i < numThreads; i++) {
            final int threadNum = i;
            executorService.submit(() -> {
                try {
                    startLatch.await(); // Wait for all threads to be ready

                    List<Passenger> passengers = new ArrayList<>();
                    for (int j = 0; j < seatsPerBooking; j++) {
                        Passenger passenger = new Passenger();
                        passenger.setName("Passenger-" + threadNum + "-" + j);
                        passenger.setAge(30);
                        passenger.setGender(Gender.M);
                        passenger.setIdProofNumber("ID-" + threadNum + "-" + j);
                        passengers.add(passenger);
                    }

                    bookingService.createBooking(
                        testFlight.getId(),
                        testDate,
                        seatsPerBooking,
                        passengers
                    );

                    successCount.incrementAndGet();

                } catch (InsufficientSeatsException e) {
                    failureCount.incrementAndGet();
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    endLatch.countDown();
                }
            });
        }

        // Start all threads at once
        startLatch.countDown();
        endLatch.await();
        executorService.shutdown();

        // Assert: Only 3 bookings should succeed (3 * 3 = 9 seats)
        // The 4th booking should fail due to insufficient seats
        assertEquals(3, successCount.get(), "Expected 3 successful bookings");
        assertEquals(2, failureCount.get(), "Expected 2 failed bookings");

        // Verify seat inventory
        SeatInventory finalInventory = seatInventoryRepository
            .findByFlightIdAndDate(testFlight.getId(), testDate)
            .orElseThrow();

        // Total seats = available + blocked + booked = 10
        assertTrue(finalInventory.hasInvariant(), "Seat invariant should be maintained");
        assertEquals(10, finalInventory.getTotalSeats());
        assertEquals(1, finalInventory.getAvailableSeats()); // 10 - 9 = 1 seat left
        assertEquals(9, finalInventory.getBlockedSeats()); // 3 bookings * 3 seats = 9
        assertEquals(0, finalInventory.getBookedSeats());
    }
}
