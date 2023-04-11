package org.ngafid.flights.process;

import org.ngafid.flights.FatalFlightFileException;
import org.ngafid.flights.Flight;
import org.ngafid.flights.FlightAlreadyExistsException;
import org.ngafid.flights.MalformedFlightFileException;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

@FunctionalInterface
public interface FileProcessor {
    Flight process(int fleetId, String entry, InputStream stream, Connection connection) throws SQLException, MalformedFlightFileException, IOException, FatalFlightFileException, FlightAlreadyExistsException;

    default boolean process(int fleetId, String entry, InputStream stream, Connection connection, List<Flight> flights) throws SQLException, MalformedFlightFileException, IOException, FatalFlightFileException, FlightAlreadyExistsException {
        flights.add(process(fleetId, entry, stream, connection));

        return true;
    }
}
