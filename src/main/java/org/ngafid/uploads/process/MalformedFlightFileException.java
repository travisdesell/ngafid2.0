package org.ngafid.uploads.process;

/**
 * Thrown when the format of a file is incorrect or malformed
 */
public class MalformedFlightFileException extends Exception {
    public MalformedFlightFileException(String message) {
        super(message);
    }

    public MalformedFlightFileException(String message, Throwable cause) {
        super(message, cause);
    }
}
