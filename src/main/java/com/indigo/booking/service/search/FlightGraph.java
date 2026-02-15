package com.indigo.booking.service.search;

import com.indigo.booking.model.Flight;
import com.indigo.booking.model.FlightInstance;
import com.indigo.booking.model.SeatInventory;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public class FlightGraph {

    private static final int MAX_HOPS = 2;
    private static final int K = 10;
    private static final int MIN_LAYOVER_MINUTES = 90;
    private static final int DEFAULT_SEAT_CAPACITY = 180;
    private static final int CENTS_PER_UNIT = 100;
    private static final int SECONDS_PER_MINUTE = 60;

    private final Map<Long, List<FlightEdge>> adjacencyList;
    private final Map<String, Long> airportCodeToId;

    public FlightGraph(List<Flight> flights, Map<String, Long> airportCodeToId) {
        this.airportCodeToId = airportCodeToId;
        this.adjacencyList = new HashMap<>();

        for (Flight flight : flights) {
            Long originId = flight.getOriginAirport().getId();
            adjacencyList
                .computeIfAbsent(originId, k -> new ArrayList<>())
                .add(new FlightEdge(
                    flight.getId(),
                    flight.getFlightNumber(),
                    originId,
                    flight.getOriginAirport().getCode(),
                    flight.getDestinationAirport().getId(),
                    flight.getDestinationAirport().getCode(),
                    flight.getDepartureTime(),
                    flight.getArrivalTime(),
                    flight.getDurationMinutes(),
                    flight.getBasePrice() != null
                        ? flight.getBasePrice().multiply(BigDecimal.valueOf(CENTS_PER_UNIT)).intValue()
                        : 0
                ));
        }

        log.info("Flight graph built: {} airports, {} edges",
            adjacencyList.size(),
            adjacencyList.values().stream().mapToInt(List::size).sum());
    }

    public List<FlightPath> search(
            String originCode,
            String destinationCode,
            LocalDate travelDate,
            int numPassengers,
            FlightSortStrategy sortStrategy,
            Map<Long, FlightInstance> instanceMap,
            Map<Long, SeatInventory> inventoryMap) {

        Long originId = airportCodeToId.get(originCode.toUpperCase());
        Long destinationId = airportCodeToId.get(destinationCode.toUpperCase());

        if (originId == null || destinationId == null) {
            return Collections.emptyList();
        }

        PriorityQueue<SearchState> pq = new PriorityQueue<>(
            Comparator.comparingInt(s -> sortStrategy.getCost(s.path))
        );
        Map<Long, List<Integer>> bestCosts = new HashMap<>();
        pq.offer(new SearchState(originId, new FlightPath(), null));

        List<FlightPath> results = new ArrayList<>();

        while (!pq.isEmpty() && results.size() < K) {
            SearchState current = pq.poll();

            if (current.currentAirportId.equals(destinationId)) {
                results.add(current.path);
                continue;
            }

            if (current.path.getLegs().size() >= MAX_HOPS) {
                continue;
            }

            int currentCost = sortStrategy.getCost(current.path);
            List<Integer> cityCosts = bestCosts.computeIfAbsent(current.currentAirportId, k -> new ArrayList<>());
            if (cityCosts.size() >= K && currentCost >= cityCosts.get(K - 1)) {
                continue;
            }

            List<FlightEdge> edges = adjacencyList.getOrDefault(current.currentAirportId, Collections.emptyList());

            for (FlightEdge edge : edges) {
                FlightInstance instance = instanceMap.get(edge.flightId);
                if (instance != null && !instance.isOperating()) {
                    continue;
                }

                SeatInventory inventory = inventoryMap.get(edge.flightId);
                int available = inventory != null
                    ? inventory.getAvailableSeats()
                    : DEFAULT_SEAT_CAPACITY;
                if (available < numPassengers) {
                    continue;
                }

                if (current.lastArrivalTime != null) {
                    if (!edge.departureTime.isAfter(current.lastArrivalTime)) {
                        continue;
                    }
                    int layoverMinutes = (edge.departureTime.toSecondOfDay()
                        - current.lastArrivalTime.toSecondOfDay()) / SECONDS_PER_MINUTE;
                    if (layoverMinutes < MIN_LAYOVER_MINUTES) {
                        continue;
                    }
                }

                if (current.path.visitsAirport(edge.destinationAirportCode)) {
                    continue;
                }

                int edgePrice = instance != null
                    ? instance.getPrice().multiply(BigDecimal.valueOf(CENTS_PER_UNIT)).intValue()
                    : edge.priceCents;

                FlightPath newPath = current.path.extend(new FlightLeg(
                    edge.flightId,
                    edge.flightNumber,
                    edge.originAirportCode,
                    edge.destinationAirportCode,
                    edge.departureTime,
                    edge.arrivalTime,
                    edge.durationMinutes,
                    edgePrice,
                    available
                ));

                if (current.lastArrivalTime != null) {
                    int layover = (edge.departureTime.toSecondOfDay()
                        - current.lastArrivalTime.toSecondOfDay()) / SECONDS_PER_MINUTE;
                    newPath.setLayoverMinutes(current.path.getLayoverMinutes() + layover);
                }

                int newCost = sortStrategy.getCost(newPath);
                List<Integer> nextCityCosts = bestCosts.computeIfAbsent(
                    edge.destinationAirportId, k -> new ArrayList<>());

                if (nextCityCosts.size() < K || newCost < nextCityCosts.get(nextCityCosts.size() - 1)) {
                    pq.offer(new SearchState(edge.destinationAirportId, newPath, edge.arrivalTime));

                    nextCityCosts.add(newCost);
                    nextCityCosts.sort(Integer::compareTo);
                    if (nextCityCosts.size() > K) {
                        nextCityCosts.subList(K, nextCityCosts.size()).clear();
                    }
                }
            }
        }

        results.sort(sortStrategy.getComparator());
        return results;
    }

    @Data
    @AllArgsConstructor
    public static class FlightEdge {
        private final Long flightId;
        private final String flightNumber;
        private final Long originAirportId;
        private final String originAirportCode;
        private final Long destinationAirportId;
        private final String destinationAirportCode;
        private final LocalTime departureTime;
        private final LocalTime arrivalTime;
        private final int durationMinutes;
        private final int priceCents; // base price in cents for integer comparison
    }

    @Data
    public static class FlightPath {
        private final List<FlightLeg> legs;
        private int layoverMinutes = 0;

        public FlightPath() {
            this.legs = new ArrayList<>();
        }

        private FlightPath(List<FlightLeg> legs, int layoverMinutes) {
            this.legs = legs;
            this.layoverMinutes = layoverMinutes;
        }

        public FlightPath extend(FlightLeg leg) {
            List<FlightLeg> newLegs = new ArrayList<>(this.legs);
            newLegs.add(leg);
            return new FlightPath(newLegs, this.layoverMinutes);
        }

        public int getTotalDurationMinutes() {
            return legs.stream().mapToInt(FlightLeg::getDurationMinutes).sum() + layoverMinutes;
        }

        public int getTotalPriceCents() {
            return legs.stream().mapToInt(FlightLeg::getPriceCents).sum();
        }

        public int getAvailableSeats() {
            return legs.stream().mapToInt(FlightLeg::getAvailableSeats).min().orElse(0);
        }

        public int getStops() {
            return Math.max(0, legs.size() - 1);
        }

        public boolean isDirect() {
            return legs.size() == 1;
        }

        public boolean visitsAirport(String airportCode) {
            return legs.stream().anyMatch(leg ->
                leg.getOrigin().equals(airportCode) || leg.getDestination().equals(airportCode));
        }
    }

    @Data
    @AllArgsConstructor
    public static class FlightLeg {
        private final Long flightId;
        private final String flightNumber;
        private final String origin;
        private final String destination;
        private final LocalTime departureTime;
        private final LocalTime arrivalTime;
        private final int durationMinutes;
        private final int priceCents;
        private final int availableSeats;
    }

    @Data
    @AllArgsConstructor
    private static class SearchState {
        private final Long currentAirportId;
        private final FlightPath path;
        private final LocalTime lastArrivalTime;
    }
}
