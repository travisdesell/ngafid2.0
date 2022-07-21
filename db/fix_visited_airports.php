<?php

$cwd[__FILE__] = __FILE__;
if (is_link($cwd[__FILE__])) $cwd[__FILE__] = readlink($cwd[__FILE__]);
$cwd[__FILE__] = dirname($cwd[__FILE__]);

require_once($cwd[__FILE__] . "/my_query.php");


$result = query_ngafid_db("SELECT id FROM fleet");

while (NULL != ($row = $result->fetch_assoc())) {
    $fleet_id = $row['id'];
    echo "fleet id: $fleet_id\n";

    $airports_result = query_ngafid_db("SELECT DISTINCT(`airport`) FROM itinerary INNER JOIN flights ON itinerary.flight_id = flights.id AND flights.fleet_id = $fleet_id");
    while (NULL != ($airport_row = $airports_result->fetch_assoc())) {
        $airport = $airport_row['airport'];

        if ($airport != '') {
            print("visited airport: '$airport'\n");
            query_ngafid_db("INSERT IGNORE INTO visited_airports SET fleet_id = $fleet_id, airport = '$airport'");
        }
    }

    $runways_result = query_ngafid_db("SELECT DISTINCT(`runway`) FROM itinerary INNER JOIN flights ON itinerary.flight_id = flights.id AND flights.fleet_id = $fleet_id");
    while (NULL != ($runway_row = $runways_result->fetch_assoc())) {
        $runway = $runway_row['runway'];

        if ($runway != '') {
            print("visited runway: '$runway'\n");
            query_ngafid_db("INSERT IGNORE INTO visited_runways SET fleet_id = $fleet_id, runway = '$runway'");
        }
    }


    exit(1);
}

