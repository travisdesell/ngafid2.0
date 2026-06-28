--liquibase formatted sql

--changeset ngafid:rotorcraft-lte-event-definitions labels:flights,rotorcraft,events
INSERT INTO event_definitions (id, fleet_id, airframe_id, airframe_type_id, name, start_buffer, stop_buffer, column_names, condition_json, severity_column_names, severity_type, color)
VALUES
    (103,0, 0, 2, 'Medium Loss of Tail Rotor Effectiveness', 5, 1, '["LTE"]',
     '{"type":"GROUP","condition":"AND","filters":[{"type":"RULE","inputs":["LTE",">=","2"]},{"type":"RULE","inputs":["LTE","<","3"]}]}',
     '["LTE"]', 'MAX', NULL),
    (104,0, 0, 2, 'High Loss of Tail Rotor Effectiveness', 5, 1, '["LTE"]',
     '{"type":"GROUP","condition":"AND","filters":[{"type":"RULE","inputs":["LTE",">=","3"]}]}',
     '["LTE"]', 'MAX', NULL);
