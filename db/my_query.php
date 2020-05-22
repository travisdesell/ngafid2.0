<?php
$cwd[__FILE__] = __FILE__;
if (is_link($cwd[__FILE__])) $cwd[__FILE__] = readlink($cwd[__FILE__]);
$cwd[__FILE__] = dirname($cwd[__FILE__]);

require_once($cwd[__FILE__] . "/db_info.php");

$ngafid_db = NULL;

function db_connect($server, $user, $passwd, $db) {
    $dbcnx = new mysqli($server, $user, $passwd, $db);

    if ($dbcnx->connect_errno) {
        //echo "Failed to connect to MySQL: (" . $dbcnx->connect_errno . ") " . $dbcnx->connect_error;
        error_log("Failed to connect to MySQL: (" . $dbcnx->connect_errno . ") " . $dbcnx->connect_error);
    }   

    return $dbcnx;
}

function connect_ngafid_db() {
    global $ngafid_db, $ngafid_db_name, $ngafid_db_user, $ngafid_db_password, $ngafid_db_host;

    // don't reconnect
    if (isset($ngafid_db)) return;

    $ngafid_db = db_connect($ngafid_db_host, $ngafid_db_user, $ngafid_db_password, $ngafid_db_name);
}

function mysqli_error_msg($db, $query) {
    error_log("MYSQL Error (" . $db->errno . "): " . $db->error . ", query: $query");
    //die("MYSQL Error (" . $db->errno . "): " . $db->error . ", query: $query");
}

function query_ngafid_db($query) {
    global $ngafid_db;

    if (!$ngafid_db or !$ngafid_db->ping()) connect_ngafid_db();

    $result = $ngafid_db->query($query);

    if (!$result) mysqli_error_msg($ngafid_db, $query);

    return $result;
}

?>
