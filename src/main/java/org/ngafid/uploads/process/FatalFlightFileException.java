package org.ngafid.uploads.process;

public class FatalFlightFileException extends Exception {
    public FatalFlightFileException(String message) {
        super(message);
    }

    public FatalFlightFileException(String message, Throwable cause) {
        super(message, cause);
    }
}
