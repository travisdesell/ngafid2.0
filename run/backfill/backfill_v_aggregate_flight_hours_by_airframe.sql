-- Backfill v_aggregate_flight_hours_by_airframe with existing data
-- Run this script once after deploying the new table

TRUNCATE TABLE v_aggregate_flight_hours_by_airframe;

INSERT INTO v_aggregate_flight_hours_by_airframe (airframe_id, num_flights, total_flight_hours)
SELECT
    airframe_id,
    COUNT(*) AS num_flights,
    SUM(TIMESTAMPDIFF(SECOND, start_time, end_time)) / 3600.0 AS total_flight_hours
FROM
    flights
WHERE
    status IN ('SUCCESS', 'WARNING')
GROUP BY
    airframe_id; 