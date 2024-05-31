<?php

$cwd[__FILE__] = __FILE__;
if (is_link($cwd[__FILE__])) $cwd[__FILE__] = readlink($cwd[__FILE__]);
$cwd[__FILE__] = dirname($cwd[__FILE__]);

require_once($cwd[__FILE__] . "/my_query.php");



$result = query_ngafid_db("SELECT id, airframe FROM airframes");

echo "distinct system ids by airframe:\n";
while (NULL != ($row = $result->fetch_assoc())) {
    $airframe_id = $row['id'];
    $airframe_name = $row['airframe'];

    $count_query = "SELECT DISTINCT(system_id) FROM flights WHERE airframe_id = $airframe_id";
    $count_result = query_ngafid_db($count_query);

    $count = 0;
    while (NULL != ($count_row = $count_result->fetch_assoc())) {
        $count += 1;
    }
    echo "\t$airframe_name : $count\n";
}

$result = query_ngafid_db("SELECT id, name FROM airframe_types");

echo "distinct system ids by airframe type:\n";
while (NULL != ($row = $result->fetch_assoc())) {
    $airframe_type_id = $row['id'];
    $type_name = $row['name'];

    $count_query = "SELECT DISTINCT(system_id) FROM flights WHERE airframe_type_id = $airframe_type_id";
    $count_result = query_ngafid_db($count_query);

   $count = 0;
    while (NULL != ($count_row = $count_result->fetch_assoc())) {
        $count += 1;
    }
    echo "\t$type_name : $count\n";
}


?>
