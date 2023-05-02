package org.ngafid.flights.process;

import java.util.Set;
import java.util.ArrayList;
import java.util.logging.Logger;
import java.util.Collections;
import java.sql.Connection;
import java.sql.SQLException;

import java.nio.file.NoSuchFileException;

import org.ngafid.flights.Flight;
import org.ngafid.flights.DoubleTimeSeries;
import org.ngafid.flights.StringTimeSeries;
import org.ngafid.flights.Itinerary;
import static org.ngafid.flights.Parameters.*;
import org.ngafid.flights.FatalFlightFileException;
import org.ngafid.flights.MalformedFlightFileException;

public class ProcessItinerary extends ProcessStep {
    private static final Logger LOG = Logger.getLogger(ProcessItinerary.class.getName());

    private static Set<String> REQUIRED_DOUBLE_COLUMNS = Set.of(ALT_AGL, LATITUDE, LONGITUDE, AIRPORT_DISTANCE, RUNWAY_DISTANCE, GND_SPD, E1_RPM);
    private static Set<String> REQUIRED_STRING_COLUMNS = Set.of(NEAREST_AIRPORT, NEAREST_RUNWAY);
    private static Set<String> OUTPUT_COLUMNS = Set.of("_itinerary"); // This is a fake column; never actually created.

    public ProcessItinerary(Connection connection, FlightBuilder builder) {
        super(connection, builder);
    }

    public Set<String> getRequiredDoubleColumns() { return REQUIRED_DOUBLE_COLUMNS; }
    public Set<String> getRequiredStringColumns() { return REQUIRED_STRING_COLUMNS; }
    public Set<String> getRequiredColumns() { return REQUIRED_DOUBLE_COLUMNS; }
    public Set<String> getOutputColumns() { return OUTPUT_COLUMNS; }
    
    public boolean airframeIsValid(String airframe) { return true; }

    public void compute() throws SQLException, MalformedFlightFileException, FatalFlightFileException {
        DoubleTimeSeries groundSpeed = doubleTS.get(GND_SPD);
        DoubleTimeSeries rpm = doubleTS.get(E1_RPM);

        StringTimeSeries nearestAirportTS = stringTS.get(NEAREST_AIRPORT);
        DoubleTimeSeries airportDistanceTS = doubleTS.get(AIRPORT_DISTANCE);
        DoubleTimeSeries altitudeAGL = doubleTS.get(ALT_AGL);

        StringTimeSeries nearestRunwayTS = stringTS.get(NEAREST_RUNWAY);
        DoubleTimeSeries runwayDistanceTS = doubleTS.get(RUNWAY_DISTANCE);

        ArrayList<Itinerary> itinerary = new ArrayList<>();

        Itinerary currentItinerary = null;
        for (int i = 1; i < nearestAirportTS.size(); i++) {
            String airport = nearestAirportTS.get(i);
            String runway = nearestRunwayTS.get(i);

            if (airport != null && !airport.equals("")) {
                //We've gotten close to an airport, so create a stop if there
                //isn't one.  If there is one, update the runway being visited.
                //If the airport is a new airport (this shouldn't happen really),
                //then create a new stop.
                if (currentItinerary == null) {
                    currentItinerary = new Itinerary(airport, runway, i, altitudeAGL.get(i), airportDistanceTS.get(i), runwayDistanceTS.get(i), groundSpeed.get(i), rpm.get(i));
                } else if (airport.equals(currentItinerary.getAirport())) {
                    currentItinerary.update(runway, i, altitudeAGL.get(i), airportDistanceTS.get(i), runwayDistanceTS.get(i), groundSpeed.get(i), rpm.get(i));
                } else {
                    currentItinerary.selectBestRunway();
                    if (currentItinerary.wasApproach()) itinerary.add(currentItinerary);
                    currentItinerary = new Itinerary(airport, runway, i, altitudeAGL.get(i), airportDistanceTS.get(i), runwayDistanceTS.get(i), groundSpeed.get(i), rpm.get(i));
                }

            } else {
                //aiport is null, so if there was an airport being visited
                //then we can determine it's runway and add it to the itinerary
                if (currentItinerary != null) {
                    currentItinerary.selectBestRunway();
                    if (currentItinerary.wasApproach()) itinerary.add(currentItinerary);
                }

                //set the currentItinerary to null until we approach another
                //airport
                currentItinerary = null;
            }
        }

        //dont forget to add the last stop in the itinerary if it wasn't set to null
        if (currentItinerary != null) {
            currentItinerary.selectBestRunway();
            if (currentItinerary.wasApproach()) itinerary.add(currentItinerary);
        }

        ///////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // setting and determining itinerary type
        int itinerary_size = itinerary.size();
        for (int i = 0; i < itinerary_size; i++) {
            itinerary.get(i).determineType();
        }
        ///////////////////////////////////////////////////////////////////////////////////////////////////////////////

        // LOG.info("Itinerary:");
        // for (int i = 0; i < itinerary.size(); i++) {
        //     LOG.info(itinerary.get(i).toString());
        // }

        builder.setItinerary(itinerary);
    }

}
