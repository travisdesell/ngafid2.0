<?php

$cwd[__FILE__] = __FILE__;
if (is_link($cwd[__FILE__])) $cwd[__FILE__] = readlink($cwd[__FILE__]);
$cwd[__FILE__] = dirname($cwd[__FILE__]);

require_once($cwd[__FILE__] . "/my_query.php");

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

?>
