package org.ngafid.core.obstacles;

/**
 * Representation of the instance of when a flight passes by an obstacle. This is a intermediate class for obstacles before they
 * are parsed into events.
 */
public abstract class MarkedObstacle {
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

    /**
     * Describes the level of obstacle proximity risk associated with the obstacle 
     * ObstacleRisk
     */
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

    /**
     * Determine the obstacle proximity risk of the obstacle from a given point
     * @param horizontalDistance
     * @param verticalDistance
     * @return
     */
    abstract ObstacleRisk calculateRiskFromPoint(double horizontalDistance, double verticalDistance);


    @Override
    public String toString() {
        return "Obstacle " + obstacle.getID() + " " + totalDistanceFt + "Away";
    }
}
