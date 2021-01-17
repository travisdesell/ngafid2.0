<?php

$cwd[__FILE__] = __FILE__;
if (is_link($cwd[__FILE__])) $cwd[__FILE__] = readlink($cwd[__FILE__]);
$cwd[__FILE__] = dirname($cwd[__FILE__]);

require_once($cwd[__FILE__] . "/my_query.php");

//need this for changes to allow for display of severity webpages
query_ngafid_db("alter table events add column `fleet_id` INT(11) after `id`");

//need to update this to start tracking the time of user creation
query_ngafid_db("alter table `user` add column `registration_time` DATETIME DEFAULT NULL");


?>
