package com.indigo.booking.dto;

import com.indigo.booking.service.FlightSearchService;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FlightSearchResultDTO {

    private Long flightId;
    private String flightNumber;
    private String origin;
    private String destination;
    private LocalTime departureTime;
    private LocalTime arrivalTime;
    private Integer durationMinutes;
    private Integer availableSeats;
    private BigDecimal price;

    private static final BigDecimal CENTS_DIVISOR = BigDecimal.valueOf(100);

    public static FlightSearchResultDTO fromSearchResult(FlightSearchService.FlightSearchResult result) {
        return new FlightSearchResultDTO(
            result.flightId(),
            result.flightNumber(),
            result.origin(),
            result.destination(),
            result.departureTime(),
            result.arrivalTime(),
            result.durationMinutes(),
            result.availableSeats(),
            BigDecimal.valueOf(result.priceCents()).divide(CENTS_DIVISOR, 2, RoundingMode.HALF_UP)
        );
    }
}
