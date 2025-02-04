package org.ngafid.uploads.process.steps;

import org.ngafid.flights.DoubleTimeSeries;
import org.ngafid.flights.Itinerary;
import org.ngafid.flights.StringTimeSeries;
import org.ngafid.uploads.process.FatalFlightFileException;
import org.ngafid.uploads.process.MalformedFlightFileException;
import org.ngafid.uploads.process.format.FlightBuilder;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Set;
import java.util.logging.Logger;

import static org.ngafid.flights.Parameters.*;

public class ComputeItinerary extends ComputeStep {
    private static final Logger LOG = Logger.getLogger(ComputeItinerary.class.getName());

    private static final Set<String> REQUIRED_DOUBLE_COLUMNS = Set.of(ALT_AGL, LATITUDE, LONGITUDE, AIRPORT_DISTANCE,
            RUNWAY_DISTANCE, GND_SPD, E1_RPM);
    private static final Set<String> REQUIRED_STRING_COLUMNS = Set.of(NEAREST_AIRPORT, NEAREST_RUNWAY);
    // This is a fake column; never actually created.
    private static final Set<String> OUTPUT_COLUMNS = Set.of("_itinerary");

    public ComputeItinerary(Connection connection, FlightBuilder builder) {
        super(connection, builder);
    }

    public Set<String> getRequiredDoubleColumns() {
        return REQUIRED_DOUBLE_COLUMNS;
    }

    public Set<String> getRequiredStringColumns() {
        return REQUIRED_STRING_COLUMNS;
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

            if (airport != null && !airport.equals("")) {
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

        ///////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // setting and determining itinerary type
        int itinerarySize = itinerary.size();
        for (Itinerary value : itinerary) {
            value.determineType();
        }
        ///////////////////////////////////////////////////////////////////////////////////////////////////////////////

        // LOG.info("Itinerary:");
        // for (int i = 0; i < itinerary.size(); i++) {
        // LOG.info(itinerary.get(i).toString());
        // }

        builder.setItinerary(itinerary);
    }

}
