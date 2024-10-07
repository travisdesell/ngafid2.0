TRUNCATE TABLE rate_of_closure;
TRUNCATE TABLE event_metadata;
TRUNCATE TABLE event_statistics;
DELETE FROM events;
ALTER TABLE events AUTO_INCREMENT = 1;
TRUNCATE TABLE flight_processed;
TRUNCATE TABLE turn_to_final;

DELETE FROM itinerary;
ALTER TABLE itinerary AUTO_INCREMENT = 1;

TRUNCATE TABLE double_series;
ALTER TABLE double_series AUTO_INCREMENT = 1;

TRUNCATE TABLE string_series;
ALTER TABLE string_series AUTO_INCREMENT = 1;

TRUNCATE TABLE flight_warnings;
ALTER TABLE flight_warnings AUTO_INCREMENT = 1;

TRUNCATE TABLE flight_errors;
ALTER TABLE flight_errors AUTO_INCREMENT = 1;

TRUNCATE TABLE upload_errors;
ALTER TABLE upload_errors AUTO_INCREMENT = 1;

DELETE FROM flight_messages;
ALTER TABLE flight_messages AUTO_INCREMENT = 1;

TRUNCATE TABLE flight_tag_map;

TRUNCATE TABLE airsync_imports;

DELETE FROM flights;
ALTER TABLE flights AUTO_INCREMENT = 1;

DELETE FROM fleet_airframes;

UPDATE uploads SET
    status = 'UPLOADED', n_valid_flights = 0, n_warning_flights = 0, n_error_flights = 0 
  WHERE 
    status = 'IMPORTED' OR status = 'ERROR';
