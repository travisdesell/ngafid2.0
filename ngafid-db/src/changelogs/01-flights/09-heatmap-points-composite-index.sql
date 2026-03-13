--liquibase formatted sql

--changeset roman:heatmap-points-composite-index labels:flights,proximity,performance
-- Composite index for batch heatmap points queries
-- Optimizes WHERE event_id IN (...) AND flight_id = ... or chunked event_id IN (...)
CREATE INDEX idx_heatmap_points_event_flight
ON heatmap_points (event_id, flight_id);
