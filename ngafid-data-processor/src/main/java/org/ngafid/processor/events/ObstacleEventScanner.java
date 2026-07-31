package org.ngafid.processor.events;

import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.io.IOException;
import java.sql.Connection;

import org.ngafid.core.Database;
import org.ngafid.core.event.Event;
import org.ngafid.core.event.EventDefinition;
import org.ngafid.core.event.EventMetaData;
import org.ngafid.core.flights.DoubleTimeSeries;
import org.ngafid.core.flights.Flight;
import org.ngafid.core.flights.Parameters;
import org.ngafid.core.flights.StringTimeSeries;
import org.ngafid.core.obstacles.MarkedObstacle;
import org.ngafid.core.obstacles.Obstacles;
import org.ngafid.core.obstacles.MarkedObstacle.ObstacleRisk;

import java.util.logging.Logger;

public class ObstacleEventScanner extends AbstractEventScanner {

    private static Logger LOG = Logger.getLogger(ObstacleEventScanner.class.getName());
    private static final double MAX_DETECTION_DISTANCE = 1200;
    private static final double FIXED_WING_DETECTION_DISTANCE = 500;
    private static final double ROTOR_DETECTION_DISTANCE = 200;
    private static final double OBSTACLE_SCAN_BUFFER_SECONDS = 60 * 5; 
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

    private List<Event> processObstacles(Connection connection, Map<String, DoubleTimeSeries> doubleTimeSeries, Map<String, StringTimeSeries> stringTimeSeries) {
        
        ArrayList<Event> allEvents = new ArrayList<>();

        StringTimeSeries utcSeries = stringTimeSeries.get(Parameters.UTC_DATE_TIME);
        DoubleTimeSeries altAGL = doubleTimeSeries.get(Parameters.ALT_AGL);
        DoubleTimeSeries lat = doubleTimeSeries.get(Parameters.LATITUDE);
        DoubleTimeSeries lon = doubleTimeSeries.get(Parameters.LONGITUDE);

        HashMap<Integer, Event> tempObstacleIDMap = new HashMap<>();
        HashMap<Integer, String> tempObstacleLastUpdateMap = new HashMap<>(); 
        HashMap<Integer, MarkedObstacle> tempObstacleDistanceMap = new HashMap<>();

        int obstacleCount = 0;

        double detectionRange;
        switch (flight.getAirframeType()) {
            case "Fixed Wing":
                detectionRange = FIXED_WING_DETECTION_DISTANCE;
                LOG.info("Detecting Obstacles for Fixed Wing");
                break;
            case "Rotorcraft":
                detectionRange = ROTOR_DETECTION_DISTANCE;
                LOG.info("Detecting Obstacles for Rotorcraft");
                break;
            default:
                detectionRange = MAX_DETECTION_DISTANCE;
                LOG.info("Undefined type for obstacle detection range, using default range of 1200 ft");
                break;
        }

        // Loop through all of the flight's entries
        for (int i = 0; i < lat.size(); i++) {
            ArrayList<MarkedObstacle> nearbyObstacles = Obstacles.getNearbyObstaclesWithinRange(lat.get(i), lon.get(i), altAGL.get(i), detectionRange);

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
                    // Low risk
                    case -9:
                        if (marked.getObstacleRisk() != ObstacleRisk.LOW) {continue;}
                        break;
                    // Medium risk
                    case -8:
                        if (marked.getObstacleRisk() != ObstacleRisk.MEDIUM) {continue;}
                        break;
                    
                    default:
                        break;
                }
                

                // If they are not tracked, track them
                if (!tempObstacleIDMap.containsKey(obstacleId)) {
                    tempObstacleIDMap.put(obstacleId, new Event(utcSeries.get(i), utcSeries.get(i), i, i, super.definition.getId(), marked.getTotalDistance()));
                    tempObstacleLastUpdateMap.put(obstacleId, utcSeries.get(i));
                    tempObstacleDistanceMap.put(obstacleId, marked);
                }

                // If they are tracked, update their end time
                else {
                    Event event = tempObstacleIDMap.get(obstacleId);
                    event.updateEnd(utcSeries.get(i), i);
                    tempObstacleIDMap.put(obstacleId, event);
                    tempObstacleLastUpdateMap.put(obstacleId, utcSeries.get(i));

                    // If their current position is smaller than the inital position, update it as well
                    if (marked.getTotalDistance() < event.getSeverity()) {
                        tempObstacleIDMap.put(obstacleId, 
                            new Event(
                                event.getStartTime(), 
                                event.getEndTime(), 
                                event.getStartLine(), 
                                event.getEndLine(), 
                                event.getEventDefinitionId(), marked.getTotalDistance()));
                        tempObstacleDistanceMap.put(obstacleId, marked);
                    }
                }
            }

            ArrayList<Integer> obstacleIDs = new ArrayList<>();
            obstacleIDs.addAll(tempObstacleIDMap.keySet());

            // Loop through all of the tracked obstacle events & check if any of them should be inserted
            for (int ObstacleID : obstacleIDs) {
                
                // If their last updated time is greater than the obstacle scan buffer, the event is no longer tracked
                double diff = Duration.between(Instant.parse(tempObstacleLastUpdateMap.get(ObstacleID)), Instant.parse(utcSeries.get(i))).toMillis() / 1000.0;
                if (diff > OBSTACLE_SCAN_BUFFER_SECONDS) {
                    MarkedObstacle marked = tempObstacleDistanceMap.remove(ObstacleID);
                    tempObstacleLastUpdateMap.remove(ObstacleID);
                    Event event = tempObstacleIDMap.remove(ObstacleID);
                    event.addMetaData(new EventMetaData(EventMetaData.EventMetaDataKey.LATERAL_DISTANCE, marked.getHorizontalDistance()));
                    event.addMetaData(new EventMetaData(EventMetaData.EventMetaDataKey.VERTICAL_DISTANCE, marked.getVerticalDistance()));
                    allEvents.add(event);
                    obstacleCount++;
                }
                
            }
            
            // Insert all of the untracked obstacles into database
            try {
                Event.batchInsertion(connection, this.flight, allEvents);
                allEvents.clear();
            } catch (SQLException | IOException e) {
                LOG.warning("Unable to insert obstacle events into database through connection.");
                e.printStackTrace();
            }
            
        }

        // For all remaining events that are left over from the end, add them all and return them back for flightBuilder to insert
        // the rest of the events

        ArrayList<Integer> remainingObstacles = new ArrayList<>();
        remainingObstacles.addAll(tempObstacleIDMap.keySet());

        for (Integer obstacleID : remainingObstacles) {
            MarkedObstacle marked = tempObstacleDistanceMap.remove(obstacleID);
            Event event = tempObstacleIDMap.remove(obstacleID);
            event.addMetaData(new EventMetaData(EventMetaData.EventMetaDataKey.LATERAL_DISTANCE, marked.getHorizontalDistance()));
            event.addMetaData(new EventMetaData(EventMetaData.EventMetaDataKey.VERTICAL_DISTANCE, marked.getVerticalDistance()));
            allEvents.add(event);
        }

        LOG.info("Obstacle Events Inserted: " + (obstacleCount + allEvents.size()));
        return allEvents;
    }

    @Override
    public List<Event> scan(Map<String, DoubleTimeSeries> doubleTimeSeries, Map<String, StringTimeSeries> stringTimeSeries) throws SQLException {

        try (Connection connection = Database.getConnection()) {
            return processObstacles(connection, doubleTimeSeries, stringTimeSeries);
        }
            
    }
    
}
