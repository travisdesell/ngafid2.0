<?php

function get_flights($user_id, $fleet_id) {
    //TODO: check if user has access to this fleet

    $query = "SELECT * FROM flights WHERE fleet_id = $fleet_id LIMIT 30";
    $result = query_ngafid_db($query);
    $flights = array();
    while (($row = $result->fetch_assoc()) != NULL) {
        $flight_id = $row['id'];

        $itinerary_query = "SELECT `order`, min_altitude_index, min_altitude, airport, runway FROM itinerary WHERE flight_id = $flight_id ORDER BY `order`";
        $itinerary_result = query_ngafid_db($itinerary_query);

        $row['itinerary'] = array();
        while (($itinerary_row = $itinerary_result->fetch_assoc()) != NULL) {
            $row['itinerary'][] = $itinerary_row;
        }

        $flights[] = $row;
    }

    return $flights;
}

function get_coordinates($user_id, $flight_id) {
    //TODO: check if user has access to this flight

    $query = "SELECT * FROM double_series WHERE flight_id = $flight_id AND name = 'Longitude'";
    $longitudes = array();

    $result = query_ngafid_db($query);
    if (($row = $result->fetch_assoc()) != NULL) {
        $values = $row['values'];
        $longitudes = unpack("E*", $values);
    }

    $query = "SELECT * FROM double_series WHERE flight_id = $flight_id AND name = 'Latitude'";
    $latitudes = array();

    $result = query_ngafid_db($query);
    if (($row = $result->fetch_assoc()) != NULL) {
        $values = $row['values'];
        $latitudes = unpack("E*", $values);
    }

    if (count($latitudes) != count($longitudes)) {
        error_log("ERROR! flight data error, latitude and longitude columns not equal length");
        $response['err_title'] = "Coordinate Lookup Failure";
        $response['err_msg'] = "Flight data error, latitude and longitude columns not equal length";
        echo json_encode($response);
        exit(1);
    }

    $coordinates = array();
    $length = count($latitudes);
    for ($i = 1; $i <= $length; $i++) {
        if (is_nan($latitudes[$i]) || is_nan($longitudes[$i])) continue;

        $coordinates[] = [$longitudes[$i], $latitudes[$i]];
    }

    //error_log(json_encode($coordinates));

    $response['coordinates'] = $coordinates;

    return $response;
}

function get_double_series($user_id, $flight_id, $series_name) {
    //TODO: check if user has access to this flight

    $query = "SELECT * FROM double_series WHERE flight_id = $flight_id AND name = '$series_name'";
    error_log($query);

    $values = array();

    $result = query_ngafid_db($query);
    if (($row = $result->fetch_assoc()) != NULL) {
        $values = $row['values'];
        $values = unpack("E*", $values);
    }

    $values_fixed = array();
    $length = count($values);
    for ($i = 1; $i <= $length; $i++) {
        if (is_nan($values[$i])) continue;

        $values_fixed[] = $values[$i];
    }


    error_log(json_encode($values_fixed));

    $response['values'] = $values_fixed;

    return $response;
}


?>
