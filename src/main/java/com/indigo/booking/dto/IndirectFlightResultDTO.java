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
public class IndirectFlightResultDTO {

    private FlightLegDTO firstLeg;
    private FlightLegDTO secondLeg;
    private Integer layoverMinutes;
    private Integer totalDurationMinutes;
    private BigDecimal totalPrice;
    private Integer availableSeats;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FlightLegDTO {
        private Long flightId;
        private String flightNumber;
        private String origin;
        private String destination;
        private LocalTime departureTime;
        private LocalTime arrivalTime;
        private Integer durationMinutes;
        private BigDecimal price;
    }

    public static IndirectFlightResultDTO fromSearchResult(FlightSearchService.IndirectFlightResult result) {
        FlightLegDTO firstLeg = new FlightLegDTO(
            result.firstFlightId(),
            result.firstFlightNumber(),
            result.firstOrigin(),
            result.firstDestination(),
            result.firstDepartureTime(),
            result.firstArrivalTime(),
            result.firstDurationMinutes(),
            centsToBigDecimal(result.firstPriceCents())
        );

        FlightLegDTO secondLeg = new FlightLegDTO(
            result.secondFlightId(),
            result.secondFlightNumber(),
            result.secondOrigin(),
            result.secondDestination(),
            result.secondDepartureTime(),
            result.secondArrivalTime(),
            result.secondDurationMinutes(),
            centsToBigDecimal(result.secondPriceCents())
        );

        return new IndirectFlightResultDTO(
            firstLeg,
            secondLeg,
            result.layoverMinutes(),
            result.totalDurationMinutes(),
            centsToBigDecimal(result.totalPriceCents()),
            result.availableSeats()
        );
    }

    private static final BigDecimal CENTS_DIVISOR = BigDecimal.valueOf(100);

    private static BigDecimal centsToBigDecimal(int cents) {
        return BigDecimal.valueOf(cents).divide(CENTS_DIVISOR, 2, RoundingMode.HALF_UP);
    }
}
