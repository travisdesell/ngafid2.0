--liquibase formatted sql

--changeset josh:visited-airports labels:flights
CREATE TABLE visited_airports (
    fleet_id INT NOT NULL,
    airport VARCHAR(8),

    PRIMARY KEY (fleet_id, airport),
    FOREIGN KEY (fleet_id) REFERENCES fleet(id)
);

--changeset josh:visited-runways labels:flights
CREATE TABLE visited_runways (
    fleet_id INT NOT NULL,
    runway VARCHAR(26),

    PRIMARY KEY (fleet_id, runway),
    FOREIGN KEY (fleet_id) REFERENCES fleet(id)
);
