<?php

$cwd[__FILE__] = __FILE__;
if (is_link($cwd[__FILE__])) $cwd[__FILE__] = readlink($cwd[__FILE__]);
$cwd[__FILE__] = dirname($cwd[__FILE__]);

require_once($cwd[__FILE__] . "/my_query.php");

$result = query_ngafid_db("select * from tails where fleet_id = 1");

while (NULL != ($row = $result->fetch_assoc())) {
    $fleet_id = $row['fleet_id'];
    $system_id = $row['system_id'];
    $tail = $row['tail'];
    $confirmed = $row['confirmed'];

    echo "$fleet_id, $system_id, $tail, $confirmed\n";
}

