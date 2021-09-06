<?php

$cwd[__FILE__] = __FILE__;
if (is_link($cwd[__FILE__])) $cwd[__FILE__] = readlink($cwd[__FILE__]);
$cwd[__FILE__] = dirname($cwd[__FILE__]);

require_once($cwd[__FILE__] . "/my_query.php");

/*
$count = 1;
while ($count > 0) {
    $result = query_ngafid_db("SELECT * FROM flight_warnings WHERE message_id = 5 LIMIT 100");

    $count = 0;
    while (NULL != ($row = $result->fetch_assoc())) {
        $count++;
        $warning_id = $row['id'];
        $flight_id = $row['flight_id'];
        $message_id = $row['message_id'];

        echo "had warning with id: $warning_id, flight_id: $flight_id, message_id: $message_id";

        $airframe_result = query_ngafid_db("SELECT airframe_id FROM flights WHERE id = $flight_id");
        $airframe_row = $airframe_result->fetch_assoc();
        $airframe_id = $airframe_row['airframe_id'];

        $processed_result = query_ngafid_db("SELECT had_error FROM flight_processed WHERE flight_id = $flight_id");
        $processed_row = $processed_result->fetch_assoc();
        $had_error = $processed_row['had_error'];

        echo " and airframe_id: $airframe_id, had error? $had_error";

        if ($airframe_id == 1) {
            echo " -- WAS PA28!\n";
            query_ngafid_db("DELETE FROM flight_warnings WHERE id = $warning_id");
        } else {
            echo "\n";
        }

    }

    echo "finished loop!!!\n\n\n";
}
*/

$result = query_ngafid_db("SELECT id, n_warning_flights FROM uploads");

while (NULL != ($row = $result->fetch_assoc())) {
    $upload_id = $row['id'];
    $n_warning_flights = $row['n_warning_flights'];

    $n_warnings_result = query_ngafid_db("SELECT count(*) FROM flights WHERE upload_id = $upload_id AND EXISTS (SELECT * FROM flight_warnings WHERE flight_id = flights.id)");
    $n_warnings_row = $n_warnings_result->fetch_assoc();
    $n_warnings = $n_warnings_row['count(*)'];

    echo "upload n_warnings: $n_warning_flights, warning flights in database: $n_warnings\n";
    if ($n_warning_flights != $n_warnings) {
        $query = "UPDATE uploads SET n_warning_flights = $n_warnings WHERE id = $upload_id";
        echo "$query\n\n";
        query_ngafid_db($query);
    }
}


