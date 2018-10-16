<?php

$cwd[__FILE__] = __FILE__;
if (is_link($cwd[__FILE__])) $cwd[__FILE__] = readlink($cwd[__FILE__]);
$cwd[__FILE__] = dirname($cwd[__FILE__]);

require_once($cwd[__FILE__] . "/../db/my_query.php");
//require_once($cwd[__FILE__] . "/user.php");

error_log("BEFORE GET USER ID!");
foreach ($_FILES as $file) {
    error_log("file: " . json_encode($file));
}

foreach ($_GET as $key => $value) {
    error_log("_GET['$key']: '$value'");
}

foreach ($_POST as $key => $value) {
    error_log("_POST['$key']: '$value'");
}

// Get $id_token via HTTPS POST.
if (isset($_POST['id_token'])) {
    $id_token = $_POST['id_token'];
    $request_type = $_POST['request'];
} else {
    $id_token = $_GET['id_token'];
    $request_type = $_GET['request'];
}

connect_ngafid_db();
error_log("request is: $request_type");
//error_log("id_token: '$id_token'");

//Get our user ID for this email, create a new user if this user
//has not logged in before.
//$user_id = get_user_id($id_token);
$user_id = 1;

//TODO: make sure user has access to this fleet
//TODO: determine if a user could be uploading for multiple fleet
$fleet_id = 1;

//error_log("got user id: $user_id");

function escape_array($array) {
    global $ngafid_db;

    $new_array = array();
    foreach ($array as $item) {
        $new_array[] = $ngafid_db->real_escape_string($item);
    }
    return $new_array;
}



if ($request_type == "NEW_UPLOAD") {
    require_once($cwd[__FILE__] . "/upload.php");
    initiate_upload($user_id, $fleet_id);

} else if ($request_type == "UPLOAD") {
    require_once($cwd[__FILE__] . "/upload.php");
    process_chunk($user_id);

} else if ($request_type == "GET_MAIN_CONTENT") {
    require_once($cwd[__FILE__] . "/upload.php");
    require_once($cwd[__FILE__] . "/flights.php");

    $uploads = get_uploads($user_id, ["UPLOADING", "UPLOADED", "IMPORTED", "ERROR"]);
    $response['uploads'] = $uploads;

    $imports = get_uploads($user_id, ["IMPORTED", "ERROR"]);
    $response['imports'] = $imports;

    $response['flights'] = get_flights($user_id, $fleet_id);

    echo json_encode($response);

} else if ($request_type == "GET_UPLOAD_DETAILS") {
    require_once($cwd[__FILE__] . "/upload.php");
    $upload_id = $ngafid_db->real_escape_string($_POST['upload_id']);

    $response['details'] = get_upload_details($user_id, $upload_id);

    echo json_encode($response);

} else if ($request_type == "GET_COORDINATES") {
    require_once($cwd[__FILE__] . "/flights.php");
    $flight_id = $ngafid_db->real_escape_string($_POST['flight_id']);

    $response = get_coordinates($user_id, $flight_id);

    echo json_encode($response);


} else {
    error_log("ERROR! unknown request type: '$request_type'");
    $response['err_title'] = "Unknown Request Type";
    $response['err_msg'] = "The server received an unknown request type: '$request_type'";
    echo json_encode($response);
}


?>
