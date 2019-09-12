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
echo "deleted events!\n";
query_ngafid_db("DELETE FROM flight_processed");
echo "deleted flight_processed!\n";


//delete and reset flights
query_ngafid_db("DELETE FROM itinerary");
echo "deleted itinerary!\n";
query_ngafid_db("DELETE FROM double_series");
echo "deleted double_series!\n";
query_ngafid_db("DELETE FROM string_series");
echo "deleted string_series!\n";
query_ngafid_db("DELETE FROM flight_warnings");
echo "deleted flight_warnings!\n";
query_ngafid_db("DELETE FROM flight_errors");
echo "deleted flight_errors!\n";
query_ngafid_db("DELETE FROM upload_errors");
echo "deleted upload_errors!\n";
query_ngafid_db("DELETE FROM flight_messages");
echo "deleted flight_messages!\n";

query_ngafid_db("DELETE FROM flights");
echo "deleted flights!\n";

//reset upload status
query_ngafid_db("UPDATE uploads SET status = 'UPLOADED', n_valid_flights = 0, n_warning_flights = 0, n_error_flights = 0 WHERE status = 'IMPORTED' OR status = 'ERROR'");
echo "reset uploads!\n";

?>
