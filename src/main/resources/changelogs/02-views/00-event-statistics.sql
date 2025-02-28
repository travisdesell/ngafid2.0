--liquibase formatted sql

--changeset josh:fleet-monthly-event-counts labels:events,views context:daily-view runAlways:true
CREATE OR REPLACE VIEW v_fleet_monthly_event_counts AS
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
    FROM events AS e
    INNER JOIN flights ON flights.id = e.flight_id
    GROUP BY
        fleet_id,
        event_definition_id,
        flights.airframe_id,
        YEAR(e.start_time),
        MONTH(e.start_time);

--changeset josh:aggregate-monthly-event-counts labels:events,views context:daily-view runAlways:true
CREATE OR REPLACE VIEW v_aggregate_monthly_event_counts AS
    SELECT
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
    FROM events AS e
    INNER JOIN flights ON flights.id = e.flight_id
    GROUP BY
        event_definition_id,
        flights.airframe_id,
        YEAR(e.start_time),
        MONTH(e.start_time);


--changeset josh:yearly-fleet-event-counts labels:events,views context:daily-view runAlways:true
CREATE OR REPLACE VIEW v_fleet_yearly_event_counts AS
    SELECT
        fleet_id,
        YEAR(start_time) as year,
        COUNT(DISTINCT id) as event_count
    FROM events
    GROUP BY
        events.fleet_id,
        YEAR(events.start_time);

--changeset josh:aggregate-yearly-event-counts labels:events,views context:daily-view runAlways:true
CREATE OR REPLACE VIEW v_aggregate_yearly_event_counts AS
    SELECT
        YEAR(start_time) as year,
        COUNT(DISTINCT id) as event_count
    FROM events
    GROUP BY
        YEAR(events.start_time);

--changeset josh:fleet-event-count labels:events,views context:daily-view runAlways:true
CREATE OR REPLACE VIEW v_fleet_event_count AS
    SELECT
        fleet_id,
        COUNT(DISTINCT id) as event_count
    FROM events
    GROUP BY events.fleet_id;

--changeset josh:aggregate-event-count labels:events,views context:daily-view runAlways:true
CREATE OR REPLACE VIEW v_aggregate_event_count AS
    SELECT
        COUNT(DISTINCT id) as event_count
    FROM events;


--changeset josh:fleet-monthly-flight-counts labels:flights,views context:daily-view runAlways:true
CREATE OR REPLACE VIEW v_fleet_monthly_flight_counts AS
    SELECT
        fleet_id,
        airframe_id,
        YEAR(start_time)    as year,
        MONTH(start_time)   as month,
        COUNT(DISTINCT id)  as flight_count
    FROM flights
    GROUP BY
        fleet_id,
        airframe_id,
        YEAR(start_time),
        MONTH(start_time);

--changeset josh:aggregate-monthly-flight-counts labels:flights,views context:daily-view runAlways:true
CREATE OR REPLACE VIEW v_aggregate_monthly_flight_counts AS
    SELECT
        airframe_id,
        YEAR(start_time) as year,
        MONTH(start_time) as month,
        COUNT(DISTINCT id) as flight_count
    FROM flights
    GROUP BY
        airframe_id,
        YEAR(start_time),
        MONTH(start_time);

--changeset josh:fleet-yearly-flight-counts labels:flights,views context:daily-view runAlways:true
CREATE OR REPLACE VIEW v_fleet_yearly_flight_counts AS
    SELECT
        fleet_id,
        airframe_id,
        YEAR(start_time)    as year,
        COUNT(DISTINCT id)  as flight_count
    FROM flights
    GROUP BY
        fleet_id,
        airframe_id,
        YEAR(start_time);

--changeset josh:aggregate-yearly-flight-counts labels:flights,views context:daily-view runAlways:true
CREATE OR REPLACE VIEW v_aggregate_yearly_flight_counts AS
    SELECT
        airframe_id,
        YEAR(start_time) as year,
        COUNT(DISTINCT id) as flight_count
    FROM flights
    GROUP BY
        airframe_id,
        YEAR(start_time);

--changeset josh:fleet-flight-counts labels:flights,views context:daily-view runAlways:true
CREATE OR REPLACE VIEW v_fleet_flight_counts AS
    SELECT
        fleet_id,
        airframe_id,
        COUNT(DISTINCT id)  as flight_count
    FROM flights
    GROUP BY
        fleet_id,
        airframe_id;

--changeset josh:aggregate-flight-counts labels:flights,views context:daily-view runAlways:true
CREATE OR REPLACE VIEW v_aggregate_flight_counts AS
    SELECT
        airframe_id,
        COUNT(DISTINCT id) as flight_count
    FROM flights
    GROUP BY
        airframe_id;
