package com.indigo.booking.service;

import com.indigo.booking.exception.BookingExpiredException;
import com.indigo.booking.exception.InvalidBookingStateException;
import com.indigo.booking.exception.ResourceNotFoundException;
import com.indigo.booking.model.Booking;
import com.indigo.booking.model.BookingStatus;
import com.indigo.booking.model.Flight;
import com.indigo.booking.model.Passenger;
import com.indigo.booking.repository.BookingRepository;
import com.indigo.booking.repository.FlightRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingService {

    private final BookingRepository bookingRepository;
    private final FlightRepository flightRepository;
    private final SeatInventoryService seatInventoryService;

    private static final int BOOKING_EXPIRY_MINUTES = 10;

    @Transactional
    public Booking createBooking(Long flightId, LocalDate flightDate, int numPassengers, List<Passenger> passengers) {
        log.info("Creating booking for flight {} on {} with {} passengers", flightId, flightDate, numPassengers);

        Flight flight = flightRepository.findById(flightId)
            .orElseThrow(() -> new ResourceNotFoundException("Flight not found: " + flightId));

        seatInventoryService.blockSeats(flightId, flightDate, numPassengers);

        Booking booking = new Booking();
        booking.setBookingReference(generateBookingReference());
        booking.setFlight(flight);
        booking.setFlightDate(flightDate);
        booking.setNumPassengers(numPassengers);
        booking.setStatus(BookingStatus.PENDING);
        booking.setExpiresAt(LocalDateTime.now().plusMinutes(BOOKING_EXPIRY_MINUTES));

        for (Passenger passenger : passengers) {
            booking.addPassenger(passenger);
        }

        Booking savedBooking = bookingRepository.save(booking);
        log.info("Booking created successfully: {} (expires at {})", savedBooking.getBookingReference(), savedBooking.getExpiresAt());

        return savedBooking;
    }

    @Transactional
    public Booking confirmBooking(Long bookingId) {
        log.info("Confirming booking {}", bookingId);

        Booking booking = bookingRepository.findById(bookingId)
            .orElseThrow(() -> new ResourceNotFoundException("Booking not found: " + bookingId));

        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new InvalidBookingStateException("Booking is not in PENDING state: " + booking.getStatus());
        }

        if (booking.isExpired()) {
            throw new BookingExpiredException("Booking has expired at " + booking.getExpiresAt());
        }

        seatInventoryService.confirmBooking(
            booking.getFlight().getId(),
            booking.getFlightDate(),
            booking.getNumPassengers()
        );

        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setExpiresAt(null);

        Booking confirmedBooking = bookingRepository.save(booking);
        log.info("Booking confirmed successfully: {}", confirmedBooking.getBookingReference());

        return confirmedBooking;
    }

    @Transactional
    public Booking cancelBooking(Long bookingId) {
        log.info("Cancelling booking {}", bookingId);

        Booking booking = bookingRepository.findById(bookingId)
            .orElseThrow(() -> new ResourceNotFoundException("Booking not found: " + bookingId));

        if (booking.getStatus() == BookingStatus.CANCELLED || booking.getStatus() == BookingStatus.EXPIRED) {
            throw new InvalidBookingStateException("Booking already cancelled or expired");
        }

        if (booking.getStatus() == BookingStatus.PENDING) {
            seatInventoryService.releaseBlockedSeats(
                booking.getFlight().getId(),
                booking.getFlightDate(),
                booking.getNumPassengers()
            );
        }
        booking.setStatus(BookingStatus.CANCELLED);
        booking.setExpiresAt(null);

        Booking cancelledBooking = bookingRepository.save(booking);
        log.info("Booking cancelled successfully: {}", cancelledBooking.getBookingReference());

        return cancelledBooking;
    }

    @Transactional(readOnly = true)
    public Booking getBooking(Long bookingId) {
        return bookingRepository.findById(bookingId)
            .orElseThrow(() -> new ResourceNotFoundException("Booking not found: " + bookingId));
    }

    @Transactional(readOnly = true)
    public Booking getBookingByReference(String bookingReference) {
        return bookingRepository.findByBookingReference(bookingReference)
            .orElseThrow(() -> new ResourceNotFoundException("Booking not found: " + bookingReference));
    }

    private String generateBookingReference() {
        String uuid = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        return "BK" + uuid;
    }
}
