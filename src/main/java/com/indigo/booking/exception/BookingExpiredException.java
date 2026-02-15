package com.indigo.booking.exception;

/**
 * Thrown when attempting to confirm an expired booking.
 * Maps to HTTP 410 GONE.
 */
public class BookingExpiredException extends RuntimeException {

    public BookingExpiredException(String message) {
        super(message);
    }

    public BookingExpiredException(String message, Throwable cause) {
        super(message, cause);
    }
}
