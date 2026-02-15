package com.indigo.booking.service.search;

public final class SortStrategyFactory {

    public enum SortType {
        PRICE,
        DURATION
    }

    private static final PriceSortStrategy PRICE_STRATEGY = new PriceSortStrategy();
    private static final DurationSortStrategy DURATION_STRATEGY = new DurationSortStrategy();

    private SortStrategyFactory() {}

    public static FlightSortStrategy getStrategy(SortType sortType) {
        return switch (sortType) {
            case PRICE -> PRICE_STRATEGY;
            case DURATION -> DURATION_STRATEGY;
        };
    }

    public static FlightSortStrategy getStrategy(String sortBy) {
        if (sortBy == null || sortBy.isBlank()) {
            return PRICE_STRATEGY;
        }
        try {
            return getStrategy(SortType.valueOf(sortBy.toUpperCase()));
        } catch (IllegalArgumentException e) {
            return PRICE_STRATEGY;
        }
    }
}
