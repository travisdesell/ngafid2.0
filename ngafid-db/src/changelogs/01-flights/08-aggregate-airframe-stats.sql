--liquibase formatted sql

--changeset yourname:v_aggregate_flight_hours_by_airframe labels:flights,aggregate
CREATE TABLE v_aggregate_flight_hours_by_airframe (
    airframe_id INT NOT NULL,
    num_flights INT NOT NULL DEFAULT 0,
    total_flight_hours DOUBLE NOT NULL DEFAULT 0,
    PRIMARY KEY (airframe_id),
    FOREIGN KEY (airframe_id) REFERENCES airframes(id)
        ON DELETE CASCADE
); 