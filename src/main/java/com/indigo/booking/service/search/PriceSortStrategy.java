package com.indigo.booking.service.search;

import com.indigo.booking.service.search.FlightGraph.FlightPath;

import java.util.Comparator;

/**
 * Strategy: Sort flight results by total price (cheapest first).
 */
public class PriceSortStrategy implements FlightSortStrategy {

    @Override
    public Comparator<FlightPath> getComparator() {
        return Comparator.comparingInt(FlightPath::getTotalPriceCents)
            .thenComparingInt(FlightPath::getTotalDurationMinutes);
    }

    @Override
    public int getCost(FlightPath path) {
        return path.getTotalPriceCents();
    }
}
