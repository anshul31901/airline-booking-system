package com.indigo.booking.service;

import com.indigo.booking.exception.InsufficientSeatsException;
import com.indigo.booking.exception.ResourceNotFoundException;
import com.indigo.booking.model.Flight;
import com.indigo.booking.model.SeatInventory;
import com.indigo.booking.repository.FlightRepository;
import com.indigo.booking.repository.SeatInventoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Slf4j
public class SeatInventoryService {

    private final SeatInventoryRepository seatInventoryRepository;
    private final FlightRepository flightRepository;

    @Transactional(isolation = Isolation.READ_COMMITTED, timeout = 10)
    public SeatInventory blockSeats(Long flightId, LocalDate flightDate, int numSeats) {
        log.info("Attempting to block {} seats for flight {} on {}", numSeats, flightId, flightDate);

        SeatInventory inventory = seatInventoryRepository
            .findByFlightIdAndDateWithLock(flightId, flightDate)
            .orElseGet(() -> createInventoryIfNotExists(flightId, flightDate));

        if (!inventory.canBlock(numSeats)) {
            log.warn("Insufficient seats. Requested: {}, Available: {}", numSeats, inventory.getAvailableSeats());
            throw new InsufficientSeatsException(
                String.format("Only %d seats available for flight %d on %s",
                    inventory.getAvailableSeats(), flightId, flightDate)
            );
        }

        inventory.blockSeats(numSeats);
        SeatInventory saved = seatInventoryRepository.save(inventory);

        log.info("Successfully blocked {} seats. New state: available={}, blocked={}, booked={}",
            numSeats, saved.getAvailableSeats(), saved.getBlockedSeats(), saved.getBookedSeats());

        return saved;
    }

    @Transactional(isolation = Isolation.READ_COMMITTED, timeout = 10)
    public void confirmBooking(Long flightId, LocalDate flightDate, int numSeats) {
        log.info("Confirming {} seats for flight {} on {}", numSeats, flightId, flightDate);

        SeatInventory inventory = seatInventoryRepository
            .findByFlightIdAndDateWithLock(flightId, flightDate)
            .orElseThrow(() -> new ResourceNotFoundException("Seat inventory not found"));

        inventory.confirmSeats(numSeats);
        seatInventoryRepository.save(inventory);

        log.info("Successfully confirmed {} seats. New state: blocked={}, booked={}",
            numSeats, inventory.getBlockedSeats(), inventory.getBookedSeats());
    }

    @Transactional(isolation = Isolation.READ_COMMITTED, timeout = 10)
    public void releaseBlockedSeats(Long flightId, LocalDate flightDate, int numSeats) {
        log.info("Releasing {} blocked seats for flight {} on {}", numSeats, flightId, flightDate);

        SeatInventory inventory = seatInventoryRepository
            .findByFlightIdAndDateWithLock(flightId, flightDate)
            .orElseThrow(() -> new ResourceNotFoundException("Seat inventory not found"));

        inventory.releaseBlockedSeats(numSeats);
        seatInventoryRepository.save(inventory);

        log.info("Successfully released {} seats. New state: available={}, blocked={}",
            numSeats, inventory.getAvailableSeats(), inventory.getBlockedSeats());
    }

    @Transactional(readOnly = true)
    public SeatInventory getInventory(Long flightId, LocalDate flightDate) {
        return seatInventoryRepository
            .findByFlightIdAndDate(flightId, flightDate)
            .orElseGet(() -> createInventoryIfNotExists(flightId, flightDate));
    }

    private SeatInventory createInventoryIfNotExists(Long flightId, LocalDate flightDate) {
        Flight flight = flightRepository.findById(flightId)
            .orElseThrow(() -> new ResourceNotFoundException("Flight not found: " + flightId));

        SeatInventory inventory = new SeatInventory();
        inventory.setFlight(flight);
        inventory.setFlightDate(flightDate);
        inventory.setTotalSeats(flight.getTotalSeats());
        inventory.setAvailableSeats(flight.getTotalSeats());
        inventory.setBlockedSeats(0);
        inventory.setBookedSeats(0);

        log.info("Creating new seat inventory for flight {} on {} with {} total seats",
            flightId, flightDate, flight.getTotalSeats());

        return seatInventoryRepository.save(inventory);
    }
}
