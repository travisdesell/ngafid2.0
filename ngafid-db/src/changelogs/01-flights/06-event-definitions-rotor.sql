--liquibase formatted sql

--changeset pujan:event-definitions-static labels:messages

INSERT INTO event_definitions (id, fleet_id, airframe_id, airframe_type_id, name, start_buffer, stop_buffer, column_names, condition_json, severity_column_names, severity_type, color)
VALUES
    (73, 0, 0, 2, 'Low Climb Rate Below 10kts', 5, 1, '["GndSpd","VSpd"]',
     '{"type":"GROUP","condition":"AND","filters":[{"type":"RULE","inputs":["GndSpd","<","10"]},{"type":"RULE","inputs":["VSpd",">=","1500"]},{"type":"RULE","inputs":["VSpd","<","1750"]}]}',
     '["VSpd"]', 'MAX', NULL),
    (74,0, 0, 2, 'Medium Climb Rate Below 10kts', 5, 1, '["GndSpd","VSpd"]',
     '{"type":"GROUP","condition":"AND","filters":[{"type":"RULE","inputs":["GndSpd","<","10"]},{"type":"RULE","inputs":["VSpd",">=","1750"]},{"type":"RULE","inputs":["VSpd","<","2000"]}]}',
     '["VSpd"]', 'MAX', NULL),
    (75,0, 0, 2, 'High Climb Rate Below 10kts', 5, 1, '["GndSpd","VSpd"]',
     '{"type":"GROUP","condition":"AND","filters":[{"type":"RULE","inputs":["GndSpd","<","10"]},{"type":"RULE","inputs":["VSpd",">=","2000"]}]}',
     '["VSpd"]', 'MAX', NULL),
    (76,0, 0, 2, 'Low Descent Rate Above 300ft', 4, 1, '["AltAGL","VSpd"]',
     '{"type":"GROUP","condition":"AND","filters":[{"type":"RULE","inputs":["AltAGL",">","300"]},{"type":"RULE","inputs":["VSpd","<=","-1250"]},{"type":"RULE","inputs":["VSpd",">","-1500"]}]}',
     '["VSpd"]', 'MIN', NULL),
    (77,0, 0, 2, 'Medium Descent Rate Above 300ft', 4, 1, '["AltAGL","VSpd"]',
     '{"type":"GROUP","condition":"AND","filters":[{"type":"RULE","inputs":["AltAGL",">","300"]},{"type":"RULE","inputs":["VSpd","<=","-1500"]},{"type":"RULE","inputs":["VSpd",">","-2000"]}]}',
     '["VSpd"]', 'MIN', NULL),
    (78,0, 0, 2, 'High Descent Rate Above 300ft', 4, 1, '["AltAGL","VSpd"]',
     '{"type":"GROUP","condition":"AND","filters":[{"type":"RULE","inputs":["AltAGL",">","300"]},{"type":"RULE","inputs":["VSpd","<=","-2000"]}]}',
     '["VSpd"]', 'MIN', NULL),
    (79,0, 0, 2, 'Low Groundspeed Below 100ft', 5, 1, '["AltAGL","GndSpd"]',
     '{"type":"GROUP","condition":"AND","filters":[{"type":"RULE","inputs":["AltAGL","<","100"]},{"type":"RULE","inputs":["GndSpd",">=","90"]},{"type":"RULE","inputs":["GndSpd","<","100"]}]}',
     '["GndSpd"]', 'MAX', NULL),
    (80,0, 0, 2, 'Medium Groundspeed Below 100ft', 5, 1, '["AltAGL","GndSpd"]',
     '{"type":"GROUP","condition":"AND","filters":[{"type":"RULE","inputs":["AltAGL","<","100"]},{"type":"RULE","inputs":["GndSpd",">=","100"]},{"type":"RULE","inputs":["GndSpd","<","110"]}]}',
     '["GndSpd"]', 'MAX', NULL),
    (81,0, 0, 2, 'High Groundspeed Below 100ft', 5, 1, '["AltAGL","GndSpd"]',
     '{"type":"GROUP","condition":"AND","filters":[{"type":"RULE","inputs":["AltAGL","<","100"]},{"type":"RULE","inputs":["GndSpd",">=","110"]}]}',
     '["GndSpd"]', 'MAX', NULL),
    (82,0, 0, 2, 'High Pitch Excessive', 3, 1, '["Pitch"]',
     '{"type":"GROUP","condition":"OR","filters":[{"type":"RULE","inputs":["Pitch",">","30"]},{"type":"RULE","inputs":["Pitch","<","-30"]}]}',
     '["Pitch"]', 'MAX_ABS', NULL),
    (83,0, 0, 2, 'Low Roll Above 300ft', 4, 1, '["AltAGL","Roll"]',
     '{"type":"GROUP","condition":"AND","filters":[{"type":"RULE","inputs":["AltAGL",">=","300"]},{"type":"GROUP","condition":"OR","filters":[{"type":"GROUP","condition":"AND","filters":[{"type":"RULE","inputs":["Roll",">=","40"]},{"type":"RULE","inputs":["Roll","<","50"]}]},{"type":"GROUP","condition":"AND","filters":[{"type":"RULE","inputs":["Roll","<=","-40"]},{"type":"RULE","inputs":["Roll",">","-50"]}]}]}]}',
     '["Roll"]', 'MAX_ABS', NULL),
    (84,0, 0, 2, 'Medium Roll Above 300ft', 4, 1, '["AltAGL","Roll"]',
     '{"type":"GROUP","condition":"AND","filters":[{"type":"RULE","inputs":["AltAGL",">=","300"]},{"type":"GROUP","condition":"OR","filters":[{"type":"GROUP","condition":"AND","filters":[{"type":"RULE","inputs":["Roll",">=","50"]},{"type":"RULE","inputs":["Roll","<","55"]}]},{"type":"GROUP","condition":"AND","filters":[{"type":"RULE","inputs":["Roll","<=","-50"]},{"type":"RULE","inputs":["Roll",">","-55"]}]}]}]}',
     '["Roll"]', 'MAX_ABS', NULL),
    (85,0, 0, 2, 'High Roll Above 300ft', 4, 1, '["AltAGL","Roll"]',
     '{"type":"GROUP","condition":"AND","filters":[{"type":"RULE","inputs":["AltAGL",">=","300"]},{"type":"GROUP","condition":"OR","filters":[{"type":"RULE","inputs":["Roll",">=","55"]},{"type":"RULE","inputs":["Roll","<=","-55"]}]}]}',
     '["Roll"]', 'MAX_ABS', NULL),
    (86,0, 0, 2, 'High Roll Excessive', 1, 1, '["Roll"]',
     '{"type":"GROUP","condition":"OR","filters":[{"type":"RULE","inputs":["Roll",">","60"]},{"type":"RULE","inputs":["Roll","<","-60"]}]}',
     '["Roll"]', 'MAX_ABS', NULL),
    (87,0, 0, 2, 'Low Yaw Rate', 3, 1, '["Yaw Rate"]',
     '{"type":"GROUP","condition":"OR","filters":[{"type":"GROUP","condition":"AND","filters":[{"type":"RULE","inputs":["Yaw Rate",">=","5"]},{"type":"RULE","inputs":["Yaw Rate","<","10"]}]},{"type":"GROUP","condition":"AND","filters":[{"type":"RULE","inputs":["Yaw Rate","<=","-5"]},{"type":"RULE","inputs":["Yaw Rate",">","-10"]}]}]}',
     '["Yaw Rate"]', 'MAX_ABS', NULL),
    (88,0, 0, 2, 'Medium Yaw Rate', 3, 1, '["Yaw Rate"]',
     '{"type":"GROUP","condition":"OR","filters":[{"type":"GROUP","condition":"AND","filters":[{"type":"RULE","inputs":["Yaw Rate",">=","10"]},{"type":"RULE","inputs":["Yaw Rate","<","15"]}]},{"type":"GROUP","condition":"AND","filters":[{"type":"RULE","inputs":["Yaw Rate","<=","-10"]},{"type":"RULE","inputs":["Yaw Rate",">","-15"]}]}]}',
     '["Yaw Rate"]', 'MAX_ABS', NULL),
    (89,0, 0, 2, 'High Yaw Rate', 3, 1, '["Yaw Rate"]',
     '{"type":"GROUP","condition":"OR","filters":[{"type":"RULE","inputs":["Yaw Rate",">=","15"]},{"type":"RULE","inputs":["Yaw Rate","<=","-15"]}]}',
     '["Yaw Rate"]', 'MAX_ABS', NULL);


--changeset pujan:rotor-event-definitions-rules-and-display-names labels:messages
UPDATE event_definitions
SET name = 'Low Climb Rate Below 10kts',
    condition_json = '{"type":"GROUP","condition":"AND","filters":[{"type":"RULE","inputs":["GndSpd","<","10"]},{"type":"RULE","inputs":["VSpd",">=","1500"]},{"type":"RULE","inputs":["VSpd","<","1750"]}]}'
WHERE airframe_type_id IN (SELECT id FROM airframe_types WHERE name = 'Rotorcraft')
  AND name IN ('CLIMB_RATE_BELOW_10KTS::LOW', 'Low Climb Rate Below 10kts');

UPDATE event_definitions
SET name = 'Medium Climb Rate Below 10kts',
    condition_json = '{"type":"GROUP","condition":"AND","filters":[{"type":"RULE","inputs":["GndSpd","<","10"]},{"type":"RULE","inputs":["VSpd",">=","1750"]},{"type":"RULE","inputs":["VSpd","<","2000"]}]}'
WHERE airframe_type_id IN (SELECT id FROM airframe_types WHERE name = 'Rotorcraft')
  AND name IN ('CLIMB_RATE_BELOW_10KTS::MEDIUM', 'Medium Climb Rate Below 10kts');

UPDATE event_definitions
SET name = 'High Climb Rate Below 10kts'
WHERE airframe_type_id IN (SELECT id FROM airframe_types WHERE name = 'Rotorcraft')
  AND name IN ('CLIMB_RATE_BELOW_10KTS::HIGH', 'High Climb Rate Below 10kts');

UPDATE event_definitions
SET name = 'Low Descent Rate Above 300ft',
    condition_json = '{"type":"GROUP","condition":"AND","filters":[{"type":"RULE","inputs":["AltAGL",">","300"]},{"type":"RULE","inputs":["VSpd","<=","-1250"]},{"type":"RULE","inputs":["VSpd",">","-1500"]}]}'
WHERE airframe_type_id IN (SELECT id FROM airframe_types WHERE name = 'Rotorcraft')
  AND name IN ('DESCENT_RATE_ABOVE_300FT::LOW', 'Low Descent Rate Above 300ft');

UPDATE event_definitions
SET name = 'Medium Descent Rate Above 300ft',
    condition_json = '{"type":"GROUP","condition":"AND","filters":[{"type":"RULE","inputs":["AltAGL",">","300"]},{"type":"RULE","inputs":["VSpd","<=","-1500"]},{"type":"RULE","inputs":["VSpd",">","-2000"]}]}'
WHERE airframe_type_id IN (SELECT id FROM airframe_types WHERE name = 'Rotorcraft')
  AND name IN ('DESCENT_RATE_ABOVE_300FT::MEDIUM', 'Medium Descent Rate Above 300ft');

UPDATE event_definitions
SET name = 'High Descent Rate Above 300ft'
WHERE airframe_type_id IN (SELECT id FROM airframe_types WHERE name = 'Rotorcraft')
  AND name IN ('DESCENT_RATE_ABOVE_300FT::HIGH', 'High Descent Rate Above 300ft');

UPDATE event_definitions
SET name = 'Low Groundspeed Below 100ft',
    condition_json = '{"type":"GROUP","condition":"AND","filters":[{"type":"RULE","inputs":["AltAGL","<","100"]},{"type":"RULE","inputs":["GndSpd",">=","90"]},{"type":"RULE","inputs":["GndSpd","<","100"]}]}'
WHERE airframe_type_id IN (SELECT id FROM airframe_types WHERE name = 'Rotorcraft')
  AND name IN ('GROUNDSPEED_BELOW_100FT::LOW', 'Low Groundspeed Below 100ft');

UPDATE event_definitions
SET name = 'Medium Groundspeed Below 100ft',
    condition_json = '{"type":"GROUP","condition":"AND","filters":[{"type":"RULE","inputs":["AltAGL","<","100"]},{"type":"RULE","inputs":["GndSpd",">=","100"]},{"type":"RULE","inputs":["GndSpd","<","110"]}]}'
WHERE airframe_type_id IN (SELECT id FROM airframe_types WHERE name = 'Rotorcraft')
  AND name IN ('GROUNDSPEED_BELOW_100FT::MEDIUM', 'Medium Groundspeed Below 100ft');

UPDATE event_definitions
SET name = 'High Groundspeed Below 100ft'
WHERE airframe_type_id IN (SELECT id FROM airframe_types WHERE name = 'Rotorcraft')
  AND name IN ('GROUNDSPEED_BELOW_100FT::HIGH', 'High Groundspeed Below 100ft');

UPDATE event_definitions
SET name = 'High Pitch Excessive'
WHERE airframe_type_id IN (SELECT id FROM airframe_types WHERE name = 'Rotorcraft')
  AND name IN ('PITCH_EXCESSIVE::HIGH', 'High Pitch Excessive');

UPDATE event_definitions
SET name = 'Low Roll Above 300ft',
    condition_json = '{"type":"GROUP","condition":"AND","filters":[{"type":"RULE","inputs":["AltAGL",">=","300"]},{"type":"GROUP","condition":"OR","filters":[{"type":"GROUP","condition":"AND","filters":[{"type":"RULE","inputs":["Roll",">=","40"]},{"type":"RULE","inputs":["Roll","<","50"]}]},{"type":"GROUP","condition":"AND","filters":[{"type":"RULE","inputs":["Roll","<=","-40"]},{"type":"RULE","inputs":["Roll",">","-50"]}]}]}]}'
    WHERE airframe_type_id IN (SELECT id FROM airframe_types WHERE name = 'Rotorcraft')
  AND name IN ('ROLL_ABOVE_300FT::LOW', 'Low Roll Above 300ft');

UPDATE event_definitions
SET name = 'Medium Roll Above 300ft',
    condition_json = '{"type":"GROUP","condition":"AND","filters":[{"type":"RULE","inputs":["AltAGL",">=","300"]},{"type":"GROUP","condition":"OR","filters":[{"type":"GROUP","condition":"AND","filters":[{"type":"RULE","inputs":["Roll",">=","50"]},{"type":"RULE","inputs":["Roll","<","55"]}]},{"type":"GROUP","condition":"AND","filters":[{"type":"RULE","inputs":["Roll","<=","-50"]},{"type":"RULE","inputs":["Roll",">","-55"]}]}]}]}'
    WHERE airframe_type_id IN (SELECT id FROM airframe_types WHERE name = 'Rotorcraft')
  AND name IN ('ROLL_ABOVE_300FT::MEDIUM', 'Medium Roll Above 300ft');

UPDATE event_definitions
SET name = 'High Roll Above 300ft',
    condition_json = '{"type":"GROUP","condition":"AND","filters":[{"type":"RULE","inputs":["AltAGL",">=","300"]},{"type":"GROUP","condition":"OR","filters":[{"type":"RULE","inputs":["Roll",">=","55"]},{"type":"RULE","inputs":["Roll","<=","-55"]}]}]}'
WHERE airframe_type_id IN (SELECT id FROM airframe_types WHERE name = 'Rotorcraft')
  AND name IN ('ROLL_ABOVE_300FT::HIGH', 'High Roll Above 300ft');

UPDATE event_definitions
SET name = 'High Roll Excessive',
    condition_json = '{"type":"GROUP","condition":"OR","filters":[{"type":"RULE","inputs":["Roll",">","60"]},{"type":"RULE","inputs":["Roll","<","-60"]}]}'
WHERE airframe_type_id IN (SELECT id FROM airframe_types WHERE name = 'Rotorcraft')
  AND name IN ('ROLL_EXCESSIVE::HIGH', 'High Roll Excessive');

UPDATE event_definitions
SET name = 'Low Yaw Rate',
    condition_json = '{"type":"GROUP","condition":"OR","filters":[{"type":"GROUP","condition":"AND","filters":[{"type":"RULE","inputs":["Yaw Rate",">=","5"]},{"type":"RULE","inputs":["Yaw Rate","<","10"]}]},{"type":"GROUP","condition":"AND","filters":[{"type":"RULE","inputs":["Yaw Rate","<=","-5"]},{"type":"RULE","inputs":["Yaw Rate",">","-10"]}]}]}'
WHERE airframe_type_id IN (SELECT id FROM airframe_types WHERE name = 'Rotorcraft')
  AND name IN ('YAW_RATE::LOW', 'Low Yaw Rate');

UPDATE event_definitions
SET name = 'Medium Yaw Rate',
    condition_json = '{"type":"GROUP","condition":"OR","filters":[{"type":"GROUP","condition":"AND","filters":[{"type":"RULE","inputs":["Yaw Rate",">=","10"]},{"type":"RULE","inputs":["Yaw Rate","<","15"]}]},{"type":"GROUP","condition":"AND","filters":[{"type":"RULE","inputs":["Yaw Rate","<=","-10"]},{"type":"RULE","inputs":["Yaw Rate",">","-15"]}]}]}'
WHERE airframe_type_id IN (SELECT id FROM airframe_types WHERE name = 'Rotorcraft')
  AND name IN ('YAW_RATE::MEDIUM', 'Medium Yaw Rate');

UPDATE event_definitions
SET name = 'High Yaw Rate'
WHERE airframe_type_id IN (SELECT id FROM airframe_types WHERE name = 'Rotorcraft')
  AND name IN ('YAW_RATE::HIGH', 'High Yaw Rate');