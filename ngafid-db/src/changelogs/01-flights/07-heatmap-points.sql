--liquibase formatted sql

--changeset roman:heatmap-points labels:flights,proximity
CREATE TABLE heatmap_points (
    id INT NOT NULL AUTO_INCREMENT,
    event_id INT NOT NULL,
    flight_id INT NOT NULL,
    latitude DOUBLE NOT NULL,
    longitude DOUBLE NOT NULL,
    timestamp DATETIME NOT NULL,
    altitude_agl DOUBLE,
    
    PRIMARY KEY(id),
    INDEX(event_id),
    INDEX(flight_id),
    FOREIGN KEY(event_id) REFERENCES events(id)
        ON DELETE CASCADE,
    FOREIGN KEY(flight_id) REFERENCES flights(id)
        ON DELETE CASCADE
);