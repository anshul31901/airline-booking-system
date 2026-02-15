package com.indigo.booking.service;

import com.indigo.booking.exception.BookingExpiredException;
import com.indigo.booking.exception.InvalidBookingStateException;
import com.indigo.booking.model.*;
import com.indigo.booking.repository.BookingRepository;
import com.indigo.booking.repository.FlightRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private FlightRepository flightRepository;

    @Mock
    private SeatInventoryService seatInventoryService;

    @InjectMocks
    private BookingService bookingService;

    private Flight testFlight;
    private Booking testBooking;
    private List<Passenger> testPassengers;

    @BeforeEach
    void setUp() {
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

        testPassengers = new ArrayList<>();
        Passenger passenger = new Passenger();
        passenger.setName("John Doe");
        passenger.setAge(30);
        passenger.setGender(Gender.M);
        passenger.setIdProofNumber("ABC123");
        testPassengers.add(passenger);

        testBooking = new Booking();
        testBooking.setId(1L);
        testBooking.setBookingReference("BK12345678");
        testBooking.setFlight(testFlight);
        testBooking.setFlightDate(LocalDate.now().plusDays(1));
        testBooking.setNumPassengers(1);
        testBooking.setStatus(BookingStatus.PENDING);
        testBooking.setExpiresAt(LocalDateTime.now().plusMinutes(10));
    }

    @Test
    void createBooking_Success() {
        // Arrange
        when(flightRepository.findById(1L)).thenReturn(Optional.of(testFlight));
        when(bookingRepository.save(any(Booking.class))).thenReturn(testBooking);
        when(seatInventoryService.blockSeats(any(), any(), anyInt())).thenReturn(new SeatInventory());

        // Act
        Booking result = bookingService.createBooking(
            1L,
            LocalDate.now().plusDays(1),
            1,
            testPassengers
        );

        // Assert
        assertNotNull(result);
        assertEquals(BookingStatus.PENDING, result.getStatus());
        assertNotNull(result.getExpiresAt());
        verify(seatInventoryService).blockSeats(any(), any(), eq(1));
        verify(bookingRepository).save(any(Booking.class));
    }

    @Test
    void confirmBooking_Success() {
        // Arrange
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(testBooking));
        when(bookingRepository.save(any(Booking.class))).thenReturn(testBooking);
        doNothing().when(seatInventoryService).confirmBooking(any(), any(), anyInt());

        // Act
        Booking result = bookingService.confirmBooking(1L);

        // Assert
        assertEquals(BookingStatus.CONFIRMED, result.getStatus());
        assertNull(result.getExpiresAt());
        verify(seatInventoryService).confirmBooking(any(), any(), eq(1));
        verify(bookingRepository).save(any(Booking.class));
    }

    @Test
    void confirmBooking_Expired() {
        // Arrange
        testBooking.setExpiresAt(LocalDateTime.now().minusMinutes(1)); // Already expired
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(testBooking));

        // Act & Assert
        assertThrows(BookingExpiredException.class, () -> {
            bookingService.confirmBooking(1L);
        });

        verify(seatInventoryService, never()).confirmBooking(any(), any(), anyInt());
        verify(bookingRepository, never()).save(any(Booking.class));
    }

    @Test
    void confirmBooking_AlreadyConfirmed() {
        // Arrange
        testBooking.setStatus(BookingStatus.CONFIRMED);
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(testBooking));

        // Act & Assert
        assertThrows(InvalidBookingStateException.class, () -> {
            bookingService.confirmBooking(1L);
        });
    }

    @Test
    void cancelBooking_Success() {
        // Arrange
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(testBooking));
        when(bookingRepository.save(any(Booking.class))).thenReturn(testBooking);
        doNothing().when(seatInventoryService).releaseBlockedSeats(any(), any(), anyInt());

        // Act
        Booking result = bookingService.cancelBooking(1L);

        // Assert
        assertEquals(BookingStatus.CANCELLED, result.getStatus());
        verify(seatInventoryService).releaseBlockedSeats(any(), any(), eq(1));
        verify(bookingRepository).save(any(Booking.class));
    }
}
