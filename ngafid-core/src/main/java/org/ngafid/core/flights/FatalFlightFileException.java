package org.ngafid.core.flights;

/**
 * A wrapper around some other exception that indicates that a flight file cannot be processed.
 */
public class FatalFlightFileException extends Exception {
    /**
     * Constructs the exception with a message describing why the flight file cannot be processed.
     *
     * @param message the detail message
     */
    public FatalFlightFileException(String message) {
        super(message);
    }

    /**
     * Constructs the exception with a message and the underlying cause that made the file unprocessable.
     *
     * @param message the detail message
     * @param cause the underlying cause
     */
    public FatalFlightFileException(String message, Throwable cause) {
        super(message, cause);
    }
}
