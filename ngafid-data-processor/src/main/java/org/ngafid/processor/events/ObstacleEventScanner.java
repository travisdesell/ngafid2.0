package org.ngafid.processor.events;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.apache.commons.lang3.mutable.MutableDouble;
import org.jline.utils.Log;
import org.ngafid.core.Database;
import org.ngafid.core.airports.Airport;
import org.ngafid.core.airports.Airports;
import org.ngafid.core.event.Event;
import org.ngafid.core.event.EventDefinition;
import org.ngafid.core.event.EventMetaData;
import org.ngafid.core.flights.DoubleTimeSeries;
import org.ngafid.core.flights.Flight;
import org.ngafid.core.flights.Parameters;
import org.ngafid.core.flights.StringTimeSeries;
import org.ngafid.core.obstacles.MarkedObstacle;
import org.ngafid.core.obstacles.MarkedObstacle.ObstacleRisk;
import org.ngafid.core.obstacles.Obstacles;

/**
 * Event Scanner for obstacles.
 * This scanner scans all of the event definitions associates with obstacles and raises the appropriate
 * events. Obstacle events are linked to the corresponding obstacles by obstacle event keys table.
 * 
 * @implNote Obstacle detection range changes depending on the specific aircraft
 * ObstacleEventScanner
 */
public class ObstacleEventScanner extends AbstractEventScanner {

    private static Logger LOG = Logger.getLogger(ObstacleEventScanner.class.getName());
    private static final double MAX_DETECTION_DISTANCE = 1200;
    private static final double MAX_AIRPORT_DISTANCE = 10000;
    private static final double OBSTACLE_AGL_DETECTION_LIMIT = 300;
    private static final int OBSTACLE_SCAN_STOP_BUFFER = 30;
    private Flight flight;

    public ObstacleEventScanner(Flight flight, EventDefinition eventDefinition) {
        super(eventDefinition);
        this.flight = flight;
    }

    @Override
    protected List<String> getRequiredDoubleColumns() {
        return List.of(Parameters.ALT_MSL, Parameters.ALT_AGL, Parameters.LATITUDE, Parameters.LONGITUDE);
    }

    @Override
    protected List<String> getRequiredStringColumns() {
        return List.of(Parameters.UTC_DATE_TIME);
    }

    /**
     * For each flight entry, obstacle events are created and tracked.
     * If the same tracked obstacle gets pulled up by any of the following flight entries, 
     * the event will be updated with the new distance and time. 
     * <br> <br>
     * If the event has not been updated in a set period of time, ie., the same 
     * obstacle has not been found within range in (hard-coded to be 5 minutes for now), 
     * they are inserted into the database with severity as the distance between the obstacle & the aircraft, 
     * and horizontal & vertical distance inserted as event metadata.
     *
     * @param connection
     * @param doubleTimeSeries
     * @param stringTimeSeries
     * @return
     */
    private List<Event> processObstacles(Connection connection, Map<String, DoubleTimeSeries> doubleTimeSeries, Map<String, StringTimeSeries> stringTimeSeries) {
        
        switch (flight.getAirframeType()) {
            case "Fixed Wing":
                LOG.info("Detecting Obstacles for Fixed Wing");
                break;
            case "Rotercraft":
                LOG.info("Detecting Obstacles for Rotercraft");
                break;
            default:
                LOG.info("Detecting Obstacles for " + flight.getAirframeType() + " . Defaulting to Rotercraft detection");
                break;
        }

        StringTimeSeries utcSeries = stringTimeSeries.get(Parameters.UTC_DATE_TIME);
        DoubleTimeSeries altAGL = doubleTimeSeries.get(Parameters.ALT_AGL);
        DoubleTimeSeries lat = doubleTimeSeries.get(Parameters.LATITUDE);
        DoubleTimeSeries lon = doubleTimeSeries.get(Parameters.LONGITUDE);

        HashMap<Integer, MarkedObstacle> obstacleMap = new HashMap<>();
        HashMap<Integer, Event> obstacleEventMap = new HashMap<>();
        HashMap<Integer, Integer> obstacleStopBufferMap = new HashMap<>();
        HashMap<Event, Integer> untrackedObstacleEvents = new HashMap<>();

        ArrayList<Event> allEvents = new ArrayList<>();

        // Loop through all of the flight's entries
        for (int i = 3; i < lat.size(); i++) {

            // Ignore this entry if the flight is too low on the ground.
            if (altAGL.get(i) <= OBSTACLE_AGL_DETECTION_LIMIT) {
                continue;
            }

            ArrayList<MarkedObstacle> nearbyObstacles = Obstacles.getNearbyObstaclesWithinRange(lat.get(i), lon.get(i), altAGL.get(i), flight.getAirframeType());
            Set<Integer> trackingObstacles = new HashSet<>();

            // For each entry, check for all of the nearby objects
            for (int n = 0; n < nearbyObstacles.size(); n++) {

                MarkedObstacle marked = nearbyObstacles.get(n);
                int obstacleId = marked.getObstacleID();

                // Check for if it is the right obstacle risk
                switch (definition.getId()) {
                    // High risk
                    case -10:
                        if (marked.getObstacleRisk() != ObstacleRisk.HIGH) {continue;}
                        break;
                    // Medium risk
                    case -9:
                        if (marked.getObstacleRisk() != ObstacleRisk.MEDIUM) {continue;}
                        break;
                    // Low risk
                    case -8:
                        if (marked.getObstacleRisk() != ObstacleRisk.LOW) {continue;}
                        break;
                    default:
                        break;
                }
                

                // If they are not tracked, track them
                if (!obstacleEventMap.containsKey(obstacleId)) {
                    Event event = new Event(utcSeries.get(i), utcSeries.get(i), i, i, super.definition.getId(), marked.getTotalDistance());
                    obstacleMap.put(obstacleId, marked);
                    obstacleEventMap.put(obstacleId, event);                    
                }

                // If they are tracked, update their end time
                else {
                    Event event = obstacleEventMap.get(obstacleId);
                    event.updateEnd(utcSeries.get(i), i);

                    // If their current position is smaller than the inital position, update it as well
                    if (marked.getTotalDistance() < event.getSeverity()) {

                        Event newEvent = new Event(event.getStartTime(), event.getEndTime(), event.getStartLine(), 
                                event.getEndLine(), event.getEventDefinitionId(), marked.getTotalDistance());

                        obstacleEventMap.put(obstacleId, newEvent);
                    } else {
                        obstacleEventMap.put(obstacleId, event);
                    }
                }
                trackingObstacles.add(obstacleId);
                obstacleStopBufferMap.put(obstacleId, 1);
            }

            // Untrack the obstacle events that has not been updated
            ArrayList<Integer> copyOfAllObstacleIDs = new ArrayList<>();
            copyOfAllObstacleIDs.addAll(obstacleEventMap.keySet());

            for (Integer obstacleId : copyOfAllObstacleIDs) {
                
                if (trackingObstacles.contains(obstacleId)) {
                    continue;
                }

                obstacleStopBufferMap.put(obstacleId, obstacleStopBufferMap.get(obstacleId) + 1);

                if (obstacleStopBufferMap.get(obstacleId) > OBSTACLE_SCAN_STOP_BUFFER) { 

                    // Remove from the tracked hashmaps
                    obstacleStopBufferMap.remove(obstacleId);
                    MarkedObstacle marked = obstacleMap.remove(obstacleId);
                    Event event = obstacleEventMap.remove(obstacleId);
                    // LOG.info(marked.getObstacleID() + "| Start: " + event.getStartLine() + " | End: " + event.getEndLine());

                    // Update the Event with metadata
                    event.addMetaData(new EventMetaData(EventMetaData.EventMetaDataKey.LATERAL_DISTANCE, marked.getHorizontalDistance()));
                    event.addMetaData(new EventMetaData(EventMetaData.EventMetaDataKey.VERTICAL_DISTANCE, marked.getVerticalDistance()));
                    event.addMetaData(new EventMetaData(EventMetaData.EventMetaDataKey.OBSTACLE_ID, (double) marked.getObstacleID()));
                    
                    // Add to untrackedObstacle list
                    untrackedObstacleEvents.put(event, obstacleId);
                }
            }
            
            // Insert all of the untracked obstacles into database
            // insertObstacleEvents(connection, untrackedObstacleEvents);
            allEvents.addAll(untrackedObstacleEvents.keySet());
            untrackedObstacleEvents.clear();
            trackingObstacles.clear();
        }

        // For all remaining events that are left over from the end, add them all
        ArrayList<Integer> remainingObstacles = new ArrayList<>();
        remainingObstacles.addAll(obstacleEventMap.keySet());

        for (Integer obstacleID : remainingObstacles) {
            MarkedObstacle marked = obstacleMap.remove(obstacleID);
            Event event = obstacleEventMap.remove(obstacleID);
            // LOG.info(marked.getObstacleID() + "| Start: " + event.getStartLine() + " | End: " + event.getEndLine());
            event.addMetaData(new EventMetaData(EventMetaData.EventMetaDataKey.LATERAL_DISTANCE, marked.getHorizontalDistance()));
            event.addMetaData(new EventMetaData(EventMetaData.EventMetaDataKey.VERTICAL_DISTANCE, marked.getVerticalDistance()));
            event.addMetaData(new EventMetaData(EventMetaData.EventMetaDataKey.OBSTACLE_ID, (double) marked.getObstacleID()));
            
            untrackedObstacleEvents.put(event, obstacleID);
        }
        // insertObstacleEvents(connection, untrackedObstacleEvents);
        allEvents.addAll(untrackedObstacleEvents.keySet());
        LOG.info("Obstacle Events (" + definition.getId() + ") Found for " + flight.getAirframeType() + ": " + (allEvents.size()));
        return allEvents;
    }
  
    @Override
    public List<Event> scan(Map<String, DoubleTimeSeries> doubleTimeSeries, Map<String, StringTimeSeries> stringTimeSeries) throws SQLException {

        try (Connection connection = Database.getConnection()) {
            return processObstacles(connection, doubleTimeSeries, stringTimeSeries);
        }
    }
}
