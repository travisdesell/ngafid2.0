package org.ngafid.uploads.process;

/**
 * An exception indicating that a file extension or format is not recognized or otherwise supported.
 */
public class FlightFileFormatException extends Exception {
    private static final long serialVersionUID = 124311;

    private final String filename;

    public FlightFileFormatException(String filename) {
        this.filename = filename;
    }

    public String getMessage() {
        return "File '" + filename + "' is of an unrecognized or unsupported file format.";
    }

}
