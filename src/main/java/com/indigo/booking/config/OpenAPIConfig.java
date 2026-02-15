package com.indigo.booking.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI/Swagger configuration for interactive API documentation.
 * Access Swagger UI at: http://localhost:8080/swagger-ui.html
 */
@Configuration
public class OpenAPIConfig {

    @Bean
    public OpenAPI airlineBookingOpenAPI() {
        Server localServer = new Server()
            .url("http://localhost:8080")
            .description("Local Development Server");

        Contact contact = new Contact()
            .name("InDiGo Airlines")
            .email("support@goindigo.in")
            .url("https://www.goindigo.in");

        License license = new License()
            .name("MIT License")
            .url("https://opensource.org/licenses/MIT");

        Info info = new Info()
            .title("InDiGo Airline Booking System API")
            .version("1.0.0")
            .description("""
                Domestic flight booking system for same-day travel with graph-based search and seat inventory management.

                **Tech:** Java 17, Spring Boot 3.2, Spring Data JPA, PostgreSQL (prod) / H2 (dev), Spring Cache, SpringDoc OpenAPI

                ---

                **Search Algorithm — Graph-based K-shortest paths (modified Dijkstra)**
                - Adjacency list built from flight schedule, cached in memory
                - Priority queue search with prunable cost tracking per city
                - Returns top 10 results sorted by price (cheapest) or duration (fastest)
                - Strategy pattern for sort, Factory pattern for resolution

                **Search Constraints:**
                - Same-day travel only — connecting flight must depart after previous leg arrives
                - Minimum 90-minute layover for connections
                - Max 1 layover (2 legs)

                **Booking Flow:** Search → Create (PENDING, 10 min expiry) → Confirm (CONFIRMED) or Cancel/Expire
                - Pessimistic locking (SELECT FOR UPDATE) on seat inventory — no double-booking
                - Seat invariant enforced: available + blocked + booked = total
                - Scheduled cleanup releases expired bookings every 2 minutes

                **Flight Network:**
                - 10 airports: DEL, BOM, BLR, MAA, CCU, HYD, GOI, PNQ, JAI, COK
                - 105 flights across 22 routes, 180 seats each
                - Multiple daily flights per route with varied prices
                - Flight instances for next 7 days — use any future date within range
                """)
            .contact(contact)
            .license(license);

        return new OpenAPI()
            .info(info)
            .servers(List.of(localServer));
    }
}
