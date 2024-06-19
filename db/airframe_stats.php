<?php

$cwd[__FILE__] = __FILE__;
if (is_link($cwd[__FILE__])) $cwd[__FILE__] = readlink($cwd[__FILE__]);
$cwd[__FILE__] = dirname($cwd[__FILE__]);

require_once($cwd[__FILE__] . "/my_query.php");

$result = query_ngafid_db("SELECT * FROM airframes");
echo "got result!\n";

while (NULL != ($row = $result->fetch_assoc())) {
    $id = $row['id'];
    $airframe = $row['airframe'];


    //$duration_result = query_ngafid_db("SELECT sum(number_rows) AS time_s FROM flights WHERE airframe_id = $id AND start_time > '2020-01-01'");
    $duration_result = query_ngafid_db("SELECT sum(number_rows) AS time_s FROM flights WHERE airframe_id = $id");
    $duration_row = $duration_result->fetch_assoc();
    $time_s = $duration_row['time_s'];


    echo "$id -- $airframe -- " . ($time_s / (60.0 * 60.0)) . " hours\n";
}


?>
