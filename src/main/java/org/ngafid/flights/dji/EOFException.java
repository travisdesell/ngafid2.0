package org.ngafid.flights.dji;

public class EOFException extends Exception {
    public EOFException() {
        super("Reached end of file");
    }
}
