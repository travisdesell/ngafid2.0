package org.ngafid.flights;

public class FatalFlightFileException extends Exception {
    public FatalFlightFileException(String message) {
        super(message);
    }

    public FatalFlightFileException(String message, Throwable cause) {
        super(message, cause);
    }
}
