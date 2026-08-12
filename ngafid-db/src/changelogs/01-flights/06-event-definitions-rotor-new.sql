--liquibase formatted sql

--changeset pujan:event-definitions-static labels:messages

INSERT INTO event_definitions (id, fleet_id, airframe_id, airframe_type_id, name, start_buffer, stop_buffer, column_names, condition_json, severity_column_names, severity_type, color)
VALUES
    (90,0, 0, 2, 'Low Descent Rate Below 300ft', 4, 1, '["AltAGL","VSpd"]',
     '{"type":"GROUP","condition":"AND","filters":[{"type":"RULE","inputs":["AltAGL",">=","100"]},{"type":"RULE","inputs":["AltAGL","<","300"]},{"type":"RULE","inputs":["VSpd","<=","-800"]},{"type":"RULE","inputs":["VSpd",">","-1000"]}]}',
     '["VSpd"]', 'MIN', NULL),
    (91,0, 0, 2, 'Medium Descent Rate Below 300ft', 4, 1, '["AltAGL","VSpd"]',
     '{"type":"GROUP","condition":"AND","filters":[{"type":"RULE","inputs":["AltAGL",">=","100"]},{"type":"RULE","inputs":["AltAGL","<","300"]},{"type":"RULE","inputs":["VSpd","<=","-1000"]},{"type":"RULE","inputs":["VSpd",">","-1200"]}]}',
     '["VSpd"]', 'MIN', NULL),
    (92,0, 0, 2, 'High Descent Rate Below 300ft', 4, 1, '["AltAGL","VSpd"]',
     '{"type":"GROUP","condition":"AND","filters":[{"type":"RULE","inputs":["AltAGL",">=","100"]},{"type":"RULE","inputs":["AltAGL","<","300"]},{"type":"RULE","inputs":["VSpd","<=","-1200"]}]}',
     '["VSpd"]', 'MIN', NULL),
    (93,0, 0, 2, 'High Main Rotor Over Torque', 1, 1, '["Engine 1 Torque","Engine 2 Torque"]',
     '{"type":"GROUP","condition":"OR","filters":[{"type":"RULE","inputs":["Engine 1 Torque",">","105"]},{"type":"RULE","inputs":["Engine 2 Torque",">","105"]}]}',
     '["Engine 1 Torque","Engine 2 Torque"]', 'MAX', NULL),
    (94,0, 0, 2, 'Low Pitch High Below 30ft', 3, 1, '["AltAGL","GndSpd","VSpd","Pitch"]',
     '{"type":"GROUP","condition":"AND","filters":[{"type":"RULE","inputs":["AltAGL","<","30"]},{"type":"RULE","inputs":["GndSpd","<=","0.6"]},{"type":"RULE","inputs":["VSpd",">","90"]},{"type":"RULE","inputs":["Pitch",">=","15"]},{"type":"RULE","inputs":["Pitch","<","20"]}]}',
     '["Pitch"]', 'MAX', NULL),
    (95,0, 0, 2, 'Medium Pitch High Below 30ft', 3, 1, '["AltAGL","GndSpd","VSpd","Pitch"]',
     '{"type":"GROUP","condition":"AND","filters":[{"type":"RULE","inputs":["AltAGL","<","30"]},{"type":"RULE","inputs":["GndSpd","<=","0.6"]},{"type":"RULE","inputs":["VSpd",">","90"]},{"type":"RULE","inputs":["Pitch",">=","20"]},{"type":"RULE","inputs":["Pitch","<","25"]}]}',
     '["Pitch"]', 'MAX', NULL),
    (96,0, 0, 2, 'High Pitch High Below 30ft', 3, 1, '["AltAGL","GndSpd","VSpd","Pitch"]',
     '{"type":"GROUP","condition":"AND","filters":[{"type":"RULE","inputs":["AltAGL","<","30"]},{"type":"RULE","inputs":["GndSpd","<=","0.6"]},{"type":"RULE","inputs":["VSpd",">","90"]},{"type":"RULE","inputs":["Pitch",">=","25"]}]}',
     '["Pitch"]', 'MAX', NULL),
    (97,0, 0, 2, 'Low Pitch Low Below 30ft', 3, 1, '["AltAGL","GndSpd","VSpd","Pitch"]',
     '{"type":"GROUP","condition":"AND","filters":[{"type":"RULE","inputs":["AltAGL","<","30"]},{"type":"RULE","inputs":["GndSpd","<=","0.6"]},{"type":"RULE","inputs":["VSpd",">","90"]},{"type":"RULE","inputs":["Pitch","<=","-15"]},{"type":"RULE","inputs":["Pitch",">","-20"]}]}',
     '["Pitch"]', 'MIN', NULL),
    (98,0, 0, 2, 'Medium Pitch Low Below 30ft', 3, 1, '["AltAGL","GndSpd","VSpd","Pitch"]',
     '{"type":"GROUP","condition":"AND","filters":[{"type":"RULE","inputs":["AltAGL","<","30"]},{"type":"RULE","inputs":["GndSpd","<=","0.6"]},{"type":"RULE","inputs":["VSpd",">","90"]},{"type":"RULE","inputs":["Pitch","<=","-20"]},{"type":"RULE","inputs":["Pitch",">","-25"]}]}',
     '["Pitch"]', 'MIN', NULL),
    (99,0, 0, 2, 'High Pitch Low Below 30ft', 3, 1, '["AltAGL","GndSpd","VSpd","Pitch"]',
     '{"type":"GROUP","condition":"AND","filters":[{"type":"RULE","inputs":["AltAGL","<","30"]},{"type":"RULE","inputs":["GndSpd","<=","0.6"]},{"type":"RULE","inputs":["VSpd",">","90"]},{"type":"RULE","inputs":["Pitch","<=","-25"]}]}',
     '["Pitch"]', 'MIN', NULL),
    (100,0, 0, 2, 'Low Roll Below 300ft', 4, 1, '["AltAGL","Roll"]',
     '{"type":"GROUP","condition":"AND","filters":[{"type":"RULE","inputs":["AltAGL","<","300"]},{"type":"GROUP","condition":"OR","filters":[{"type":"GROUP","condition":"AND","filters":[{"type":"RULE","inputs":["Roll",">=","40"]},{"type":"RULE","inputs":["Roll","<","50"]}]},{"type":"GROUP","condition":"AND","filters":[{"type":"RULE","inputs":["Roll","<=","-40"]},{"type":"RULE","inputs":["Roll",">","-50"]}]}]}]}',
     '["Roll"]', 'MAX_ABS', NULL),
    (101,0, 0, 2, 'Medium Roll Below 300ft', 4, 1, '["AltAGL","Roll"]',
     '{"type":"GROUP","condition":"AND","filters":[{"type":"RULE","inputs":["AltAGL","<","300"]},{"type":"GROUP","condition":"OR","filters":[{"type":"GROUP","condition":"AND","filters":[{"type":"RULE","inputs":["Roll",">=","50"]},{"type":"RULE","inputs":["Roll","<","55"]}]},{"type":"GROUP","condition":"AND","filters":[{"type":"RULE","inputs":["Roll","<=","-50"]},{"type":"RULE","inputs":["Roll",">","-55"]}]}]}]}',
     '["Roll"]', 'MAX_ABS', NULL),
    (102,0, 0, 2, 'High Roll Below 300ft', 4, 1, '["AltAGL","Roll"]',
     '{"type":"GROUP","condition":"AND","filters":[{"type":"RULE","inputs":["AltAGL","<","300"]},{"type":"GROUP","condition":"OR","filters":[{"type":"RULE","inputs":["Roll",">=","55"]},{"type":"RULE","inputs":["Roll","<=","-55"]}]}]}',
     '["Roll"]', 'MAX_ABS', NULL);


--changeset pujan:rotor-event-definitions-absolute-values labels:messages
-- Pitch/roll glossary limits use absolute values, so both positive and negative excursions must trigger.
UPDATE event_definitions
SET condition_json = '{"type":"GROUP","condition":"OR","filters":[{"type":"RULE","inputs":["Pitch",">","30"]},{"type":"RULE","inputs":["Pitch","<","-30"]}]}',
    severity_type = 'MAX_ABS'
WHERE airframe_type_id IN (SELECT id FROM airframe_types WHERE name = 'Rotorcraft')
  AND name IN ('PITCH_EXCESSIVE::HIGH', 'High Pitch Excessive');

UPDATE event_definitions
SET condition_json = '{"type":"GROUP","condition":"AND","filters":[{"type":"RULE","inputs":["AltAGL",">=","300"]},{"type":"GROUP","condition":"OR","filters":[{"type":"GROUP","condition":"AND","filters":[{"type":"RULE","inputs":["Roll",">=","40"]},{"type":"RULE","inputs":["Roll","<","50"]}]},{"type":"GROUP","condition":"AND","filters":[{"type":"RULE","inputs":["Roll","<=","-40"]},{"type":"RULE","inputs":["Roll",">","-50"]}]}]}]}',
    severity_type = 'MAX_ABS'
WHERE airframe_type_id IN (SELECT id FROM airframe_types WHERE name = 'Rotorcraft')
  AND name IN ('ROLL_ABOVE_300FT::LOW', 'Low Roll Above 300ft');

UPDATE event_definitions
SET condition_json = '{"type":"GROUP","condition":"AND","filters":[{"type":"RULE","inputs":["AltAGL",">=","300"]},{"type":"GROUP","condition":"OR","filters":[{"type":"GROUP","condition":"AND","filters":[{"type":"RULE","inputs":["Roll",">=","50"]},{"type":"RULE","inputs":["Roll","<","55"]}]},{"type":"GROUP","condition":"AND","filters":[{"type":"RULE","inputs":["Roll","<=","-50"]},{"type":"RULE","inputs":["Roll",">","-55"]}]}]}]}',
    severity_type = 'MAX_ABS'
WHERE airframe_type_id IN (SELECT id FROM airframe_types WHERE name = 'Rotorcraft')
  AND name IN ('ROLL_ABOVE_300FT::MEDIUM', 'Medium Roll Above 300ft');

UPDATE event_definitions
SET condition_json = '{"type":"GROUP","condition":"AND","filters":[{"type":"RULE","inputs":["AltAGL",">=","300"]},{"type":"GROUP","condition":"OR","filters":[{"type":"RULE","inputs":["Roll",">=","55"]},{"type":"RULE","inputs":["Roll","<=","-55"]}]}]}',
    severity_type = 'MAX_ABS'
WHERE airframe_type_id IN (SELECT id FROM airframe_types WHERE name = 'Rotorcraft')
  AND name IN ('ROLL_ABOVE_300FT::HIGH', 'High Roll Above 300ft');

UPDATE event_definitions
SET condition_json = '{"type":"GROUP","condition":"OR","filters":[{"type":"RULE","inputs":["Roll",">","60"]},{"type":"RULE","inputs":["Roll","<","-60"]}]}',
    severity_type = 'MAX_ABS'
WHERE airframe_type_id IN (SELECT id FROM airframe_types WHERE name = 'Rotorcraft')
  AND name IN ('ROLL_EXCESSIVE::HIGH', 'High Roll Excessive');