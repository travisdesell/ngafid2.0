--liquibase formatted sql

--changeset josh:flights labels:flights
CREATE TABLE flights (
    id INT NOT NULL AUTO_INCREMENT,
    fleet_id INT NOT NULL,
    uploader_id INT NOT NULL,
    upload_id INT NOT NULL,
    system_id VARCHAR(128) NOT NULL,
    airframe_id INT NOT NULL,
    airframe_type_id INT NOT NULL,
    start_time DATETIME,
    end_time DATETIME,
    start_timestamp INT,
    end_timestamp INT,
    time_offset VARCHAR(6),
    min_latitude DOUBLE,
    max_latitude DOUBLE,
    min_longitude DOUBLE,
    max_longitude DOUBLE,
    filename VARCHAR(256),
    md5_hash VARCHAR(32),
    number_rows INT,
    status VARCHAR(32) NOT NULL,
    has_coords TINYINT(1) NOT NULL,
    has_agl TINYINT(1) NOT NULL,
    events_calculated INT(1) NOT NULL DEFAULT 0,
    insert_completed INT(1) NOT NULL DEFAULT 0,
    processing_status BIGINT(20) default 0,

    PRIMARY KEY (id),
    UNIQUE KEY (fleet_id, md5_hash),
    INDEX (fleet_id),
    INDEX (uploader_id),
    INDEX (system_id),
    INDEX (airframe_id),
    INDEX (start_time),
    INDEX (end_time),
    INDEX (start_timestamp),
    INDEX (end_timestamp),
    FOREIGN KEY (fleet_id) REFERENCES fleet(id),
    FOREIGN KEY (uploader_id) REFERENCES user(id),
    FOREIGN KEY (airframe_id) REFERENCES airframes(id),
    FOREIGN KEY (airframe_type_id) REFERENCES airframe_types(id),
    FOREIGN KEY (fleet_id, system_id) REFERENCES tails(fleet_id, system_id)
);

--changeset josh:airsync-imports-flight-fk labels:flights
ALTER TABLE airsync_imports ADD CONSTRAINT fk_airsync_imports_flight_id
    FOREIGN KEY (flight_id) REFERENCES flights(id);

--changeset josh:flight-warnings-flight-fk labels:flights
ALTER TABLE flight_warnings ADD CONSTRAINT fk_flight_warnings_flight_id
    FOREIGN KEY (flight_id) REFERENCES flights(id);

--changeset josh:flights-processed labels:flights
CREATE TABLE flight_processed (
    fleet_id INT NOT NULL,
    flight_id INT NOT NULL,
    event_definition_id INT NOT NULL,
    count INT DEFAULT 0,
    sum_duration DOUBLE DEFAULT 0.0,
    min_duration DOUBLE DEFAULT 1e500,
    max_duration DOUBLE DEFAULT -1e500,
    sum_severity DOUBLE DEFAULT 0.0,
    min_severity DOUBLE DEFAULT 1e500,
    max_severity DOUBLE DEFAULT -1e500,
    had_error TINYINT(1) DEFAULT 0,

    PRIMARY KEY(flight_id, event_definition_id),
    INDEX (fleet_id),
    INDEX (count),
    INDEX (had_error),
    FOREIGN KEY (fleet_id) REFERENCES fleet(id),
    FOREIGN KEY (flight_id) REFERENCES flights(id)
);

--changeset josh:flight-tag-map labels:flights,messages
CREATE TABLE flight_tag_map (
    flight_id INT NOT NULL,
    tag_id INT NOT NULL,

     UNIQUE KEY (flight_id, tag_id),
    FOREIGN KEY (flight_id) REFERENCES flights(id),
    FOREIGN KEY (tag_id)    REFERENCES flight_tags(id)
);
