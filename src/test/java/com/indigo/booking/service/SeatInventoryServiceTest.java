package com.indigo.booking.service;

import com.indigo.booking.exception.InsufficientSeatsException;
import com.indigo.booking.model.Airport;
import com.indigo.booking.model.Flight;
import com.indigo.booking.model.SeatInventory;
import com.indigo.booking.repository.FlightRepository;
import com.indigo.booking.repository.SeatInventoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SeatInventoryServiceTest {

    @Mock
    private SeatInventoryRepository seatInventoryRepository;

    @Mock
    private FlightRepository flightRepository;

    @InjectMocks
    private SeatInventoryService seatInventoryService;

    private Flight testFlight;
    private SeatInventory testInventory;
    private LocalDate testDate;

    @BeforeEach
    void setUp() {
        testDate = LocalDate.now().plusDays(1);

        Airport origin = new Airport();
        origin.setId(1L);
        origin.setCode("DEL");

        Airport destination = new Airport();
        destination.setId(2L);
        destination.setCode("BOM");

        testFlight = new Flight();
        testFlight.setId(1L);
        testFlight.setFlightNumber("6E-2001");
        testFlight.setOriginAirport(origin);
        testFlight.setDestinationAirport(destination);
        testFlight.setTotalSeats(180);
        testFlight.setDepartureTime(LocalTime.of(6, 0));
        testFlight.setArrivalTime(LocalTime.of(8, 0));

        testInventory = new SeatInventory();
        testInventory.setId(1L);
        testInventory.setFlight(testFlight);
        testInventory.setFlightDate(testDate);
        testInventory.setTotalSeats(180);
        testInventory.setAvailableSeats(180);
        testInventory.setBlockedSeats(0);
        testInventory.setBookedSeats(0);
    }

    @Test
    void blockSeats_Success() {
        // Arrange
        when(seatInventoryRepository.findByFlightIdAndDateWithLock(1L, testDate))
            .thenReturn(Optional.of(testInventory));
        when(seatInventoryRepository.save(any(SeatInventory.class)))
            .thenReturn(testInventory);

        // Act
        SeatInventory result = seatInventoryService.blockSeats(1L, testDate, 50);

        // Assert
        assertNotNull(result);
        assertEquals(130, result.getAvailableSeats());
        assertEquals(50, result.getBlockedSeats());
        verify(seatInventoryRepository).save(any(SeatInventory.class));
    }

    @Test
    void blockSeats_InsufficientSeats() {
        // Arrange
        when(seatInventoryRepository.findByFlightIdAndDateWithLock(1L, testDate))
            .thenReturn(Optional.of(testInventory));

        // Act & Assert
        assertThrows(InsufficientSeatsException.class, () -> {
            seatInventoryService.blockSeats(1L, testDate, 200);
        });

        verify(seatInventoryRepository, never()).save(any(SeatInventory.class));
    }

    @Test
    void confirmBooking_Success() {
        // Arrange
        testInventory.setAvailableSeats(130);
        testInventory.setBlockedSeats(50);

        when(seatInventoryRepository.findByFlightIdAndDateWithLock(1L, testDate))
            .thenReturn(Optional.of(testInventory));
        when(seatInventoryRepository.save(any(SeatInventory.class)))
            .thenReturn(testInventory);

        // Act
        seatInventoryService.confirmBooking(1L, testDate, 50);

        // Assert
        assertEquals(0, testInventory.getBlockedSeats());
        assertEquals(50, testInventory.getBookedSeats());
        verify(seatInventoryRepository).save(any(SeatInventory.class));
    }

    @Test
    void releaseBlockedSeats_Success() {
        // Arrange
        testInventory.setAvailableSeats(130);
        testInventory.setBlockedSeats(50);

        when(seatInventoryRepository.findByFlightIdAndDateWithLock(1L, testDate))
            .thenReturn(Optional.of(testInventory));
        when(seatInventoryRepository.save(any(SeatInventory.class)))
            .thenReturn(testInventory);

        // Act
        seatInventoryService.releaseBlockedSeats(1L, testDate, 50);

        // Assert
        assertEquals(180, testInventory.getAvailableSeats());
        assertEquals(0, testInventory.getBlockedSeats());
        verify(seatInventoryRepository).save(any(SeatInventory.class));
    }
}
