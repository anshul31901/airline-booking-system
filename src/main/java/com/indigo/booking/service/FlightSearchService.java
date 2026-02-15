package com.indigo.booking.service;

import com.indigo.booking.config.CacheNames;
import com.indigo.booking.exception.ResourceNotFoundException;
import com.indigo.booking.model.*;
import com.indigo.booking.repository.AirportRepository;
import com.indigo.booking.repository.FlightInstanceRepository;
import com.indigo.booking.repository.FlightRepository;
import com.indigo.booking.repository.SeatInventoryRepository;
import com.indigo.booking.service.search.FlightGraph;
import com.indigo.booking.service.search.FlightGraph.FlightLeg;
import com.indigo.booking.service.search.FlightGraph.FlightPath;
import com.indigo.booking.service.search.FlightSortStrategy;
import com.indigo.booking.service.search.SortStrategyFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class FlightSearchService {

    private final FlightRepository flightRepository;
    private final AirportRepository airportRepository;
    private final SeatInventoryRepository seatInventoryRepository;
    private final FlightInstanceRepository flightInstanceRepository;

    @Cacheable(CacheNames.FLIGHT_GRAPH)
    public FlightGraph buildFlightGraph() {
        log.info("Building in-memory flight graph...");

        List<Flight> activeFlights = flightRepository.findAll().stream()
            .filter(Flight::isActive)
            .collect(Collectors.toList());

        for (Flight f : activeFlights) {
            f.getOriginAirport().getCode();
            f.getDestinationAirport().getCode();
        }

        Map<String, Long> codeToId = airportRepository.findAll().stream()
            .collect(Collectors.toMap(Airport::getCode, Airport::getId));

        FlightGraph graph = new FlightGraph(activeFlights, codeToId);
        log.info("Flight graph built successfully");
        return graph;
    }

    @Transactional(readOnly = true)
    public SearchResults searchFlights(
            String originCode,
            String destinationCode,
            LocalDate travelDate,
            boolean includeIndirect,
            String sortBy) {

        log.info("Searching flights: {} -> {} on {} (indirect={}, sort={})",
            originCode, destinationCode, travelDate, includeIndirect, sortBy);

        airportRepository.findByCode(originCode.toUpperCase())
            .orElseThrow(() -> new ResourceNotFoundException("Airport not found: " + originCode));
        airportRepository.findByCode(destinationCode.toUpperCase())
            .orElseThrow(() -> new ResourceNotFoundException("Airport not found: " + destinationCode));

        FlightGraph graph = buildFlightGraph();
        FlightSortStrategy strategy = SortStrategyFactory.getStrategy(sortBy);
        Map<Long, FlightInstance> instanceMap = loadFlightInstances(travelDate);
        Map<Long, SeatInventory> inventoryMap = loadSeatInventory(travelDate);

        List<FlightPath> allPaths = graph.search(
            originCode, destinationCode, travelDate,
            1, strategy, instanceMap, inventoryMap
        );

        List<FlightSearchResult> directFlights = new ArrayList<>();
        List<IndirectFlightResult> indirectFlights = new ArrayList<>();

        for (FlightPath path : allPaths) {
            if (path.isDirect()) {
                FlightLeg leg = path.getLegs().get(0);
                directFlights.add(new FlightSearchResult(
                    leg.getFlightId(),
                    leg.getFlightNumber(),
                    leg.getOrigin(),
                    leg.getDestination(),
                    leg.getDepartureTime(),
                    leg.getArrivalTime(),
                    leg.getDurationMinutes(),
                    leg.getAvailableSeats(),
                    leg.getPriceCents()
                ));
            } else if (includeIndirect && path.getLegs().size() == 2) {
                FlightLeg first = path.getLegs().get(0);
                FlightLeg second = path.getLegs().get(1);
                indirectFlights.add(new IndirectFlightResult(
                    first.getFlightId(),
                    first.getFlightNumber(),
                    first.getOrigin(),
                    first.getDestination(),
                    first.getDepartureTime(),
                    first.getArrivalTime(),
                    first.getDurationMinutes(),
                    first.getPriceCents(),
                    second.getFlightId(),
                    second.getFlightNumber(),
                    second.getOrigin(),
                    second.getDestination(),
                    second.getDepartureTime(),
                    second.getArrivalTime(),
                    second.getDurationMinutes(),
                    second.getPriceCents(),
                    path.getLayoverMinutes(),
                    path.getTotalDurationMinutes(),
                    path.getTotalPriceCents(),
                    path.getAvailableSeats()
                ));
            }
        }

        log.info("Search found {} direct, {} indirect flights", directFlights.size(), indirectFlights.size());
        return new SearchResults(directFlights, indirectFlights);
    }

    @Transactional(readOnly = true)
    public List<FlightSearchResult> searchDirectFlights(String originCode, String destinationCode, LocalDate travelDate) {
        return searchFlights(originCode, destinationCode, travelDate, false, "price").directFlights();
    }

    @Transactional(readOnly = true)
    public List<IndirectFlightResult> searchIndirectFlights(String originCode, String destinationCode, LocalDate travelDate) {
        return searchFlights(originCode, destinationCode, travelDate, true, "duration").indirectFlights();
    }

    private Map<Long, FlightInstance> loadFlightInstances(LocalDate travelDate) {
        return flightInstanceRepository
            .findByDateAndStatus(travelDate, FlightInstanceStatus.SCHEDULED)
            .stream()
            .collect(Collectors.toMap(fi -> fi.getFlight().getId(), fi -> fi, (a, b) -> a));
    }

    private Map<Long, SeatInventory> loadSeatInventory(LocalDate travelDate) {
            List<Flight> allFlights = flightRepository.findAll();
        List<Long> flightIds = allFlights.stream().map(Flight::getId).collect(Collectors.toList());
        if (flightIds.isEmpty()) return Collections.emptyMap();

        return seatInventoryRepository
            .findByFlightIdsAndDate(flightIds, travelDate)
            .stream()
            .collect(Collectors.toMap(si -> si.getFlight().getId(), si -> si, (a, b) -> a));
    }

    public record SearchResults(
        List<FlightSearchResult> directFlights,
        List<IndirectFlightResult> indirectFlights
    ) {}

    public record FlightSearchResult(
        Long flightId,
        String flightNumber,
        String origin,
        String destination,
        LocalTime departureTime,
        LocalTime arrivalTime,
        int durationMinutes,
        int availableSeats,
        int priceCents
    ) {}

    public record IndirectFlightResult(
        Long firstFlightId,
        String firstFlightNumber,
        String firstOrigin,
        String firstDestination,
        LocalTime firstDepartureTime,
        LocalTime firstArrivalTime,
        int firstDurationMinutes,
        int firstPriceCents,
        Long secondFlightId,
        String secondFlightNumber,
        String secondOrigin,
        String secondDestination,
        LocalTime secondDepartureTime,
        LocalTime secondArrivalTime,
        int secondDurationMinutes,
        int secondPriceCents,
        int layoverMinutes,
        int totalDurationMinutes,
        int totalPriceCents,
        int availableSeats
    ) {}
}
