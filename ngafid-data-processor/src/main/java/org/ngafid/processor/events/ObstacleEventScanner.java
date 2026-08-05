package org.ngafid.processor.events;

import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;

import org.jline.utils.Log;
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

        StringTimeSeries utcSeries = stringTimeSeries.get(Parameters.UTC_DATE_TIME);
        DoubleTimeSeries altAGL = doubleTimeSeries.get(Parameters.ALT_AGL);
        DoubleTimeSeries lat = doubleTimeSeries.get(Parameters.LATITUDE);
        DoubleTimeSeries lon = doubleTimeSeries.get(Parameters.LONGITUDE);

        HashMap<Integer, MarkedObstacle> obstacleMap = new HashMap<>();
        HashMap<Integer, Event> obstacleEventMap = new HashMap<>();
        HashMap<Integer, String> obstacleLastUpdateMap = new HashMap<>(); 
        HashMap<Event, Integer> untrackedObstacleEvents = new HashMap<>();
        
        int insertedEvents = 0;

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
                if (!obstacleEventMap.containsKey(obstacleId)) {

                    Event event = new Event(utcSeries.get(i), utcSeries.get(i), i, i, super.definition.getId(), marked.getTotalDistance());
                    obstacleMap.put(obstacleId, marked);
                    obstacleEventMap.put(obstacleId, event);
                    obstacleLastUpdateMap.put(obstacleId, utcSeries.get(i));
                }

                // If they are tracked, update their end time
                else {
                    Event event = obstacleEventMap.get(obstacleId);
                    event.updateEnd(utcSeries.get(i), i);
                    obstacleEventMap.put(obstacleId, event);
                    obstacleLastUpdateMap.put(obstacleId, utcSeries.get(i));

                    // If their current position is smaller than the inital position, update it as well
                    if (marked.getTotalDistance() < event.getSeverity()) {

                        event = new Event(event.getStartTime(), event.getEndTime(), event.getStartLine(), 
                                event.getEndLine(), event.getEventDefinitionId(), marked.getTotalDistance());

                        obstacleEventMap.put(obstacleId, event);
                        obstacleMap.put(obstacleId, marked);

                    }
                }
            }

            // Untrack the obstacle events that has not been updated
            
            ArrayList<Integer> copyOfAllObstacleIDs = new ArrayList<>();
            copyOfAllObstacleIDs.addAll(obstacleEventMap.keySet());

            for (Integer obstacleId : copyOfAllObstacleIDs) {
                
                double diff = Duration.between(Instant.parse(obstacleLastUpdateMap.get(obstacleId)), Instant.parse(utcSeries.get(i))).toMillis() / 1000.0;
                if (diff > OBSTACLE_SCAN_BUFFER_SECONDS) {

                    // Remove from the tracked hashmaps
                    obstacleLastUpdateMap.remove(obstacleId);
                    MarkedObstacle marked = obstacleMap.remove(obstacleId);
                    Event event = obstacleEventMap.remove(obstacleId);

                    // Update the Event with metadata
                    event.addMetaData(new EventMetaData(EventMetaData.EventMetaDataKey.LATERAL_DISTANCE, marked.getHorizontalDistance()));
                    event.addMetaData(new EventMetaData(EventMetaData.EventMetaDataKey.VERTICAL_DISTANCE, marked.getVerticalDistance()));
                    
                    // Add to untrackedObstacle list
                    untrackedObstacleEvents.put(event, obstacleId);
                }
            }
            
            // Insert all of the untracked obstacles into database
            insertObstacleEvents(connection, untrackedObstacleEvents);
            insertedEvents += untrackedObstacleEvents.size();
            untrackedObstacleEvents.clear();
        }

        // For all remaining events that are left over from the end, add them all
        ArrayList<Integer> remainingObstacles = new ArrayList<>();
        remainingObstacles.addAll(obstacleEventMap.keySet());

        for (Integer obstacleID : remainingObstacles) {
            MarkedObstacle marked = obstacleMap.remove(obstacleID);
            Event event = obstacleEventMap.remove(obstacleID);
            event.addMetaData(new EventMetaData(EventMetaData.EventMetaDataKey.LATERAL_DISTANCE, marked.getHorizontalDistance()));
            event.addMetaData(new EventMetaData(EventMetaData.EventMetaDataKey.VERTICAL_DISTANCE, marked.getVerticalDistance()));
            untrackedObstacleEvents.put(event, obstacleID);
        }
        insertObstacleEvents(connection, untrackedObstacleEvents);
        insertedEvents += untrackedObstacleEvents.size();

        LOG.info("Obstacle Events Inserted: " + (insertedEvents));
        return new ArrayList<>();
    }

    private void insertObstacleEvents(Connection connection, HashMap<Event, Integer> untrackedObstacleEvents){
        
        if (untrackedObstacleEvents.size() == 0) {return;}

        ArrayList<Event> allEvents = new ArrayList<>();
        allEvents.addAll(untrackedObstacleEvents.keySet());

        // Insert the obstacle events into db
        try {
            Event.batchInsertion(connection, this.flight, allEvents);
        } catch (SQLException | IOException e) {
            LOG.warning("Unable to insert obstacle events into database through connection.");
            e.printStackTrace();
        }

        String sql = """
                INSERT INTO obstacle_event_keys (event_id, obstacle_id)
                VALUES (?, ?)
                """;

        // Insert the obstacle event keys into db
        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            for (Event event: allEvents) {
                preparedStatement.setInt(1, event.getId());
                preparedStatement.setDouble(2, untrackedObstacleEvents.get(event));
                preparedStatement.addBatch();
            }
            preparedStatement.executeBatch();
        } catch (SQLException e) {
            LOG.warning("Unable to insert obstacle events keys into database through connection.");
            e.printStackTrace();
        }
    }
  
    @Override
    public List<Event> scan(Map<String, DoubleTimeSeries> doubleTimeSeries, Map<String, StringTimeSeries> stringTimeSeries) throws SQLException {

        try (Connection connection = Database.getConnection()) {
            return processObstacles(connection, doubleTimeSeries, stringTimeSeries);
        }
            
    }



}
