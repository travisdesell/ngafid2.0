--liquibase formatted sql

--changeset roman:flight-label-sections-tail-airframe labels:flights,labels
ALTER TABLE flight_label_section
    ADD COLUMN tail_number VARCHAR(64) NULL AFTER flight_id,
    ADD COLUMN airframe VARCHAR(64) NULL AFTER tail_number;

-- Backfill existing rows from flights
UPDATE flight_label_section s
JOIN flights f ON s.flight_id = f.id
LEFT JOIN tails t ON f.fleet_id = t.fleet_id AND f.system_id = t.system_id
JOIN airframes a ON f.airframe_id = a.id
SET s.tail_number = t.tail, s.airframe = a.airframe;
