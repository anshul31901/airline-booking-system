package com.indigo.booking.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class BookingLifecycleTest {

    @Test
    void newBooking_shouldBePendingByDefault() {
        Booking booking = new Booking();
        assertEquals(BookingStatus.PENDING, booking.getStatus());
    }

    @Test
    void isExpired_shouldReturnTrue_whenPendingAndPastExpiry() {
        Booking booking = new Booking();
        booking.setStatus(BookingStatus.PENDING);
        booking.setExpiresAt(LocalDateTime.now().minusMinutes(1));

        assertTrue(booking.isExpired());
    }

    @Test
    void isExpired_shouldReturnFalse_whenPendingAndBeforeExpiry() {
        Booking booking = new Booking();
        booking.setStatus(BookingStatus.PENDING);
        booking.setExpiresAt(LocalDateTime.now().plusMinutes(10));

        assertFalse(booking.isExpired());
    }

    @Test
    void isExpired_shouldReturnFalse_whenConfirmed() {
        Booking booking = new Booking();
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setExpiresAt(LocalDateTime.now().minusMinutes(1));

        assertFalse(booking.isExpired());
    }

    @Test
    void isExpired_shouldReturnFalse_whenNoExpirySet() {
        Booking booking = new Booking();
        booking.setStatus(BookingStatus.PENDING);
        booking.setExpiresAt(null);

        assertFalse(booking.isExpired());
    }

    @Test
    void addPassenger_shouldSetBidirectionalRelationship() {
        Booking booking = new Booking();
        Passenger passenger = new Passenger();
        passenger.setName("John Doe");

        booking.addPassenger(passenger);

        assertEquals(1, booking.getPassengers().size());
        assertEquals(booking, passenger.getBooking());
    }
}
