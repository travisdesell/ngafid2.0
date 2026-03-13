--liquibase formatted sql

--changeset ngafid:events-bbox-columns labels:flights,events
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'events' AND COLUMN_NAME = 'min_latitude'
-- Add bounding box columns to events table for heatmap / proximity_events_in_box.
-- New installs get these from 05-events.sql; this migration updates existing DBs that were created before those columns existed.
ALTER TABLE events ADD COLUMN min_latitude DOUBLE DEFAULT NULL;
ALTER TABLE events ADD COLUMN max_latitude DOUBLE DEFAULT NULL;
ALTER TABLE events ADD COLUMN min_longitude DOUBLE DEFAULT NULL;
ALTER TABLE events ADD COLUMN max_longitude DOUBLE DEFAULT NULL;
