package org.ngafid.processor.events;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import java.sql.Connection;

import org.ngafid.core.Database;
import org.ngafid.core.event.Event;
import org.ngafid.core.event.EventDefinition;
import org.ngafid.core.flights.DoubleTimeSeries;
import org.ngafid.core.flights.Flight;
import org.ngafid.core.flights.Parameters;
import org.ngafid.core.flights.StringTimeSeries;
import org.ngafid.core.obstacles.MarkedObstacle;
import org.ngafid.core.obstacles.Obstacle;
import org.ngafid.core.obstacles.Obstacles;
import org.ngafid.processor.events.proximity.FlightTimeLocation;

import java.util.logging.Logger;

public class ObstacleEventScanner extends AbstractEventScanner {

    private static Logger LOG = Logger.getLogger(ObstacleEventScanner.class.getName());
    private static double MAX_DETECTION_DISTANCE = 1200;

    public ObstacleEventScanner(EventDefinition eventDefinition) {
        super(eventDefinition);
    }

    @Override
    protected List<String> getRequiredDoubleColumns() {
        return List.of(Parameters.ALT_MSL, Parameters.ALT_AGL, Parameters.LATITUDE, Parameters.LONGITUDE);
    }

    @Override
    public List<Event> scan(Map<String, DoubleTimeSeries> doubleTimeSeries, Map<String, StringTimeSeries> stringTimeSeries) throws SQLException {

        ArrayList<Event> allEvents = new ArrayList<>();

        StringTimeSeries utcSeries = stringTimeSeries.get(Parameters.UTC_DATE_TIME);
        DoubleTimeSeries altAGL = doubleTimeSeries.get(Parameters.ALT_AGL);
        DoubleTimeSeries lat = doubleTimeSeries.get(Parameters.LATITUDE);
        DoubleTimeSeries lon = doubleTimeSeries.get(Parameters.LONGITUDE);

        HashMap<Integer, Event> tempObstacleIDMap = new HashMap<>();

        for (int i = 0; i < lat.size(); i++) {
            ArrayList<MarkedObstacle> nearbyObstacles = Obstacles.getNearbyObstaclesWithin(lat.get(i), lon.get(i), altAGL.get(i), MAX_DETECTION_DISTANCE);

            for (int n = 0; n < nearbyObstacles.size(); n++) {
                MarkedObstacle marked = nearbyObstacles.get(n);
                int obstacleId = marked.getObstacleID();
                if (!tempObstacleIDMap.containsKey(obstacleId)) {
                    tempObstacleIDMap.put(obstacleId, new Event(utcSeries.get(i), utcSeries.get(i), i, i, super.definition.getId(), marked.getTotalDistance()));
                }
                else {
                    Event event = tempObstacleIDMap.get(obstacleId);
                    event.updateEnd(utcSeries.get(i), i);
                }
            }
        }
        
        return allEvents;
            
    }
    
}
