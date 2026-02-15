package com.indigo.booking.exception;

/**
 * Thrown when there are not enough available seats for a booking.
 * Maps to HTTP 409 CONFLICT.
 */
public class InsufficientSeatsException extends RuntimeException {

    public InsufficientSeatsException(String message) {
        super(message);
    }

    public InsufficientSeatsException(String message, Throwable cause) {
        super(message, cause);
    }
}
