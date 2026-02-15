package com.indigo.booking.controller;

import com.indigo.booking.dto.BookingResponseDTO;
import com.indigo.booking.dto.CreateBookingRequestDTO;
import com.indigo.booking.dto.ErrorResponseDTO;
import com.indigo.booking.dto.PassengerDTO;
import com.indigo.booking.model.Booking;
import com.indigo.booking.model.Gender;
import com.indigo.booking.model.Passenger;
import com.indigo.booking.service.BookingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/bookings")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Booking Management", description = "APIs for creating, confirming, cancelling, and retrieving bookings")
public class BookingController {

    private final BookingService bookingService;

    @Operation(
        summary = "Create a new booking",
        description = "Creates a booking with PENDING status. Seats are blocked for 10 minutes. Booking must be confirmed within this time or it will expire automatically."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Booking created successfully",
            content = @Content(schema = @Schema(implementation = BookingResponseDTO.class))),
        @ApiResponse(responseCode = "400", description = "Invalid request data",
            content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
        @ApiResponse(responseCode = "404", description = "Flight not found",
            content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
        @ApiResponse(responseCode = "409", description = "Insufficient seats available",
            content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    @PostMapping
    public ResponseEntity<BookingResponseDTO> createBooking(
        @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Booking details with passenger information",
            required = true
        )
        @Valid @RequestBody CreateBookingRequestDTO request) {
        log.info("Creating booking for flight {} on {}", request.getFlightId(), request.getFlightDate());

        List<Passenger> passengers = request.getPassengers().stream()
            .map(this::toPassenger)
            .collect(Collectors.toList());

        Booking booking = bookingService.createBooking(
            request.getFlightId(),
            request.getFlightDate(),
            request.getNumPassengers(),
            passengers
        );

        BookingResponseDTO response = BookingResponseDTO.fromBooking(booking);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(
        summary = "Confirm a pending booking",
        description = "Confirms a PENDING booking and moves seats from blocked to booked status. Booking must not be expired."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Booking confirmed successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid booking state"),
        @ApiResponse(responseCode = "404", description = "Booking not found"),
        @ApiResponse(responseCode = "410", description = "Booking has expired")
    })
    @PostMapping("/{id}/confirm")
    public ResponseEntity<BookingResponseDTO> confirmBooking(
        @Parameter(description = "Booking ID", required = true)
        @PathVariable Long id) {
        log.info("Confirming booking {}", id);

        Booking booking = bookingService.confirmBooking(id);
        BookingResponseDTO response = BookingResponseDTO.fromBooking(booking);

        return ResponseEntity.ok(response);
    }

    @Operation(
        summary = "Cancel a booking",
        description = "Cancels a booking and releases blocked seats back to available. Cannot cancel already cancelled or expired bookings."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Booking cancelled successfully"),
        @ApiResponse(responseCode = "400", description = "Booking already cancelled or expired"),
        @ApiResponse(responseCode = "404", description = "Booking not found")
    })
    @PostMapping("/{id}/cancel")
    public ResponseEntity<BookingResponseDTO> cancelBooking(
        @Parameter(description = "Booking ID", required = true)
        @PathVariable Long id) {
        log.info("Cancelling booking {}", id);

        Booking booking = bookingService.cancelBooking(id);
        BookingResponseDTO response = BookingResponseDTO.fromBooking(booking);

        return ResponseEntity.ok(response);
    }

    @Operation(
        summary = "Get booking by ID",
        description = "Retrieves complete booking details including passenger information and current status."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Booking found"),
        @ApiResponse(responseCode = "404", description = "Booking not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<BookingResponseDTO> getBooking(
        @Parameter(description = "Booking ID", required = true)
        @PathVariable Long id) {
        log.info("Fetching booking {}", id);

        Booking booking = bookingService.getBooking(id);
        BookingResponseDTO response = BookingResponseDTO.fromBooking(booking);

        return ResponseEntity.ok(response);
    }

    @Operation(
        summary = "Get booking by reference number",
        description = "Retrieves booking details using the booking reference code (e.g., BK12345678)."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Booking found"),
        @ApiResponse(responseCode = "404", description = "Booking not found")
    })
    @GetMapping("/reference/{reference}")
    public ResponseEntity<BookingResponseDTO> getBookingByReference(
        @Parameter(description = "Booking reference code", example = "BK12345678", required = true)
        @PathVariable String reference) {
        log.info("Fetching booking by reference: {}", reference);

        Booking booking = bookingService.getBookingByReference(reference);
        BookingResponseDTO response = BookingResponseDTO.fromBooking(booking);

        return ResponseEntity.ok(response);
    }

    private Passenger toPassenger(PassengerDTO dto) {
        Passenger passenger = new Passenger();
        passenger.setName(dto.getName());
        passenger.setAge(dto.getAge());
        passenger.setGender(dto.getGender());
        passenger.setIdProofNumber(dto.getIdProofNumber());
        return passenger;
    }
}
