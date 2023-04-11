package org.ngafid.flights.process;

import org.ngafid.flights.*;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

public class CSVFileProcessor extends FlightFileProcessor {
    public CSVFileProcessor(InputStream stream, String filename) {
        super(stream, filename);
    }

    @Override
    public Stream<FlightBuilder> parse() throws FlightProcessingException {
        Map<String, DoubleTimeSeries> doubleTimeSeries = new HashMap<>();
        Map<String, StringTimeSeries> stringTimeSeries = new HashMap<>();



        return new Flight(fleetId, entry, stream, connection);

        FlightBuilder builder = new FlightBuilder(new FlightMeta(), null, null);
    }


}
