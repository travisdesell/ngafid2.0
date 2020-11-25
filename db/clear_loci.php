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
query_ngafid_db("DELETE FROM loci_processed");
echo "deleted loci_processed!\n";


//delete and reset loci dts data
query_ngafid_db("DELETE FROM double_series WHERE name = 'True Airspeed(ft/min)' OR name = 'Stall Index' OR name = 'LOC-I Index' OR name = 'Coordination Index' OR name = 'Yaw Rate'");
echo "deleted double_series - loci data only!\n";

?>
