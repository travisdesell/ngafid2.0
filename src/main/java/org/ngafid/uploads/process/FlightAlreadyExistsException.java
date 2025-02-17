package org.ngafid.uploads.process;

public class FlightAlreadyExistsException extends Exception {
    public FlightAlreadyExistsException(String message) {
        super(message);
    }

    public FlightAlreadyExistsException(String message, Throwable cause) {
        super(message, cause);
    }
}
