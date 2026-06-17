package main.java.org.ngafid.processor.steps;

import java.sql.Connection;
import java.sql.SQLException;

import static org.ngafid.core.flights.Parameters.*;

import org.ngafid.core.flights.Parameters.Unit;
import org.ngafid.core.obstacles.Obstacles;
import org.ngafid.processor.format.FlightBuilder;
import org.ngafid.processor.steps.ComputeStep;
import org.ngafid.processor.steps.FatalFlightFileException;
import org.ngafid.processor.steps.MalformedFlightFileException;
import org.ngafid.core.flights.StringTimeSeries;
import org.ngafid.core.flights.DoubleTimeSeries;

public class ComputeObstacleProximity extends ComputeStep {

    private static final double MAX_OBSTACLES_DISTANCE_FT = 1200;

    public ComputeObstacleProximity(Connection connection, FlightBuilder builder) {
        super(connection, builder);
    }

    @Override @Override
    public void compute() throws SQLException, MalformedFlightFileException, FatalFlightFileException {
        DoubleTimeSeries latitudeTS = builder.getDoubleTimeSeries(LATITUDE);
        DoubleTimeSeries longitudeTS = builder.getDoubleTimeSeries(LONGITUDE);
        DoubleTimeSeries altitudeAGLTS = builder.getDoubleTimeSeries(ALT_AGL);

        int sizeHint = latitudeTS.size();

        DoubleTimeSeries nearestObstacleTS = new DoubleTimeSeries(NEAREST_OBSTACLE, Unit.OBSTACLE_ID, sizeHint);
        DoubleTimeSeries obstacleDistanceTS = new DoubleTimeSeries(OBSTACLE_DISTANCE, Unit.FT, sizeHint);

        for (int i = 0; i < latitudeTS.size(); i++) {
            double latitude = latitudeTS.get(i);
            double longitude = longitudeTS.get(i);
            double altitudeAGL = altitudeAGLTS.get(i);

            MutableDouble obstacleDistance = new MutableDouble();
            Obstacle obstacle = Obstacles.getNearestObstacleWithin(latitude, longitude,MAX_OBSTACLES_DISTANCE_FT, obstacleDistance);
        
            if (obstacle == null) {
                nearestObstacleTS.add(Double.NaN);
                obstacleDistanceTS.add(Double.NaN);
            } else {
                nearestObstacleTS.add((Double) obstacle.getID());
                obstacleDistanceTS.add(obstacleDistance.getValue());
            }
        }

        builder.addTimeSeries(NEAREST_OBSTACLE, nearestObstacleTS);
        builder.addTimeSeries(OBSTACLE_DISTANCE, obstacleDistanceTS);
    }
    
}
