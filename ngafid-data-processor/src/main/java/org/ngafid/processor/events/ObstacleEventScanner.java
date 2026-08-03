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
import org.ngafid.core.obstacles.Obstacle;
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

        ArrayList<Event> allEvents = new ArrayList<>();

        StringTimeSeries utcSeries = stringTimeSeries.get(Parameters.UTC_DATE_TIME);
        DoubleTimeSeries altAGL = doubleTimeSeries.get(Parameters.ALT_AGL);
        DoubleTimeSeries lat = doubleTimeSeries.get(Parameters.LATITUDE);
        DoubleTimeSeries lon = doubleTimeSeries.get(Parameters.LONGITUDE);

        HashMap<Integer, MarkedObstacle> obstacleMap = new HashMap<>();
        HashMap<Integer, Event> obstacleEventMap = new HashMap<>();
        HashMap<Integer, String> obstacleLastUpdateMap = new HashMap<>(); 
        HashMap<Integer, Integer> eventIDToObstacleID = new HashMap<>();
        
        int obstacleCount = 0;

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
                    eventIDToObstacleID.put(event.getId(), obstacleId);
                    System.out.println(event.getId() + " : " + obstacleId);
                }

                // If they are tracked, update their end time
                else {
                    Event event = obstacleEventMap.get(obstacleId);
                    event.updateEnd(utcSeries.get(i), i);
                    obstacleEventMap.put(obstacleId, event);
                    obstacleLastUpdateMap.put(obstacleId, utcSeries.get(i));

                    // If their current position is smaller than the inital position, update it as well
                    if (marked.getTotalDistance() < event.getSeverity()) {

                        eventIDToObstacleID.remove(event.getId());

                        event = new Event(event.getStartTime(), event.getEndTime(), event.getStartLine(), 
                                event.getEndLine(), event.getEventDefinitionId(), marked.getTotalDistance());

                        obstacleEventMap.put(obstacleId, event);
                        obstacleMap.put(obstacleId, marked);
                        eventIDToObstacleID.put(event.getId(), obstacleId);

                    }
                }
            }

            // Untrack the obstacle events that has not been updated
            ArrayList<Integer> remainingObstacles = new ArrayList<>();
            remainingObstacles.addAll(obstacleEventMap.keySet());
            allEvents.addAll(untrackObstacles(remainingObstacles, obstacleMap, obstacleEventMap, obstacleLastUpdateMap, utcSeries.get(i)));
            
            // Insert all of the untracked obstacles into database
            // try {
            //     // insertObstacleEvents(connection, allEvents, eventIDToObstacleID);
            //     Event.batchInsertion(connection, this.flight, allEvents);
            //     allEvents.clear();
            // } catch (SQLException | IOException e) {
            //     LOG.warning("Unable to insert obstacle events into database through connection.");
            //     e.printStackTrace();
            // }
            

            insertObstacleEvents(connection, allEvents, eventIDToObstacleID);
            allEvents.clear();
        }

        // For all remaining events that are left over from the end, add them all and return them back for flightBuilder to insert
        // the rest of the events
        ArrayList<Integer> remainingObstacles = new ArrayList<>();
        remainingObstacles.addAll(obstacleEventMap.keySet());

        for (Integer obstacleID : remainingObstacles) {
            MarkedObstacle marked = obstacleMap.remove(obstacleID);
            Event event = obstacleEventMap.remove(obstacleID);
            event.addMetaData(new EventMetaData(EventMetaData.EventMetaDataKey.LATERAL_DISTANCE, marked.getHorizontalDistance()));
            event.addMetaData(new EventMetaData(EventMetaData.EventMetaDataKey.VERTICAL_DISTANCE, marked.getVerticalDistance()));
            allEvents.add(event);
        }

        LOG.info("Obstacle Events Inserted: " + (obstacleCount + allEvents.size()));
        return allEvents;
    }

    private ArrayList<Event> untrackObstacles(Collection<Integer> trackedIDs, 
        HashMap<Integer, MarkedObstacle> obstacleMap,
        HashMap<Integer, Event> obstacleEventMap,
         HashMap<Integer, String> obstacleLastUpdateMap,
        String currentTime) {

        ArrayList<Event> untrackedEvents = new ArrayList<>();

        // Loop through all of the tracked obstacle events & check if any of them should be inserted
        for (int trackedID : trackedIDs) {
            
            // If their last updated time is greater than the obstacle scan buffer, the event is no longer tracked
            double diff = Duration.between(Instant.parse(obstacleLastUpdateMap.get(trackedID)), Instant.parse(currentTime)).toMillis() / 1000.0;
            if (diff > OBSTACLE_SCAN_BUFFER_SECONDS) {

                // Remove from the tracked hashmaps
                obstacleLastUpdateMap.remove(trackedID);
                MarkedObstacle marked = obstacleMap.remove(trackedID);
                Event event = obstacleEventMap.remove(trackedID);

                // Update the Event with metadata
                event.addMetaData(new EventMetaData(EventMetaData.EventMetaDataKey.LATERAL_DISTANCE, marked.getHorizontalDistance()));
                event.addMetaData(new EventMetaData(EventMetaData.EventMetaDataKey.VERTICAL_DISTANCE, marked.getVerticalDistance()));
                untrackedEvents.add(event);
            }
        }

        return untrackedEvents;
    }

    private void insertObstacleEvents(Connection connection, ArrayList<Event> events, HashMap<Integer, Integer> eventIDToObstacleID){
        
        if (events.size() == 0) {return;}

        // System.out.println(">>> Inserting obstacle events of size " + events.size() + "into database");
        // Insert the obstacle events into db
        try {
            Event.batchInsertion(connection, this.flight, events);
        } catch (SQLException | IOException e) {
            LOG.warning("Unable to insert obstacle events into database through connection.");
            e.printStackTrace();
        }

        String sql = """
                INSERT INTO obstacle_event_keys (event_id, obstacle_id)
                VALUES (?, ?)
                """;

        // LOG.info(">>> Inserting obstacle event keys into db");
        // System.out.println(">>> Inserting obstacle event keys into db");
        // Insert the obstacle event keys into db
        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            for (Event event: events) {
                preparedStatement.setInt(1, event.getId());
                preparedStatement.setDouble(2, eventIDToObstacleID.get(event.getId()));
                preparedStatement.addBatch();
                LOG.info(">>> Inserting event " + event.getId() + " with Obstacle" + eventIDToObstacleID.get(event.getId()));
            }
            preparedStatement.executeBatch();
        } catch (SQLException e) {
            LOG.warning("Unable to insert obstacle events keys into database through connection.");
            e.printStackTrace();
        }
    }

    public static <K, V> String mapToReadableString(Map<K, V> map) {
    if (map == null || map.isEmpty()) {
        return "{}";
    }

    StringBuilder sb = new StringBuilder();
    sb.append("{\n");

    for (Map.Entry<K, V> entry : map.entrySet()) {
        sb.append("  ")
          .append(entry.getKey())
          .append(" -> ")
          .append(entry.getValue())
          .append("\n");
    }

    sb.append("}");

    return sb.toString();
}

    public static <T> String setToReadableString(Set<T> set) {
    if (set == null || set.isEmpty()) {
        return "{}";
    }

    StringBuilder sb = new StringBuilder();
    sb.append("{\n");

    for (T item : set) {
        sb.append("  ")
          .append(item)
          .append("\n");
    }

    sb.append("}");

    return sb.toString();
}

    @Override
    public List<Event> scan(Map<String, DoubleTimeSeries> doubleTimeSeries, Map<String, StringTimeSeries> stringTimeSeries) throws SQLException {

        try (Connection connection = Database.getConnection()) {
            return processObstacles(connection, doubleTimeSeries, stringTimeSeries);
        }
            
    }



}
