package org.ngafid.uploads.process.steps;

import org.ngafid.common.MutableDouble;
import org.ngafid.common.airports.Airport;
import org.ngafid.common.airports.Airports;
import org.ngafid.common.airports.Runway;
import org.ngafid.flights.DoubleTimeSeries;
import org.ngafid.flights.StringTimeSeries;
import org.ngafid.uploads.process.FatalFlightFileException;
import org.ngafid.uploads.process.MalformedFlightFileException;
import org.ngafid.uploads.process.format.FlightBuilder;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Set;

import static org.ngafid.flights.Parameters.*;

/**
 * Computes the set of series related to airport proximity. This provides the nearest runway, nearest airport, and the
 * distance to them in two string series and two double series.
 */
public class ComputeAirportProximity extends ComputeStep {
    private static final Set<String> REQUIRED_DOUBLE_COLUMNS = Set.of(LATITUDE, LONGITUDE, ALT_AGL);
    private static final Set<String> OUTPUT_COLUMNS = Set.of(NEAREST_RUNWAY, AIRPORT_DISTANCE, RUNWAY_DISTANCE,
            NEAREST_AIRPORT);
    private static final double MAX_AIRPORT_DISTANCE_FT = 10000;
    private static final double MAX_RUNWAY_DISTANCE_FT = 100;

    public ComputeAirportProximity(Connection connection, FlightBuilder builder) {
        super(connection, builder);
    }

    @Override
    public Set<String> getRequiredDoubleColumns() {
        return REQUIRED_DOUBLE_COLUMNS;
    }

    @Override
    public Set<String> getRequiredStringColumns() {
        return Collections.<String>emptySet();
    }

    @Override
    public Set<String> getRequiredColumns() {
        return REQUIRED_DOUBLE_COLUMNS;
    }

    @Override
    public Set<String> getOutputColumns() {
        return OUTPUT_COLUMNS;
    }

    public boolean airframeIsValid(String airframe) {
        return true;
    }

    @Override
    public void compute() throws SQLException, MalformedFlightFileException, FatalFlightFileException {
        DoubleTimeSeries latitudeTS = builder.getDoubleTimeSeries(LATITUDE);
        DoubleTimeSeries longitudeTS = builder.getDoubleTimeSeries(LONGITUDE);
        DoubleTimeSeries altitudeAGLTS = builder.getDoubleTimeSeries(ALT_AGL);

        int sizeHint = latitudeTS.size();

        StringTimeSeries nearestAirportTS = new StringTimeSeries(NEAREST_RUNWAY, Unit.IATA_CODE, sizeHint);
        DoubleTimeSeries airportDistanceTS = new DoubleTimeSeries(AIRPORT_DISTANCE, Unit.FT, sizeHint);
        StringTimeSeries nearestRunwayTS = new StringTimeSeries(NEAREST_RUNWAY, Unit.IATA_CODE, sizeHint);
        DoubleTimeSeries runwayDistanceTS = new DoubleTimeSeries(RUNWAY_DISTANCE, Unit.FT, sizeHint);

        for (int i = 0; i < latitudeTS.size(); i++) {
            double latitude = latitudeTS.get(i);
            double longitude = longitudeTS.get(i);
            double altitudeAGL = altitudeAGLTS.get(i);

            MutableDouble airportDistance = new MutableDouble();
            Airport airport = null;
            if (altitudeAGL <= 2000) {
                airport = Airports.getNearestAirportWithin(latitude, longitude, MAX_AIRPORT_DISTANCE_FT, airportDistance);
            }

            if (airport == null) {
                nearestAirportTS.add("");
                airportDistanceTS.add(Double.NaN);
                nearestRunwayTS.add("");
                runwayDistanceTS.add(Double.NaN);
            } else {
                nearestAirportTS.add(airport.iataCode);
                airportDistanceTS.add(airportDistance.getValue());

                MutableDouble runwayDistance = new MutableDouble();
                Runway runway = airport.getNearestRunwayWithin(latitude, longitude, MAX_RUNWAY_DISTANCE_FT,
                        runwayDistance);

                if (runway == null) {
                    nearestRunwayTS.add("");
                    runwayDistanceTS.add(Double.NaN);
                } else {
                    nearestRunwayTS.add(runway.getName());
                    runwayDistanceTS.add(runwayDistance.getValue());
                }
            }
        }

        builder.addTimeSeries(NEAREST_RUNWAY, nearestRunwayTS);
        builder.addTimeSeries(NEAREST_AIRPORT, nearestAirportTS);
        builder.addTimeSeries(RUNWAY_DISTANCE, runwayDistanceTS);
        builder.addTimeSeries(AIRPORT_DISTANCE, airportDistanceTS);
    }
}
