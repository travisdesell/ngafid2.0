--liquibase formatted sql

--changeset roman:rotorcraft-specs-user-permissions labels:accounts,rotorcraft,airframe-specs
--preconditions onFail:MARK_RAN onError:HALT
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'user' AND COLUMN_NAME = 'rotorcraft_specs_view'
ALTER TABLE user ADD COLUMN rotorcraft_specs_view BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE user ADD COLUMN rotorcraft_specs_edit BOOLEAN NOT NULL DEFAULT FALSE;

--changeset roman:rotorcraft-specs-drop-fleet-access labels:flights,rotorcraft,airframe-specs
DROP TABLE IF EXISTS rotorcraft_airframe_spec_fleet_access;

--changeset roman:rotorcraft-specs-drop-fleet-columns labels:flights,rotorcraft,airframe-specs
--preconditions onFail:MARK_RAN onError:HALT
--precondition-sql-check expectedResult:1 SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'rotorcraft_airframe_specs' AND COLUMN_NAME = 'owner_fleet_id'
ALTER TABLE rotorcraft_airframe_specs DROP INDEX uq_rotorcraft_airframe_specs_owner_identity;
ALTER TABLE rotorcraft_airframe_specs DROP COLUMN owner_fleet_id;
ALTER TABLE rotorcraft_airframe_specs DROP COLUMN is_public;
ALTER TABLE rotorcraft_airframe_specs ADD CONSTRAINT uq_rotorcraft_airframe_specs_identity UNIQUE (manufacturer, model, series);
