<?php

$cwd[__FILE__] = __FILE__;
if (is_link($cwd[__FILE__])) $cwd[__FILE__] = readlink($cwd[__FILE__]);
$cwd[__FILE__] = dirname($cwd[__FILE__]);

require_once($cwd[__FILE__] . "/my_query.php");

$drop_tables = false;

query_ngafid_db("DROP TABLE users");
query_ngafid_db("DROP TABLE uploads");
query_ngafid_db("DROP TABLE flights");
query_ngafid_db("DROP TABLE itinerary");
query_ngafid_db("DROP TABLE double_series");
query_ngafid_db("DROP TABLE string_series");
query_ngafid_db("DROP TABLE flight_warnings");
query_ngafid_db("DROP TABLE flight_errors");
query_ngafid_db("DROP TABLE upload_errors");

if ($drop_tables) {
}


$query = "CREATE TABLE `users` (
    `id` INT(11) NOT NULL AUTO_INCREMENT,
    `email` VARCHAR(256) NOT NULL,
    `name` VARCHAR(128) NOT NULL,
    `given_name` VARCHAR(64) NOT NULL,
    `family_name` VARCHAR(64) NOT NULL,

    PRIMARY KEY(`id`),
    UNIQUE KEY(`email`)
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
    UNIQUE KEY (`fleet_id`, `md5_hash`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1";

query_ngafid_db($query);

$query = "CREATE TABLE `flights` (
    `id` INT(11) NOT NULL AUTO_INCREMENT,
    `fleet_id` INT(11) NOT NULL,
    `uploader_id` INT(11) NOT NULL,
    `upload_id` INT(11) NOT NULL,
    `tail_number` VARCHAR(16),
    `airframe_type` VARCHAR(64),
    `start_time` DATETIME,
    `end_time` DATETIME,
    `filename` VARCHAR(256),
    `md5_hash` VARCHAR(32),
    `number_rows` INT(11),
    `status` VARCHAR(32) NOT NULL,
    `has_coords` TINYINT(1) NOT NULL,
    `has_agl` TINYINT(1) NOT NULL,
    `events_calculated` INT(1) NOT NULL DEFAULT 0,

    PRIMARY KEY(`id`),
    UNIQUE KEY(`md5_hash`)
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

    PRIMARY KEY(`id`)
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

    PRIMARY KEY(`id`)
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

    PRIMARY KEY(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1";

query_ngafid_db($query);

$query = "CREATE TABLE `flight_warnings` (
    `id` INT(11) NOT NULL AUTO_INCREMENT,
    `flight_id` INT(11) NOT NULL,
    `message` VARCHAR(512) NOT NULL,
    `stack_trace` VARCHAR(2048) NOT NULL,

    PRIMARY KEY(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1";

query_ngafid_db($query);

$query = "CREATE TABLE `flight_errors` (
    `id` INT(11) NOT NULL AUTO_INCREMENT,
    `upload_id` INT(11) NOT NULL,
    `filename` VARCHAR(512) NOT NULL,
    `message` VARCHAR(512) NOT NULL,
    `stack_trace` VARCHAR(2048) NOT NULL,

    PRIMARY KEY(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1";

query_ngafid_db($query);


$query = "CREATE TABLE `upload_errors` (
    `id` INT(11) NOT NULL AUTO_INCREMENT,
    `upload_id` INT(11) NOT NULL,
    `message` VARCHAR(512) NOT NULL,
    `stack_trace` VARCHAR(2048) NOT NULL,

    PRIMARY KEY(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1";

query_ngafid_db($query);






?>
