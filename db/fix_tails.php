<?php

$cwd[__FILE__] = __FILE__;
if (is_link($cwd[__FILE__])) $cwd[__FILE__] = readlink($cwd[__FILE__]);
$cwd[__FILE__] = dirname($cwd[__FILE__]);

require_once($cwd[__FILE__] . "/my_query.php");


$tails = ["N297ND", "N491ND", "N492ND", "N494ND", "N510ND", "N511ND", "N512ND", "N513ND", "N514ND", "N515ND", "N516ND", "N517ND", "N519ND", "N520ND", "N525ND", "N528ND", "N530ND", "N531ND", "N532ND", "N538ND", "N539ND", "N540ND", "N543ND", "N545ND", "N551ND", "N552ND", "N560ND", "N563ND", "N564ND", "N565ND", "N566ND", "N567ND", "N568ND", "N569ND", "N570ND", "N574ND", "N577ND", "N580ND", "N581ND", "N582ND", "N583ND", "N585ND", "N586ND", "N588ND", "N590ND", "N593ND", "N595ND", "N596ND", "N598ND", "N599ND", "N608ND", "N652ND", "N653ND", "N654ND", "N655ND", "N677ND", "N697ND", "N740ND", "N781ND", "N745ND", "N783ND", "N719ND", "N770ND", "N732ND", "N695ND", "N782ND", "N718ND", "N731ND", "N785ND", "N641ND", "N769ND"];


$missing_tails = [];
$misassigned_tails = [];


foreach ($tails as $path_tail) {
    $result = query_ngafid_db("select distinct(system_id) from flights where filename like '%$path_tail%' and fleet_id = 1");

    echo "tail: '$path_tail'\n";

    $count = 0;
    while (NULL != ($row = $result->fetch_assoc())) {
        $system_id = $row['system_id'];
        echo "\tsystem id: $system_id\n";
        $count++;

        $assigned_tail_result = query_ngafid_db("SELECT tail FROM tails WHERE system_id = '$system_id' and fleet_id = 1");
        while (NULL != ($assigned_tail_row = $assigned_tail_result->fetch_assoc())) {
            $assigned_tail = $assigned_tail_row['tail'];
            echo "\t\tassigned tail: '" . $assigned_tail . "'\n";

            if ($assigned_tail != $path_tail) {
                if ($assigned_tail == "fdm") {
                    query_ngafid_db("UPDATE tails SET tail = '$path_tail' WHERE system_id = '$system_id' AND fleet_id = 1");
                } else {
                    $res_assigned = query_ngafid_db("SELECT count(*) FROM flights WHERE system_id = '$system_id' AND filename like '%$assigned_tail%'");
                    $row_assigned = $res_assigned->fetch_assoc();
                    $assigned_count = $row_assigned['count(*)'];
                    echo "\t\t\tassigned count: $assigned_count\n";

                    $res_path = query_ngafid_db("SELECT count(*) FROM flights WHERE system_id = '$system_id' AND filename like '%$path_tail%'");
                    $row_path = $res_path->fetch_assoc();
                    $path_count = $row_path['count(*)'];
                    echo "\t\t\tpath count: $path_count\n";

                    $misassigned_tails[] = [ "path_tail" => $path_tail, "path_count" => $path_count, "system_id" => $system_id, "assigned_tail" => $assigned_tail, "assigned_count" => $assigned_count ];

                    if ($path_count > $assigned_count) {
                        echo("\t\tWILL BE UPDATING TAIL!\n\n");
                        query_ngafid_db("UPDATE tails SET tail = '$path_tail' WHERE system_id = '$system_id' AND fleet_id = 1");

                    }
                }
            }
        }
    }

    if ($count == 0) $missing_tails[] = $path_tail;
}

echo "tails not matched to any system id: [";

for ($i = 0; $i < count($missing_tails); $i++) {
    if ($i > 0) echo ", ";
    echo "\"" . $missing_tails[$i] . "\"";
}
echo "]\n";

echo "misassigned tails:\n";
foreach ($misassigned_tails as $misassigned_tail) {
    $path_tail = $misassigned_tail['path_tail'];
    $path_count = $misassigned_tail['path_count'];
    $system_id = $misassigned_tail['system_id'];
    $assigned_tail = $misassigned_tail['assigned_tail'];
    $assigned_count = $misassigned_tail['assigned_count'];

    echo "$path_tail ($path_count) $system_id $assigned_tail ($assigned_count) \n";
}

