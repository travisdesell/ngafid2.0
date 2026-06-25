package org.ngafid.processor.steps;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Set;

import static org.ngafid.core.flights.Parameters.*;
import java.util.logging.Logger;
import org.ngafid.core.flights.Parameters.Unit;
import org.ngafid.core.obstacles.Obstacle;
import org.ngafid.core.obstacles.Obstacles;
import org.ngafid.processor.format.FlightBuilder;
import org.ngafid.processor.steps.ComputeStep;
import org.ngafid.core.flights.StringTimeSeries;
import org.apache.commons.lang3.mutable.MutableDouble;
import org.ngafid.core.flights.DoubleTimeSeries;
import org.ngafid.core.flights.FatalFlightFileException;
import org.ngafid.core.flights.MalformedFlightFileException;

public class ComputeObstacleProximity extends ComputeStep {
    private static Logger LOG = Logger.getLogger(ComputeObstacleProximity.class.getName());
    private static final double MAX_OBSTACLES_DISTANCE_FT = 1200;
    private static final Set<String> REQUIRED_DOUBLE_COLUMNS = Set.of(LATITUDE, LONGITUDE, ALT_AGL);
    private static final Set<String> OUTPUT_COLUMNS =
            Set.of(NEAREST_OBSTACLE, OBSTACLE_DISTANCE);


            public ComputeObstacleProximity(Connection connection, FlightBuilder builder) {
        super(connection, builder);
    }
    
    @Override
    public Set<String> getRequiredDoubleColumns() {
        return REQUIRED_DOUBLE_COLUMNS;
    }

    @Override
    public Set<String> getRequiredStringColumns() {
        return Collections.emptySet();
    }

    @Override
    public Set<String> getRequiredColumns() {
        return REQUIRED_DOUBLE_COLUMNS;
    }

    @Override
    public Set<String> getOutputColumns() {
        return OUTPUT_COLUMNS;
    }

    @Override
    public void compute() throws SQLException, MalformedFlightFileException, FatalFlightFileException {
        DoubleTimeSeries latitudeTS = builder.getDoubleTimeSeries(LATITUDE);
        DoubleTimeSeries longitudeTS = builder.getDoubleTimeSeries(LONGITUDE);
        DoubleTimeSeries altitudeAGLTS = builder.getDoubleTimeSeries(ALT_AGL);

        int sizeHint = latitudeTS.size();

        DoubleTimeSeries nearestObstacleTS = new DoubleTimeSeries(NEAREST_OBSTACLE, Unit.OBSTACLE_ID, sizeHint);
        DoubleTimeSeries obstacleDistanceTS = new DoubleTimeSeries(OBSTACLE_DISTANCE, Unit.FT, sizeHint);

        LOG.info("Obstacle Compute Proximity Class was ran");

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
                nearestObstacleTS.add(Double.valueOf(obstacle.getID()));
                obstacleDistanceTS.add(obstacleDistance.getValue());
            }
        }

        builder.addTimeSeries(NEAREST_OBSTACLE, nearestObstacleTS);
        builder.addTimeSeries(OBSTACLE_DISTANCE, obstacleDistanceTS);
    }

}
