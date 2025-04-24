--liquibase formatted sql

--changeset josh:fleet-monthly-event-counts labels:events,views,materialized-views context:daily-materialized-view runAlways:true
CREATE OR REPLACE VIEW
    v_fleet_airframe_monthly_event_counts AS
SELECT
    e.fleet_id                  as fleet_id,
    event_definition_id         as event_definition_id,
    flights.airframe_id         as airframe_id,
    YEAR(e.start_time)          as year,
    MONTH(e.start_time)         as month,
    COUNT(DISTINCT e.id)        as event_count,
    COUNT(DISTINCT flights.id)  as flight_count,
    MIN(end_line - start_line)  as min_duration,
    AVG(end_line - start_line)  as avg_duration,
    MAX(end_line - start_line)  as max_duration,
    MIN(severity)               as min_severity,
    AVG(severity)               as avg_severity,
    MAX(severity)               as max_severity
FROM
    events AS e
INNER JOIN
    flights ON flights.id = e.flight_id
GROUP BY
    fleet_id,
    event_definition_id,
    flights.airframe_id,
    YEAR(e.start_time),
    MONTH(e.start_time);

CREATE TABLE IF NOT EXISTS m_fleet_airframe_monthly_event_counts (
    fleet_id INT NOT NULL,
    event_definition_id INT NOT NULL,
    airframe_id INT NOT NULL,
    year INT NOT NULL,
    month INT NOT NULL,
    event_count INT NOT NULL,
    flight_count INT NOT NULL,
    min_duration DOUBLE NOT NULL,
    avg_duration DOUBLE NOT NULL,
    max_duration DOUBLE NOT NULL,
    min_severity DOUBLE NOT NULL,
    avg_severity DOUBLE NOT NULL,
    max_severity DOUBLE NOT NULL,

    PRIMARY KEY (year, month, fleet_id, event_definition_id, airframe_id),
    FOREIGN KEY (fleet_id) REFERENCES fleet(id),
    FOREIGN KEY (event_definition_id) REFERENCES event_definitions(id),
    FOREIGN KEY (airframe_id) REFERENCES airframes(id)
);

TRUNCATE TABLE m_fleet_airframe_monthly_event_counts;

INSERT INTO m_fleet_airframe_monthly_event_counts
    SELECT * FROM v_fleet_airframe_monthly_event_counts;

CREATE OR REPLACE VIEW
    v_fleet_monthly_event_counts AS
SELECT
    fleet_id,
    event_definition_id,
    year,
    month,
    SUM(fleet_monthly.event_count) as event_count,
    SUM(fleet_monthly.flight_count) as flight_count,
    MIN(fleet_monthly.min_duration) as min_duration,
    SUM(fleet_monthly.avg_duration * fleet_monthly.event_count) / SUM(fleet_monthly.event_count) as avg_duration,
    MAX(fleet_monthly.max_duration) as max_duration,
    MIN(fleet_monthly.min_severity) as min_severity,
    SUM(fleet_monthly.avg_severity * fleet_monthly.event_count) / SUM(fleet_monthly.event_count) as avg_severity,
    MAX(fleet_monthly.max_severity) as max_severity
FROM
    m_fleet_airframe_monthly_event_counts as fleet_monthly
GROUP BY
    fleet_id,
    event_definition_id,
    year,
    month;


--changeset josh:aggregate-monthly-event-counts labels:events,views
CREATE VIEW
    v_aggregate_airframe_monthly_event_counts AS
SELECT
    event_definition_id,
    airframe_id,
    year,
    month,
    SUM(fleet_monthly.event_count) as event_count,
    SUM(fleet_monthly.flight_count) as flight_count,
    MIN(fleet_monthly.min_duration) as min_duration,
    SUM(fleet_monthly.avg_duration * fleet_monthly.event_count) / SUM(fleet_monthly.event_count) as avg_duration,
    MAX(fleet_monthly.max_duration) as max_duration,
    MIN(fleet_monthly.min_severity) as min_severity,
    SUM(fleet_monthly.avg_severity * fleet_monthly.event_count) / SUM(fleet_monthly.event_count) as avg_severity,
    MAX(fleet_monthly.max_severity) as max_severity
FROM
    m_fleet_airframe_monthly_event_counts as fleet_monthly
GROUP BY
    event_definition_id,
    airframe_id,
    year,
    month;

CREATE VIEW
    v_aggregate_monthly_event_counts AS
SELECT
    event_definition_id,
    year,
    month,
    SUM(fleet_monthly.event_count) as event_count,
    SUM(fleet_monthly.flight_count) as flight_count,
    MIN(fleet_monthly.min_duration) as min_duration,
    SUM(fleet_monthly.avg_duration * fleet_monthly.event_count) / SUM(fleet_monthly.event_count) as avg_duration,
    MAX(fleet_monthly.max_duration) as max_duration,
    MIN(fleet_monthly.min_severity) as min_severity,
    SUM(fleet_monthly.avg_severity * fleet_monthly.event_count) / SUM(fleet_monthly.event_count) as avg_severity,
    MAX(fleet_monthly.max_severity) as max_severity
FROM
    m_fleet_airframe_monthly_event_counts as fleet_monthly
GROUP BY
    event_definition_id,
    year,
    month;


--changeset josh:fleet-30-day-event-counts labels:events,views,materialized-views context:hourly-materialized-view runAlways:true
CREATE OR REPLACE VIEW
    v_fleet_airframe_30_day_event_counts AS
SELECT
    e.fleet_id                  as fleet_id,
    event_definition_id         as event_definition_id,
    flights.airframe_id         as airframe_id,
    YEAR(e.start_time)          as year,
    MONTH(e.start_time)         as month,
    COUNT(DISTINCT e.id)        as event_count,
    COUNT(DISTINCT flights.id)  as flight_count,
    MIN(end_line - start_line)  as min_duration,
    AVG(end_line - start_line)  as avg_duration,
    MAX(end_line - start_line)  as max_duration,
    MIN(severity)               as min_severity,
    AVG(severity)               as avg_severity,
    MAX(severity)               as max_severity
FROM
    events AS e
    INNER JOIN
        flights
    ON
        flights.id = e.flight_id
WHERE
    (CURRENT_DATE - INTERVAL '30' DAY) <= e.end_time
GROUP BY
    fleet_id,
    event_definition_id,
    flights.airframe_id,
    YEAR(e.start_time),
    MONTH(e.start_time);

CREATE TABLE IF NOT EXISTS m_fleet_airframe_30_day_event_counts (
    fleet_id INT NOT NULL,
    event_definition_id INT NOT NULL,
    airframe_id INT NOT NULL,
    year INT NOT NULL,
    month INT NOT NULL,
    event_count INT NOT NULL,
    flight_count INT NOT NULL,
    min_duration DOUBLE NOT NULL,
    avg_duration DOUBLE NOT NULL,
    max_duration DOUBLE NOT NULL,
    min_severity DOUBLE NOT NULL,
    avg_severity DOUBLE NOT NULL,
    max_severity DOUBLE NOT NULL,

    PRIMARY KEY (year, month, fleet_id, event_definition_id, airframe_id),
    FOREIGN KEY (fleet_id) REFERENCES fleet(id),
    FOREIGN KEY (event_definition_id) REFERENCES event_definitions(id),
    FOREIGN KEY (airframe_id) REFERENCES airframes(id)
);

TRUNCATE TABLE m_fleet_airframe_30_day_event_counts;

INSERT INTO m_fleet_airframe_30_day_event_counts
SELECT * FROM v_fleet_airframe_30_day_event_counts;

CREATE OR REPLACE VIEW
    v_fleet_30_day_event_counts AS
SELECT
    fleet_id,
    event_definition_id,
    year,
    month,
    SUM(fleet_monthly.event_count) as event_count,
    SUM(fleet_monthly.flight_count) as flight_count,
    MIN(fleet_monthly.min_duration) as min_duration,
    SUM(fleet_monthly.avg_duration * fleet_monthly.event_count) / SUM(fleet_monthly.event_count) as avg_duration,
    MAX(fleet_monthly.max_duration) as max_duration,
    MIN(fleet_monthly.min_severity) as min_severity,
    SUM(fleet_monthly.avg_severity * fleet_monthly.event_count) / SUM(fleet_monthly.event_count) as avg_severity,
    MAX(fleet_monthly.max_severity) as max_severity
FROM
    m_fleet_airframe_30_day_event_counts as fleet_monthly
GROUP BY
    fleet_id,
    event_definition_id,
    year,
    month;

--changeset josh:aggregate-30-day-event-counts labels:events,views
CREATE VIEW
    v_aggregate_airframe_30_day_event_counts AS
SELECT
    event_definition_id,
    airframe_id,
    year,
    month,
    SUM(fleet_monthly.event_count) as event_count,
    SUM(fleet_monthly.flight_count) as flight_count,
    MIN(fleet_monthly.min_duration) as min_duration,
    SUM(fleet_monthly.avg_duration * fleet_monthly.event_count) / SUM(fleet_monthly.event_count) as avg_duration,
    MAX(fleet_monthly.max_duration) as max_duration,
    MIN(fleet_monthly.min_severity) as min_severity,
    SUM(fleet_monthly.avg_severity * fleet_monthly.event_count) / SUM(fleet_monthly.event_count) as avg_severity,
    MAX(fleet_monthly.max_severity) as max_severity
FROM
    m_fleet_airframe_30_day_event_counts as fleet_monthly
GROUP BY
    event_definition_id,
    airframe_id,
    year,
    month;

CREATE VIEW
    v_aggregate_30_day_event_counts AS
SELECT
    event_definition_id,
    year,
    month,
    SUM(fleet_monthly.event_count) as event_count,
    SUM(fleet_monthly.flight_count) as flight_count,
    MIN(fleet_monthly.min_duration) as min_duration,
    SUM(fleet_monthly.avg_duration * fleet_monthly.event_count) / SUM(fleet_monthly.event_count) as avg_duration,
    MAX(fleet_monthly.max_duration) as max_duration,
    MIN(fleet_monthly.min_severity) as min_severity,
    SUM(fleet_monthly.avg_severity * fleet_monthly.event_count) / SUM(fleet_monthly.event_count) as avg_severity,
    MAX(fleet_monthly.max_severity) as max_severity
FROM
    m_fleet_airframe_30_day_event_counts as fleet_monthly
GROUP BY
    event_definition_id,
    year,
    month;

--changeset josh:fleet-yearly-event-counts labels:events,views
CREATE VIEW
    v_fleet_airframe_yearly_event_counts AS
SELECT
    fleet_id,
    event_definition_id,
    airframe_id,
    year,
    SUM(fleet_monthly.event_count) as event_count,
    SUM(fleet_monthly.flight_count) as flight_count,
    MIN(fleet_monthly.min_duration) as min_duration,
    SUM(fleet_monthly.avg_duration * fleet_monthly.event_count) / SUM(fleet_monthly.event_count) as avg_duration,
    MAX(fleet_monthly.max_duration) as max_duration,
    MIN(fleet_monthly.min_severity) as min_severity,
    SUM(fleet_monthly.avg_severity * fleet_monthly.event_count) / SUM(fleet_monthly.event_count) as avg_severity,
    MAX(fleet_monthly.max_severity) as max_severity
FROM
    m_fleet_airframe_monthly_event_counts as fleet_monthly
GROUP BY
    fleet_id,
    event_definition_id,
    airframe_id,
    year;

CREATE VIEW
    v_fleet_yearly_event_counts AS
SELECT
    fleet_id,
    event_definition_id,
    year,
    SUM(fleet_monthly.event_count) as event_count,
    SUM(fleet_monthly.flight_count) as flight_count,
    MIN(fleet_monthly.min_duration) as min_duration,
    SUM(fleet_monthly.avg_duration * fleet_monthly.event_count) / SUM(fleet_monthly.event_count) as avg_duration,
    MAX(fleet_monthly.max_duration) as max_duration,
    MIN(fleet_monthly.min_severity) as min_severity,
    SUM(fleet_monthly.avg_severity * fleet_monthly.event_count) / SUM(fleet_monthly.event_count) as avg_severity,
    MAX(fleet_monthly.max_severity) as max_severity
FROM
    m_fleet_airframe_monthly_event_counts as fleet_monthly
GROUP BY
    fleet_id,
    event_definition_id,
    year;

--changeset josh:aggregate-yearly-event-counts labels:events,views
CREATE VIEW
    v_aggregate_airframe_yearly_event_counts AS
SELECT
    event_definition_id,
    airframe_id,
    year,
    SUM(fleet_monthly.event_count) as event_count,
    SUM(fleet_monthly.flight_count) as flight_count,
    MIN(fleet_monthly.min_duration) as min_duration,
    SUM(fleet_monthly.avg_duration * fleet_monthly.event_count) / SUM(fleet_monthly.event_count) as avg_duration,
    MAX(fleet_monthly.max_duration) as max_duration,
    MIN(fleet_monthly.min_severity) as min_severity,
    SUM(fleet_monthly.avg_severity * fleet_monthly.event_count) / SUM(fleet_monthly.event_count) as avg_severity,
    MAX(fleet_monthly.max_severity) as max_severity
FROM
    m_fleet_airframe_monthly_event_counts as fleet_monthly
GROUP BY
    event_definition_id,
    airframe_id,
    year;

CREATE VIEW
    v_aggregate_yearly_event_counts AS
SELECT
    event_definition_id,
    year,
    SUM(fleet_monthly.event_count) as event_count,
    SUM(fleet_monthly.flight_count) as flight_count,
    MIN(fleet_monthly.min_duration) as min_duration,
    SUM(fleet_monthly.avg_duration * fleet_monthly.event_count) / SUM(fleet_monthly.event_count) as avg_duration,
    MAX(fleet_monthly.max_duration) as max_duration,
    MIN(fleet_monthly.min_severity) as min_severity,
    SUM(fleet_monthly.avg_severity * fleet_monthly.event_count) / SUM(fleet_monthly.event_count) as avg_severity,
    MAX(fleet_monthly.max_severity) as max_severity
FROM
    m_fleet_airframe_monthly_event_counts as fleet_monthly
GROUP BY
    event_definition_id,
    year;

--changeset josh:fleet-event-counts labels:events,views
CREATE VIEW
    v_fleet_airframe_event_counts AS
SELECT
    fleet_id,
    airframe_id,
    event_definition_id,
    SUM(fleet_monthly.event_count) as event_count,
    SUM(fleet_monthly.flight_count) as flight_count,
    MIN(fleet_monthly.min_duration) as min_duration,
    SUM(fleet_monthly.avg_duration * fleet_monthly.event_count) / SUM(fleet_monthly.event_count) as avg_duration,
    MAX(fleet_monthly.max_duration) as max_duration,
    MIN(fleet_monthly.min_severity) as min_severity,
    SUM(fleet_monthly.avg_severity * fleet_monthly.event_count) / SUM(fleet_monthly.event_count) as avg_severity,
    MAX(fleet_monthly.max_severity) as max_severity
FROM
    m_fleet_airframe_monthly_event_counts as fleet_monthly
GROUP BY
    fleet_id,
    event_definition_id,
    airframe_id;

CREATE VIEW
    v_fleet_event_counts AS
SELECT
    fleet_id,
    event_definition_id,
    SUM(fleet_monthly.event_count) as event_count,
    SUM(fleet_monthly.flight_count) as flight_count,
    MIN(fleet_monthly.min_duration) as min_duration,
    SUM(fleet_monthly.avg_duration * fleet_monthly.event_count) / SUM(fleet_monthly.event_count) as avg_duration,
    MAX(fleet_monthly.max_duration) as max_duration,
    MIN(fleet_monthly.min_severity) as min_severity,
    SUM(fleet_monthly.avg_severity * fleet_monthly.event_count) / SUM(fleet_monthly.event_count) as avg_severity,
    MAX(fleet_monthly.max_severity) as max_severity
FROM
    m_fleet_airframe_monthly_event_counts as fleet_monthly
GROUP BY
    fleet_id,
    event_definition_id;


--changeset josh:aggregate-event-counts labels:events,views
CREATE VIEW
    v_aggregate_airframe_event_counts AS
SELECT
    event_definition_id,
    airframe_id,
    SUM(fleet_monthly.event_count) as event_count,
    SUM(fleet_monthly.flight_count) as flight_count,
    MIN(fleet_monthly.min_duration) as min_duration,
    SUM(fleet_monthly.avg_duration * fleet_monthly.event_count) / SUM(fleet_monthly.event_count) as avg_duration,
    MAX(fleet_monthly.max_duration) as max_duration,
    MIN(fleet_monthly.min_severity) as min_severity,
    SUM(fleet_monthly.avg_severity * fleet_monthly.event_count) / SUM(fleet_monthly.event_count) as avg_severity,
    MAX(fleet_monthly.max_severity) as max_severity
FROM
    m_fleet_airframe_monthly_event_counts as fleet_monthly
GROUP BY
    event_definition_id,
    airframe_id;

CREATE VIEW
    v_aggregate_event_counts AS
SELECT
    event_definition_id,
    SUM(fleet_monthly.event_count) as event_count,
    SUM(fleet_monthly.flight_count) as flight_count,
    MIN(fleet_monthly.min_duration) as min_duration,
    SUM(fleet_monthly.avg_duration * fleet_monthly.event_count) / SUM(fleet_monthly.event_count) as avg_duration,
    MAX(fleet_monthly.max_duration) as max_duration,
    MIN(fleet_monthly.min_severity) as min_severity,
    SUM(fleet_monthly.avg_severity * fleet_monthly.event_count) / SUM(fleet_monthly.event_count) as avg_severity,
    MAX(fleet_monthly.max_severity) as max_severity
FROM
    m_fleet_airframe_monthly_event_counts as fleet_monthly
GROUP BY
    event_definition_id;


--changeset josh:fleet-total-event-count labels:events,views
CREATE VIEW
    v_fleet_airframe_total_event_counts AS
SELECT
    fleet_id,
    airframe_id,
    SUM(fleet_monthly.event_count) as event_count,
    MIN(fleet_monthly.min_duration) as min_duration,
    SUM(fleet_monthly.avg_duration * fleet_monthly.event_count) / SUM(fleet_monthly.event_count) as avg_duration,
    MAX(fleet_monthly.max_duration) as max_duration
FROM
    m_fleet_airframe_monthly_event_counts as fleet_monthly
GROUP BY
    fleet_id,
    airframe_id;

CREATE VIEW
    v_fleet_total_event_counts AS
SELECT
    fleet_id,
    SUM(fleet_monthly.event_count) as event_count,
    MIN(fleet_monthly.min_duration) as min_duration,
    SUM(fleet_monthly.avg_duration * fleet_monthly.event_count) / SUM(fleet_monthly.event_count) as avg_duration,
    MAX(fleet_monthly.max_duration) as max_duration
FROM
    m_fleet_airframe_monthly_event_counts as fleet_monthly
GROUP BY
    fleet_id;


--changeset josh:aggregate-total-event-count labels:events,views
CREATE VIEW
    v_aggregate_airframe_total_event_counts AS
SELECT
    airframe_id,
    SUM(fleet_monthly.event_count) as event_count,
    MIN(fleet_monthly.min_duration) as min_duration,
    SUM(fleet_monthly.avg_duration * fleet_monthly.event_count) / SUM(fleet_monthly.event_count) as avg_duration,
    MAX(fleet_monthly.max_duration) as max_duration
FROM
    m_fleet_airframe_monthly_event_counts as fleet_monthly
GROUP BY
    airframe_id;

CREATE VIEW
    v_aggregate_total_event_count AS
SELECT
    SUM(fleet_monthly.event_count) as event_count,
    MIN(fleet_monthly.min_duration) as min_duration,
    SUM(fleet_monthly.avg_duration * fleet_monthly.event_count) / SUM(fleet_monthly.event_count) as avg_duration,
    MAX(fleet_monthly.max_duration) as max_duration
FROM
    m_fleet_airframe_monthly_event_counts as fleet_monthly;


--changeset josh:fleet-event-processed-flight-count labels:events,views,materialized-views context:hourly-materialized-view runAlways:true
CREATE OR REPLACE VIEW
    v_fleet_airframe_event_processed_flight_count AS
SELECT
    flights.fleet_id as fleet_id,
    event_definition_id,
    flights.airframe_id,
    COUNT(DISTINCT flights.id) as count
FROM
    flight_processed
    INNER JOIN
        flights
    ON
        flights.id = flight_processed.flight_id
GROUP BY
    flights.fleet_id,
    flight_processed.event_definition_id,
    flights.airframe_id;

CREATE TABLE IF NOT EXISTS m_fleet_airframe_event_processed_flight_count (
    fleet_id INT NOT NULL,
    event_definition_id INT NOT NULL,
    airframe_id INT NOT NULL,
    count INT NOT NULL,

    PRIMARY KEY (fleet_id, event_definition_id, airframe_id),
    FOREIGN KEY (fleet_id) REFERENCES fleet(id),
    FOREIGN KEY (event_definition_id) REFERENCES event_definitions(id),
    FOREIGN KEY (airframe_id) REFERENCES airframes(id)
);

TRUNCATE TABLE m_fleet_airframe_event_processed_flight_count;

INSERT INTO m_fleet_airframe_event_processed_flight_count
SELECT * FROM v_fleet_airframe_event_processed_flight_count;
