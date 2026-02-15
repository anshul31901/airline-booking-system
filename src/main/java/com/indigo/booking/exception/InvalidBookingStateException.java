package com.indigo.booking.exception;

/**
 * Thrown when a booking operation is invalid for the current booking state.
 * Maps to HTTP 400 BAD REQUEST.
 */
public class InvalidBookingStateException extends RuntimeException {

    public InvalidBookingStateException(String message) {
        super(message);
    }

    public InvalidBookingStateException(String message, Throwable cause) {
        super(message, cause);
    }
}
