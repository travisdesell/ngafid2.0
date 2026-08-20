package org.ngafid.core.obstacles;

public class FixedWingMarkedObstacle extends MarkedObstacle {

    public FixedWingMarkedObstacle(Obstacle obstacle, Double totalDistanceFt, Double horizontalDistanceFt,
            Double verticalDistanceFt) {
        super(obstacle, totalDistanceFt, horizontalDistanceFt, verticalDistanceFt);
    }

    @Override
    public ObstacleRisk calculateRiskFromPoint(double horizontalDistance, double verticalDistance) {
        if ((horizontalDistance <= 500) 
            && (verticalDistance < 500)) {
            return ObstacleRisk.HIGH;
        }
        else if ((500 < horizontalDistance) && (horizontalDistance <= 1250)
            && (verticalDistance < 500)) {
            return ObstacleRisk.MEDIUM;
        }
        else if ((1250 < horizontalDistance) && (horizontalDistance <= 2000)
            && (verticalDistance < 500)) {
            return ObstacleRisk.LOW;
        }
        else {
            return ObstacleRisk.NONE;
        }
    }
    
}
