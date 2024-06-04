<?php

$cwd[__FILE__] = __FILE__;
if (is_link($cwd[__FILE__])) $cwd[__FILE__] = readlink($cwd[__FILE__]);
$cwd[__FILE__] = dirname($cwd[__FILE__]);

require_once($cwd[__FILE__] . "/my_query.php");

$result = query_ngafid_db("SELECT DISTINCT(upload_id) FROM flights");

while (NULL != ($row = $result->fetch_assoc())) {
    $upload_id = $row['upload_id'];
    //print("distinct upload id: $upload_id\n");

    $flight_result = query_ngafid_db("SELECT id, filename FROM uploads WHERE id = $upload_id");

    if (NULL != ($flight_row = $flight_result->fetch_assoc())) {
        //print("\tupload exists in db!\n");
    } else {
        print("\tUPLOAD $upload_id DOES NOT EXIST!\n");
    }

}

