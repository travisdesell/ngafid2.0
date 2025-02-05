--liquibase formatted sql

--changeset josh:event-definitions-static labels:messages
INSERT INTO `event_definitions` (id, fleet_id, airframe_id, name, start_buffer, stop_buffer, column_names,
                                 condition_json, severity_column_names, severity_type, color)
VALUES (-6, 0, 3, 'Low Ending Fuel', 1, 30, '["Total Fuel"]',
        '{"text" : "Average fuel the past 15 seconds was less than 17.56"}', '["Total Fuel"]', 'MIN', NULL),
       (-5, 0, 2, 'Low Ending Fuel', 1, 30, '["Total Fuel"]',
        '{"text" : "Average fuel the past 15 seconds was less than 8.00"}', '["Total Fuel"]', 'MIN', NULL),
       (-4, 0, 1, 'Low Ending Fuel', 1, 30, '["Total Fuel"]',
        '{"text" : "Average fuel the past 15 seconds was less than 8.25"}', '["Total Fuel"]', 'MIN', NULL),
       (-3, 0, 0, 'High Altitude Spin', 1, 30, '["NormAc", "AltAGL", "VSpd", "IAS"]',
        '{"text" : "Spin at or above 4,000'' AGL"}', '["NormAc"]', 'MIN', NULL),
       (-2, 0, 0, 'Low Altitude Spin', 1, 30, '["NormAc", "AltAGL", "VSpd", "IAS"]',
        '{"text" : "Spin below 4,000'' AGL"}', '["NormAc"]', 'MIN', NULL),
       (-1, 0, 0, 'Proximity', 1, 30,
        '["AltAGL", "AltMSL", "Latitude", "Longitude", "Lcl Date", "Lcl Time", "UTCOfst"]',
        '{"text" : "Aircraft within 500 ft of another aircraft and above above 50ft AGL"}', '[]', 'MIN', NULL),
       (1, 0, 0, 'Low Pitch', 1, 30, '["Pitch"]',
        '{"type":"GROUP","condition":"AND","filters":[{"type":"RULE","inputs":["Pitch","<","-30"]}]}', '["Pitch"]',
        'MAX', NULL),
       (2, 0, 0, 'High Pitch', 1, 30, '["Pitch"]',
        '{"type":"GROUP","condition":"AND","filters":[{"type":"RULE","inputs":["Pitch",">","30"]}]}', '["Pitch"]',
        'MAX', NULL),
       (3, 0, 0, 'Low Lateral Acceleration', 1, 30, '["LatAc"]',
        '{"type":"GROUP","condition":"AND","filters":[{"type":"RULE","inputs":["LatAc","<","-1.33"]}]}', '["LatAc"]',
        'MAX', NULL),
       (4, 0, 0, 'High Lateral Acceleration', 1, 30, '["LatAc"]',
        '{"type":"GROUP","condition":"AND","filters":[{"type":"RULE","inputs":["LatAc",">","1.33"]}]}', '["LatAc"]',
        'MAX', NULL),
       (5, 0, 0, 'Low Vertical Acceleration', 1, 30, '["NormAc"]',
        '{"type":"GROUP","condition":"AND","filters":[{"type":"RULE","inputs":["NormAc","<","-2.52"]}]}', '["NormAc"]',
        'MAX', NULL),
       (6, 0, 0, 'High Vertical Acceleration', 1, 30, '["NormAc"]',
        '{"type":"GROUP","condition":"AND","filters":[{"type":"RULE","inputs":["NormAc",">","2.8"]}]}', '["NormAc"]',
        'MAX', NULL),
       (7, 0, 0, 'Roll', 1, 30, '["Roll"]',
        '{"type":"GROUP","condition":"OR","filters":[{"type":"RULE","inputs":["Roll","<","-60"]},{"type":"RULE","inputs":["Roll",">","60"]}]}',
        '["Roll"]', 'MAX_ABS', NULL),
       (8, 0, 0, 'VSI on Final', 1, 30, '["VSpd","AltAGL"]',
        '{"type":"GROUP","condition":"AND","filters":[{"type":"RULE","inputs":["VSpd","<=","-1500"]},{"type":"RULE","inputs":["AltAGL","<=","500"]}]}',
        '["VSpd"]', 'MIN', NULL),
       (9, 0, 2, 'Airspeed', 1, 30, '["IAS"]',
        '{"type":"GROUP","condition":"AND","filters":[{"type":"RULE","inputs":["IAS",">","163"]}]}', '["IAS"]', 'MAX',
        NULL),
       (11, 0, 1, 'Airspeed', 1, 30, '["IAS"]',
        '{"type":"GROUP","condition":"AND","filters":[{"type":"RULE","inputs":["IAS",">","154"]}]}', '["IAS"]', 'MAX',
        NULL),
       (12, 0, 3, 'Airspeed', 1, 30, '["IAS"]',
        '{"type":"GROUP","condition":"AND","filters":[{"type":"RULE","inputs":["IAS",">","202"]}]}', '["IAS"]', 'MAX',
        NULL),
       (13, 0, 4, 'Airspeed', 1, 30, '["IAS"]',
        '{"type":"GROUP","condition":"AND","filters":[{"type":"RULE","inputs":["IAS",">","200"]}]}', '["IAS"]', 'MAX',
        NULL),
       (14, 0, 2, 'Altitude', 1, 30, '["AltMSL"]',
        '{"type":"GROUP","condition":"AND","filters":[{"type":"RULE","inputs":["AltMSL",">","12800"]}]}', '["AltMSL"]',
        'MAX', NULL),
       (15, 0, 4, 'Altitude', 1, 30, '["AltMSL"]',
        '{"type":"GROUP","condition":"AND","filters":[{"type":"RULE","inputs":["AltMSL",">","12800"]}]}', '["AltMSL"]',
        'MAX', NULL),
       (16, 0, 1, 'Altitude', 1, 30, '["AltMSL"]',
        '{"type":"GROUP","condition":"AND","filters":[{"type":"RULE","inputs":["AltMSL",">","12800"]}]}', '["AltMSL"]',
        'MAX', NULL),
       (17, 0, 3, 'Altitude', 1, 30, '["AltMSL"]',
        '{"type":"GROUP","condition":"AND","filters":[{"type":"RULE","inputs":["AltMSL",">","12800"]}]}', '["AltMSL"]',
        'MAX', NULL),
       (21, 0, 2, 'Cylinder Head Temperature', 1, 30, '["E1 CHT4","E1 CHT3","E1 CHT2","E1 CHT1"]',
        '{"type":"GROUP","condition":"OR","filters":[{"type":"RULE","inputs":["E1 CHT1",">","500"]},{"type":"RULE","inputs":["E1 CHT2",">","500"]},{"type":"RULE","inputs":["E1 CHT3",">","500"]},{"type":"RULE","inputs":["E1 CHT4",">","500"]}]}',
        '["E1 CHT4","E1 CHT3","E1 CHT2","E1 CHT1"]', 'MAX', NULL),
       (22, 0, 4, 'Cylinder Head Temperature', 1, 30, '["E1 CHT4","E1 CHT3","E1 CHT6","E1 CHT5","E1 CHT2","E1 CHT1"]',
        '{"type":"GROUP","condition":"OR","filters":[{"type":"RULE","inputs":["E1 CHT1",">","460"]},{"type":"RULE","inputs":["E1 CHT2",">","460"]},{"type":"RULE","inputs":["E1 CHT3",">","460"]},{"type":"RULE","inputs":["E1 CHT4",">","460"]},{"type":"RULE","inputs":["E1 CHT5",">","460"]},{"type":"RULE","inputs":["E1 CHT6",">","460"]}]}',
        '["E1 CHT4","E1 CHT3","E1 CHT6","E1 CHT5","E1 CHT2","E1 CHT1"]', 'MAX', NULL),
       (23, 0, 3, 'Cylinder Head Temperature', 1, 30, '["E1 CHT1","E2 CHT1"]',
        '{"type":"GROUP","condition":"OR","filters":[{"type":"RULE","inputs":["E1 CHT1",">","500"]},{"type":"RULE","inputs":["E2 CHT1",">","500"]}]}',
        '["E1 CHT1","E2 CHT1"]', 'MAX', NULL),
       (24, 0, 2, 'Low Oil Pressure', 1, 30, '["E1 OilP","E1 RPM"]',
        '{"type":"GROUP","condition":"AND","filters":[{"type":"RULE","inputs":["E1 OilP","<","20"]},{"type":"RULE","inputs":["E1 RPM",">","500"]}]}',
        '["E1 OilP"]', 'MIN', NULL),
       (25, 0, 4, 'Low Oil Pressure', 1, 30, '["E1 OilP","E1 RPM"]',
        '{"type":"GROUP","condition":"AND","filters":[{"type":"RULE","inputs":["E1 OilP","<","10"]},{"type":"RULE","inputs":["E1 RPM",">","500"]}]}',
        '["E1 OilP"]', 'MIN', NULL),
       (26, 0, 1, 'Low Oil Pressure', 1, 30, '["E1 OilP","E1 RPM"]',
        '{"type":"GROUP","condition":"AND","filters":[{"type":"RULE","inputs":["E1 OilP","<","25"]},{"type":"RULE","inputs":["E1 RPM",">","500"]}]}',
        '["E1 OilP"]', 'MIN', NULL),
       (27, 0, 3, 'Low Oil Pressure', 1, 30, '["E1 OilP","E2 RPM","E2 OilP","E1 RPM"]',
        '{"type":"GROUP","condition":"OR","filters":[{"type":"GROUP","condition":"AND","filters":[{"type":"RULE","inputs":["E1 OilP","<","25"]},{"type":"RULE","inputs":["E1 RPM",">","500"]}]},{"type":"GROUP","condition":"AND","filters":[{"type":"RULE","inputs":["E2 OilP","<","25"]},{"type":"RULE","inputs":["E2 RPM",">","500"]}]}]}',
        '["E1 OilP","E2 OilP"]', 'MIN', NULL),
       (28, 0, 2, 'Low Fuel', 1, 30, '["Pitch","Total Fuel"]',
        '{"type":"GROUP","condition":"AND","filters":[{"type":"RULE","inputs":["Total Fuel","<","8.0"]},{"type":"RULE","inputs":["Pitch","<","5"]}]}',
        '["Total Fuel"]', 'MIN', NULL),
       (29, 0, 1, 'Low Fuel', 1, 30, '["Pitch","Total Fuel"]',
        '{"type":"GROUP","condition":"AND","filters":[{"type":"RULE","inputs":["Total Fuel","<","8.25"]},{"type":"RULE","inputs":["Pitch","<","5"]}]}',
        '["Total Fuel"]', 'MIN', NULL),
       (30, 0, 3, 'Low Fuel', 1, 30, '["Pitch","Total Fuel"]',
        '{"type":"GROUP","condition":"AND","filters":[{"type":"RULE","inputs":["Total Fuel","<","17.56"]},{"type":"RULE","inputs":["Pitch","<","5"]}]}',
        '["Total Fuel"]', 'MIN', NULL),
       (31, 0, 2, 'Low Airspeed on Approach', 1, 30, '["AltMSL Lag Diff","IAS","AltAGL"]',
        '{"type":"GROUP","condition":"AND","filters":[{"type":"RULE","inputs":["IAS","<","57"]},{"type":"RULE","inputs":["AltMSL Lag Diff","<","0"]},{"type":"RULE","inputs":["AltAGL",">","100"]},{"type":"RULE","inputs":["AltAGL","<","500"]}]}',
        '["IAS"]', 'MIN', NULL),
       (32, 0, 1, 'Low Airspeed on Approach', 1, 30, '["AltMSL Lag Diff","IAS","AltAGL"]',
        '{"type":"GROUP","condition":"AND","filters":[{"type":"RULE","inputs":["IAS","<","56"]},{"type":"RULE","inputs":["AltMSL Lag Diff","<","0"]},{"type":"RULE","inputs":["AltAGL",">","100"]},{"type":"RULE","inputs":["AltAGL","<","500"]}]}',
        '["IAS"]', 'MIN', NULL),
       (33, 0, 3, 'Low Airspeed on Approach', 1, 30, '["AltMSL Lag Diff","IAS","AltAGL"]',
        '{"type":"GROUP","condition":"AND","filters":[{"type":"RULE","inputs":["IAS","<","66"]},{"type":"RULE","inputs":["AltMSL Lag Diff","<","0"]},{"type":"RULE","inputs":["AltAGL",">","100"]},{"type":"RULE","inputs":["AltAGL","<","500"]}]}',
        '["IAS"]', 'MIN', NULL),
       (34, 0, 4, 'Low Airspeed on Approach', 1, 30, '["AltMSL Lag Diff","IAS","AltAGL"]',
        '{"type":"GROUP","condition":"AND","filters":[{"type":"RULE","inputs":["IAS","<","67"]},{"type":"RULE","inputs":["AltMSL Lag Diff","<","0"]},{"type":"RULE","inputs":["AltAGL",">","100"]},{"type":"RULE","inputs":["AltAGL","<","500"]}]}',
        '["IAS"]', 'MIN', NULL),
       (35, 0, 2, 'Low Airspeed on Climbout', 1, 30, '["AltMSL Lag Diff","IAS","AltAGL"]',
        '{"type":"GROUP","condition":"AND","filters":[{"type":"RULE","inputs":["IAS",">=","20"]},{"type":"RULE","inputs":["IAS","<","52"]},{"type":"RULE","inputs":["AltMSL Lag Diff",">","0"]},{"type":"RULE","inputs":["AltAGL",">","100"]},{"type":"RULE","inputs":["AltAGL","<","500"]}]}',
        '["IAS"]', 'MIN', NULL),
       (36, 0, 1, 'Low Airspeed on Climbout', 1, 30, '["AltMSL Lag Diff","IAS","AltAGL"]',
        '{"type":"GROUP","condition":"AND","filters":[{"type":"RULE","inputs":["IAS",">=","20"]},{"type":"RULE","inputs":["IAS","<","59"]},{"type":"RULE","inputs":["AltMSL Lag Diff",">","0"]},{"type":"RULE","inputs":["AltAGL",">","100"]},{"type":"RULE","inputs":["AltAGL","<","500"]}]}',
        '["IAS"]', 'MIN', NULL),
       (37, 0, 3, 'Low Airspeed on Climbout', 1, 30, '["AltMSL Lag Diff","IAS","AltAGL"]',
        '{"type":"GROUP","condition":"AND","filters":[{"type":"RULE","inputs":["IAS",">=","20"]},{"type":"RULE","inputs":["IAS","<","70"]},{"type":"RULE","inputs":["AltMSL Lag Diff",">","0"]},{"type":"RULE","inputs":["AltAGL",">","100"]},{"type":"RULE","inputs":["AltAGL","<","500"]}]}',
        '["IAS"]', 'MIN', NULL),
       (38, 0, 4, 'Low Airspeed on Climbout', 1, 30, '["AltMSL Lag Diff","IAS","AltAGL"]',
        '{"type":"GROUP","condition":"AND","filters":[{"type":"RULE","inputs":["IAS",">=","20"]},{"type":"RULE","inputs":["IAS","<","60"]},{"type":"RULE","inputs":["AltMSL Lag Diff",">","0"]},{"type":"RULE","inputs":["AltAGL",">","100"]},{"type":"RULE","inputs":["AltAGL","<","500"]}]}',
        '["IAS"]', 'MIN', NULL),
       (39, 0, 2, 'CHT Sensor Divergence', 1, 30, '["E1 CHT Variance"]',
        '{"type":"GROUP","condition":"AND","filters":[{"type":"RULE","inputs":["E1 CHT Divergence",">","100"]}]}',
        '["E1 CHT Variance"]', 'MAX', NULL),
       (40, 0, 1, 'CHT Sensor Divergence', 1, 30, '["E1 CHT Variance"]',
        '{"type":"GROUP","condition":"AND","filters":[{"type":"RULE","inputs":["E1 CHT Divergence",">","100"]}]}',
        '["E1 CHT Variance"]', 'MAX', NULL),
       (41, 0, 4, 'CHT Sensor Divergence', 1, 30, '["E1 CHT Variance"]',
        '{"type":"GROUP","condition":"AND","filters":[{"type":"RULE","inputs":["E1 CHT Divergence",">","100"]}]}',
        '["E1 CHT Variance"]', 'MAX', NULL),
       (42, 0, 2, 'EGT Sensor Divergence', 1, 30, '["E1 EGT Variance"]',
        '{"type":"GROUP","condition":"OR","filters":[{"type":"RULE","inputs":["E1 EGT Divergence",">","400"]}]}',
        '["E1 EGT Variance"]', 'MAX', NULL),
       (43, 0, 4, 'EGT Sensor Divergence', 1, 30, '["E1 EGT Variance"]',
        '{"type":"GROUP","condition":"OR","filters":[{"type":"RULE","inputs":["E1 EGT Divergence",">","400"]}]}',
        '["E1 EGT Variance"]', 'MAX', NULL),
       (44, 0, 1, 'EGT Sensor Divergence', 1, 30, '["E1 EGT Variance"]',
        '{"type":"GROUP","condition":"OR","filters":[{"type":"RULE","inputs":["E1 EGT Divergence",">","400"]}]}',
        '["E1 EGT Variance"]', 'MAX', NULL),
       (45, 0, 3, 'EGT Sensor Divergence', 1, 30, '["E2 EGT Variance","E1 EGT Variance"]',
        '{"type":"GROUP","condition":"OR","filters":[{"type":"RULE","inputs":["E1 EGT Divergence",">","400"]},{"type":"RULE","inputs":["E2 EGT Divergence",">","400"]}]}',
        '["E2 EGT Variance","E1 EGT Variance"]', 'MAX', NULL),
       (46, 0, 2, 'Engine Shutdown Below 3000 Ft', 1, 30, '["E1 RPM","AltAGL"]',
        '{"type":"GROUP","condition":"AND","filters":[{"type":"RULE","inputs":["E1 RPM","<","100"]},{"type":"RULE","inputs":["AltAGL",">","500"]},{"type":"RULE","inputs":["AltAGL","<","3000"]}]}',
        '["AltAGL"]', 'MIN', NULL),
       (47, 0, 1, 'Engine Shutdown Below 3000 Ft', 1, 30, '["E1 RPM","AltAGL"]',
        '{"type":"GROUP","condition":"AND","filters":[{"type":"RULE","inputs":["E1 RPM","<","100"]},{"type":"RULE","inputs":["AltAGL",">","500"]},{"type":"RULE","inputs":["AltAGL","<","3000"]}]}',
        '["AltAGL"]', 'MIN', NULL),
       (48, 0, 4, 'Engine Shutdown Below 3000 Ft', 1, 30, '["E1 RPM","AltAGL"]',
        '{"type":"GROUP","condition":"AND","filters":[{"type":"RULE","inputs":["E1 RPM","<","100"]},{"type":"RULE","inputs":["AltAGL",">","500"]},{"type":"RULE","inputs":["AltAGL","<","3000"]}]}',
        '["AltAGL"]', 'MIN', NULL),
       (49, 0, 3, 'Engine Shutdown Below 3000 Ft', 1, 30, '["E2 RPM","E1 RPM","AltAGL"]',
        '{"type":"GROUP","condition":"AND","filters":[{"type":"RULE","inputs":["AltAGL",">","500"]},{"type":"RULE","inputs":["AltAGL","<","3000"]},{"type":"GROUP","condition":"OR","filters":[{"type":"RULE","inputs":["E1 RPM","<","100"]},{"type":"RULE","inputs":["E2 RPM","<","100"]}]}]}',
        '["AltAGL"]', 'MIN', NULL),
       (50, 0, 2, 'High Altitude LOC-I', 2, 1, '["AltAGL","LOC-I Index","Coordination Index","Yaw Rate","AltB"]',
        '{"type":"GROUP","condition":"AND","filters":[{"type":"RULE","inputs":["LOC-I Index",">=","1"]},{"type":"RULE","inputs":["AltAGL",">=","1500"]}]}',
        '["Yaw Rate", "AltAGL"]', 'MAX', NULL),
       (51, 0, 0, 'High Altitude Stall', 2, 1, '["AltAGL","Stall Index","Coordination Index","AOASimple","AltB"]',
        '{"type":"GROUP","condition":"AND","filters":[{"type":"RULE","inputs":["Stall Index",">=","1"]},{"type":"RULE","inputs":["AltAGL",">=","1500"]}]}',
        '["AOASimple", "AltAGL"]', 'MAX', NULL),
       (52, 0, 0, 'Low Altitude Stall', 2, 1, '["AltAGL","Stall Index","Coordination Index","AOASimple","AltB"]',
        '{"type":"GROUP","condition":"AND","filters":[{"type":"RULE","inputs":["Stall Index",">=","1"]},{"type":"RULE","inputs":["AltAGL","<=","1500"]},{"type":"RULE","inputs":["AltAGL",">=","100"]}]}',
        '["AOASimple", "AltAGL"]', 'MAX', NULL),
       (53, 0, 2, 'Low Altitude LOC-I', 2, 1, '["AltAGL","LOC-I Index","Coordination Index","Yaw Rate","AltB"]',
        '{"type":"GROUP","condition":"AND","filters":[{"type":"RULE","inputs":["LOC-I Index",">=","1"]},{"type":"RULE","inputs":["AltAGL","<=","1500"]},{"type":"RULE","inputs":["AltAGL",">=","100"]}]}',
        '["Yaw Rate", "AltAGL"]', 'MAX', NULL);

