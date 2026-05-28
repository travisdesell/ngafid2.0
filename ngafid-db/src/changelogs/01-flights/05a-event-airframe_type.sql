--liquibase formatted sql

--changeset pujan:event-definitions-airframe-type-column labels:flights,events
ALTER TABLE event_definitions
    ADD COLUMN airframe_type_id INT NULL DEFAULT NULL AFTER airframe_id;

--changeset pujan:event-definitions-airframe-type-fk labels:flights,events
ALTER TABLE event_definitions
    ADD CONSTRAINT fk_event_definitions_airframe_type_id
        FOREIGN KEY (airframe_type_id) REFERENCES airframe_types(id);

--changeset pujan:event-definitions-airframe-type-idx labels:flights,events
CREATE INDEX idx_event_definitions_scope
    ON event_definitions (fleet_id, airframe_id, airframe_type_id);