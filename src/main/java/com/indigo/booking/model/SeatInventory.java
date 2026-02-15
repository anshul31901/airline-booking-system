package com.indigo.booking.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "seat_inventories",
    indexes = {
        @Index(name = "idx_seat_inventory_flight_date", columnList = "flight_id,flight_date", unique = true)
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_flight_date", columnNames = {"flight_id", "flight_date"})
    }
)
@EntityListeners(AuditingEntityListener.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SeatInventory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "flight_id", nullable = false)
    private Flight flight;

    @Column(nullable = false)
    private LocalDate flightDate;

    @Column(nullable = false)
    private Integer totalSeats;

    @Column(nullable = false)
    private Integer availableSeats;

    @Column(nullable = false)
    private Integer blockedSeats = 0;

    @Column(nullable = false)
    private Integer bookedSeats = 0;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Version
    private Long version;

    public void blockSeats(int numSeats) {
        if (!canBlock(numSeats)) {
            throw new IllegalStateException(
                String.format("Cannot block %d seats. Available: %d", numSeats, availableSeats)
            );
        }
        this.availableSeats -= numSeats;
        this.blockedSeats += numSeats;
        validateInvariant();
    }

    public void confirmSeats(int numSeats) {
        if (this.blockedSeats < numSeats) {
            throw new IllegalStateException(
                String.format("Cannot confirm %d seats. Blocked: %d", numSeats, blockedSeats)
            );
        }
        this.blockedSeats -= numSeats;
        this.bookedSeats += numSeats;
        validateInvariant();
    }

    public void releaseBlockedSeats(int numSeats) {
        if (this.blockedSeats < numSeats) {
            throw new IllegalStateException(
                String.format("Cannot release %d seats. Blocked: %d", numSeats, blockedSeats)
            );
        }
        this.blockedSeats -= numSeats;
        this.availableSeats += numSeats;
        validateInvariant();
    }

    public boolean canBlock(int numSeats) {
        return this.availableSeats >= numSeats;
    }

    public boolean hasInvariant() {
        return (availableSeats + blockedSeats + bookedSeats) == totalSeats;
    }

    private void validateInvariant() {
        if (!hasInvariant()) {
            throw new IllegalStateException(
                String.format(
                    "Seat invariant violated! Total: %d, Available: %d, Blocked: %d, Booked: %d",
                    totalSeats, availableSeats, blockedSeats, bookedSeats
                )
            );
        }
    }
}
