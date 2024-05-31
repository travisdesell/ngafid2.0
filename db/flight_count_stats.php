<?php

$cwd[__FILE__] = __FILE__;
if (is_link($cwd[__FILE__])) $cwd[__FILE__] = readlink($cwd[__FILE__]);
$cwd[__FILE__] = dirname($cwd[__FILE__]);

require_once($cwd[__FILE__] . "/my_query.php");


for ($year = 2015; $year < 2022; $year++) {
    for ($month = 1; $month < 13; $month++) {

        $start_date = $year . "-" . $month . "-1";
        $end_date = "";
        if ($month == 12) {
            $end_date = ($year + 1) . "-1-1";
        } else {
            $end_date = $year . "-" . ($month + 1) . "-1";
        }

        $result = query_ngafid_db("SELECT sum(number_rows) AS time_s FROM flights WHERE start_timestamp >= UNIX_TIMESTAMP(DATE('$start_date')) AND start_timestamp <= UNIX_TIMESTAMP(DATE('$end_date'))");

        while (NULL != ($row = $result->fetch_assoc())) {
            $time_s = $row['time_s'];

            echo "$start_date, " . ($time_s / (60.0 * 60.0)) . " hours\n";
        }

    }
}

?>
