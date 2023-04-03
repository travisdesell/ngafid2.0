package org.ngafid.flights.processing;

import org.ngafid.flights.FatalFlightFileException;
import org.ngafid.flights.Flight;
import org.ngafid.flights.FlightAlreadyExistsException;
import org.ngafid.flights.MalformedFlightFileException;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;

@FunctionalInterface
public interface FileProcessor {
    Flight process(int fleetId, String entry, InputStream stream, Connection connection) throws SQLException, MalformedFlightFileException, IOException, FatalFlightFileException, FlightAlreadyExistsException;
}
