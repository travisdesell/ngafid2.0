--liquibase formatted sql

--changeset josh:series-names labels:flights,series
CREATE TABLE double_series_names (
    id INT NOT NULL AUTO_INCREMENT,
    name VARCHAR(64) NOT NULL,

    PRIMARY KEY(id),
    UNIQUE KEY(name)
);
CREATE TABLE string_series_names (
    id INT NOT NULL AUTO_INCREMENT,
    name VARCHAR(64) NOT NULL,

    PRIMARY KEY(id),
    UNIQUE KEY(name)
);

ALTER TABLE user_preferences_metrics ADD CONSTRAINT fk_metric_id
    FOREIGN KEY (metric_id) REFERENCES double_series_names(id);
    
--changeset josh:data-type-names labels:flights,series
CREATE TABLE data_type_names (
    id INT NOT NULL AUTO_INCREMENT,
    name VARCHAR(64) NOT NULL,

    PRIMARY KEY(id),
    UNIQUE KEY(name)
);

--changeset josh:double-series labels:flights,series
CREATE TABLE double_series (
    id              INT         NOT NULL AUTO_INCREMENT,
    flight_id       INT         NOT NULL,
    name_id         INT         NOT NULL,
    data_type_id    INT         NOT NULL,
    length          INT         NOT NULL,
    valid_length    INT         NOT NULL,
    min             double,
    avg             double,
    max             double,
    data            MEDIUMBLOB,

    PRIMARY KEY(id),
    INDEX(flight_id),
    INDEX(name_id),
    FOREIGN KEY(flight_id) REFERENCES flights(id)
        ON DELETE CASCADE,
    FOREIGN KEY(name_id) REFERENCES double_series_names(id),
    FOREIGN KEY(data_type_id) REFERENCES data_type_names(id)
);

--changeset josh:string-series labels:flights,series
CREATE TABLE string_series (
    id INT NOT NULL AUTO_INCREMENT,
    flight_id INT NOT NULL,
    name_id INT NOT NULL,
    data_type_id INT NOT NULL,
    length INT NOT NULL,
    valid_length INT NOT NULL,
    data MEDIUMBLOB,

    PRIMARY KEY(id),
    INDEX(flight_id),
    INDEX(name_id),
    FOREIGN KEY(flight_id) REFERENCES flights(id)
        ON DELETE CASCADE,
    FOREIGN KEY(name_id) REFERENCES string_series_names(id),
    FOREIGN KEY(data_type_id) REFERENCES data_type_names(id)
);


