package org.ngafid.flights;

/**
 * Represents an exception that occurs during flight file processing which is not recoverable -- the flight file
 * cannot be imported.
 */
public class FatalFlightFileException extends Exception {
    public FatalFlightFileException(String message) {
        super(message);
    }

    public FatalFlightFileException(String message, Throwable cause) {
        super(message, cause);
    }
}
