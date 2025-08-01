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
    lateral_distance DOUBLE,
    vertical_distance DOUBLE,
    airspeed DOUBLE,
    total_fuel DOUBLE,
    pitch DOUBLE,
    lat_ac DOUBLE,
    norm_ac DOUBLE,
    roll DOUBLE,
    v_spd DOUBLE,
    alt_msl DOUBLE,
    e1_cht1 DOUBLE,
    e1_cht2 DOUBLE,
    e1_cht3 DOUBLE,
    e1_cht4 DOUBLE,
    e1_cht5 DOUBLE,
    e1_cht6 DOUBLE,
    e2_cht1 DOUBLE,
    e1_oil_p DOUBLE,
    e1_rpm DOUBLE,
    e2_rpm DOUBLE,
    e2_oil_p DOUBLE,
    e1_cht_divergence DOUBLE,
    e1_egt_divergence DOUBLE,
    e2_egt_divergence DOUBLE,
    battery_percent DOUBLE,
    batt_charging DOUBLE,
    gps_health DOUBLE,
    
    PRIMARY KEY(id),
    INDEX(event_id),
    INDEX(flight_id),
    FOREIGN KEY(event_id) REFERENCES events(id)
        ON DELETE CASCADE,
    FOREIGN KEY(flight_id) REFERENCES flights(id)
        ON DELETE CASCADE
);