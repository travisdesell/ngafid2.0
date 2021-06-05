<?php

$cwd[__FILE__] = __FILE__;
if (is_link($cwd[__FILE__])) $cwd[__FILE__] = readlink($cwd[__FILE__]);
$cwd[__FILE__] = dirname($cwd[__FILE__]);

require_once($cwd[__FILE__] . "/my_query.php");



/*
//need this for changes to allow for display of severity webpages
query_ngafid_db("alter table events add column `fleet_id` INT(11) after `id`");

//need to update this to start tracking the time of user creation
query_ngafid_db("alter table `user` add column `registration_time` DATETIME DEFAULT NULL");

//need this for proximity events
query_ngafid_db("alter table events add column `other_flight_id` INT(11) after `severity`");
query_ngafid_db("REPLACE INTO event_definitions SET id = -1, fleet_id = 0, airframe_id = 0, name = 'Proximity', start_buffer = 1, stop_buffer = 30, column_names = '[\"AltAGL\", \"AltMSL\", \"Latitude\", \"Longitude\", \"Lcl Date\", \"Lcl Time\", \"UTCOfst\"]', condition_json ='{\"text\" : \"Aircraft within 500 ft of another aircraft and above above 50ft AGL\"}', severity_column_names = '[]', severity_type = 'min', color = 'null'");

query_ngafid_db("alter table flights add column start_timestamp INT(11) after end_time");
query_ngafid_db("alter table flights add column end_timestamp INT(11) after start_timestamp");
query_ngafid_db("update flights set start_timestamp = UNIX_TIMESTAMP(start_time), end_timestamp = UNIX_TIMESTAMP(end_time)");
query_ngafid_db("alter table flights add index `start_timestamp_index` (`start_timestamp`) using btree");
query_ngafid_db("alter table flights add index `end_timestamp_index` (`end_timestamp`) using btree");
*/
//
//new double_series_names table and user preferences update
//
query_ngafid_db("alter table double_series change name name_id int not null");
query_ngafid_db("alter table user_preferences drop column metrics");
$query = "CREATE TABLE `double_series_names` (

    `id` INT(11) NOT NULL AUTO_INCREMENT,
    `name` VARCHAR(64) NOT NULL,

    PRIMARY KEY(`id`),
    UNIQUE KEY(`id`, `name`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1";

query_ngafid_db($query);

$query = "CREATE TABLE `user_preferences_metrics` (
    `user_id` INT(11) NOT NULL,
    `metric_id` INT(11) NOT NULL,

    PRIMARY KEY(`user_id`,`metric_id`),
    FOREIGN KEY(`user_id`) REFERENCES user(`id`),
    FOREIGN KEY(`metric_id`) REFERENCES double_series_names(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1";

query_ngafid_db($query);

//Saved filters table
//TODO: replace this
query_ngafid_db($query);

//for user preferences
/*
$query = "CREATE TABLE `user_preferences` (
    `user_id` INT(11) NOT NULL,
    `decimal_precision` INT(11) NOT NULL,
    `metrics` VARCHAR(4096) NOT NULL,

    PRIMARY KEY(`user_id`),
    FOREIGN KEY(`user_id`) REFERENCES user(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1";

query_ngafid_db($query);
*/


//need to update this to be able to differentiate between flights by type (e.g., fixed wing, rotorcraft, uas)
/*
$query = "CREATE TABLE `airframe_types` (
    `id` INT(11) NOT NULL AUTO_INCREMENT,
    `name` VARCHAR(32) NOT NULL,

    PRIMARY KEY(`id`),
    UNIQUE KEY `name_key` (`name`) 
) ENGINE=InnoDB DEFAULT CHARSET=latin1";
query_ngafid_db($query);
query_ngafid_db("INSERT INTO airframe_types SET name = 'Fixed Wing'");
query_ngafid_db("INSERT INTO airframe_types SET name = 'Rotorcraft'");
query_ngafid_db("INSERT INTO airframe_types SET name = 'UAS Fixed Wing'");
query_ngafid_db("INSERT INTO airframe_types SET name = 'UAS Rotorcraft'");

query_ngafid_db("alter table `flights` add column `airframe_type_id` INT(11) NOT NULL AFTER `airframe_id`");
query_ngafid_db("ALTER TABLE `flights` ADD CONSTRAINT `airframe_type_key` FOREIGN KEY (`airframe_type_id`) REFERENCES airframe_types(`id`)");
query_ngafid_db("ALTER TABLE `flights` ADD INDEX `airframe_type_id_index` (`airframe_type_id`)");
 */



/*
    +----+----------------------------+
    | id | airframe                   |
    +----+----------------------------+
    | 10 | Cessna 172R                |
    |  2 | Cessna 172S                |
    |  8 | Cessna 182T                |
    |  4 | Cirrus SR20                |
    |  7 | Diamond DA 40              |
    | 11 | Diamond DA 40 F            |
    | 14 | Diamond DA40               |
    |  6 | Diamond DA40NG             |
    | 13 | Diamond DA42NG             |
    |  5 | Garmin Flight Display      |
    |  1 | PA-28-181                  |
    |  3 | PA-44-180                  |
    |  9 | Piper PA-46-500TP Meridian |
    | 12 | Unknown Aircraft           |
    +----+----------------------------+
    14 rows in set (0.01 sec)
 */

//1 - PA-28-181
//2 - Cessna 172S
//3 - PA-44-180
//4 - Cirrus SR20

query_ngafid_db("INSERT INTO airframes SET airframe = 'PA-28-181'");
query_ngafid_db("INSERT INTO airframes SET airframe = 'Cessna 172S'");
query_ngafid_db("INSERT INTO airframes SET airframe = 'PA-44-180'");
query_ngafid_db("INSERT INTO airframes SET airframe = 'Cirrus SR20'");


query_ngafid_db("alter table event_definitions modify column severity_type VARCHAR(7)");                 
query_ngafid_db("update event_definitions set severity_type = 'max abs' where severity_type = 'abs'");   


/*
 * +-----------------------+--------------+------+-----+---------+----------------+
 * | Field                 | Type         | Null | Key | Default | Extra          |
 * +-----------------------+--------------+------+-----+---------+----------------+
 * | id                    | int(11)      | NO   | PRI | NULL    | auto_increment |
 * | fleet_id              | int(11)      | NO   |     | NULL    |                |
 * | airframe_id           | int(11)      | NO   |     | NULL    |                |
 * | name                  | varchar(64)  | NO   |     | NULL    |                |
 * | start_buffer          | int(11)      | YES  |     | NULL    |                |
 * | stop_buffer           | int(11)      | YES  |     | NULL    |                |
 * | column_names          | varchar(128) | YES  |     | NULL    |                |
 * | condition_json        | varchar(512) | YES  |     | NULL    |                |
 * | severity_column_names | varchar(128) | YES  |     | NULL    |                |
 * | severity_type         | varchar(3)   | YES  |     | NULL    |                |
 * | color                 | varchar(6)   | YES  |     | NULL    |                |
 * +-----------------------+--------------+------+-----+---------+----------------+
 */

query_ngafid_db("alter table user add column admin BOOLEAN default 0");
query_ngafid_db("alter table user add column aggregate_view BOOLEAN default 0");

?>
