package com.indigo.booking.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Represents a specific instance of a Flight on a given date.
 * Maps to the FlightInstance concept: a realization of a FlightTemplate (Flight) for a specific date.
 * Contains per-date status (SCHEDULED/CANCELLED/DELAYED) and dynamic pricing.
 */
@Entity
@Table(name = "flight_instances",
    indexes = {
        @Index(name = "idx_fi_flight_date", columnList = "flight_id,flight_date", unique = true),
        @Index(name = "idx_fi_status", columnList = "status")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_fi_flight_date", columnNames = {"flight_id", "flight_date"})
    }
)
@EntityListeners(AuditingEntityListener.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FlightInstance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "flight_id", nullable = false)
    private Flight flight;

    @Column(name = "flight_date", nullable = false)
    private LocalDate flightDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FlightInstanceStatus status = FlightInstanceStatus.SCHEDULED;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(nullable = false)
    private LocalDateTime departureDateTime;

    @Column(nullable = false)
    private LocalDateTime arrivalDateTime;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Version
    private Long version;

    public boolean isOperating() {
        return status == FlightInstanceStatus.SCHEDULED || status == FlightInstanceStatus.DELAYED;
    }
}
