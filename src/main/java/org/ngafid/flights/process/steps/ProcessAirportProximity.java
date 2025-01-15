package org.ngafid.flights.process.steps;

import org.ngafid.airports.Airport;
import org.ngafid.airports.Airports;
import org.ngafid.airports.Runway;
import org.ngafid.common.MutableDouble;
import org.ngafid.flights.DoubleTimeSeries;
import org.ngafid.flights.FatalFlightFileException;
import org.ngafid.flights.MalformedFlightFileException;
import org.ngafid.flights.StringTimeSeries;
import org.ngafid.flights.process.FlightBuilder;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Set;

import static org.ngafid.flights.Parameters.*;
import java.util.Set;
import java.util.Collections;
import java.sql.Connection;
import java.sql.SQLException;

import org.ngafid.flights.*;

import static org.ngafid.flights.Parameters.*;

public class ProcessAirportProximity extends ProcessStep {
    private static final Set<String> REQUIRED_DOUBLE_COLUMNS = Set.of(LATITUDE, LONGITUDE, ALT_AGL);
    private static final Set<String> OUTPUT_COLUMNS = Set.of(NEAREST_RUNWAY, AIRPORT_DISTANCE, RUNWAY_DISTANCE,
            NEAREST_AIRPORT);
    private static final double MAX_AIRPORT_DISTANCE_FT = 10000;
    private static final double MAX_RUNWAY_DISTANCE_FT = 100;

    public ProcessAirportProximity(Connection connection, FlightBuilder builder) {
        super(connection, builder);
    }

    public Set<String> getRequiredDoubleColumns() {
        return REQUIRED_DOUBLE_COLUMNS;
    }

    public Set<String> getRequiredStringColumns() {
        return Collections.<String>emptySet();
    }

    public Set<String> getRequiredColumns() {
        return REQUIRED_DOUBLE_COLUMNS;
    }

    public Set<String> getOutputColumns() {
        return OUTPUT_COLUMNS;
    }

    public boolean airframeIsValid(String airframe) {
        return true;
    }

    public void compute() throws SQLException, MalformedFlightFileException, FatalFlightFileException {
        DoubleTimeSeries latitudeTS = builder.getDoubleTimeSeries(LATITUDE);
        DoubleTimeSeries longitudeTS = builder.getDoubleTimeSeries(LONGITUDE);
        DoubleTimeSeries altitudeAGLTS = builder.getDoubleTimeSeries(ALT_AGL);

        int sizeHint = latitudeTS.size();

        StringTimeSeries nearestAirportTS = new StringTimeSeries(NEAREST_RUNWAY, Unit.IATA_CODE, sizeHint);
        DoubleTimeSeries airportDistanceTS = new DoubleTimeSeries(AIRPORT_DISTANCE, Unit.FT, sizeHint);
        StringTimeSeries nearestRunwayTS = new StringTimeSeries(NEAREST_RUNWAY, "IATA Code", sizeHint);
        DoubleTimeSeries runwayDistanceTS = new DoubleTimeSeries(RUNWAY_DISTANCE, "ft", sizeHint);

        Flight.getNearbyLandingAreas(latitudeTS, longitudeTS, altitudeAGLTS, nearestAirportTS,
                airportDistanceTS, nearestRunwayTS, runwayDistanceTS, MAX_AIRPORT_DISTANCE_FT, MAX_RUNWAY_DISTANCE_FT);

        builder.addTimeSeries(NEAREST_RUNWAY, nearestRunwayTS);
        builder.addTimeSeries(NEAREST_AIRPORT, nearestAirportTS);
        builder.addTimeSeries(RUNWAY_DISTANCE, runwayDistanceTS);
        builder.addTimeSeries(AIRPORT_DISTANCE, airportDistanceTS);
    }
}
