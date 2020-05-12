<?php
$cwd[__FILE__] = __FILE__;
if (is_link($cwd[__FILE__])) $cwd[__FILE__] = readlink($cwd[__FILE__]);
$cwd[__FILE__] = dirname($cwd[__FILE__]);

require_once($cwd[__FILE__] . "/my_query.php");


$q = "SELECT table_schema `ngafid_test`
           , SUM(data_length + index_length) `Database Size in bytes`
      FROM information_schema.TABLES
      GROUP BY table_schema";


$res = query_ngafid_db($q);
for ($x = 0; $x < 5; $x++) {
    $row = $res->fetch_array(MYSQLI_NUM);
    print_r($row);
}
?>