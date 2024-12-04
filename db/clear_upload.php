<?php

$cwd[__FILE__] = __FILE__;
if (is_link($cwd[__FILE__])) $cwd[__FILE__] = readlink($cwd[__FILE__]);
$cwd[__FILE__] = dirname($cwd[__FILE__]);

require_once($cwd[__FILE__] . "/my_query.php");

$count = 0;

$result = query_ngafid_db("SELECT id FROM flights WHERE upload_id = 4137");
while (NULL != ($row = $result->fetch_assoc())) {
    //print_r($row);
    $id = $row['id'];
    print("$count - flight id: $id\n");

    query_ngafid_db("DELETE from double_series WHERE flight_id = $id");
    query_ngafid_db("DELETE from string_series WHERE flight_id = $id");

    $event_result = query_ngafid_db("SELECT id FROM events WHERE flight_id = $id AND event_definition_id = -1");
    $event_count = 0;
    while (NULL != ($event_row = $event_result->fetch_assoc())) {
        $event_id = $event_row['id'];
        print("proximity event! id: $event_id\n");
        $event_count++;

        query_ngafid_db("DELETE FROM rate_of_closure WHERE event_id = $event_id");
    }

    query_ngafid_db("DELETE from events WHERE flight_id = $id");
    if ($event_count > 0) exit(1);
    query_ngafid_db("DELETE from flight_processed WHERE flight_id = $id");
    query_ngafid_db("DELETE from itinerary WHERE flight_id = $id");
    query_ngafid_db("DELETE from turn_to_final WHERE flight_id = $id");
    //query_ngafid_db("DELETE from rate_of_closure WHERE flight_id = $id");

    $count++;
}
