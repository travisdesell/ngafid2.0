--liquibase formatted sql

--changeset josh:itinerary labels:flights
CREATE TABLE itinerary (
    id INT NOT NULL AUTO_INCREMENT,
    flight_id INT NOT NULL,
    `order` INT NOT NULL,
    min_altitude_index INT NOT NULL,
    min_altitude double,
    min_airport_distance double,
    min_runway_distance double,
    airport VARCHAR(8),
    runway VARCHAR(16),
    start_of_approach INT NOT NULL,
    end_of_approach INT NOT NULL,
    start_of_takeoff INT NOT NULL,
    end_of_takeoff INT NOT NULL,
    type VARCHAR(32),

    PRIMARY KEY(id),
    INDEX(airport),
    INDEX(runway),
    FOREIGN KEY(flight_id) REFERENCES flights(id)
);
