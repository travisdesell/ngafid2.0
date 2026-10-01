--liquibase formatted sql

--changeset mingfeng:event-static-metadata-keys labels:flights,events
-- Add in metadata to store obstacle id
INSERT INTO event_metadata_keys (name) VALUES ('obstacle_id');