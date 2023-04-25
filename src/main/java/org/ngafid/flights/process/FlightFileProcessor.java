package org.ngafid.flights.process;

import java.io.InputStream;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;

public abstract class FlightFileProcessor {

    public static FlightFileProcessor create(ZipEntry entry) throws FlightFileFormatException {
        String filename = entry.getName();

        int index = filename.lastIndexOf('.');
        String extension = index >= 0 ? filename.substring(index) : "";

        switch (extension) {
            // TODO: Add supported extensions here!
            case "": // No extension
            default:
                throw new FlightFileFormatException(filename);
        }
    }

    public final String filename;
    public final InputStream stream;

    public FlightFileProcessor(InputStream stream, String filename, Object... args) {
        this.stream = stream;
        this.filename = filename;
    }


    public abstract Stream<FlightBuilder> parse() throws FlightProcessingException;
}
