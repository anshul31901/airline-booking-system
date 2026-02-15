package com.indigo.booking;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Main application class for InDiGo Airline Booking System.
 *
 * Features:
 * - Flight search (direct and indirect)
 * - Seat inventory management with pessimistic locking
 * - Booking creation with 10-minute expiry
 * - Booking confirmation and cancellation
 * - Scheduled cleanup of expired bookings
 */
@SpringBootApplication
@EnableJpaAuditing
public class AirlineBookingApplication {

    public static void main(String[] args) {
        SpringApplication.run(AirlineBookingApplication.class, args);
    }
}
