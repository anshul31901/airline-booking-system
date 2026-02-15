package com.indigo.booking.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SeatInventoryInvariantTest {

    private SeatInventory inventory;

    @BeforeEach
    void setUp() {
        inventory = new SeatInventory();
        inventory.setTotalSeats(180);
        inventory.setAvailableSeats(180);
        inventory.setBlockedSeats(0);
        inventory.setBookedSeats(0);
    }

    @Test
    void blockSeats_shouldMoveFromAvailableToBlocked() {
        inventory.blockSeats(5);

        assertEquals(175, inventory.getAvailableSeats());
        assertEquals(5, inventory.getBlockedSeats());
        assertEquals(0, inventory.getBookedSeats());
        assertTrue(inventory.hasInvariant());
    }

    @Test
    void confirmSeats_shouldMoveFromBlockedToBooked() {
        inventory.blockSeats(5);
        inventory.confirmSeats(5);

        assertEquals(175, inventory.getAvailableSeats());
        assertEquals(0, inventory.getBlockedSeats());
        assertEquals(5, inventory.getBookedSeats());
        assertTrue(inventory.hasInvariant());
    }

    @Test
    void releaseBlockedSeats_shouldMoveBackToAvailable() {
        inventory.blockSeats(5);
        inventory.releaseBlockedSeats(5);

        assertEquals(180, inventory.getAvailableSeats());
        assertEquals(0, inventory.getBlockedSeats());
        assertEquals(0, inventory.getBookedSeats());
        assertTrue(inventory.hasInvariant());
    }

    @Test
    void blockSeats_shouldFailWhenInsufficientAvailable() {
        inventory.setAvailableSeats(3);
        inventory.setBlockedSeats(177);

        assertThrows(IllegalStateException.class, () -> inventory.blockSeats(5));
    }

    @Test
    void confirmSeats_shouldFailWhenInsufficientBlocked() {
        assertThrows(IllegalStateException.class, () -> inventory.confirmSeats(5));
    }

    @Test
    void canBlock_shouldReturnCorrectly() {
        assertTrue(inventory.canBlock(180));
        assertTrue(inventory.canBlock(1));
        assertFalse(inventory.canBlock(181));

        inventory.blockSeats(180);
        assertFalse(inventory.canBlock(1));
    }

    @Test
    void invariant_shouldAlwaysHold() {
        // Simulate full booking lifecycle
        inventory.blockSeats(10);   // available=170, blocked=10, booked=0
        assertTrue(inventory.hasInvariant());

        inventory.confirmSeats(5);  // available=170, blocked=5, booked=5
        assertTrue(inventory.hasInvariant());

        inventory.releaseBlockedSeats(3); // available=173, blocked=2, booked=5
        assertTrue(inventory.hasInvariant());

        assertEquals(180, inventory.getAvailableSeats() + inventory.getBlockedSeats() + inventory.getBookedSeats());
    }
}
