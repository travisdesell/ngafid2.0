package org.ngafid.flights;

/**
 * Represents an error within a flight file that does not necessarily preclude it from being processed, but it may
 * prevent subsequent processing steps from completing successfully.
 */
public class MalformedFlightFileException extends Exception {
    public MalformedFlightFileException(String message) {
        super(message);
    }

    public MalformedFlightFileException(String message, Throwable cause) {
        super(message, cause);
    }
}
