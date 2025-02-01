package org.ngafid.uploads.process;

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
