package org.ngafid.core.obstacles;

/**
 * Representation of the instance of when a flight passes by an obstacle. This is a intermediate class for obstacles before they
 * are parsed into events.
 */
public class MarkedObstacle {
    private Obstacle obstacle;
    private Double totalDistanceFt;
    private Double horizontalDistanceFt;
    private Double verticalDistanceFt;
    private ObstacleRisk obstacleRisk;
    

    public MarkedObstacle(Obstacle obstacle, Double totalDistanceFt, Double horizontalDistanceFt, Double verticalDistanceFt) {
        this.obstacle = obstacle;
        this.totalDistanceFt = totalDistanceFt;
        this.horizontalDistanceFt = horizontalDistanceFt;
        this.verticalDistanceFt = verticalDistanceFt;
        this.obstacleRisk = calculateRiskFromPoint(horizontalDistanceFt, verticalDistanceFt);
    }

    public Obstacle getObstacle() {return this.obstacle;}
    public Double getTotalDistance() {return this.totalDistanceFt;}
    public Double getHorizontalDistance() {return this.horizontalDistanceFt;}
    public Double getVerticalDistance() {return this.verticalDistanceFt;}
    public ObstacleRisk getObstacleRisk() {return this.obstacleRisk;}
    public int getObstacleID() {return this.obstacle.getID();}

    public enum ObstacleRisk {
        HIGH("High"),
        MEDIUM("Medium"),
        LOW("Low"),
        NONE("None");

        private final String value;

        ObstacleRisk(String name) {
            value = name;
        }

        @Override
        public String toString() {
            return value;
        }
    }

    public static ObstacleRisk calculateRiskFromPoint(double horizontalDistance, double verticalDistance) {
        if ((horizontalDistance <= 500) || (verticalDistance <= 75)) {
            return ObstacleRisk.HIGH;
        }

        else if (((500 <= horizontalDistance) && (horizontalDistance <= 1000))
            && ((75 <= verticalDistance) && (verticalDistance <= 200))) {
            return ObstacleRisk.LOW;
        }

        else if (((horizontalDistance <= 1000) && ((75 <= verticalDistance) && (verticalDistance <= 200)))
            || ((500 <= horizontalDistance) && (horizontalDistance <= 1000)) && (verticalDistance <= 200)) {
            return ObstacleRisk.MEDIUM;
        }
        
        else {
            return ObstacleRisk.NONE;
        }
    }


    @Override
    public String toString() {
        return "Obstacle " + obstacle.getID() + " " + totalDistanceFt + "Away";
    }
}
