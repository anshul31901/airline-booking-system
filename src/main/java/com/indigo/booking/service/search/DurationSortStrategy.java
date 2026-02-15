package com.indigo.booking.service.search;

import com.indigo.booking.service.search.FlightGraph.FlightPath;

import java.util.Comparator;

/**
 * Strategy: Sort flight results by total duration (fastest first).
 */
public class DurationSortStrategy implements FlightSortStrategy {

    @Override
    public Comparator<FlightPath> getComparator() {
        return Comparator.comparingInt(FlightPath::getTotalDurationMinutes)
            .thenComparingInt(FlightPath::getTotalPriceCents);
    }

    @Override
    public int getCost(FlightPath path) {
        return path.getTotalDurationMinutes();
    }
}
