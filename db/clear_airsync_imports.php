<?php

$cwd[__FILE__] = __FILE__;
if (is_link($cwd[__FILE__])) $cwd[__FILE__] = readlink($cwd[__FILE__]);
$cwd[__FILE__] = dirname($cwd[__FILE__]);

require_once($cwd[__FILE__] . "/my_query.php");

$drop_tables = false;

//delete and reset events
$query = "SELECT id FROM user WHERE first_name = 'airsync' AND last_name = 'user' AND email = 'info@airsync.com'";
$result = query_ngafid_db($query);

if (!$result) {
    echo "Unable to get AirSync user info. \n";
    exit;
}

$row = $result->fetch_assoc();
$as_uploader_id = $row['id'];

query_ngafid_db("DELETE FROM airsync_imports");
query_ngafid_db("DELETE FROM uploads WHERE uploader_id = " . $as_uploader_id . ";");
query_ngafid_db("DELETE FROM double_series WHERE flight_id IN (SELECT id FROM flights WHERE uploader_id = " . $as_uploader_id . ");");
query_ngafid_db("DELETE FROM string_series WHERE flight_id IN (SELECT id FROM flights WHERE uploader_id = " . $as_uploader_id . ");");
query_ngafid_db("DELETE FROM events WHERE flight_id IN (SELECT id FROM flights WHERE uploader_id = " . $as_uploader_id . ");");
query_ngafid_db("DELETE FROM flight_processed WHERE flight_id IN (SELECT id FROM flights WHERE uploader_id = " . $as_uploader_id . ");");
query_ngafid_db("DELETE FROM itinerary WHERE flight_id IN (SELECT id FROM flights WHERE uploader_id = " . $as_uploader_id . ");");
query_ngafid_db("DELETE FROM turn_to_final WHERE flight_id IN (SELECT id FROM flights WHERE uploader_id = " . $as_uploader_id . ");");
query_ngafid_db("DELETE FROM flights WHERE uploader_id = " . $as_uploader_id . ";");
query_ngafid_db("UPDATE airsync_fleet_info SET last_upload_time = '1970-01-01 00:00:00', mutex = 0;");

echo "Cleared all AirSync uploads from DB\n";

//TODO: write code to use env vars to clear archive dir as well
$arch_dir = getenv("NGAFID_ARCHIVE_DIR");

if (!$arch_dir) {
    echo "Cant find NGAFID_ARCHIVE_DIR in vars. Make sure you have these properly defined";
    exit;
}

$airsync_uploads = $arch_dir . "/AirSyncUploader";

system('rm -rf ' . $airsync_uploads);

echo "Cleared all AirSync uploads from Archive\n";

?>
