package com.indigo.booking.service;

import com.indigo.booking.model.*;
import com.indigo.booking.repository.BookingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingCleanupServiceTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private SeatInventoryService seatInventoryService;

    @InjectMocks
    private BookingCleanupService bookingCleanupService;

    private List<Booking> expiredBookings;

    @BeforeEach
    void setUp() {
        expiredBookings = new ArrayList<>();

        Airport origin = new Airport();
        origin.setId(1L);
        origin.setCode("DEL");

        Airport destination = new Airport();
        destination.setId(2L);
        destination.setCode("BOM");

        Flight flight = new Flight();
        flight.setId(1L);
        flight.setFlightNumber("6E-2001");
        flight.setOriginAirport(origin);
        flight.setDestinationAirport(destination);
        flight.setTotalSeats(180);
        flight.setDepartureTime(LocalTime.of(6, 0));
        flight.setArrivalTime(LocalTime.of(8, 0));

        for (int i = 1; i <= 3; i++) {
            Booking booking = new Booking();
            booking.setId((long) i);
            booking.setBookingReference("BK1234567" + i);
            booking.setFlight(flight);
            booking.setFlightDate(LocalDate.now().plusDays(1));
            booking.setNumPassengers(2);
            booking.setStatus(BookingStatus.PENDING);
            booking.setExpiresAt(LocalDateTime.now().minusMinutes(5)); // Expired
            expiredBookings.add(booking);
        }
    }

    @Test
    void cleanupExpiredBookings_Success() {
        // Arrange
        when(bookingRepository.findExpiredBookings(
            eq(BookingStatus.PENDING),
            any(LocalDateTime.class),
            any(PageRequest.class)
        )).thenReturn(expiredBookings);

        when(bookingRepository.save(any(Booking.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        doNothing().when(seatInventoryService).releaseBlockedSeats(any(), any(), anyInt());

        // Act
        bookingCleanupService.cleanupExpiredBookings();

        // Assert
        verify(seatInventoryService, times(3)).releaseBlockedSeats(any(), any(), eq(2));
        verify(bookingRepository, times(3)).save(any(Booking.class));
    }

    @Test
    void cleanupExpiredBookings_NoExpiredBookings() {
        // Arrange
        when(bookingRepository.findExpiredBookings(
            eq(BookingStatus.PENDING),
            any(LocalDateTime.class),
            any(PageRequest.class)
        )).thenReturn(new ArrayList<>());

        // Act
        bookingCleanupService.cleanupExpiredBookings();

        // Assert
        verify(seatInventoryService, never()).releaseBlockedSeats(any(), any(), anyInt());
        verify(bookingRepository, never()).save(any(Booking.class));
    }
}
