--liquibase formatted sql

--changeset mingfeng:event-definitions-static labels:messages

INSERT INTO `event_definitions` (`id`, `fleet_id`, `airframe_id`, `name`, `start_buffer`, `stop_buffer`, `column_names`,
                                 `condition_json`, `severity_column_names`, `severity_type`, `color`)
VALUES (-8, 0, 0, 'Obstacle Collision Risk', 1, 30,
        '[\"AltAGL\", \"AltMSL\", \"Latitude\", \"Longitude\", \"UTCOfst\"]',
        '{\"text\" : \"Aircraft within 1200 feet of obstacles\"}', '[]', 'MIN', 'null');