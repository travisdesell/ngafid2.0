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

    try {
        if (!$ngafid_db or !$ngafid_db->ping()) connect_ngafid_db();

        $result = $ngafid_db->query($query);

        if (!$result) mysqli_error_msg($ngafid_db, $query);
        
        return $result;
    } catch (Exception $e) {
        echo 'Caught exception: ',  $e->getMessage(), "\n";
    }
}

function get_number_rows($table) {
    $query = "SELECT COUNT(*) FROM $table";
    $result = query_ngafid_db($query);
    $all = $result->fetch_all();
    return $all[0][0];
}

function iterated_table_deletion($table, $id) {
    $size = get_number_rows($table);
    echo "size = $size \n";
    for ($i = 1; $i < $size; $i += 100000) {
        query_ngafid_db("DELETE FROM $table WHERE $id < $i");
        echo "deleted $table $id < $i!\n";
    }
}

?>
