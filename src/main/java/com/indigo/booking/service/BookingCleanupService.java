package com.indigo.booking.service;

import com.indigo.booking.model.Booking;
import com.indigo.booking.model.BookingStatus;
import com.indigo.booking.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingCleanupService {

    private final BookingRepository bookingRepository;
    private final SeatInventoryService seatInventoryService;

    private static final int BATCH_SIZE = 100;
    private static final long CLEANUP_INTERVAL_MS = 120_000;
    private static final long CLEANUP_INITIAL_DELAY_MS = 60_000;

    @Scheduled(fixedDelay = CLEANUP_INTERVAL_MS, initialDelay = CLEANUP_INITIAL_DELAY_MS)
    @Transactional
    public void cleanupExpiredBookings() {
        log.info("Starting booking cleanup job");

        LocalDateTime now = LocalDateTime.now();

        List<Booking> expiredBookings = bookingRepository.findExpiredBookings(
            BookingStatus.PENDING,
            now,
            PageRequest.of(0, BATCH_SIZE)
        );

        if (expiredBookings.isEmpty()) {
            log.debug("No expired bookings found");
            return;
        }

        log.info("Found {} expired bookings to process", expiredBookings.size());

        int successCount = 0;
        int failureCount = 0;

        for (Booking booking : expiredBookings) {
            try {
                seatInventoryService.releaseBlockedSeats(
                    booking.getFlight().getId(),
                    booking.getFlightDate(),
                    booking.getNumPassengers()
                );

                booking.setStatus(BookingStatus.EXPIRED);
                bookingRepository.save(booking);

                successCount++;
                log.debug("Expired booking: {}", booking.getBookingReference());

            } catch (Exception e) {
                failureCount++;
                log.error("Failed to expire booking {}: {}", booking.getBookingReference(), e.getMessage(), e);
            }
        }

        log.info("Booking cleanup completed. Success: {}, Failures: {}", successCount, failureCount);
    }
}
