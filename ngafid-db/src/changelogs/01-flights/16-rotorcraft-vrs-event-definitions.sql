--liquibase formatted sql

--changeset ngafid:rotorcraft-vrs-event-definitions labels:flights,rotorcraft,events

INSERT INTO event_definitions (id, fleet_id, airframe_id, airframe_type_id, name, start_buffer, stop_buffer, column_names, condition_json, severity_column_names, severity_type, color)
VALUES
    (105,0, 0, 2, 'Medium Vortex Ring State', 5, 1, '["VRS"]',
     '{"type":"GROUP","condition":"AND","filters":[{"type":"RULE","inputs":["VRS",">=","2"]},{"type":"RULE","inputs":["VRS","<","3"]}]}',
     '["VRS"]', 'MAX', NULL),
    (106,0, 0, 2, 'High Vortex Ring State', 5, 1, '["VRS"]',
     '{"type":"GROUP","condition":"AND","filters":[{"type":"RULE","inputs":["VRS",">=","3"]}]}',
     '["VRS"]', 'MAX', NULL);