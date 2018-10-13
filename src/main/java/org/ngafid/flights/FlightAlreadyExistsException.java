package org.ngafid.flights;

public class FlightAlreadyExistsException extends Exception {
    public FlightAlreadyExistsException(String message) {
        super(message);
    }

    public FlightAlreadyExistsException(String message, Throwable cause) {
        super(message, cause);
    }
}
