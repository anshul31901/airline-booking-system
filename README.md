# IndiGo Airline Booking System

Domestic flight booking system for same-day travel with **graph-based search**, **seat inventory management**, and **concurrent booking protection**.

> Built with Java 17, Spring Boot 3.2, and a modified K-shortest-paths algorithm for multi-leg flight search.

[![Deploy to Render](https://render.com/images/deploy-to-render-button.svg)](https://render.com/deploy?repo=https://github.com/anshul31901/airline-booking-system)

---

## Table of Contents

- [Quick Start](#quick-start)
- [Tech Stack](#tech-stack)
- [System Architecture](#system-architecture)
- [Flight Search Algorithm](#flight-search-algorithm)
- [Booking Lifecycle](#booking-lifecycle)
- [Seat Inventory Management](#seat-inventory-management)
- [API Reference](#api-reference)
- [Entity Relationship Diagram](#entity-relationship-diagram)
- [Design Patterns](#design-patterns)
- [Error Handling](#error-handling)
- [Flight Network](#flight-network)
- [Test Suite](#test-suite)
- [Configuration](#configuration)
- [Production HLD (10M DAU)](#production-hld--scaling-to-10m-dau)

---

## Quick Start

### Prerequisites

- **Java 17+** (verify: `java -version`)
- **Maven 3.9+** (or use the included Maven wrapper)

### 1. Clone & Run (Dev Mode — no database setup needed)

```bash
git clone <repo-url>
cd airline-booking-system

# Linux/Mac
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

# Windows
mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=dev
```

The app starts on **http://localhost:8080** with an **H2 in-memory database**. Sample data (10 airports, 105 flights, 735 instances) loads automatically.

### 2. Open Swagger UI

Browse to **http://localhost:8080/swagger-ui.html** to explore and test all endpoints interactively.

### 3. Try a Search

```bash
curl "http://localhost:8080/api/v1/flights/search?origin=DEL&destination=BLR&travelDate=2026-02-20&includeIndirect=true&sortBy=PRICE"
```

### 4. Run Tests

```bash
# Linux/Mac
./mvnw test

# Windows
mvnw.cmd test
```

All **38 tests** (unit + integration) should pass.

### Production Mode (PostgreSQL)

```bash
# Start PostgreSQL
docker run --name airline-postgres \
  -e POSTGRES_DB=airline_booking \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=postgres \
  -p 5432:5432 -d postgres:15

# Run app (default profile uses PostgreSQL)
./mvnw spring-boot:run
```

---

## Tech Stack

| Layer | Technology | Purpose |
|-------|-----------|---------|
| Language | Java 17 | LTS release with records, sealed classes |
| Framework | Spring Boot 3.2.0 | REST API, DI, transaction management |
| ORM | Spring Data JPA / Hibernate 6.3 | Data access, pessimistic locking |
| Database | PostgreSQL 15 (prod) / H2 (dev/test) | Persistent / in-memory storage |
| Caching | Spring Cache (ConcurrentMapCache) | In-memory flight graph cache |
| API Docs | SpringDoc OpenAPI 3 (Swagger UI) | Interactive API documentation |
| Validation | Jakarta Bean Validation | Input validation with annotations |
| Build | Maven 3.9 (with wrapper) | Dependency management, build |
| Testing | JUnit 5, Mockito | Unit and integration tests |
| Utilities | Lombok | Boilerplate reduction |

---

## System Architecture

```
                          ┌─────────────────────────────────────────┐
                          │              Client / Swagger UI         │
                          └───────────────────┬─────────────────────┘
                                              │ HTTP (JSON)
                                              ▼
                          ┌─────────────────────────────────────────┐
                          │           REST Controllers               │
                          │     /api/v1/flights  /api/v1/bookings   │
                          │    ┌──────────────┐ ┌───────────────┐   │
                          │    │FlightSearch  │ │  Booking      │   │
                          │    │Controller    │ │  Controller   │   │
                          │    │(@Validated)  │ │               │   │
                          │    └──────┬───────┘ └───────┬───────┘   │
                          └───────────┼─────────────────┼───────────┘
                                      │                 │
                          ┌───────────▼─────────────────▼───────────┐
                          │           Service Layer                  │
                          │  ┌────────────────┐ ┌────────────────┐  │
                          │  │FlightSearch    │ │  Booking       │  │
                          │  │Service         │ │  Service       │  │
                          │  │(@Cacheable)    │ │(@Transactional)│  │
                          │  └────────┬───────┘ └────────┬───────┘  │
                          │           │                  │          │
                          │  ┌────────▼───────┐ ┌────────▼───────┐  │
                          │  │ FlightGraph    │ │SeatInventory   │  │
                          │  │ (in-memory     │ │Service         │  │
                          │  │  search)       │ │(pessimistic    │  │
                          │  │               │ │ locking)       │  │
                          │  └───────────────┘ └────────┬───────┘  │
                          └─────────────────────────────┼──────────┘
                                                        │
                          ┌─────────────────────────────▼──────────┐
                          │        Repository Layer (JPA)           │
                          │  SELECT ... FOR UPDATE on seat_inventory│
                          └─────────────────────┬──────────────────┘
                                                │
                          ┌─────────────────────▼──────────────────┐
                          │     PostgreSQL (prod) / H2 (dev)        │
                          └─────────────────────────────────────────┘

  Background:
  ┌───────────────────────────────────┐
  │ BookingCleanupService (@Scheduled)│  ← Runs every 2 min
  │ Expires PENDING bookings          │  ← Releases blocked seats
  │ past 10-min timeout               │
  └───────────────────────────────────┘

  Startup:
  ┌───────────────────────────────────┐
  │ DataInitializationService         │  ← @EventListener(ApplicationReadyEvent)
  │ Seeds 10 airports, 105 flights,   │  ← Runs once if DB is empty
  │ 735 instances + seat inventories  │
  └───────────────────────────────────┘
```

### Layer Responsibilities

| Layer | Role |
|-------|------|
| **Controller** | REST endpoints, input validation (`@Validated`, `@Pattern`), API versioning (`/api/v1/`) |
| **Service** | Business logic, transaction boundaries (`@Transactional`), caching (`@Cacheable`) |
| **Repository** | Data access, pessimistic locking queries (`SELECT FOR UPDATE`) |
| **Model** | JPA entities with domain methods (seat invariant enforcement, expiry checks) |

---

## Flight Search Algorithm

### Overview

The search uses a **modified K-shortest-paths algorithm with a priority queue** (Dijkstra-inspired) over an in-memory flight graph.

### How the Graph is Built

```
 ┌─────┐     6E-2001     ┌─────┐     6E-2005     ┌─────┐
 │ DEL ├────────────────►│ BOM ├────────────────►│ BLR │
 │     │     ₹3,200      │     │     ₹3,200      │     │
 └──┬──┘                 └──┬──┘                 └─────┘
    │                       │                       ▲
    │      6E-2003          │       6E-2010         │
    │      ₹5,500           │       ₹2,800          │
    └───────────────────────┼───────────────────────┘
                            │
                    (adjacency list)

  adjacencyList = {
    DEL → [Edge(BOM, ₹3200), Edge(BLR, ₹5500), ...],
    BOM → [Edge(BLR, ₹3200), Edge(DEL, ₹3800), ...],
    ...
  }
```

- Built once from the flight schedule, stored as an **adjacency list** (`Map<airportId, List<FlightEdge>>`)
- **Cached in memory** via `@Cacheable` — zero database hits during search

### Search Flow

```
  ┌──────────────────────────────────────────────────────────┐
  │                  SEARCH ALGORITHM                        │
  │                                                          │
  │  1. Initialize priority queue with origin airport        │
  │     (ordered by sort strategy cost)                      │
  │                          │                               │
  │                          ▼                               │
  │  2. ┌─── Pop lowest-cost state from queue                │
  │     │                    │                               │
  │     │                    ▼                               │
  │     │    At destination? ──YES──► Add to results         │
  │     │         │                   (up to K=10)           │
  │     │         NO                                         │
  │     │         │                                          │
  │     │         ▼                                          │
  │     │    Max hops (2) reached? ──YES──► Skip             │
  │     │         │                                          │
  │     │         NO                                         │
  │     │         │                                          │
  │     │         ▼                                          │
  │     │    For each outgoing flight edge:                  │
  │     │    ┌──────────────────────────────┐                │
  │     │    │ Filter checks:              │                │
  │     │    │  - Flight operating? (not    │                │
  │     │    │    cancelled)               │                │
  │     │    │  - Seats available? (>=pax) │                │
  │     │    │  - Same-day valid? (departs │                │
  │     │    │    after prev arrival)      │                │
  │     │    │  - Layover >= 90 min?       │                │
  │     │    │  - No cycle? (airport not   │                │
  │     │    │    already visited)         │                │
  │     │    └──────────────┬───────────────┘                │
  │     │                   │ passed                         │
  │     │                   ▼                                │
  │     │    Prune: cost competitive vs best-K for city?     │
  │     │         │                                          │
  │     │         YES                                        │
  │     │         │                                          │
  │     │         ▼                                          │
  │     │    Enqueue extended path                           │
  │     │         │                                          │
  │     └─────────┘  (repeat until queue empty or K found)   │
  │                                                          │
  │  3. Sort results by strategy, return top 10              │
  └──────────────────────────────────────────────────────────┘
```

### Search Constraints

| Rule | Value | Rationale |
|------|-------|-----------|
| Max legs | 2 (direct + 1 stop) | Domestic travel, reasonable connections |
| Min layover | 90 minutes | Buffer for deplaning, terminal transfer |
| Same-day only | Connecting flight departs after previous arrival | No overnight connections |
| Max results | K = 10 | Top 10 best routes returned |
| Cycle prevention | No revisiting airports | Avoids circular routes |

### Sort Strategies (Strategy Pattern)

```
           ┌──────────────────┐
           │FlightSortStrategy│ (interface)
           │  getComparator() │
           │  getCost()       │
           └────────┬─────────┘
                    │
          ┌─────────┴─────────┐
          │                   │
  ┌───────▼────────┐  ┌──────▼─────────┐
  │PriceSortStrategy│  │DurationSort    │
  │                │  │Strategy        │
  │ Primary: price │  │ Primary: time  │
  │ Tiebreak: time │  │ Tiebreak: price│
  └────────────────┘  └────────────────┘
          ▲                   ▲
          │                   │
  ┌───────┴───────────────────┴──────┐
  │ SortStrategyFactory              │
  │ getStrategy("PRICE") → Price     │
  │ getStrategy("DURATION") → Dur    │
  │ (case-insensitive)               │
  └──────────────────────────────────┘
```

- **`PRICE`** — cheapest total fare first; ties broken by fastest duration
- **`DURATION`** — fastest total travel time first; ties broken by cheapest price

---

## Booking Lifecycle

### State Machine

```
                    ┌──────────────────────────────────────────────┐
                    │              BOOKING LIFECYCLE                │
                    │                                              │
                    │     ┌─────────────────────────┐              │
                    │     │    POST /bookings        │              │
                    │     │    (seats blocked)       │              │
                    │     └───────────┬─────────────┘              │
                    │                 │                             │
                    │                 ▼                             │
                    │          ┌──────────┐                        │
                    │          │ PENDING  │ ◄── 10-min expiry set  │
                    │          └────┬─────┘                        │
                    │               │                              │
                    │     ┌─────────┼──────────┐                   │
                    │     │         │          │                   │
                    │     ▼         ▼          ▼                   │
                    │ ┌────────┐ ┌────────┐ ┌─────────┐           │
                    │ │CONFIRM │ │ CANCEL │ │ EXPIRED │           │
                    │ │  ED    │ │  LED   │ │  (auto) │           │
                    │ └────────┘ └────────┘ └─────────┘           │
                    │  blocked    blocked    blocked               │
                    │  → booked   → avail   → avail               │
                    │  (permanent) (released) (cleanup job)        │
                    │                                              │
                    │  Cannot be   Cannot be                       │
                    │  cancelled   re-confirmed                    │
                    └──────────────────────────────────────────────┘
```

### State Transitions

| From | Action | To | Seat Effect |
|------|--------|----|-------------|
| — | Create booking | PENDING | `available -= N`, `blocked += N` |
| PENDING | Confirm | CONFIRMED | `blocked -= N`, `booked += N` |
| PENDING | Cancel | CANCELLED | `blocked -= N`, `available += N` |
| PENDING | 10 min timeout | EXPIRED | `blocked -= N`, `available += N` (cleanup job) |
| CONFIRMED | Cancel | CANCELLED | No seat change (booked seats kept) |

### Booking Reference

Each booking gets a unique reference like `BK1a2b3c4d` (prefix `BK` + 8 random hex chars).

### Auto-Expiry

`BookingCleanupService` runs every **2 minutes** via `@Scheduled`:
1. Finds PENDING bookings past their `expiresAt` timestamp (batch of 100)
2. Releases blocked seats back to available
3. Marks bookings as EXPIRED

---

## Seat Inventory Management

### Invariant (always enforced)

```
  ┌─────────────────────────────────────────────────────────┐
  │  available + blocked + booked = totalSeats  (= 180)     │
  └─────────────────────────────────────────────────────────┘
```

This invariant is validated on every seat operation. Any violation throws an exception and rolls back the transaction.

### Seat Flow Example

```
  State            available   blocked   booked   total
  ─────────────────────────────────────────────────────
  Initial              180         0        0      180
  Book 2 pax (PEND)    178         2        0      180   ✓ invariant
  Book 3 pax (PEND)    175         5        0      180   ✓ invariant
  Confirm 2-pax        175         3        2      180   ✓ invariant
  Cancel 3-pax         178         0        2      180   ✓ invariant
  ─────────────────────────────────────────────────────
```

### Concurrency Control

```
  Thread A (Book 3 seats)          Thread B (Book 3 seats)
  ────────────────────             ────────────────────
  BEGIN TRANSACTION                BEGIN TRANSACTION
  SELECT ... FOR UPDATE ◄── acquires row lock
  available=180, block 3           SELECT ... FOR UPDATE ◄── WAITS (locked)
  available=177, blocked=3                    │
  COMMIT ──────────────────────►  lock released, reads
                                  available=177, block 3
                                  available=174, blocked=6
                                  COMMIT
```

- **Pessimistic locking** (`SELECT ... FOR UPDATE`) on the `seat_inventories` row
- Lock timeout: **10 seconds** (`jakarta.persistence.lock.timeout=10000`)
- Transaction isolation: **READ_COMMITTED**
- `@Version` field as secondary optimistic locking defense

---

## API Reference

**Base URL:** `http://localhost:8080/api/v1`
**Swagger UI:** `http://localhost:8080/swagger-ui.html`

### Flight Search

#### `GET /flights/search`

Search for direct and connecting flights.

| Parameter | Type | Required | Validation | Example |
|-----------|------|----------|------------|---------|
| `origin` | String | Yes | 3 uppercase letters (`[A-Z]{3}`) | `DEL` |
| `destination` | String | Yes | 3 uppercase letters (`[A-Z]{3}`) | `BLR` |
| `travelDate` | LocalDate | Yes | Future date (ISO format) | `2026-02-20` |
| `includeIndirect` | boolean | No | — | `true` (default: `false`) |
| `sortBy` | SortType | No | `PRICE` or `DURATION` | `PRICE` (default) |

**Example Request:**
```bash
curl "http://localhost:8080/api/v1/flights/search?\
origin=DEL&destination=BLR&travelDate=2026-02-20&includeIndirect=true&sortBy=PRICE"
```

**Example Response (200 OK):**
```json
{
  "directFlights": [
    {
      "flightId": 9,
      "flightNumber": "6E-2003",
      "origin": "DEL",
      "destination": "BLR",
      "departureTime": "14:00:00",
      "arrivalTime": "16:45:00",
      "durationMinutes": 165,
      "availableSeats": 180,
      "price": 5500.00
    }
  ],
  "indirectFlights": [
    {
      "firstLeg": {
        "flightId": 1,
        "flightNumber": "6E-2001",
        "origin": "DEL",
        "destination": "BOM",
        "departureTime": "06:00:00",
        "arrivalTime": "08:10:00",
        "durationMinutes": 130,
        "price": 3200.00
      },
      "secondLeg": {
        "flightId": 21,
        "flightNumber": "6E-2005",
        "origin": "BOM",
        "destination": "BLR",
        "departureTime": "10:00:00",
        "arrivalTime": "11:35:00",
        "durationMinutes": 95,
        "price": 3200.00
      },
      "layoverMinutes": 110,
      "totalDurationMinutes": 335,
      "totalPrice": 6400.00,
      "availableSeats": 180
    }
  ],
  "totalResults": 2
}
```

---

### Booking Operations

#### `POST /bookings` — Create Booking

Creates a booking in PENDING status. Seats are blocked for 10 minutes.

**Request Body:**
```json
{
  "flightId": 1,
  "flightDate": "2026-03-01",
  "numPassengers": 2,
  "passengers": [
    {
      "name": "Rahul Sharma",
      "age": 30,
      "gender": "M",
      "idProofNumber": "ABCDE1234F"
    },
    {
      "name": "Priya Sharma",
      "age": 28,
      "gender": "F",
      "idProofNumber": "FGHIJ5678K"
    }
  ]
}
```

**Response (201 Created):**
```json
{
  "id": 1,
  "bookingReference": "BK1a2b3c4d",
  "flightId": 1,
  "flightNumber": "6E-2001",
  "origin": "DEL",
  "destination": "BOM",
  "flightDate": "2026-03-01",
  "numPassengers": 2,
  "status": "PENDING",
  "expiresAt": "2026-02-15T10:40:00",
  "passengers": [
    { "name": "Rahul Sharma", "age": 30, "gender": "M", "idProofNumber": "ABCDE1234F" },
    { "name": "Priya Sharma", "age": 28, "gender": "F", "idProofNumber": "FGHIJ5678K" }
  ],
  "createdAt": "2026-02-15T10:30:00"
}
```

#### `POST /bookings/{id}/confirm` — Confirm Booking

Moves seats from blocked to booked. Only works on PENDING bookings that haven't expired.

**Response (200 OK):** Same as above with `"status": "CONFIRMED"` and `"expiresAt": null`.

#### `POST /bookings/{id}/cancel` — Cancel Booking

Releases blocked seats (if PENDING). Confirmed bookings can also be cancelled but booked seats are retained.

**Response (200 OK):** Same as above with `"status": "CANCELLED"`.

#### `GET /bookings/{id}` — Get Booking by ID

#### `GET /bookings/reference/{reference}` — Get Booking by Reference

---

### Error Responses

All errors return a consistent `ErrorResponseDTO`:

```json
{
  "status": 409,
  "error": "INSUFFICIENT_SEATS",
  "message": "Not enough seats available. Requested: 5, Available: 3",
  "timestamp": "2026-02-15T10:30:00"
}
```

| HTTP Status | Error Code | When |
|-------------|-----------|------|
| 400 | `VALIDATION_ERROR` | Invalid input, malformed JSON, missing params, type mismatch |
| 400 | `INVALID_BOOKING_STATE` | Invalid state transition (e.g., confirming a cancelled booking) |
| 404 | `RESOURCE_NOT_FOUND` | Flight, airport, or booking not found |
| 405 | `VALIDATION_ERROR` | Wrong HTTP method |
| 409 | `INSUFFICIENT_SEATS` | Not enough available seats |
| 410 | `BOOKING_EXPIRED` | Attempting to confirm an expired booking |
| 415 | `VALIDATION_ERROR` | Content-Type must be application/json |
| 500 | `INTERNAL_SERVER_ERROR` | Unexpected server error |

**Validation errors** include per-field details:
```json
{
  "status": 400,
  "error": "VALIDATION_ERROR",
  "message": "Validation failed for one or more fields",
  "fieldErrors": {
    "numPassengers": "must be greater than or equal to 1",
    "flightDate": "must be a future date"
  },
  "timestamp": "2026-02-15T10:30:00"
}
```

---

## Entity Relationship Diagram

```
  ┌──────────────┐         ┌────────────────┐         ┌──────────────┐
  │   Airport    │         │  FlightRoute   │         │   Airport    │
  │──────────────│    1    │────────────────│    1    │──────────────│
  │ id (PK)      │◄────────│ origin_id (FK) │         │ id (PK)      │
  │ code (UQ)    │         │ dest_id (FK)   │────────►│ code (UQ)    │
  │ name         │         │ distance_km    │         │ name         │
  │ city         │         │ est_duration   │         │ city         │
  │ country      │         │ isActive       │         │ country      │
  └──────┬───────┘         └────────────────┘         └──────────────┘
         │
         │ 1
         │
         │ ╔══════════════════════════════════════════════════════════╗
         │ ║                 Per-Flight Cluster                      ║
         │ ║                                                         ║
         │ ║  ┌──────────────────┐                                   ║
         ▼ ║  │     Flight       │                                   ║
  ┌──────────║─│──────────────────│                                   ║
  │      * ║  │ id (PK)          │                                   ║
  │        ║  │ flight_number(UQ)│                                   ║
  │        ║  │ origin_id (FK)   │                                   ║
  │        ║  │ dest_id (FK)     │                                   ║
  │        ║  │ departure_time   │                                   ║
  │        ║  │ arrival_time     │                                   ║
  │        ║  │ duration_minutes │                                   ║
  │        ║  │ total_seats (180)│                                   ║
  │        ║  │ base_price       │                                   ║
  │        ║  │ isActive         │                                   ║
  │        ║  └────────┬─────────┘                                   ║
  │        ║           │                                             ║
  │        ║     ┌─────┴──────┐                                      ║
  │        ║     │ 1       1  │                                      ║
  │        ║     ▼            ▼                                      ║
  │        ║  ┌────────────┐ ┌────────────────┐                      ║
  │        ║  │Flight      │ │ SeatInventory  │                      ║
  │        ║  │Instance    │ │────────────────│  ◄── PESSIMISTIC     ║
  │        ║  │────────────│ │ id (PK)        │      WRITE LOCK      ║
  │        ║  │ id (PK)    │ │ flight_id (FK) │                      ║
  │        ║  │ flight_id  │ │ flight_date    │                      ║
  │        ║  │ flight_date│ │ total_seats    │                      ║
  │        ║  │ status     │ │ available_seats│                      ║
  │        ║  │ price      │ │ blocked_seats  │                      ║
  │        ║  │ dep_dt     │ │ booked_seats   │                      ║
  │        ║  │ arr_dt     │ │ version        │                      ║
  │        ║  └────────────┘ └────────────────┘                      ║
  │        ╚══════════════════════════════════════════════════════════╝
  │
  │      *
  ▼
  ┌──────────────────┐         ┌──────────────────┐
  │    Booking       │    *    │   Passenger      │
  │──────────────────│────────►│──────────────────│
  │ id (PK)          │         │ id (PK)          │
  │ booking_ref (UQ) │         │ booking_id (FK)  │
  │ flight_id (FK)   │         │ name             │
  │ flight_date      │         │ age              │
  │ num_passengers   │         │ gender (M/F/O)   │
  │ status           │         │ id_proof_number  │
  │ expires_at       │         └──────────────────┘
  │ version          │
  └──────────────────┘

  Statuses:
    FlightInstance: SCHEDULED | CANCELLED | DELAYED
    Booking:        PENDING | CONFIRMED | CANCELLED | EXPIRED
    Gender:         M (Male) | F (Female) | O (Other)
```

### Database Tables & Key Indexes

```sql
airports       (id, code, name, city, country)
flight_routes  (id, origin_airport_id, destination_airport_id, distance_km, estimated_duration_minutes, is_active)
flights        (id, flight_number, origin_airport_id, destination_airport_id, departure_time, arrival_time,
                duration_minutes, total_seats, base_price, is_active)
flight_instances (id, flight_id, flight_date, status, price, departure_date_time, arrival_date_time)
seat_inventories (id, flight_id, flight_date, total_seats, available_seats, blocked_seats, booked_seats, version)
bookings       (id, booking_reference, flight_id, flight_date, num_passengers, status, expires_at, version)
passengers     (id, booking_id, name, age, gender, id_proof_number)
```

**Key Indexes:**
- `(flight_id, flight_date)` on `seat_inventories` — lock granularity
- `(origin_airport_id, destination_airport_id, departure_time)` on `flights` — search performance
- `booking_reference` unique index — reference code lookups
- `status` + `expires_at` on `bookings` — cleanup job queries

---

## Design Patterns

```
  ┌───────────────────────────────────────────────────────────────┐
  │                    DESIGN PATTERNS MAP                        │
  │                                                               │
  │  Strategy ─────── FlightSortStrategy                          │
  │  (pluggable         ├── PriceSortStrategy                     │
  │   sort order)       └── DurationSortStrategy                  │
  │                                                               │
  │  Factory ──────── SortStrategyFactory                         │
  │  (resolve from      getStrategy("PRICE") → PriceSortStrategy  │
  │   user input)       getStrategy("DURATION") → DurationSort    │
  │                                                               │
  │  Template Method ─ SeatInventory                              │
  │  (state transitions   blockSeats() → validate → update        │
  │   with validation)    confirmSeats() → validate → update      │
  │                       releaseBlockedSeats() → validate → upd  │
  │                                                               │
  │  Observer ─────── DataInitializationService                   │
  │  (startup hook)     @EventListener(ApplicationReadyEvent)     │
  │                                                               │
  │  Repository ───── Spring Data JPA repositories                │
  │  (data access       FlightRepository, BookingRepository, etc. │
  │   abstraction)                                                │
  │                                                               │
  │  Singleton ────── Cached FlightGraph (@Cacheable)             │
  │  (one instance      Built once, reused across all searches    │
  │   per lifecycle)                                              │
  └───────────────────────────────────────────────────────────────┘
```

| Pattern | Where | Why |
|---------|-------|-----|
| **Strategy** | `FlightSortStrategy` → `PriceSortStrategy`, `DurationSortStrategy` | Pluggable sort criteria; each defines comparator + cost function for priority queue |
| **Factory** | `SortStrategyFactory.getStrategy(SortType)` | Resolves strategy from user input (case-insensitive) |
| **Template Method** | `SeatInventory.blockSeats()`, `confirmSeats()`, `releaseBlockedSeats()` | Each method validates preconditions, mutates state, enforces invariant |
| **Observer** | `@EventListener(ApplicationReadyEvent)` for data init | Decoupled startup initialization |
| **Repository** | Spring Data JPA repositories with custom queries | Data access abstraction with pessimistic lock support |
| **Singleton** | Cached `FlightGraph` via `@Cacheable` | In-memory graph built once, zero DB hits on search |

---

## Error Handling

All exceptions are caught by `GlobalExceptionHandler` (`@RestControllerAdvice`):

```
  Exception Hierarchy
  ───────────────────

  RuntimeException
  ├── ResourceNotFoundException        → 404  RESOURCE_NOT_FOUND
  ├── InsufficientSeatsException       → 409  INSUFFICIENT_SEATS
  ├── BookingExpiredException          → 410  BOOKING_EXPIRED
  └── InvalidBookingStateException     → 400  INVALID_BOOKING_STATE

  Spring Framework Exceptions (also handled)
  ├── MethodArgumentNotValidException  → 400  VALIDATION_ERROR  (field-level errors)
  ├── HttpMessageNotReadableException  → 400  VALIDATION_ERROR  (malformed JSON)
  ├── MissingServletRequestParameter   → 400  VALIDATION_ERROR  (missing query param)
  ├── MethodArgumentTypeMismatch       → 400  VALIDATION_ERROR  (bad param type)
  ├── ConstraintViolationException     → 400  VALIDATION_ERROR  (constraint violations)
  ├── HttpMediaTypeNotSupported        → 415  VALIDATION_ERROR  (wrong Content-Type)
  ├── HttpRequestMethodNotSupported    → 405  VALIDATION_ERROR  (wrong HTTP method)
  ├── NoResourceFoundException         → 404  RESOURCE_NOT_FOUND (unknown endpoint)
  └── Exception (fallback)             → 500  INTERNAL_SERVER_ERROR
```

**Error codes** are a type-safe `ErrorCode` enum (not raw strings).

---

## Flight Network

**10 airports** across India, connected by **22 routes** and **105 daily flights**.

```
                              ┌─────┐
                         ┌───►│ JAI │◄───┐
                         │    └─────┘    │
                         │               │
  ┌─────┐    ┌─────┐    │    ┌─────┐    │    ┌─────┐
  │ GOI │◄──►│ BOM │◄───┼───►│ DEL │◄───┼───►│ CCU │
  └─────┘    └──┬──┘    │    └──┬──┘    │    └──┬──┘
                │       │       │       │       │
  ┌─────┐      │       │       │       │    ┌──▼──┐
  │ PNQ │◄─────┘       │       │       └───►│ HYD │
  └─────┘              │       │            └──┬──┘
                       │    ┌──▼──┐            │
                       └───►│ BLR │◄───────────┘
                            └──┬──┘
                               │
                            ┌──▼──┐
                            │ MAA │
                            └──┬──┘
                               │
                            ┌──▼──┐
                            │ COK │
                            └─────┘
```

### Airports

| Code | City | Role |
|------|------|------|
| DEL | Delhi | Hub (7 outbound routes) |
| BOM | Mumbai | Hub (7 outbound routes) |
| BLR | Bangalore | Hub (4 outbound routes) |
| MAA | Chennai | Secondary |
| CCU | Kolkata | Secondary |
| HYD | Hyderabad | Secondary |
| GOI | Goa | Spoke |
| PNQ | Pune | Spoke |
| JAI | Jaipur | Spoke |
| COK | Kochi | Spoke |

### Sample Routes

| Route | Daily Flights | Price Range | Duration |
|-------|--------------|-------------|----------|
| DEL → BOM | 6 | ₹3,200 – ₹5,500 | 130 min |
| BOM → BLR | 5 | ₹2,900 – ₹4,100 | 95 min |
| DEL → BLR | 4 | ₹4,500 – ₹5,500 | 165 min |
| DEL → CCU | 3 | ₹4,800 – ₹5,800 | 150 min |
| BOM → GOI | 3 | ₹2,400 – ₹3,200 | 65 min |

**Total daily capacity:** ~18,900 seats (105 flights x 180 seats)

---

## Test Suite

### Summary: 38 Tests (All Passing)

```
  Test Results
  ────────────
  ████████████████████████████████████████  38/38 passed  ✓

  Unit Tests:        34
  Integration Tests:  4
```

### Test Breakdown

| Test Class | Tests | Type | What It Verifies |
|-----------|-------|------|-----------------|
| **SeatInventoryServiceTest** | 9 | Unit | Block/confirm/release seats, insufficient seats, inventory creation |
| **SeatInventoryInvariantTest** | 7 | Unit | Seat invariant (`avail + blocked + booked = total`) holds across all operations |
| **BookingServiceTest** | 5 | Unit | Create/confirm/cancel booking, expiry handling, state validation |
| **BookingLifecycleTest** | 6 | Unit | Default status, expiry logic, bidirectional passenger relationship |
| **FlightGraphTest** | 6 | Unit | Direct/indirect search, sort by price/duration, no-route, overnight rejection, min layover |
| **SortStrategyTest** | 4 | Unit | Price/duration comparators, factory resolution, case-insensitive input |
| **BookingCleanupServiceTest** | 2 | Unit | Expired booking cleanup with/without expired bookings |
| **SeatInventoryRepositoryTest** | 3 | Integration | Pessimistic lock query, find by flight+date, H2 compatibility |
| **ConcurrentBookingTest** | 1 | Integration | 5 threads race to book 3 seats each (10 total) — verifies no double-booking |

### Key Test Scenarios

**Concurrency Test** — The most critical test:
- Flight with **10 seats**, **5 concurrent threads** each trying to book **3 seats**
- Only **3 bookings succeed** (9 seats blocked), **2 fail** with `InsufficientSeatsException`
- Validates pessimistic locking prevents double-booking
- Validates seat invariant holds under concurrent access

**Search Algorithm Tests:**
- Direct flight found between connected airports
- Indirect route (DEL → BOM → BLR) found and correctly priced
- Overnight connection rejected (arrive 22:00, depart 06:00)
- Short layover rejected (30 min < 90 min minimum)
- No route returns empty results
- Duration sort orders by fastest, with price tiebreaker

---

## Configuration

### Profiles

| Profile | Database | DDL | SQL Logging | Usage |
|---------|----------|-----|-------------|-------|
| `dev` | H2 in-memory | `create-drop` | Enabled | Local development |
| `test` | H2 in-memory | `create-drop` | Disabled | Automated tests |
| (default) | PostgreSQL 15 | `update` | Disabled | Production |

### Key Settings

| Setting | Value | Purpose |
|---------|-------|---------|
| `server.port` | 8080 | App port |
| `hikari.maximum-pool-size` | 20 | Max DB connections |
| `lock.timeout` | 10000 (10s) | Pessimistic lock wait timeout |
| `hibernate.jdbc.batch_size` | 20 | Batch insert optimization |
| `task.scheduling.pool.size` | 2 | Scheduled task thread pool |
| `jackson.time-zone` | UTC | JSON date/time zone |

### H2 Console (Dev Mode)

Available at `http://localhost:8080/h2-console` when running with `-Dspring-boot.run.profiles=dev`:
- JDBC URL: `jdbc:h2:mem:airline_booking`
- Username: `sa`
- Password: *(empty)*

---

## Production HLD — Scaling to 10M DAU

### Current vs Target

| Metric | Current (Dev) | Production Target |
|--------|--------------|-------------------|
| DAU | Single user | 10M |
| Search RPS | In-memory graph | 3,000 peak |
| Booking RPS | Pessimistic lock | 300 peak |
| Search p90 | < 100ms (in-memory) | < 100ms |
| Booking p90 | Single DB round-trip | < 200ms |
| Airports | 10 | 50 |
| Daily flights | 105 | 4,500 |

### High-Level Architecture

```
                           ┌──────────────┐
                           │  CDN / API   │
                           │   Gateway    │
                           └──────┬───────┘
                                  │
                      ┌───────────┼───────────┐
                      │           │           │
                ┌─────▼─────┐ ┌──▼──────┐ ┌──▼──────┐
                │  Search    │ │ Search  │ │ Search  │   ← Stateless, scales horizontally
                │  Service   │ │ Service │ │ Service │
                └─────┬──────┘ └────┬────┘ └────┬────┘
                      │             │            │
                ┌─────▼─────────────▼────────────▼────┐
                │          Redis Cluster               │   ← Cached flight graph
                │   (graph, seat snapshots, hot routes)│      + inventory snapshots
                └──────────────────────────────────────┘

                ┌─────────────┐
                │  Booking    │   ← Fewer instances (300 RPS vs 3K)
                │  Service    │
                └──────┬──────┘
                       │
                ┌──────▼──────┐
                │ Kafka /     │   ← Async seat blocking
                │ RabbitMQ    │     Decouples lock duration from HTTP response
                └──────┬──────┘
                       │
                ┌──────▼──────┐
                │ PostgreSQL  │   ← Sharded by flight_date
                │ (Primary)   │     Pessimistic lock per shard
                └──────┬──────┘
                       │
                ┌──────▼──────┐
                │ PostgreSQL  │   ← Read replicas for booking lookups
                │ (Replicas)  │
                └─────────────┘
```

### Scaling Strategy

**Search path (read-heavy, 3K RPS):**
1. API Gateway routes to any Search Service instance (stateless)
2. Search Service reads cached `FlightGraph` from Redis (or local cache)
3. Priority queue search runs entirely in-memory — no DB hit
4. Hot routes (DEL → BOM, BOM → BLR) can be pre-computed and cached
5. Seat availability is a snapshot from Redis (updated on booking events)
6. p90 < 100ms achievable — pure computation over in-memory graph

**Booking path (write-heavy, 300 RPS):**
1. Booking Service receives request, validates input
2. Publishes `SeatBlockRequest` to Kafka topic (partitioned by `flight_id`)
3. Consumer acquires pessimistic lock on `seat_inventories` row, blocks seats
4. Responds with PENDING booking + 10-minute expiry
5. Confirmation/cancellation via separate topic
6. p90 < 200ms: single DB round-trip for lock + update

### Consistency Guarantees

| Operation | Consistency | Mechanism |
|-----------|------------|-----------|
| Seat blocking | Strong (serializable per flight) | `SELECT FOR UPDATE` on sharded DB |
| Search availability | Eventual (seconds stale) | Redis snapshot updated on booking events |
| Booking status | Strong | DB write + event publish (outbox pattern) |

### Why This Scales

- **Search is 10:1 read-heavy** → scales horizontally with zero DB load
- **Booking contention is per-flight-per-date** → sharding eliminates cross-shard locks
- **Kafka decouples** lock duration from HTTP response time
- **Redis absorbs** read amplification from search (3K RPS x graph lookups)
- **Same graph algorithm** scales linearly with edges (O(V + E) per search)
