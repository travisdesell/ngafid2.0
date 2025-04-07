package org.ngafid.uploads.process;

/**
 * A wrapper around some other exception that indicates that a flight file cannot be processed.
 */
public class FatalFlightFileException extends Exception {
    public FatalFlightFileException(String message) {
        super(message);
    }

    public FatalFlightFileException(String message, Throwable cause) {
        super(message, cause);
    }
}
