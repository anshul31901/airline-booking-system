package com.indigo.booking.service.search;

import com.indigo.booking.model.Airport;
import com.indigo.booking.model.Flight;
import com.indigo.booking.model.FlightInstance;
import com.indigo.booking.model.SeatInventory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class FlightGraphTest {

    private FlightGraph graph;
    private Map<String, Long> airportCodeToId;

    @BeforeEach
    void setUp() {
        // Build airports
        Airport del = buildAirport(1L, "DEL");
        Airport bom = buildAirport(2L, "BOM");
        Airport blr = buildAirport(3L, "BLR");

        airportCodeToId = Map.of("DEL", 1L, "BOM", 2L, "BLR", 3L);

        // Build flights: DEL->BOM, BOM->BLR, DEL->BLR
        List<Flight> flights = List.of(
            buildFlight(1L, "6E-2001", del, bom, LocalTime.of(6, 0), LocalTime.of(8, 0), 120, new BigDecimal("4500")),
            buildFlight(2L, "6E-2002", bom, blr, LocalTime.of(10, 0), LocalTime.of(11, 30), 90, new BigDecimal("3500")),
            buildFlight(3L, "6E-2003", del, blr, LocalTime.of(14, 0), LocalTime.of(16, 30), 150, new BigDecimal("5500"))
        );

        graph = new FlightGraph(flights, airportCodeToId);
    }

    @Test
    void searchDirectFlight_shouldReturnDirectPath() {
        List<FlightGraph.FlightPath> results = graph.search(
            "DEL", "BOM", LocalDate.now().plusDays(1), 1,
            new PriceSortStrategy(), Collections.emptyMap(), Collections.emptyMap()
        );

        assertEquals(1, results.size());
        assertTrue(results.get(0).isDirect());
        assertEquals("DEL", results.get(0).getLegs().get(0).getOrigin());
        assertEquals("BOM", results.get(0).getLegs().get(0).getDestination());
    }

    @Test
    void searchIndirectFlight_shouldReturnConnectingPath() {
        List<FlightGraph.FlightPath> results = graph.search(
            "DEL", "BLR", LocalDate.now().plusDays(1), 1,
            new PriceSortStrategy(), Collections.emptyMap(), Collections.emptyMap()
        );

        // Should find both direct (DEL->BLR) and indirect (DEL->BOM->BLR)
        assertEquals(2, results.size());

        // Sorted by price: indirect (4500+3500=8000) should come after direct (5500)
        FlightGraph.FlightPath cheapest = results.get(0);
        assertTrue(cheapest.isDirect());
        assertEquals(550000, cheapest.getTotalPriceCents()); // 5500 * 100

        FlightGraph.FlightPath indirect = results.get(1);
        assertFalse(indirect.isDirect());
        assertEquals(2, indirect.getLegs().size());
        assertEquals(800000, indirect.getTotalPriceCents()); // 8000 * 100
    }

    @Test
    void searchByDuration_shouldSortByFastest() {
        List<FlightGraph.FlightPath> results = graph.search(
            "DEL", "BLR", LocalDate.now().plusDays(1), 1,
            new DurationSortStrategy(), Collections.emptyMap(), Collections.emptyMap()
        );

        assertEquals(2, results.size());
        // Direct DEL->BLR is 150 min, indirect DEL->BOM->BLR is 120+90+120(layover)=330 min
        assertTrue(results.get(0).isDirect());
        assertEquals(150, results.get(0).getTotalDurationMinutes());
    }

    @Test
    void searchNoRoute_shouldReturnEmpty() {
        List<FlightGraph.FlightPath> results = graph.search(
            "BLR", "DEL", LocalDate.now().plusDays(1), 1,
            new PriceSortStrategy(), Collections.emptyMap(), Collections.emptyMap()
        );

        assertTrue(results.isEmpty());
    }

    @Test
    void sameDayTravel_shouldRejectOvernightConnection() {
        // Build flights where connecting flight departs BEFORE first leg arrives
        Airport del = buildAirport(1L, "DEL");
        Airport bom = buildAirport(2L, "BOM");
        Airport blr = buildAirport(3L, "BLR");

        List<Flight> flights = List.of(
            buildFlight(1L, "6E-1", del, bom, LocalTime.of(20, 0), LocalTime.of(22, 0), 120, new BigDecimal("4000")),
            buildFlight(2L, "6E-2", bom, blr, LocalTime.of(6, 0), LocalTime.of(7, 30), 90, new BigDecimal("3000"))
        );

        FlightGraph nightGraph = new FlightGraph(flights, airportCodeToId);
        List<FlightGraph.FlightPath> results = nightGraph.search(
            "DEL", "BLR", LocalDate.now().plusDays(1), 1,
            new PriceSortStrategy(), Collections.emptyMap(), Collections.emptyMap()
        );

        // Should NOT find any route because BOM->BLR departs at 06:00 before DEL->BOM arrives at 22:00
        assertTrue(results.isEmpty());
    }

    @Test
    void searchShouldRespectMinLayover() {
        Airport del = buildAirport(1L, "DEL");
        Airport bom = buildAirport(2L, "BOM");
        Airport blr = buildAirport(3L, "BLR");

        // Layover is only 30 min (08:00 -> 08:30), below 90 min minimum
        List<Flight> flights = List.of(
            buildFlight(1L, "6E-1", del, bom, LocalTime.of(6, 0), LocalTime.of(8, 0), 120, new BigDecimal("4000")),
            buildFlight(2L, "6E-2", bom, blr, LocalTime.of(8, 30), LocalTime.of(10, 0), 90, new BigDecimal("3000"))
        );

        FlightGraph shortLayoverGraph = new FlightGraph(flights, airportCodeToId);
        List<FlightGraph.FlightPath> results = shortLayoverGraph.search(
            "DEL", "BLR", LocalDate.now().plusDays(1), 1,
            new PriceSortStrategy(), Collections.emptyMap(), Collections.emptyMap()
        );

        assertTrue(results.isEmpty());
    }

    // --- Helpers ---

    private Airport buildAirport(Long id, String code) {
        Airport a = new Airport();
        a.setId(id);
        a.setCode(code);
        a.setName(code + " Airport");
        a.setCity(code);
        a.setCountry("India");
        return a;
    }

    private Flight buildFlight(Long id, String number, Airport origin, Airport destination,
                               LocalTime dep, LocalTime arr, int duration, BigDecimal price) {
        Flight f = new Flight();
        f.setId(id);
        f.setFlightNumber(number);
        f.setOriginAirport(origin);
        f.setDestinationAirport(destination);
        f.setDepartureTime(dep);
        f.setArrivalTime(arr);
        f.setDurationMinutes(duration);
        f.setTotalSeats(180);
        f.setBasePrice(price);
        f.setActive(true);
        return f;
    }
}
