package org.ngafid.uploads.process;

public class MalformedFlightFileException extends Exception {
    public MalformedFlightFileException(String message) {
        super(message);
    }

    public MalformedFlightFileException(String message, Throwable cause) {
        super(message, cause);
    }
}
