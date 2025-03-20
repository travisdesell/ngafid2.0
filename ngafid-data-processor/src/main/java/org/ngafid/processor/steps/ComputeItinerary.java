package org.ngafid.processor.steps;

import org.ngafid.core.flights.*;
import org.ngafid.processor.format.FlightBuilder;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Set;
import java.util.logging.Logger;

import static org.ngafid.core.flights.Parameters.*;

/**
 * Computes the itinerary for the flight. We represent the itinerary as a list of {@link Itinerary} objects,
 * where each object represents a single visit to an airport.
 */
public class ComputeItinerary extends ComputeStep {
    private static final Logger LOG = Logger.getLogger(ComputeItinerary.class.getName());

    private static final Set<String> REQUIRED_DOUBLE_COLUMNS = Set.of(ALT_AGL, LATITUDE, LONGITUDE, AIRPORT_DISTANCE,
            RUNWAY_DISTANCE, GND_SPD, E1_RPM);
    private static final Set<String> REQUIRED_STRING_COLUMNS = Set.of(NEAREST_AIRPORT, NEAREST_RUNWAY);
    // This is a fake column; never actually created, but for steps that rely on the itinerary they can use this dummy column.
    private static final Set<String> OUTPUT_COLUMNS = Set.of("_itinerary");

    public ComputeItinerary(Connection connection, FlightBuilder builder) {
        super(connection, builder);
    }

    @Override
    public Set<String> getRequiredDoubleColumns() {
        return REQUIRED_DOUBLE_COLUMNS;
    }

    @Override
    public Set<String> getRequiredStringColumns() {
        return REQUIRED_STRING_COLUMNS;
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
        DoubleTimeSeries groundSpeed = builder.getDoubleTimeSeries(GND_SPD);
        DoubleTimeSeries rpm = builder.getDoubleTimeSeries(E1_RPM);

        StringTimeSeries nearestAirportTS = builder.getStringTimeSeries(NEAREST_AIRPORT);
        DoubleTimeSeries airportDistanceTS = builder.getDoubleTimeSeries(AIRPORT_DISTANCE);
        DoubleTimeSeries altitudeAGL = builder.getDoubleTimeSeries(ALT_AGL);

        StringTimeSeries nearestRunwayTS = builder.getStringTimeSeries(NEAREST_RUNWAY);
        DoubleTimeSeries runwayDistanceTS = builder.getDoubleTimeSeries(RUNWAY_DISTANCE);

        ArrayList<Itinerary> itinerary = new ArrayList<>();

        Itinerary currentItinerary = null;
        for (int i = 1; i < nearestAirportTS.size(); i++) {
            String airport = nearestAirportTS.get(i);
            String runway = nearestRunwayTS.get(i);

            if (airport != null && !airport.isEmpty()) {
                // We've gotten close to an airport, so create a stop if there
                // isn't one. If there is one, update the runway being visited.
                // If the airport is a new airport (this shouldn't happen really),
                // then create a new stop.
                if (currentItinerary == null) {
                    currentItinerary = new Itinerary(airport, runway, i, altitudeAGL.get(i), airportDistanceTS.get(i),
                            runwayDistanceTS.get(i), groundSpeed.get(i), rpm.get(i));
                } else if (airport.equals(currentItinerary.getAirport())) {
                    currentItinerary.update(runway, i, altitudeAGL.get(i), airportDistanceTS.get(i),
                            runwayDistanceTS.get(i), groundSpeed.get(i), rpm.get(i));
                } else {
                    currentItinerary.selectBestRunway();
                    if (currentItinerary.wasApproach())
                        itinerary.add(currentItinerary);
                    currentItinerary = new Itinerary(airport, runway, i, altitudeAGL.get(i), airportDistanceTS.get(i),
                            runwayDistanceTS.get(i), groundSpeed.get(i), rpm.get(i));
                }

            } else {
                // aiport is null, so if there was an airport being visited
                // then we can determine it's runway and add it to the itinerary
                if (currentItinerary != null) {
                    currentItinerary.selectBestRunway();
                    if (currentItinerary.wasApproach())
                        itinerary.add(currentItinerary);
                }

                // set the currentItinerary to null until we approach another
                // airport
                currentItinerary = null;
            }
        }

        // dont forget to add the last stop in the itinerary if it wasn't set to null
        if (currentItinerary != null) {
            currentItinerary.selectBestRunway();
            if (currentItinerary.wasApproach())
                itinerary.add(currentItinerary);
        }

        for (Itinerary value : itinerary) {
            value.determineType();
        }

        builder.setItinerary(itinerary);
    }

}
