<?php

$cwd[__FILE__] = __FILE__;
if (is_link($cwd[__FILE__])) $cwd[__FILE__] = readlink($cwd[__FILE__]);
$cwd[__FILE__] = dirname($cwd[__FILE__]);

require_once($cwd[__FILE__] . "/my_query.php");

$event_id = -2;

//delete and reset events
query_ngafid_db("DELETE FROM event_statistics WHERE event_definition_id = $event_id");
echo "deleted event statistics!\n";
query_ngafid_db("DELETE FROM events WHERE event_definition_id = $event_id");
echo "deleted events!\n";
query_ngafid_db("DELETE FROM flight_processed WHERE event_definition_id = $event_id");
echo "deleted flight_processed!\n";

?>
