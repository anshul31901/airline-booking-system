package com.indigo.booking.controller;

import com.indigo.booking.dto.ErrorResponseDTO;
import com.indigo.booking.dto.FlightSearchResponseDTO;
import com.indigo.booking.dto.FlightSearchResultDTO;
import com.indigo.booking.dto.IndirectFlightResultDTO;
import com.indigo.booking.service.FlightSearchService;
import com.indigo.booking.service.search.SortStrategyFactory.SortType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/flights")
@RequiredArgsConstructor
@Validated
@Slf4j
@Tag(name = "Flight Search", description = "APIs for searching direct and indirect flights")
public class FlightSearchController {

    private final FlightSearchService flightSearchService;

    @Operation(
        summary = "Search for flights",
        description = """
            Searches for direct flights and optionally indirect flights (with one layover)
            using a graph-based K-shortest-path algorithm. Returns top 10 results.

            **Direct flights:** Non-stop flights from origin to destination.

            **Indirect flights:** Flights with one layover. Constraints:
            - Same-day travel only (no overnight connections)
            - Minimum 90-minute layover between legs
            - Connecting flight must depart after previous leg arrives

            **Sort options:** `price` (cheapest first, default) or `duration` (fastest first).

            **Airports:** DEL, BOM, BLR, MAA, CCU, HYD, GOI, PNQ, JAI, COK
            """
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Search completed successfully",
            content = @Content(schema = @Schema(implementation = FlightSearchResponseDTO.class))),
        @ApiResponse(responseCode = "404", description = "Airport not found",
            content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    @GetMapping("/search")
    public ResponseEntity<FlightSearchResponseDTO> searchFlights(
        @Parameter(description = "Origin airport code", example = "DEL", required = true)
        @RequestParam @Size(min = 3, max = 3) @Pattern(regexp = "[A-Z]{3}", message = "Airport code must be 3 uppercase letters") String origin,

        @Parameter(description = "Destination airport code", example = "BOM", required = true)
        @RequestParam @Size(min = 3, max = 3) @Pattern(regexp = "[A-Z]{3}", message = "Airport code must be 3 uppercase letters") String destination,

        @Parameter(description = "Travel date (YYYY-MM-DD)", example = "2026-03-01", required = true)
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate travelDate,

        @Parameter(description = "Include indirect flights with layovers", example = "true")
        @RequestParam(defaultValue = "false") boolean includeIndirect,

        @Parameter(description = "Sort by: PRICE (cheapest first) or DURATION (fastest first)")
        @RequestParam(defaultValue = "PRICE") SortType sortBy
    ) {
        log.info("Searching flights: {} -> {} on {} (includeIndirect={}, sortBy={})",
            origin, destination, travelDate, includeIndirect, sortBy);

        FlightSearchService.SearchResults results = flightSearchService.searchFlights(
            origin, destination, travelDate, includeIndirect, sortBy.name()
        );

        List<FlightSearchResultDTO> directFlights = results.directFlights().stream()
            .map(FlightSearchResultDTO::fromSearchResult)
            .collect(Collectors.toList());

        List<IndirectFlightResultDTO> indirectFlights = results.indirectFlights().stream()
            .map(IndirectFlightResultDTO::fromSearchResult)
            .collect(Collectors.toList());

        FlightSearchResponseDTO response = new FlightSearchResponseDTO(directFlights, indirectFlights);

        log.info("Search completed: {} direct, {} indirect flights found",
            directFlights.size(), indirectFlights.size());

        return ResponseEntity.ok(response);
    }
}
