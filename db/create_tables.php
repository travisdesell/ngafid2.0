<?php

$cwd[__FILE__] = __FILE__;
if (is_link($cwd[__FILE__])) $cwd[__FILE__] = readlink($cwd[__FILE__]);
$cwd[__FILE__] = dirname($cwd[__FILE__]);

require_once($cwd[__FILE__] . "/my_query.php");

$drop_tables = false;
query_ngafid_db("DROP TABLE user");
query_ngafid_db("DROP TABLE fleet");
query_ngafid_db("DROP TABLE fleet_access");

if ($drop_tables) {
    query_ngafid_db("DROP TABLE uploads");
    query_ngafid_db("DROP TABLE flights");
    query_ngafid_db("DROP TABLE itinerary");
    query_ngafid_db("DROP TABLE double_series");
    query_ngafid_db("DROP TABLE string_series");
    query_ngafid_db("DROP TABLE flight_warnings");
    query_ngafid_db("DROP TABLE flight_errors");
    query_ngafid_db("DROP TABLE upload_errors");
}


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

    PRIMARY KEY(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1";

query_ngafid_db($query);

$query = "CREATE TABLE `fleet` (
    `id` INT(11) NOT NULL AUTO_INCREMENT,
    `fleet_name` VARCHAR(256),

    PRIMARY KEY(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1";

query_ngafid_db($query);

$query = "CREATE TABLE `fleet_access` (
    `user_id` INT(11),
    `fleet_id` INT(11),
    `type` VARCHAR(32),

    PRIMARY KEY(`user_id`, `fleet_id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1";

query_ngafid_db($query);


$query = "CREATE TABLE `events` (
    `id` INT(11) NOT NULL AUTO_INCREMENT,
    `flight_id` INT(11) NOT NULL,
    `event_type` VARCHAR(128), 
    `start_line` INT(11),
    `end_line` INT(11),
    `start_time` datetime,
    `end_time` datetime,

    PRIMARY KEY(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1";

query_ngafid_db($query);


$query = "CREATE TABLE `event_type` (
    `id` INT(11) NOT NULL AUTO_INCREMENT,
    `type` INT(11),
    `name` VARCHAR(64) NOT NULL,
    
    PRIMARY KEY(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1";

query_ngafid_db($query);

?>
