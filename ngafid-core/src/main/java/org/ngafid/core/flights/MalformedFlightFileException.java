package org.ngafid.core.flights;

/**
 * Thrown when the format of a file is incorrect or malformed
 */
public class MalformedFlightFileException extends Exception {
    /**
     * Constructs the exception with a message describing how the file is malformed.
     *
     * @param message the detail message
     */
    public MalformedFlightFileException(String message) {
        super(message);
    }

    /**
     * Constructs the exception with a message and an underlying cause.
     *
     * @param message the detail message
     * @param cause the underlying cause
     */
    public MalformedFlightFileException(String message, Throwable cause) {
        super(message, cause);
    }
}
