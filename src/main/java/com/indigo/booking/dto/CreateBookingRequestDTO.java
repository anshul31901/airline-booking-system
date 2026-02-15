package com.indigo.booking.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to create a new booking")
public class CreateBookingRequestDTO {

    @Schema(description = "Flight ID from search results", example = "1", required = true)
    @NotNull(message = "Flight ID is required")
    private Long flightId;

    @Schema(description = "Date of travel (must be in future)", example = "2026-03-01", required = true)
    @NotNull(message = "Flight date is required")
    @Future(message = "Flight date must be in the future")
    private LocalDate flightDate;

    @Schema(description = "Number of passengers (1-9)", example = "2", required = true)
    @NotNull(message = "Number of passengers is required")
    @Min(value = 1, message = "At least one passenger is required")
    @Max(value = 9, message = "Maximum 9 passengers per booking")
    private Integer numPassengers;

    @Schema(description = "List of passenger details", required = true)
    @NotNull(message = "Passenger list is required")
    @NotEmpty(message = "Passenger list cannot be empty")
    @Valid
    private List<PassengerDTO> passengers;

    @AssertTrue(message = "Number of passengers must match passenger list size")
    public boolean isPassengerCountValid() {
        return passengers != null && passengers.size() == numPassengers;
    }
}
