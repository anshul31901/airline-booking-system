package com.indigo.booking.dto;

import com.indigo.booking.model.Booking;
import com.indigo.booking.model.BookingStatus;
import com.indigo.booking.model.Gender;
import com.indigo.booking.model.Passenger;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookingResponseDTO {

    private Long id;
    private String bookingReference;
    private Long flightId;
    private String flightNumber;
    private String origin;
    private String destination;
    private LocalDate flightDate;
    private Integer numPassengers;
    private BookingStatus status;
    private LocalDateTime expiresAt;
    private List<PassengerInfo> passengers;
    private LocalDateTime createdAt;

    public static BookingResponseDTO fromBooking(Booking booking) {
        BookingResponseDTO dto = new BookingResponseDTO();
        dto.setId(booking.getId());
        dto.setBookingReference(booking.getBookingReference());
        dto.setFlightId(booking.getFlight().getId());
        dto.setFlightNumber(booking.getFlight().getFlightNumber());
        dto.setOrigin(booking.getFlight().getOriginAirport().getCode());
        dto.setDestination(booking.getFlight().getDestinationAirport().getCode());
        dto.setFlightDate(booking.getFlightDate());
        dto.setNumPassengers(booking.getNumPassengers());
        dto.setStatus(booking.getStatus());
        dto.setExpiresAt(booking.getExpiresAt());
        dto.setCreatedAt(booking.getCreatedAt());

        dto.setPassengers(booking.getPassengers().stream()
            .map(PassengerInfo::fromPassenger)
            .collect(Collectors.toList()));

        return dto;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PassengerInfo {
        private String name;
        private Integer age;
        private Gender gender;
        private String idProofNumber;

        public static PassengerInfo fromPassenger(Passenger passenger) {
            return new PassengerInfo(
                passenger.getName(),
                passenger.getAge(),
                passenger.getGender(),
                passenger.getIdProofNumber()
            );
        }
    }
}
