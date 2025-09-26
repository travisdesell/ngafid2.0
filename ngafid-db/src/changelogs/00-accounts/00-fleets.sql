--liquibase formatted sql

--changeset josh:fleet labels:accounts
CREATE TABLE fleet (
    id INT NOT NULL AUTO_INCREMENT,
    fleet_name VARCHAR(256),

    PRIMARY KEY (id)
);

--changeset josh:fleet-unique-name labels:accounts
ALTER TABLE fleet ADD UNIQUE (fleet_name);

--changeset josh:fleet-test labels:accounts
CREATE TABLE test (
    id INT NOT NULL AUTO_INCREMENT,
    PRIMARY KEY (id)
);

--changeset josh:tails labels:accounts
CREATE TABLE tails (
    system_id VARCHAR(64) NOT NULL,
    fleet_id INT NOT NULL,
    tail VARCHAR(16),
    confirmed BOOLEAN NOT NULL,

    PRIMARY KEY (fleet_id, system_id),
    INDEX (fleet_id),
    INDEX (tail),
    FOREIGN KEY (fleet_id) REFERENCES fleet(id)
)

--changeset josh:flight-tags labels:accounts
CREATE TABLE flight_tags (
    id INT NOT NULL AUTO_INCREMENT,
    fleet_id INT NOT NULL,
    name VARCHAR(128) NOT NULL,
    description VARCHAR(4096) NOT NULL,
    color VARCHAR(8) NOT NULL,

    PRIMARY KEY(id),
    UNIQUE KEY(fleet_id, name),
    FOREIGN KEY(fleet_id) REFERENCES fleet(id)
);

--changeset josh:stored-filters labels:accounts
CREATE TABLE stored_filters (
    fleet_id INT NOT NULL,
    name VARCHAR(512) NOT NULL,
    color VARCHAR(8) NOT NULL,
    filter_json VARCHAR(2048) NOT NULL,

    PRIMARY KEY(fleet_id,name),
    FOREIGN KEY(fleet_id) REFERENCES fleet(id)
);

--changeset aidan:multifleet-invites labels:accounts
CREATE TABLE multifleet_invites (
    email VARCHAR(128) NOT NULL,
    fleet_id INT NOT NULL,
    invited_by VARCHAR(128) NOT NULL,

    PRIMARY KEY (email, fleet_id),
    FOREIGN KEY (fleet_id) REFERENCES fleet(id)
);