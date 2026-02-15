package com.indigo.booking.service;

import com.indigo.booking.model.*;
import com.indigo.booking.repository.AirportRepository;
import com.indigo.booking.repository.FlightInstanceRepository;
import com.indigo.booking.repository.FlightRepository;
import com.indigo.booking.repository.FlightRouteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Initializes sample data on application startup.
 * Creates 10 airports, routes, ~100 flights, and flight instances for testing.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DataInitializationService {

    private final AirportRepository airportRepository;
    private final FlightRouteRepository flightRouteRepository;
    private final FlightRepository flightRepository;
    private final FlightInstanceRepository flightInstanceRepository;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void initializeData() {
        log.info("Initializing sample data...");

        if (airportRepository.count() > 0) {
            log.info("Data already exists, skipping initialization");
            return;
        }

        // --- 10 major Indian airports ---
        Airport del = createAirport("DEL", "Indira Gandhi International Airport", "Delhi", "India");
        Airport bom = createAirport("BOM", "Chhatrapati Shivaji Maharaj International Airport", "Mumbai", "India");
        Airport blr = createAirport("BLR", "Kempegowda International Airport", "Bangalore", "India");
        Airport maa = createAirport("MAA", "Chennai International Airport", "Chennai", "India");
        Airport ccu = createAirport("CCU", "Netaji Subhas Chandra Bose International Airport", "Kolkata", "India");
        Airport hyd = createAirport("HYD", "Rajiv Gandhi International Airport", "Hyderabad", "India");
        Airport goi = createAirport("GOI", "Goa International Airport", "Goa", "India");
        Airport pnq = createAirport("PNQ", "Pune Airport", "Pune", "India");
        Airport jai = createAirport("JAI", "Jaipur International Airport", "Jaipur", "India");
        Airport cok = createAirport("COK", "Cochin International Airport", "Kochi", "India");

        log.info("Created {} airports", airportRepository.count());

        // --- Routes (bidirectional where applicable) ---
        // DEL hub
        createRoute(del, bom, 1140, 130);
        createRoute(del, blr, 1740, 165);
        createRoute(del, maa, 1760, 170);
        createRoute(del, ccu, 1300, 140);
        createRoute(del, hyd, 1260, 135);
        createRoute(del, jai, 260, 55);
        createRoute(del, goi, 1500, 155);
        // BOM hub
        createRoute(bom, blr, 840, 95);
        createRoute(bom, maa, 1030, 110);
        createRoute(bom, hyd, 620, 80);
        createRoute(bom, goi, 440, 60);
        createRoute(bom, pnq, 150, 45);
        createRoute(bom, cok, 1070, 115);
        createRoute(bom, ccu, 1660, 155);
        // BLR hub
        createRoute(blr, maa, 290, 50);
        createRoute(blr, hyd, 500, 70);
        createRoute(blr, cok, 540, 70);
        createRoute(blr, goi, 510, 70);
        // Others
        createRoute(hyd, maa, 520, 70);
        createRoute(ccu, blr, 1560, 155);
        createRoute(maa, cok, 530, 70);
        createRoute(jai, bom, 960, 115);

        log.info("Created {} routes", flightRouteRepository.count());

        // --- ~100 flights across routes with varied timings and prices ---
        int flightCount = 0;

        // DEL -> BOM (high traffic: 6 daily flights at different times & prices)
        createFlight("6E-101", del, bom, LocalTime.of(5, 30), LocalTime.of(7, 40), 130, 180, new BigDecimal("3200.00"));
        createFlight("6E-103", del, bom, LocalTime.of(7, 0), LocalTime.of(9, 10), 130, 180, new BigDecimal("4500.00"));
        createFlight("6E-105", del, bom, LocalTime.of(10, 30), LocalTime.of(12, 40), 130, 180, new BigDecimal("5100.00"));
        createFlight("6E-107", del, bom, LocalTime.of(14, 0), LocalTime.of(16, 10), 130, 180, new BigDecimal("4800.00"));
        createFlight("6E-109", del, bom, LocalTime.of(17, 30), LocalTime.of(19, 40), 130, 180, new BigDecimal("5500.00"));
        createFlight("6E-111", del, bom, LocalTime.of(21, 0), LocalTime.of(23, 10), 130, 180, new BigDecimal("3800.00"));
        flightCount += 6;

        // DEL -> BLR (4 flights)
        createFlight("6E-201", del, blr, LocalTime.of(6, 0), LocalTime.of(8, 45), 165, 180, new BigDecimal("5500.00"));
        createFlight("6E-203", del, blr, LocalTime.of(9, 30), LocalTime.of(12, 15), 165, 180, new BigDecimal("6200.00"));
        createFlight("6E-205", del, blr, LocalTime.of(14, 0), LocalTime.of(16, 45), 165, 180, new BigDecimal("5800.00"));
        createFlight("6E-207", del, blr, LocalTime.of(20, 0), LocalTime.of(22, 45), 165, 180, new BigDecimal("4900.00"));
        flightCount += 4;

        // DEL -> MAA (3 flights)
        createFlight("6E-301", del, maa, LocalTime.of(6, 30), LocalTime.of(9, 20), 170, 180, new BigDecimal("5200.00"));
        createFlight("6E-303", del, maa, LocalTime.of(12, 0), LocalTime.of(14, 50), 170, 180, new BigDecimal("5800.00"));
        createFlight("6E-305", del, maa, LocalTime.of(18, 0), LocalTime.of(20, 50), 170, 180, new BigDecimal("4600.00"));
        flightCount += 3;

        // DEL -> CCU (3 flights)
        createFlight("6E-401", del, ccu, LocalTime.of(7, 0), LocalTime.of(9, 20), 140, 180, new BigDecimal("4800.00"));
        createFlight("6E-403", del, ccu, LocalTime.of(13, 0), LocalTime.of(15, 20), 140, 180, new BigDecimal("5200.00"));
        createFlight("6E-405", del, ccu, LocalTime.of(19, 0), LocalTime.of(21, 20), 140, 180, new BigDecimal("4200.00"));
        flightCount += 3;

        // DEL -> HYD (3 flights)
        createFlight("6E-501", del, hyd, LocalTime.of(6, 0), LocalTime.of(8, 15), 135, 180, new BigDecimal("4600.00"));
        createFlight("6E-503", del, hyd, LocalTime.of(11, 30), LocalTime.of(13, 45), 135, 180, new BigDecimal("5300.00"));
        createFlight("6E-505", del, hyd, LocalTime.of(17, 0), LocalTime.of(19, 15), 135, 180, new BigDecimal("4900.00"));
        flightCount += 3;

        // DEL -> JAI (3 flights - short hop)
        createFlight("6E-601", del, jai, LocalTime.of(7, 0), LocalTime.of(7, 55), 55, 180, new BigDecimal("2200.00"));
        createFlight("6E-603", del, jai, LocalTime.of(12, 0), LocalTime.of(12, 55), 55, 180, new BigDecimal("2500.00"));
        createFlight("6E-605", del, jai, LocalTime.of(18, 0), LocalTime.of(18, 55), 55, 180, new BigDecimal("2800.00"));
        flightCount += 3;

        // DEL -> GOI (2 flights)
        createFlight("6E-701", del, goi, LocalTime.of(8, 0), LocalTime.of(10, 35), 155, 180, new BigDecimal("5800.00"));
        createFlight("6E-703", del, goi, LocalTime.of(16, 0), LocalTime.of(18, 35), 155, 180, new BigDecimal("6200.00"));
        flightCount += 2;

        // BOM -> BLR (5 flights - busy route)
        createFlight("6E-801", bom, blr, LocalTime.of(6, 0), LocalTime.of(7, 35), 95, 180, new BigDecimal("3200.00"));
        createFlight("6E-803", bom, blr, LocalTime.of(9, 0), LocalTime.of(10, 35), 95, 180, new BigDecimal("3800.00"));
        createFlight("6E-805", bom, blr, LocalTime.of(12, 0), LocalTime.of(13, 35), 95, 180, new BigDecimal("4100.00"));
        createFlight("6E-807", bom, blr, LocalTime.of(15, 30), LocalTime.of(17, 5), 95, 180, new BigDecimal("3600.00"));
        createFlight("6E-809", bom, blr, LocalTime.of(20, 0), LocalTime.of(21, 35), 95, 180, new BigDecimal("2900.00"));
        flightCount += 5;

        // BOM -> MAA (3 flights)
        createFlight("6E-901", bom, maa, LocalTime.of(7, 0), LocalTime.of(8, 50), 110, 180, new BigDecimal("3400.00"));
        createFlight("6E-903", bom, maa, LocalTime.of(13, 0), LocalTime.of(14, 50), 110, 180, new BigDecimal("3900.00"));
        createFlight("6E-905", bom, maa, LocalTime.of(19, 0), LocalTime.of(20, 50), 110, 180, new BigDecimal("3100.00"));
        flightCount += 3;

        // BOM -> HYD (4 flights)
        createFlight("6E-1001", bom, hyd, LocalTime.of(6, 30), LocalTime.of(7, 50), 80, 180, new BigDecimal("2800.00"));
        createFlight("6E-1003", bom, hyd, LocalTime.of(10, 0), LocalTime.of(11, 20), 80, 180, new BigDecimal("3200.00"));
        createFlight("6E-1005", bom, hyd, LocalTime.of(14, 30), LocalTime.of(15, 50), 80, 180, new BigDecimal("3500.00"));
        createFlight("6E-1007", bom, hyd, LocalTime.of(19, 0), LocalTime.of(20, 20), 80, 180, new BigDecimal("2600.00"));
        flightCount += 4;

        // BOM -> GOI (3 flights - short hop)
        createFlight("6E-1101", bom, goi, LocalTime.of(8, 0), LocalTime.of(9, 0), 60, 180, new BigDecimal("2200.00"));
        createFlight("6E-1103", bom, goi, LocalTime.of(14, 0), LocalTime.of(15, 0), 60, 180, new BigDecimal("2800.00"));
        createFlight("6E-1105", bom, goi, LocalTime.of(19, 30), LocalTime.of(20, 30), 60, 180, new BigDecimal("2400.00"));
        flightCount += 3;

        // BOM -> PNQ (3 flights - very short)
        createFlight("6E-1201", bom, pnq, LocalTime.of(7, 0), LocalTime.of(7, 45), 45, 180, new BigDecimal("1800.00"));
        createFlight("6E-1203", bom, pnq, LocalTime.of(13, 0), LocalTime.of(13, 45), 45, 180, new BigDecimal("2100.00"));
        createFlight("6E-1205", bom, pnq, LocalTime.of(18, 0), LocalTime.of(18, 45), 45, 180, new BigDecimal("1900.00"));
        flightCount += 3;

        // BOM -> COK (3 flights)
        createFlight("6E-1301", bom, cok, LocalTime.of(6, 30), LocalTime.of(8, 25), 115, 180, new BigDecimal("3600.00"));
        createFlight("6E-1303", bom, cok, LocalTime.of(12, 0), LocalTime.of(13, 55), 115, 180, new BigDecimal("4200.00"));
        createFlight("6E-1305", bom, cok, LocalTime.of(17, 30), LocalTime.of(19, 25), 115, 180, new BigDecimal("3800.00"));
        flightCount += 3;

        // BOM -> CCU (2 flights)
        createFlight("6E-1401", bom, ccu, LocalTime.of(8, 0), LocalTime.of(10, 35), 155, 180, new BigDecimal("5400.00"));
        createFlight("6E-1403", bom, ccu, LocalTime.of(16, 0), LocalTime.of(18, 35), 155, 180, new BigDecimal("5000.00"));
        flightCount += 2;

        // BLR -> MAA (4 flights - short shuttle)
        createFlight("6E-1501", blr, maa, LocalTime.of(6, 30), LocalTime.of(7, 20), 50, 180, new BigDecimal("2000.00"));
        createFlight("6E-1503", blr, maa, LocalTime.of(10, 0), LocalTime.of(10, 50), 50, 180, new BigDecimal("2400.00"));
        createFlight("6E-1505", blr, maa, LocalTime.of(14, 0), LocalTime.of(14, 50), 50, 180, new BigDecimal("2600.00"));
        createFlight("6E-1507", blr, maa, LocalTime.of(19, 0), LocalTime.of(19, 50), 50, 180, new BigDecimal("2100.00"));
        flightCount += 4;

        // BLR -> HYD (3 flights)
        createFlight("6E-1601", blr, hyd, LocalTime.of(7, 0), LocalTime.of(8, 10), 70, 180, new BigDecimal("2600.00"));
        createFlight("6E-1603", blr, hyd, LocalTime.of(12, 30), LocalTime.of(13, 40), 70, 180, new BigDecimal("3000.00"));
        createFlight("6E-1605", blr, hyd, LocalTime.of(18, 0), LocalTime.of(19, 10), 70, 180, new BigDecimal("2800.00"));
        flightCount += 3;

        // BLR -> COK (3 flights)
        createFlight("6E-1701", blr, cok, LocalTime.of(8, 0), LocalTime.of(9, 10), 70, 180, new BigDecimal("2400.00"));
        createFlight("6E-1703", blr, cok, LocalTime.of(13, 0), LocalTime.of(14, 10), 70, 180, new BigDecimal("2900.00"));
        createFlight("6E-1705", blr, cok, LocalTime.of(18, 30), LocalTime.of(19, 40), 70, 180, new BigDecimal("2600.00"));
        flightCount += 3;

        // BLR -> GOI (2 flights)
        createFlight("6E-1801", blr, goi, LocalTime.of(9, 0), LocalTime.of(10, 10), 70, 180, new BigDecimal("2800.00"));
        createFlight("6E-1803", blr, goi, LocalTime.of(16, 0), LocalTime.of(17, 10), 70, 180, new BigDecimal("3200.00"));
        flightCount += 2;

        // HYD -> MAA (3 flights)
        createFlight("6E-1901", hyd, maa, LocalTime.of(7, 30), LocalTime.of(8, 40), 70, 180, new BigDecimal("2200.00"));
        createFlight("6E-1903", hyd, maa, LocalTime.of(12, 0), LocalTime.of(13, 10), 70, 180, new BigDecimal("2600.00"));
        createFlight("6E-1905", hyd, maa, LocalTime.of(17, 30), LocalTime.of(18, 40), 70, 180, new BigDecimal("2400.00"));
        flightCount += 3;

        // HYD -> BLR (3 flights - reverse)
        createFlight("6E-2001", hyd, blr, LocalTime.of(6, 30), LocalTime.of(7, 40), 70, 180, new BigDecimal("2500.00"));
        createFlight("6E-2003", hyd, blr, LocalTime.of(11, 0), LocalTime.of(12, 10), 70, 180, new BigDecimal("2900.00"));
        createFlight("6E-2005", hyd, blr, LocalTime.of(17, 0), LocalTime.of(18, 10), 70, 180, new BigDecimal("2700.00"));
        flightCount += 3;

        // CCU -> BLR (2 flights)
        createFlight("6E-2101", ccu, blr, LocalTime.of(7, 0), LocalTime.of(9, 35), 155, 180, new BigDecimal("5600.00"));
        createFlight("6E-2103", ccu, blr, LocalTime.of(15, 0), LocalTime.of(17, 35), 155, 180, new BigDecimal("5200.00"));
        flightCount += 2;

        // MAA -> COK (3 flights)
        createFlight("6E-2201", maa, cok, LocalTime.of(8, 0), LocalTime.of(9, 10), 70, 180, new BigDecimal("2100.00"));
        createFlight("6E-2203", maa, cok, LocalTime.of(13, 30), LocalTime.of(14, 40), 70, 180, new BigDecimal("2500.00"));
        createFlight("6E-2205", maa, cok, LocalTime.of(19, 0), LocalTime.of(20, 10), 70, 180, new BigDecimal("2300.00"));
        flightCount += 3;

        // JAI -> BOM (3 flights)
        createFlight("6E-2301", jai, bom, LocalTime.of(6, 0), LocalTime.of(7, 55), 115, 180, new BigDecimal("3200.00"));
        createFlight("6E-2303", jai, bom, LocalTime.of(11, 0), LocalTime.of(12, 55), 115, 180, new BigDecimal("3600.00"));
        createFlight("6E-2305", jai, bom, LocalTime.of(17, 0), LocalTime.of(18, 55), 115, 180, new BigDecimal("3400.00"));
        flightCount += 3;

        // Return flights on key routes for bidirectional coverage
        // BOM -> DEL (4 flights)
        createFlight("6E-3001", bom, del, LocalTime.of(6, 0), LocalTime.of(8, 10), 130, 180, new BigDecimal("3400.00"));
        createFlight("6E-3003", bom, del, LocalTime.of(10, 0), LocalTime.of(12, 10), 130, 180, new BigDecimal("4800.00"));
        createFlight("6E-3005", bom, del, LocalTime.of(15, 0), LocalTime.of(17, 10), 130, 180, new BigDecimal("5200.00"));
        createFlight("6E-3007", bom, del, LocalTime.of(20, 30), LocalTime.of(22, 40), 130, 180, new BigDecimal("3900.00"));
        flightCount += 4;

        // BLR -> DEL (3 flights)
        createFlight("6E-3101", blr, del, LocalTime.of(5, 30), LocalTime.of(8, 15), 165, 180, new BigDecimal("5200.00"));
        createFlight("6E-3103", blr, del, LocalTime.of(11, 0), LocalTime.of(13, 45), 165, 180, new BigDecimal("5800.00"));
        createFlight("6E-3105", blr, del, LocalTime.of(18, 0), LocalTime.of(20, 45), 165, 180, new BigDecimal("4800.00"));
        flightCount += 3;

        // BLR -> BOM (3 flights)
        createFlight("6E-3201", blr, bom, LocalTime.of(6, 30), LocalTime.of(8, 5), 95, 180, new BigDecimal("3000.00"));
        createFlight("6E-3203", blr, bom, LocalTime.of(12, 0), LocalTime.of(13, 35), 95, 180, new BigDecimal("3600.00"));
        createFlight("6E-3205", blr, bom, LocalTime.of(19, 0), LocalTime.of(20, 35), 95, 180, new BigDecimal("3200.00"));
        flightCount += 3;

        // MAA -> BLR (3 flights)
        createFlight("6E-3301", maa, blr, LocalTime.of(7, 0), LocalTime.of(7, 50), 50, 180, new BigDecimal("1900.00"));
        createFlight("6E-3303", maa, blr, LocalTime.of(12, 0), LocalTime.of(12, 50), 50, 180, new BigDecimal("2300.00"));
        createFlight("6E-3305", maa, blr, LocalTime.of(18, 0), LocalTime.of(18, 50), 50, 180, new BigDecimal("2100.00"));
        flightCount += 3;

        // MAA -> DEL (2 flights)
        createFlight("6E-3401", maa, del, LocalTime.of(6, 0), LocalTime.of(8, 50), 170, 180, new BigDecimal("5000.00"));
        createFlight("6E-3403", maa, del, LocalTime.of(17, 0), LocalTime.of(19, 50), 170, 180, new BigDecimal("5400.00"));
        flightCount += 2;

        // HYD -> BOM (3 flights)
        createFlight("6E-3501", hyd, bom, LocalTime.of(7, 0), LocalTime.of(8, 20), 80, 180, new BigDecimal("2700.00"));
        createFlight("6E-3503", hyd, bom, LocalTime.of(13, 0), LocalTime.of(14, 20), 80, 180, new BigDecimal("3100.00"));
        createFlight("6E-3505", hyd, bom, LocalTime.of(18, 30), LocalTime.of(19, 50), 80, 180, new BigDecimal("2900.00"));
        flightCount += 3;

        // HYD -> DEL (2 flights)
        createFlight("6E-3601", hyd, del, LocalTime.of(6, 0), LocalTime.of(8, 15), 135, 180, new BigDecimal("4500.00"));
        createFlight("6E-3603", hyd, del, LocalTime.of(16, 0), LocalTime.of(18, 15), 135, 180, new BigDecimal("5000.00"));
        flightCount += 2;

        // COK -> BLR (2 flights)
        createFlight("6E-3701", cok, blr, LocalTime.of(8, 0), LocalTime.of(9, 10), 70, 180, new BigDecimal("2500.00"));
        createFlight("6E-3703", cok, blr, LocalTime.of(16, 0), LocalTime.of(17, 10), 70, 180, new BigDecimal("2800.00"));
        flightCount += 2;

        // COK -> BOM (2 flights)
        createFlight("6E-3801", cok, bom, LocalTime.of(7, 0), LocalTime.of(8, 55), 115, 180, new BigDecimal("3500.00"));
        createFlight("6E-3803", cok, bom, LocalTime.of(15, 0), LocalTime.of(16, 55), 115, 180, new BigDecimal("3900.00"));
        flightCount += 2;

        // GOI -> BOM (2 flights)
        createFlight("6E-3901", goi, bom, LocalTime.of(7, 30), LocalTime.of(8, 30), 60, 180, new BigDecimal("2300.00"));
        createFlight("6E-3903", goi, bom, LocalTime.of(17, 0), LocalTime.of(18, 0), 60, 180, new BigDecimal("2700.00"));
        flightCount += 2;

        // PNQ -> BOM (2 flights)
        createFlight("6E-4001", pnq, bom, LocalTime.of(8, 0), LocalTime.of(8, 45), 45, 180, new BigDecimal("1700.00"));
        createFlight("6E-4003", pnq, bom, LocalTime.of(17, 0), LocalTime.of(17, 45), 45, 180, new BigDecimal("2000.00"));
        flightCount += 2;

        // CCU -> DEL (2 flights)
        createFlight("6E-4101", ccu, del, LocalTime.of(6, 30), LocalTime.of(8, 50), 140, 180, new BigDecimal("4600.00"));
        createFlight("6E-4103", ccu, del, LocalTime.of(16, 0), LocalTime.of(18, 20), 140, 180, new BigDecimal("5000.00"));
        flightCount += 2;

        // CCU -> BOM (2 flights)
        createFlight("6E-4201", ccu, bom, LocalTime.of(7, 0), LocalTime.of(9, 35), 155, 180, new BigDecimal("5100.00"));
        createFlight("6E-4203", ccu, bom, LocalTime.of(14, 0), LocalTime.of(16, 35), 155, 180, new BigDecimal("4800.00"));
        flightCount += 2;

        log.info("Created {} flights", flightCount);

        // Create flight instances for next 7 days
        LocalDate today = LocalDate.now();
        int instanceCount = 0;
        for (Flight flight : flightRepository.findAll()) {
            for (int i = 1; i <= 7; i++) {
                LocalDate date = today.plusDays(i);
                createFlightInstance(flight, date, flight.getBasePrice());
                instanceCount++;
            }
        }

        log.info("Created {} flight instances", instanceCount);
        log.info("Sample data initialization completed: {} airports, {} routes, {} flights, {} instances",
            airportRepository.count(), flightRouteRepository.count(), flightCount, instanceCount);
    }

    private Airport createAirport(String code, String name, String city, String country) {
        Airport airport = new Airport();
        airport.setCode(code);
        airport.setName(name);
        airport.setCity(city);
        airport.setCountry(country);
        return airportRepository.save(airport);
    }

    private FlightRoute createRoute(Airport origin, Airport destination, int distanceKm, int durationMinutes) {
        FlightRoute route = new FlightRoute();
        route.setOriginAirport(origin);
        route.setDestinationAirport(destination);
        route.setDistanceKm(distanceKm);
        route.setEstimatedDurationMinutes(durationMinutes);
        route.setActive(true);
        return flightRouteRepository.save(route);
    }

    private Flight createFlight(String flightNumber, Airport origin, Airport destination,
                                LocalTime departureTime, LocalTime arrivalTime,
                                int durationMinutes, int totalSeats, BigDecimal basePrice) {
        Flight flight = new Flight();
        flight.setFlightNumber(flightNumber);
        flight.setOriginAirport(origin);
        flight.setDestinationAirport(destination);
        flight.setDepartureTime(departureTime);
        flight.setArrivalTime(arrivalTime);
        flight.setDurationMinutes(durationMinutes);
        flight.setTotalSeats(totalSeats);
        flight.setBasePrice(basePrice);
        flight.setActive(true);
        return flightRepository.save(flight);
    }

    private FlightInstance createFlightInstance(Flight flight, LocalDate date, BigDecimal price) {
        FlightInstance instance = new FlightInstance();
        instance.setFlight(flight);
        instance.setFlightDate(date);
        instance.setStatus(FlightInstanceStatus.SCHEDULED);
        instance.setPrice(price);
        instance.setDepartureDateTime(LocalDateTime.of(date, flight.getDepartureTime()));
        instance.setArrivalDateTime(LocalDateTime.of(date, flight.getArrivalTime()));
        return flightInstanceRepository.save(instance);
    }
}
