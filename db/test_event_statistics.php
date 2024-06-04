<?php
function time_query($query) {
    $startTime = microtime(true);
    # NOTE THESE VALUES SHOULD BE MODIFIED TO YOUR SETUP
    $mysqli = new mysqli("127.0.0.1", "ngafid_user", "creepy crawly critters", "ngafid");
    if ($mysqli->connect_error) {
        die("Connection failed: " . $mysqli->connect_error);
    }
    $result = $mysqli->query($query);
    $endTime = microtime(true);
    $executionTime = $endTime - $startTime;
    $mysqli->close();
    return $executionTime;
}

# Get counts for each event
$query = "SELECT events.event_definition_id, COUNT(*) AS event_count FROM events GROUP BY events.event_definition_id";
$executionTime = time_query($query);
echo "Getting event id and count took $executionTime seconds\n";

# Get airframe and counts for each event 
$query = "SELECT events.event_definition_id, event_definitions.airframe_id, COUNT(*) AS event_count FROM events JOIN event_definitions ON events.event_definition_id = event_definitions.id GROUP BY events.event_definition_id, event_definitions.airframe_id";
$executionTime = time_query($query);
echo "Getting event_definition_id, airframe_id, event_count took $executionTime seconds\n";

# getEventCounts function in EventStatistics.java
$query = "SELECT airframe_id, event_definition_id, flights_with_event, total_flights, total_events FROM event_statistics WHERE flights_with_event > 0";
$executionTime = time_query($query);
echo "Getting all event counts filtering flights with no event took $executionTime seconds\n";

# getMonthlyEventCounts function in EventStatistics.java
$query = "SELECT event_definition_id, fleet_id, airframe_id, DATE_FORMAT(month_first_day, '%Y-%m') AS month_year, SUM(total_events) AS total_events FROM event_statistics GROUP BY event_definition_id, fleet_id, airframe_id, month_year ORDER BY event_definition_id, fleet_id, airframe_id, month_year";
$executionTime = time_query($query);
echo "Getting all event counts by month took $executionTime seconds\n";

# Percentage of Flights With Event (Modify the 1 in fleet_id = 1 with whatever fleet id you want)
$query = "SELECT event_definition_id, fleet_id, SUM(CASE WHEN fleet_id = 1 THEN 1 ELSE 0 END) AS events_with_given_fleet, SUM(1) AS total_events, (SUM(CASE WHEN fleet_id = 1 THEN 1 ELSE 0 END) / SUM(1)) * 100 AS percentage FROM event_statistics GROUP BY event_definition_id, fleet_id";
$executionTime = time_query($query);
echo "Getting flight event percentages by fleet took $executionTime seconds\n";
?>
