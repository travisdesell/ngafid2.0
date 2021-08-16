<?php

$cwd[__FILE__] = __FILE__;
if (is_link($cwd[__FILE__])) $cwd[__FILE__] = readlink($cwd[__FILE__]);
$cwd[__FILE__] = dirname($cwd[__FILE__]);

require_once($cwd[__FILE__] . "/my_query.php");



$result = query_ngafid_db("SELECT * FROM fleet_airframes");

while (NULL != ($row = $result->fetch_assoc())) {
    $fleet_id = $row['fleet_id'];
    $airframe_id = $row['airframe_id'];

    $count_result = query_ngafid_db("SELECT count(*) AS cnt FROM flights WHERE fleet_id = $fleet_id and airframe_id = $airframe_id ");
    $count_row = $count_result->fetch_assoc();
    $count = $count_row['cnt'];

    echo "fleet id: $fleet_id, aairframe id: $airframe_id, count: $count\n";

    if ($count == 0) {
        query_ngafid_db("DELETE FROM fleet_airframes WHERE fleet_id = $fleet_id AND airframe_id = $airframe_id");
    }
}
