<?php

$cwd[__FILE__] = __FILE__;
if (is_link($cwd[__FILE__])) $cwd[__FILE__] = readlink($cwd[__FILE__]);
$cwd[__FILE__] = dirname($cwd[__FILE__]);

require_once($cwd[__FILE__] . "/my_query.php");

$drop_tables = false;

//need to drop and reload these tables for 2020_05_16 changes

query_ngafid_db("DROP TABLE sim_aircraft");

/*
query_ngafid_db("DROP TABLE visited_airports");
query_ngafid_db("DROP TABLE visited_runways");
query_ngafid_db("DROP TABLE flight_tag_map");
query_ngafid_db("DROP TABLE flight_tags");
query_ngafid_db("DROP TABLE itinerary");
query_ngafid_db("DROP TABLE double_series");
query_ngafid_db("DROP TABLE string_series");
query_ngafid_db("DROP TABLE flight_processed");
query_ngafid_db("DROP TABLE event_statistics");
query_ngafid_db("DROP TABLE events");
query_ngafid_db("DROP TABLE flights");
query_ngafid_db("DROP TABLE tails");
 */


if ($drop_tables) {
    query_ngafid_db("DROP TABLE events");
    query_ngafid_db("DROP TABLE flight_processed");
    query_ngafid_db("DROP TABLE event_statistics");
    query_ngafid_db("DROP TABLE event_definitions");

    query_ngafid_db("DROP TABLE itinerary");
    query_ngafid_db("DROP TABLE double_series");
    query_ngafid_db("DROP TABLE string_series");
    query_ngafid_db("DROP TABLE flight_warnings");
    query_ngafid_db("DROP TABLE flight_errors");
    query_ngafid_db("DROP TABLE upload_errors");
    query_ngafid_db("DROP TABLE flight_messages");

    query_ngafid_db("DROP TABLE flights");
    query_ngafid_db("DROP TABLE tails");
    query_ngafid_db("DROP TABLE airframes");
    query_ngafid_db("DROP TABLE fleet_airframes");

    query_ngafid_db("DROP TABLE uploads");

    query_ngafid_db("DROP TABLE fleet_access");
    query_ngafid_db("DROP TABLE fleet");
    query_ngafid_db("DROP TABLE user");
}


$query = "CREATE TABLE `fleet` (
    `id` INT(11) NOT NULL AUTO_INCREMENT,
    `fleet_name` VARCHAR(256),

    PRIMARY KEY(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1";

query_ngafid_db($query);

$query = "CREATE TABLE `visited_airports` (
    `fleet_id` INT(11) NOT NULL,
    `airport` VARCHAR(8),

    PRIMARY KEY(`airport`),
    FOREIGN KEY(`fleet_id`) REFERENCES fleet(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1";

query_ngafid_db($query);

$query = "CREATE TABLE `visited_runways` (
    `fleet_id` INT(11) NOT NULL,
    `runway` VARCHAR(26),

    PRIMARY KEY(`runway`),
    FOREIGN KEY(`fleet_id`) REFERENCES fleet(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1";

query_ngafid_db($query);



$query = "CREATE TABLE `user` (
    `id` INT(11) NOT NULL AUTO_INCREMENT,
    `email` VARCHAR(128) NOT NULL,
    `password_token` VARCHAR(64) NOT NULL,
    `first_name` VARCHAR(64),
    `last_name` VARCHAR(64),
    `address` VARCHAR(256) NOT NULL,
    `city` VARCHAR(64),
    `country` VARCHAR(128),
    `state` VARCHAR(64),
    `zip_code` VARCHAR(16),
    `phone_number` VARCHAR(24),
    `reset_phrase` VARCHAR(64),

    PRIMARY KEY(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1";

query_ngafid_db($query);


$query = "CREATE TABLE `uploads` (
    `id` INT(11) NOT NULL AUTO_INCREMENT,
    `fleet_id` INT(11) NOT NULL,
    `uploader_id` INT(11) NOT NULL,
    `filename` VARCHAR(256) NOT NULL,
    `identifier` VARCHAR(128) NOT NULL,
    `number_chunks` int(11) NOT NULL,
    `uploaded_chunks` int(11) NOT NULL,
    `chunk_status` VARCHAR(8096) NOT NULL,
    `md5_hash` VARCHAR(32) NOT NULL,
    `size_bytes` BIGINT NOT NULL,
    `bytes_uploaded` BIGINT DEFAULT 0,
    `status` varchar(16),
    `start_time` datetime,
    `end_time` datetime,

    `n_valid_flights` INT DEFAULT 0,
    `n_warning_flights` INT DEFAULT 0,
    `n_error_flights` INT DEFAULT 0,

    PRIMARY KEY (`id`),
    UNIQUE KEY (`uploader_id`, `md5_hash`),
    UNIQUE KEY (`fleet_id`, `md5_hash`),
    FOREIGN KEY(`fleet_id`) REFERENCES fleet(`id`),
    FOREIGN KEY(`uploader_id`) REFERENCES user(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1";

query_ngafid_db($query);

$query = "CREATE TABLE `airframes` (
    `id` INT(11) NOT NULL AUTO_INCREMENT,
    `airframe` VARCHAR(64),

    PRIMARY KEY(`id`),
    UNIQUE KEY(`airframe`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1";

query_ngafid_db($query);

$query = "CREATE TABLE `fleet_airframes` (
    `fleet_id` INT(11) NOT NULL,
    `airframe_id` INT(11) NOT NULL,

    PRIMARY KEY(`fleet_id`, `airframe_id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1";

query_ngafid_db($query);


$query = "CREATE TABLE `tails` (
    `system_id` VARCHAR(64) NOT NULL,
    `fleet_id` INT(11) NOT NULL,
    `tail` VARCHAR(16),
    `confirmed` TINYINT(1) NOT NULL,

    PRIMARY KEY(`fleet_id`, `system_id`),
    INDEX(`fleet_id`),
    INDEX(`tail`),
    FOREIGN KEY(`fleet_id`) REFERENCES fleet(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1";

query_ngafid_db($query);


$query = "CREATE TABLE `flight_tags` (
    `id` INT(11) NOT NULL AUTO_INCREMENT,
    `fleet_id` INT(11) NOT NULL,
    `name` VARCHAR(128) NOT NULL,
    `description` VARCHAR(4096) NOT NULL,
    `color` VARCHAR(8) NOT NULL,

    PRIMARY KEY(`id`),
    UNIQUE KEY(`fleet_id`, `name`),
    FOREIGN KEY(`fleet_id`) REFERENCES fleet(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1";

query_ngafid_db($query);


$query = "CREATE TABLE `flights` (
    `id` INT(11) NOT NULL AUTO_INCREMENT,
    `fleet_id` INT(11) NOT NULL,
    `uploader_id` INT(11) NOT NULL,
    `upload_id` INT(11) NOT NULL,
    `system_id` VARCHAR(16) NOT NULL,
    `airframe_id` INT(11) NOT NULL,
    `start_time` DATETIME,
    `end_time` DATETIME,
    `filename` VARCHAR(256),
    `md5_hash` VARCHAR(32),
    `number_rows` INT(11),
    `status` VARCHAR(32) NOT NULL,
    `has_coords` TINYINT(1) NOT NULL,
    `has_agl` TINYINT(1) NOT NULL,
    `events_calculated` INT(1) NOT NULL DEFAULT 0,
    `insert_completed` INT(1) NOT NULL DEFAULT 0,

    PRIMARY KEY(`id`),
    UNIQUE KEY(`fleet_id`, `md5_hash`),
    INDEX(`fleet_id`),
    INDEX(`uploader_id`),
    INDEX(`system_id`),
    INDEX(`airframe_id`),
    INDEX(`start_time`),
    INDEX(`end_time`),
    FOREIGN KEY(`fleet_id`) REFERENCES fleet(`id`),
    FOREIGN KEY(`uploader_id`) REFERENCES user(`id`),
    FOREIGN KEY(`airframe_id`) REFERENCES airframes(`id`),
    FOREIGN KEY(`fleet_id`, `system_id`) REFERENCES tails(`fleet_id`, `system_id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1";

query_ngafid_db($query);

$query = "CREATE TABLE `flight_tag_map` (
    `flight_id` INT(11) NOT NULL,
    `tag_id` INT(11) NOT NULL,

    UNIQUE KEY(`flight_id`, `tag_id`),
    FOREIGN KEY(`flight_id`) REFERENCES flights(`id`),
    FOREIGN KEY(`tag_id`) REFERENCES flight_tags(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1";

query_ngafid_db($query);


$query = "CREATE TABLE `itinerary` (
    `id` INT(11) NOT NULL AUTO_INCREMENT,
    `flight_id` INT(11) NOT NULL,
    `order` INT(11) NOT NULL,
    `min_altitude_index` INT(11) NOT NULL,
    `min_altitude` double,
    `min_airport_distance` double,
    `min_runway_distance` double,
    `airport` VARCHAR(8),
    `runway` VARCHAR(16),
    `start_of_approach` INT(11) NOT NULL,
    `end_of_approach` INT(11) NOT NULL,
    `start_of_takeoff` INT(11) NOT NULL,
    `end_of_takeoff` INT(11) NOT NULL,
    `type` VARCHAR(32),

    PRIMARY KEY(`id`),
    INDEX(`airport`),
    INDEX(`runway`),
    FOREIGN KEY(`flight_id`) REFERENCES flights(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1";

query_ngafid_db($query);

$query = "CREATE TABLE `double_series` (
    `id` INT(11) NOT NULL AUTO_INCREMENT,
    `flight_id` INT(11) NOT NULL,
    `name` VARCHAR(64) NOT NULL,
    `data_type` VARCHAR(64) NOT NULL,
    `length` INT(11) NOT NULL,
    `valid_length` INT(11) NOT NULL,
    `min` double,
    `avg` double,
    `max` double,
    data MEDIUMBLOB,

    PRIMARY KEY(`id`),
    INDEX(`flight_id`),
    INDEX(`name`),
    FOREIGN KEY(`flight_id`) REFERENCES flights(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1";

query_ngafid_db($query);

$query = "CREATE TABLE `string_series` (
    `id` INT(11) NOT NULL AUTO_INCREMENT,
    `flight_id` INT(11) NOT NULL,
    `name` VARCHAR(64) NOT NULL,
    `data_type` VARCHAR(64) NOT NULL,
    `length` INT(11) NOT NULL,
    `valid_length` INT(11) NOT NULL,
    data MEDIUMBLOB,

    PRIMARY KEY(`id`),
    INDEX(`flight_id`),
    INDEX(`name`),
    FOREIGN KEY(`flight_id`) REFERENCES flights(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1";

query_ngafid_db($query);

$query = "CREATE TABLE `flight_messages` (
    `id` INT(11) NOT NULL AUTO_INCREMENT,
    `message` VARCHAR(512),

    PRIMARY KEY(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1";

query_ngafid_db($query);

$query = "CREATE TABLE `flight_warnings` (
    `id` INT(11) NOT NULL AUTO_INCREMENT,
    `flight_id` INT(11) NOT NULL,
    `message_id` INT(11) NOT NULL,

    PRIMARY KEY(`id`),
    FOREIGN KEY(`message_id`) REFERENCES flight_messages(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1";

query_ngafid_db($query);

$query = "CREATE TABLE `flight_errors` (
    `id` INT(11) NOT NULL AUTO_INCREMENT,
    `upload_id` INT(11) NOT NULL,
    `filename` VARCHAR(512) NOT NULL,
    `message_id` INT(11) NOT NULL,

    PRIMARY KEY(`id`),
    FOREIGN KEY(`message_id`) REFERENCES flight_messages(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1";

query_ngafid_db($query);

$query = "CREATE TABLE `upload_errors` (
    `id` INT(11) NOT NULL AUTO_INCREMENT,
    `upload_id` INT(11) NOT NULL,
    `message_id` INT(11) NOT NULL,

    PRIMARY KEY(`id`),
    FOREIGN KEY(`upload_id`) REFERENCES uploads(`id`),
    FOREIGN KEY(`message_id`) REFERENCES flight_messages(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1";

query_ngafid_db($query);

$query = "CREATE TABLE `fleet_access` (
    `user_id` INT(11),
    `fleet_id` INT(11),
    `type` VARCHAR(32),

    PRIMARY KEY(`user_id`, `fleet_id`),
    FOREIGN KEY(`user_id`) REFERENCES user(`id`),
    FOREIGN KEY(`fleet_id`) REFERENCES fleet(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1";

query_ngafid_db($query);


$query = "CREATE TABLE `event_definitions` (
    `id` INT(11) NOT NULL AUTO_INCREMENT,
    `fleet_id` INT(11) NOT NULL,
    `airframe_id` INT(11) NOT NULL,
    `name` VARCHAR(64) NOT NULL,
    `start_buffer` INT(11),
    `stop_buffer` INT(11),
    `column_names` VARCHAR(128),
    `condition_json` VARCHAR(512),
    `severity_column_names` VARCHAR(128),
    `severity_type` VARCHAR(3),
    `color` VARCHAR(6) DEFAULT NULL,

    PRIMARY KEY(`id`),
    UNIQUE KEY(`name`, `airframe_id`, `fleet_id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1";

query_ngafid_db($query);

$query = "CREATE TABLE `events` (
    `id` INT(11) NOT NULL AUTO_INCREMENT,
    `flight_id` INT(11) NOT NULL,
    `event_definition_id` INT(11) NOT NULL, 

    `start_line` INT(11),
    `end_line` INT(11),
    `start_time` datetime,
    `end_time` datetime,

    `severity` DOUBLE NOT NULL,

    PRIMARY KEY(`id`),
    FOREIGN KEY(`flight_id`) REFERENCES flights(`id`),
    INDEX(`start_time`),
    INDEX(`end_time`),
    FOREIGN KEY(`event_definition_id`) REFERENCES event_definitions(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1";

query_ngafid_db($query);


$query = "CREATE TABLE `flight_processed` (
    `fleet_id` INT(11) NOT NULL,
    `flight_id` INT(11) NOT NULL,
    `event_definition_id` INT(11) NOT NULL,
    `count` INT(11),
    `sum_duration` DOUBLE,
    `min_duration` DOUBLE,
    `max_duration` DOUBLE,
    `sum_severity` DOUBLE,
    `min_severity` DOUBLE,
    `max_severity` DOUBLE,
    `had_error` TINYINT(1),

    PRIMARY KEY(`flight_id`, `event_definition_id`),
    INDEX(`fleet_id`),
    INDEX(`count`),
    INDEX(`had_error`),
    FOREIGN KEY(`fleet_id`) REFERENCES fleet(`id`),
    FOREIGN KEY(`flight_id`) REFERENCES flights(`id`),
    FOREIGN KEY(`event_definition_id`) REFERENCES event_definitions(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1";

query_ngafid_db($query);

$query = "CREATE TABLE `event_statistics` (
    `fleet_id` INT(11) NOT NULL,
    `airframe_id` INT(11) NOT NULL,
    `event_definition_id` INT(11) NOT NULL,
    `month_first_day` DATE NOT NULL,
    `flights_with_event` INT(11) DEFAULT 0,
    `total_flights` INT(11) DEFAULT 0,
    `total_events` INT(11) DEFAULT 0,
    `min_duration` DOUBLE,
    `sum_duration` DOUBLE,
    `max_duration` DOUBLE,
    `min_severity` DOUBLE,
    `sum_severity` DOUBLE,
    `max_severity` DOUBLE,

    PRIMARY KEY(`fleet_id`, `event_definition_id`, `month_first_day`),
    INDEX(`month_first_day`),
    FOREIGN KEY(`fleet_id`) REFERENCES fleet(`id`),
    FOREIGN KEY(`airframe_id`) REFERENCES airframes(`id`),
    FOREIGN KEY(`event_definition_id`) REFERENCES event_definitions(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1";

query_ngafid_db($query);

$query = "CREATE TABLE `sim_aircraft` (
    `id` INT(11) NOT NULL AUTO_INCREMENT,
    `fleet_id` INT(11) NOT NULL,
    `path` VARCHAR(2048) NOT NULL,

    PRIMARY KEY(`id`),
    FOREIGN KEY(`fleet_id`) REFERENCES fleet(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1";

query_ngafid_db($query);

$query = "CREATE TABLE `loci_processed` (
    `fleet_id` INT(11) NOT NULL,
    `flight_id` INT(11) NOT NULL,
    `type` VARCHAR(2048) NOT NULL,

    PRIMARY KEY(`flight_id`),
    FOREIGN KEY(`fleet_id`) REFERENCES fleet(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1";

query_ngafid_db($query);

$query = "CREATE TABLE `user_preferences` (
    `user_id` INT(11) NOT NULL,
    `decimal_precision` INT(11) NOT NULL,
    `metrics` VARCHAR(4096) NOT NULL,

    PRIMARY KEY(`user_id`),
    FOREIGN KEY(`user_id`) REFERENCES user(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1";

query_ngafid_db($query);

?>
