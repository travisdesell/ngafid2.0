--liquibase formatted sql

--changeset josh:event-definitions labels:flights,events
CREATE TABLE event_definitions (
    id INT NOT NULL AUTO_INCREMENT,
    fleet_id INT NOT NULL,
    airframe_id INT NOT NULL,
    name VARCHAR(64) NOT NULL,
    start_buffer INT,
    stop_buffer INT,
    column_names TEXT,
    condition_json TEXT,
    severity_column_names TEXT,
    severity_type ENUM('MIN', 'MAX', 'MIN_ABS', 'MAX_ABS'),
    color VARCHAR(6) DEFAULT NULL,

    PRIMARY KEY(id),
    UNIQUE KEY(name, airframe_id, fleet_id)
);

ALTER TABLE flight_processed ADD CONSTRAINT fk_event_definition
    FOREIGN KEY (event_definition_id) REFERENCES event_definitions(id)
        ON DELETE CASCADE;

--changeset josh:events labels:flights,events
CREATE TABLE events (
    id INT NOT NULL AUTO_INCREMENT,
    fleet_id INT NOT NULL,
    flight_id INT NOT NULL,
    event_definition_id INT NOT NULL,
    other_flight_id INT DEFAULT -1,

    start_line INT,
    end_line INT,
    start_time datetime,
    end_time datetime,

    severity DOUBLE NOT NULL,

    min_latitude DOUBLE DEFAULT NULL,
    max_latitude DOUBLE DEFAULT NULL,
    min_longitude DOUBLE DEFAULT NULL,
    max_longitude DOUBLE DEFAULT NULL,

    PRIMARY KEY(id),
    FOREIGN KEY(fleet_id) REFERENCES fleet(id),
    FOREIGN KEY(flight_id) REFERENCES flights(id)
        ON DELETE CASCADE,
    FOREIGN KEY(other_flight_id) REFERENCES flights(id)
        ON DELETE CASCADE,
    INDEX(start_time),
    INDEX(end_time),
    INDEX(event_definition_id, flight_id),
    INDEX(event_definition_id, other_flight_id),
    FOREIGN KEY(event_definition_id) REFERENCES event_definitions(id)
        ON DELETE CASCADE
);


--changeset josh:turn-to-final labels:flights,events
CREATE TABLE turn_to_final (
    flight_id INT NOT NULL,
    version BIGINT NOT NULL,
    data MEDIUMBLOB,

    PRIMARY KEY(flight_id),
    FOREIGN KEY(flight_id) REFERENCES flights(id)
        ON DELETE CASCADE
);

--changeset josh:rate-of-closure labels:flights,events
CREATE TABLE rate_of_closure (
    id INT NOT NULL AUTO_INCREMENT,
    event_id INT NOT NULL,
    size INT NOT NULL,
    data MEDIUMBLOB,
    
    PRIMARY KEY(id),
    FOREIGN KEY(event_id) REFERENCES events(id)
        ON DELETE CASCADE
);

--changeset josh:event-metadata-keys labels:flights,events
CREATE TABLE event_metadata_keys (
    id INT NOT NULL AUTO_INCREMENT,
    name VARCHAR(512) NOT NULL,
    
    PRIMARY KEY(id)
);

--changeset josh:event-static-metadata-keys labels:flights,events
INSERT INTO event_metadata_keys (name) VALUES ('lateral_distance');
INSERT INTO event_metadata_keys (name) VALUES ('vertical_distance');

--changeset josh:event-metadata labels:flights,events
CREATE TABLE event_metadata (
    event_id INT NOT NULL,
    key_id INT NOT NULL,
    value DOUBLE NOT NULL,

    FOREIGN KEY(event_id) REFERENCES events(id)
        ON DELETE CASCADE,
    FOREIGN KEY(key_id) REFERENCES event_metadata_keys(id)
);