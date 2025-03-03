--liquibase formatted sql

--changeset josh:airframe-types labels:flights,airframes
CREATE TABLE airframe_types (
    id INT NOT NULL AUTO_INCREMENT,
    name VARCHAR(32) NOT NULL,

    PRIMARY KEY(id),
    UNIQUE KEY name_key (name)
);

--changeset josh:static-airframe-types labels:flights,airframes
INSERT INTO airframe_types SET name = 'Fixed Wing';
INSERT INTO airframe_types SET name = 'Rotorcraft';
INSERT INTO airframe_types SET name = 'UAS Fixed Wing';
INSERT INTO airframe_types SET name = 'UAS Rotorcraft';

--changeset josh:airframes labels:flights,airframes
CREATE TABLE airframes (
    id INT NOT NULL AUTO_INCREMENT,
    airframe VARCHAR(64) NOT NULL,
    type_id INT NOT NULL,

    PRIMARY KEY(id),
    UNIQUE  KEY(airframe),
    FOREIGN KEY(type_id) REFERENCES airframe_types(id)
);

--changeset josh:airframes-static labels:flights,airframes

INSERT INTO airframes SET airframe = 'PA-28-181', type_id = 1;
INSERT INTO airframes SET airframe = 'Cessna 172S', type_id = 1;
INSERT INTO airframes SET airframe = 'PA-44-180', type_id = 1;
INSERT INTO airframes SET airframe = 'Cirrus SR20', type_id = 1;

--changeset josh:airframes-static-unknown labels:flights,airframes
INSERT INTO airframes SET airframe = 'Unknown', type_id = 1;


--changeset josh:fleet-airframes labels:flights,airframes
CREATE TABLE fleet_airframes (
    fleet_id INT NOT NULL,
    airframe_id INT NOT NULL,

    PRIMARY KEY(fleet_id, airframe_id)
);

--changeset josh:sim-aircraft labels:flights,airframes
CREATE TABLE sim_aircraft (
    id INT NOT NULL AUTO_INCREMENT,
    fleet_id INT NOT NULL,
    path VARCHAR(2048) NOT NULL,

    PRIMARY KEY(id),
    FOREIGN KEY(fleet_id) REFERENCES fleet(id)
);

--changeset josh:flight-data-recorder labels:flights,airframes
CREATE TABLE flight_data_recorders (
    id INT NOT NULL AUTO_INCREMENT,
    name VARCHAR(256),

    PRIMARY KEY (id),
    UNIQUE KEY (name),
    INDEX(name)
);

INSERT INTO flight_data_recorders SET name = 'G1000';
INSERT INTO flight_data_recorders SET name = 'G5';
INSERT INTO flight_data_recorders SET name = 'G3X';
INSERT INTO flight_data_recorders SET name = 'Bad Elf 2200';
INSERT INTO flight_data_recorders SET name = 'ScanEagle';
INSERT INTO flight_data_recorders SET name = 'DJI';
INSERT INTO flight_data_recorders SET name = 'Parrot SkyController';