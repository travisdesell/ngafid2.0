--liquibase formatted sql

--changeset josh:airframes labels:flights,airframes
CREATE TABLE airframes (
    id INT NOT NULL AUTO_INCREMENT,
    airframe VARCHAR(64),

    PRIMARY KEY(id),
    UNIQUE  KEY(airframe)
);

--changeset josh:airframes-static labels:flights,airframes
INSERT INTO airframes SET airframe = 'PA-28-181';
INSERT INTO airframes SET airframe = 'Cessna 172S';
INSERT INTO airframes SET airframe = 'PA-44-180';
INSERT INTO airframes SET airframe = 'Cirrus SR20';

--changeset josh:fleet-airframes labels:flights,airframes
CREATE TABLE fleet_airframes (
    fleet_id INT NOT NULL,
    airframe_id INT NOT NULL,

    PRIMARY KEY(fleet_id, airframe_id)
);

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


--changeset josh:sim-aircraft labels:flights,airframes
CREATE TABLE sim_aircraft (
    id INT NOT NULL AUTO_INCREMENT,
    fleet_id INT NOT NULL,
    path VARCHAR(2048) NOT NULL,

    PRIMARY KEY(id),
    FOREIGN KEY(fleet_id) REFERENCES fleet(id)
);
