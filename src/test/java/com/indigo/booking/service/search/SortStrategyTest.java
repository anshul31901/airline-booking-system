package com.indigo.booking.service.search;

import com.indigo.booking.service.search.FlightGraph.FlightLeg;
import com.indigo.booking.service.search.FlightGraph.FlightPath;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;
import java.util.Comparator;

import static org.junit.jupiter.api.Assertions.*;

class SortStrategyTest {

    @Test
    void priceSortStrategy_shouldSortByCost() {
        FlightSortStrategy strategy = SortStrategyFactory.getStrategy("price");
        assertInstanceOf(PriceSortStrategy.class, strategy);

        FlightPath cheap = buildPath(100, 300);
        FlightPath expensive = buildPath(200, 100);

        assertTrue(strategy.getCost(cheap) < strategy.getCost(expensive));
    }

    @Test
    void durationSortStrategy_shouldSortByDuration() {
        FlightSortStrategy strategy = SortStrategyFactory.getStrategy("duration");
        assertInstanceOf(DurationSortStrategy.class, strategy);

        FlightPath fast = buildPath(200, 100);
        FlightPath slow = buildPath(100, 300);

        assertTrue(strategy.getCost(fast) < strategy.getCost(slow));
    }

    @Test
    void factoryDefaultsToPriceStrategy() {
        assertInstanceOf(PriceSortStrategy.class, SortStrategyFactory.getStrategy((String) null));
        assertInstanceOf(PriceSortStrategy.class, SortStrategyFactory.getStrategy(""));
        assertInstanceOf(PriceSortStrategy.class, SortStrategyFactory.getStrategy("unknown"));
    }

    @Test
    void factoryRecognizesCaseInsensitiveNames() {
        assertInstanceOf(DurationSortStrategy.class, SortStrategyFactory.getStrategy("duration"));
        assertInstanceOf(DurationSortStrategy.class, SortStrategyFactory.getStrategy("DURATION"));
        assertInstanceOf(PriceSortStrategy.class, SortStrategyFactory.getStrategy("Price"));
    }

    private FlightPath buildPath(int priceCents, int durationMinutes) {
        FlightPath path = new FlightPath();
        FlightPath extended = path.extend(new FlightLeg(
            1L, "6E-1", "DEL", "BOM",
            LocalTime.of(6, 0), LocalTime.of(8, 0),
            durationMinutes, priceCents, 180
        ));
        return extended;
    }
}
