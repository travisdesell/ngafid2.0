<?php

$cwd[__FILE__] = __FILE__;
if (is_link($cwd[__FILE__])) $cwd[__FILE__] = readlink($cwd[__FILE__]);
$cwd[__FILE__] = dirname($cwd[__FILE__]);

require_once($cwd[__FILE__] . "/my_query.php");

$drop_tables = false;

//delete and reset events
$result = query_ngafid_db("SELECT id FROM user WHERE first_name = 'airsync' AND last_name = 'user' AND email = 'info@airsync.com';");
$row = $result->fetch_assoc();
$as_uploader_id = $row['id'];

query_ngafid_db("DELETE FROM uploads WHERE uploader_id = " . $as_uploader_id . ";");
query_ngafid_db("DELETE FROM double_series WHERE flight_id IN (SELECT id FROM flights WHERE uploader_id = " . $as_uploader_id . ");");
query_ngafid_db("DELETE FROM string_series WHERE flight_id IN (SELECT id FROM flights WHERE uploader_id = " . $as_uploader_id . ");");
query_ngafid_db("DELETE FROM events WHERE flight_id IN (SELECT id FROM flights WHERE uploader_id = " . $as_uploader_id . ");");
query_ngafid_db("DELETE FROM flight_processed WHERE flight_id IN (SELECT id FROM flights WHERE uploader_id = " . $as_uploader_id . ");");
query_ngafid_db("DELETE FROM itinerary WHERE flight_id IN (SELECT id FROM flights WHERE uploader_id = " . $as_uploader_id . ");");
query_ngafid_db("DELETE FROM flights WHERE uploader_id = " . $as_uploader_id . ";");
query_ngafid_db("DELETE FROM airsync_imports;");

echo "Cleared all AirSync uploads\n";

?>
