package com.indigo.booking.service.search;

import com.indigo.booking.service.search.FlightGraph.FlightPath;

import java.util.Comparator;

/**
 * Strategy pattern for sorting flight search results.
 * Allows swapping sort criteria at runtime (price vs duration vs stops).
 */
public interface FlightSortStrategy {

    /**
     * Returns a comparator for ordering flight paths.
     */
    Comparator<FlightPath> getComparator();

    /**
     * Returns the cost metric used for priority queue ordering during search.
     * Lower cost = higher priority.
     */
    int getCost(FlightPath path);
}
