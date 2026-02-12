--liquibase formatted sql

--changeset roman:events-composite-index-performance labels:flights,events,performance
-- Add composite index for optimized event queries
-- This index supports queries that filter by event_definition_id, fleet_id, and end_time
-- Expected performance improvement: 2-5x speedup on queries fetching events by definition and date range
CREATE INDEX idx_events_definition_fleet_endtime 
ON events (event_definition_id, fleet_id, end_time);
