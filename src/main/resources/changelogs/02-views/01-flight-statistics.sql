--liquibase formatted sql

-- Most of the views defined in this file should be reasonably fast. If not,
-- we just have to create some additional materialized view tables.

--changeset josh:fleet-monthly-flight-counts labels:flights,views,materialized-views context:hourly-materialized-view runAlways:true
CREATE OR REPLACE VIEW
    v_fleet_monthly_flight_counts AS
SELECT
    fleet_id,
    airframe_id,
    YEAR(start_time)    as year,
    MONTH(start_time)   as month,
    COUNT(DISTINCT id)  as count
FROM flights
GROUP BY
    fleet_id,
    airframe_id,
    YEAR(start_time),
    MONTH(start_time);

CREATE TABLE IF NOT EXISTS m_fleet_monthly_flight_counts (
    fleet_id INT NOT NULL,
    airframe_id INT NOT NULL,
    year INT NOT NULL,
    month INT NOT NULL,
    count INT NOT NULL,

    PRIMARY KEY (fleet_id, airframe_id, year, month),
    FOREIGN KEY (fleet_id) REFERENCES fleet(id),
    FOREIGN KEY (airframe_id) REFERENCES airframes(id)
);

TRUNCATE TABLE m_fleet_monthly_flight_counts;

INSERT INTO m_fleet_monthly_flight_counts
SELECT * FROM v_fleet_monthly_flight_counts;


--changeset josh:aggregate-monthly-flight-counts labels:flights,views
CREATE VIEW v_aggregate_monthly_flight_counts AS
SELECT
    airframe_id,
    year,
    month,
    SUM(m_fleet_monthly_flight_counts.count) as count
FROM
    m_fleet_monthly_flight_counts
GROUP BY
    airframe_id,
    year,
    month;


--changeset josh:fleet-yearly-flight-counts labels:flights,views
CREATE VIEW v_fleet_yearly_flight_counts AS
SELECT
    fleet_id,
    airframe_id,
    year,
    SUM(m_fleet_monthly_flight_counts.count) as count
FROM
    m_fleet_monthly_flight_counts
GROUP BY
    fleet_id,
    airframe_id,
    year;


--changeset josh:aggregate-yearly-flight-counts labels:flights,views
CREATE VIEW v_aggregate_yearly_flight_counts AS
SELECT
    airframe_id,
    year,
    SUM(m_fleet_monthly_flight_counts.count) as count
FROM
    m_fleet_monthly_flight_counts
GROUP BY
    airframe_id,
    year;


--changeset josh:fleet-flight-counts labels:flights,views
CREATE VIEW
    v_fleet_flight_counts AS
SELECT
    fleet_id,
    airframe_id,
    SUM(m_fleet_monthly_flight_counts.count) as count
FROM
    m_fleet_monthly_flight_counts
GROUP BY
    fleet_id,
    airframe_id;


--changeset josh:aggregate-flight-counts labels:flights,views
CREATE VIEW
    v_aggregate_flight_counts AS
SELECT
    fleet_id,
    airframe_id,
    SUM(m_fleet_monthly_flight_counts.count) as count
FROM
    m_fleet_monthly_flight_counts
GROUP BY
    fleet_id,
    airframe_id;


--changeset josh:fleet-monthly-flight-time labels:flights,views,materialized-views context:daily-materialized-view runAlways:true
CREATE OR REPLACE VIEW
    v_fleet_monthly_flight_time AS
SELECT
    fleet_id,
    airframe_id,
    YEAR(start_time) as year,
    MONTH(start_time) as month,
    SUM(TIMESTAMPDIFF(SECOND, start_time, end_time)) as flight_time_seconds
FROM
    flights
GROUP BY
    fleet_id,
    airframe_id,
    YEAR(start_time),
    MONTH(start_time);

CREATE TABLE IF NOT EXISTS m_fleet_monthly_flight_time (
    fleet_id INT NOT NULL,
    airframe_id INT NOT NULL,
    year INT NOT NULL,
    month INT NOT NULL,
    flight_time_seconds INT NOT NULL,

    PRIMARY KEY (fleet_id, airframe_id, year, month),
    FOREIGN KEY (fleet_id) REFERENCES fleet(id),
    FOREIGN KEY (airframe_id) REFERENCES airframes(id)
);

TRUNCATE TABLE m_fleet_monthly_flight_time;

INSERT INTO m_fleet_monthly_flight_time
SELECT * FROM v_fleet_monthly_flight_time;


--changeset josh:aggregate-monthly-flight-time labels:flights,views
CREATE VIEW
    v_aggregate_monthly_flight_time AS
SELECT
    airframe_id,
    year,
    month,
    SUM(monthly.flight_time_seconds) as flight_time_seconds
FROM
    m_monthly_fleet_flight_time as monthly
GROUP BY
    airframe_id,
    year,
    month;


--changeset josh:fleet-yearly-flight-time labels:flights,views
CREATE VIEW
    v_fleet_yearly_flight_time AS
SELECT
    fleet_id,
    airframe_id,
    year,
    SUM(monthly.flight_time_seconds) as flight_time_seconds
FROM
    m_monthly_fleet_flight_time as monthly
GROUP BY
    fleet_id,
    airframe_id,
    year;


--changeset josh:aggregate-yearly-flight-time labels:flights,views
CREATE VIEW
    v_aggregate_yearly_flight_time AS
SELECT
    airframe_id,
    year,
    SUM(monthly.flight_time_seconds) as flight_time_seconds
FROM
    m_monthly_fleet_flight_time as monthly
GROUP BY
    airframe_id,
    year;


--changeset josh:fleet-flight-time labels:flights,views
CREATE VIEW
    v_fleet_flight_time AS
SELECT
    fleet_id,
    SUM(monthly.flight_time_seconds) as flight_time_seconds
FROM
    m_monthly_fleet_flight_time as monthly
GROUP BY
    fleet_id;

--changeset josh:aggregate-flight-time labels:flights,views
CREATE VIEW
    v_aggregate_flight_time AS
SELECT
    SUM(monthly.flight_time_seconds) as flight_time_seconds
FROM
    m_monthly_fleet_flight_time as monthly
