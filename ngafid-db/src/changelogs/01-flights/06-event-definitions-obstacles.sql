--liquibase formatted sql

--changeset mingfeng:event-definitions-static labels:messages

INSERT INTO `event_definitions` (`id`, `fleet_id`, `airframe_id`, `name`, `start_buffer`, `stop_buffer`, `column_names`,
                                 `condition_json`, `severity_column_names`, `severity_type`, `color`)
VALUES (-10, 0, 0, 'High Obstacle Collision Risk', 1, 30,
        '[\"AltAGL\", \"AltMSL\", \"Latitude\", \"Longitude\", \"UTCOfst\"]',
        '{\"text\" : \"Aircraft within 500 feet of horizontal distance and 75 feet of vertical distance of an obstacle\"}', '[]', 'MIN', NULL),
        (-9, 0, 0, 'Medium Obstacle Collision Risk', 1, 30,
        '[\"AltAGL\", \"AltMSL\", \"Latitude\", \"Longitude\", \"UTCOfst\"]',
        '{\"text\" : \"Aircraft within 500 feet of horizontal distance and 75-200 feet of vertical distance of an obstacle, or 500-100 feet of horizontal distance and vertical distance of 0-75 feet\"}', '[]', 'MIN', NULL),
        (-8, 0, 0, 'Low Obstacle Collision Risk', 1, 30,
        '[\"AltAGL\", \"AltMSL\", \"Latitude\", \"Longitude\", \"UTCOfst\"]',
        '{\"text\" : \"Aircraft within 500-1000 feet of horizontal distance and 75-200 feet of vertical distance of an obstacle\"}', '[]', 'MIN', NULL);
        