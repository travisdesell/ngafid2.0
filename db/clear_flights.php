<?php

$cwd[__FILE__] = __FILE__;
if (is_link($cwd[__FILE__])) $cwd[__FILE__] = readlink($cwd[__FILE__]);
$cwd[__FILE__] = dirname($cwd[__FILE__]);

require_once($cwd[__FILE__] . "/my_query.php");

$drop_tables = false;

//delete and reset events
query_ngafid_db("DELETE FROM event_statistics");
echo "deleted event statistics!\n";
query_ngafid_db("DELETE FROM events");
query_ngafid_db("ALTER TABLE events AUTO_INCREMENT = 1");
echo "deleted events!\n";
query_ngafid_db("DELETE FROM flight_processed");
echo "deleted flight_processed!\n";

query_ngafid_db("DELETE FROM turn_to_final");

//delete and reset flights
query_ngafid_db("DELETE FROM itinerary");
query_ngafid_db("ALTER TABLE itinerary AUTO_INCREMENT = 1");
echo "deleted itinerary!\n";

for ($i = 100000; $i < 38552302; $i += 100000) {
    query_ngafid_db("DELETE FROM double_series WHERE id < $i");
    echo "deleted double_series < $i!\n";
}
query_ngafid_db("DELETE FROM double_series");
query_ngafid_db("ALTER TABLE double_series AUTO_INCREMENT = 1");
echo "deleted double_series!\n";

for ($i = 10000; $i < 38000000; $i += 10000) {
    query_ngafid_db("DELETE FROM string_series WHERE id < $i");
    echo "deleted string_series < $i!\n";
}
query_ngafid_db("DELETE FROM string_series");
query_ngafid_db("ALTER TABLE string_series AUTO_INCREMENT = 1");
echo "deleted string_series!\n";

query_ngafid_db("DELETE FROM flight_warnings");
query_ngafid_db("ALTER TABLE flight_warnings AUTO_INCREMENT = 1");
echo "deleted flight_warnings!\n";
query_ngafid_db("DELETE FROM flight_errors");
query_ngafid_db("ALTER TABLE flight_errors AUTO_INCREMENT = 1");
echo "deleted flight_errors!\n";
query_ngafid_db("DELETE FROM upload_errors");
query_ngafid_db("ALTER TABLE upload_errors AUTO_INCREMENT = 1");
echo "deleted upload_errors!\n";
query_ngafid_db("DELETE FROM flight_messages");
query_ngafid_db("ALTER TABLE flight_messages AUTO_INCREMENT = 1");
echo "deleted flight_messages!\n";
query_ngafid_db("DELETE FROM flight_tag_map");
echo "deleted flight tag map!\n";
//query_ngafid_db("DELETE FROM flight_tags");
//query_ngafid_db("ALTER TABLE flight_tags AUTO_INCREMENT = 1");
//echo "deleted flight tags!\n";

for ($i = 10000; $i < 804912; $i += 10000) {
    query_ngafid_db("DELETE FROM flights WHERE id < $i");
    echo "deleted flights < $i!\n";
}

query_ngafid_db("DELETE FROM flights");
query_ngafid_db("ALTER TABLE flights AUTO_INCREMENT = 1");
echo "deleted flights, reset counter!\n";

query_ngafid_db("DELETE FROM airframes");
query_ngafid_db("ALTER TABLE airframes AUTO_INCREMENT = 1");
echo "deleted airframes, reset counter!\n";

//reset upload status
query_ngafid_db("UPDATE uploads SET status = 'UPLOADED', n_valid_flights = 0, n_warning_flights = 0, n_error_flights = 0 WHERE status = 'IMPORTED' OR status = 'ERROR'");
echo "reset uploads!\n";

?>
