--liquibase formatted sql

-- changeset josh:airsync labels:accounts,airsync
CREATE TABLE airsync_fleet_info (
    fleet_id INT NOT NULL, 
    airsync_fleet_name TEXT NOT NULL,
    api_key varchar(32) NOT NULL,
    api_secret varchar(64) NOT NULL,
    last_upload_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    timeout INT DEFAULT NULL,
    override tinyint(4) DEFAULT NULL,

    PRIMARY KEY(fleet_id),
    FOREIGN KEY(fleet_id) REFERENCES fleet(id)
);

-- Node that the id here is not an auto-generated key, it is the id that airsync provides.
CREATE TABLE airsync_imports (
    id INT NOT NULL,
    upload_id INT NOT NULL,
    fleet_id INT NOT NULL,
    flight_id INT DEFAULT NULL,
    tail varchar(512) NOT NULL,
    time_received TIMESTAMP NULL DEFAULT NULL,

    PRIMARY KEY (id),
    FOREIGN KEY (fleet_id)    REFERENCES fleet (id)
);
