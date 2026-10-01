package org.ngafid.core.obstacles;

public class RotercraftMarkedObstacle extends MarkedObstacle {

    public RotercraftMarkedObstacle(Obstacle obstacle, Double totalDistanceFt, Double horizontalDistanceFt,
            Double verticalDistanceFt) {
        super(obstacle, totalDistanceFt, horizontalDistanceFt, verticalDistanceFt);
    }

    public ObstacleRisk calculateRiskFromPoint(double horizontalDistance, double verticalDistance) {
        if ((horizontalDistance <= 500) && (verticalDistance <= 75)) {
            return ObstacleRisk.HIGH;
        }

        else if (((500 < horizontalDistance) && (horizontalDistance < 1000))
            && ((75 <= verticalDistance) && (verticalDistance <= 200))) {
            return ObstacleRisk.LOW;
        }

        else if (((horizontalDistance <= 500) && ((75 < verticalDistance) && (verticalDistance < 200)))
            || ((500 < horizontalDistance) && (horizontalDistance < 1000)) && (verticalDistance <= 75)) {
            return ObstacleRisk.MEDIUM;
        }
        
        else {
            return ObstacleRisk.NONE;
        }
    }
    
}
