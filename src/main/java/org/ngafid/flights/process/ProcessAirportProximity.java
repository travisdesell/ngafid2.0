package org.ngafid.flights.process;

import java.util.Set;
import java.util.Collections;
import java.sql.Connection;
import java.sql.SQLException;

import org.ngafid.airports.*;
import org.ngafid.common.MutableDouble;
import org.ngafid.flights.DoubleTimeSeries;
import org.ngafid.flights.StringTimeSeries;
import static org.ngafid.flights.Parameters.*;
import org.ngafid.flights.FatalFlightFileException;
import org.ngafid.flights.MalformedFlightFileException;

public class ProcessAirportProximity extends ProcessStep {
    private static Set<String> REQUIRED_DOUBLE_COLUMNS = Set.of(LATITUDE, LONGITUDE, ALT_AGL);
    private static Set<String> OUTPUT_COLUMNS = Set.of(NEAREST_RUNWAY, AIRPORT_DISTANCE, RUNWAY_DISTANCE,
            NEAREST_AIRPORT);
    private final static double MAX_AIRPORT_DISTANCE_FT = 10000;
    private final static double MAX_RUNWAY_DISTANCE_FT = 100;

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

        for (int i = 0; i < latitudeTS.size(); i++) {
            double latitude = latitudeTS.get(i);
            double longitude = longitudeTS.get(i);
            double altitudeAGL = altitudeAGLTS.get(i);

            MutableDouble airportDistance = new MutableDouble();

            Airport airport = null;
            if (altitudeAGL <= 2000) {
                airport = Airports.getNearestAirportWithin(latitude, longitude, MAX_AIRPORT_DISTANCE_FT,
                        airportDistance);
            }

            if (airport == null) {
                nearestAirportTS.add("");
                airportDistanceTS.add(Double.NaN);
                nearestRunwayTS.add("");
                runwayDistanceTS.add(Double.NaN);
            } else {
                nearestAirportTS.add(airport.iataCode);
                airportDistanceTS.add(airportDistance.get());

                MutableDouble runwayDistance = new MutableDouble();
                Runway runway = airport.getNearestRunwayWithin(latitude, longitude, MAX_RUNWAY_DISTANCE_FT,
                        runwayDistance);
                if (runway == null) {
                    nearestRunwayTS.add("");
                    runwayDistanceTS.add(Double.NaN);
                } else {
                    nearestRunwayTS.add(runway.name);
                    runwayDistanceTS.add(runwayDistance.get());
                }
            }

        }

        builder.addTimeSeries(NEAREST_RUNWAY, nearestRunwayTS);
        builder.addTimeSeries(NEAREST_AIRPORT, nearestAirportTS);
        builder.addTimeSeries(RUNWAY_DISTANCE, runwayDistanceTS);
        builder.addTimeSeries(AIRPORT_DISTANCE, airportDistanceTS);
    }
}