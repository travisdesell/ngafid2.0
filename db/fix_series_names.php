<?php

$cwd[__FILE__] = __FILE__;
if (is_link($cwd[__FILE__])) $cwd[__FILE__] = readlink($cwd[__FILE__]);
$cwd[__FILE__] = dirname($cwd[__FILE__]);

require_once($cwd[__FILE__] . "/my_query.php");


function set_name_ids($type) {
    $result = query_ngafid_db("SELECT distinct(name) FROM " . $type . "_series");

    while (NULL != ($row = $result->fetch_assoc())) {
        $name = $row['name'];

        echo "distinct $type series name: '$name'\n";
        query_ngafid_db("INSERT INTO " . $type . "_series_names SET name = '$name'");
    }

    $result = query_ngafid_db("SELECT distinct(data_type) FROM " . $type . "_series");

    while (NULL != ($row = $result->fetch_assoc())) {
        $name = $row['data_type'];

        echo "distinct $type data type: '$name'\n";
        if ($name == "") $name = "none";

        query_ngafid_db("INSERT INTO data_type_names SET name = '$name'");
    }
}

//set_name_ids("double");
//set_name_ids("string");

function assign_ids($type) {
    $result = query_ngafid_db("SELECT min(id) FROM " . $type . "_series");
    $row = $result->fetch_assoc();
    $min_id = $row['min(id)'];

    $result = query_ngafid_db("SELECT max(id) FROM " . $type . "_series");
    $row = $result->fetch_assoc();
    $max_id = $row['max(id)'];

    $increment = 100000;

    echo "min_id: $min_id, max_id: $max_id, increment: $increment\n";

    for ($i = $min_id; $i < $max_id; $i += $increment) {
        echo "\r$i / " . ($max_id);

        $query = "UPDATE " . $type . "_series SET name_id = (SELECT id FROM " . $type . "_series_names WHERE " . $type . "_series_names.name = " . $type . "_series.name) WHERE id >= $i AND id <= " . ($i + $increment);
        //echo "\n" . $query . "\n";
        query_ngafid_db($query);
    }
    echo "\n";

    for ($i = $min_id; $i < $max_id; $i += $increment) {
        echo "\r$i / " . ($max_id);

        $query = "UPDATE " . $type . "_series SET data_type_id = (SELECT id FROM data_type_names WHERE data_type_names.name = " . $type ."_series.data_type) WHERE id >= $i AND id <= " . ($i + $increment);
        //echo "\n" . $query . "\n";
        query_ngafid_db($query);
    }
    echo "\n";

}

//assign_ids("double");
//assign_ids("string");

function fix_nulls($type) {
    $result = query_ngafid_db("SELECT min(id) FROM " . $type . "_series");
    $row = $result->fetch_assoc();
    $min_id = $row['min(id)'];

    $result = query_ngafid_db("SELECT max(id) FROM " . $type . "_series");
    $row = $result->fetch_assoc();
    $max_id = $row['max(id)'];

    $increment = 100000;

    echo "min_id: $min_id, max_id: $max_id, increment: $increment\n";

    for ($i = $min_id; $i < $max_id; $i += $increment) {
        echo "\r$i / " . ($max_id);

        $query = "update " . $type . "_series set data_type_id = (SELECT id FROM data_type_names WHERE name = 'none'), data_type = 'none' where data_type_id is null AND id >= $i AND id <= " . ($i + $increment);
        //echo "\n" . $query . "\n";
        query_ngafid_db($query);
    }
    echo "\n";

}

//fix_nulls("string");
//fix_nulls("double");



//query_ngafid_db("ALTER TABLE string_series ADD CONSTRAINT `name_ibfk_1` FOREIGN KEY(`name_id`) REFERENCES string_series_names(`id`), ADD CONSTRAINT `type_ibfk_1` FOREIGN KEY(`data_type_id`) REFERENCES data_type_names(`id`)");
//query_ngafid_db("ALTER TABLE string_series DROP COLUMN `name`, DROP COLUMN `data_type`, ALGORITHM=INPLACE, LOCK=NONE");

//query_ngafid_db("ALTER TABLE double_series ADD CONSTRAINT `double_series_name_ibfk_1` FOREIGN KEY(`name_id`) REFERENCES double_series_names(`id`), ADD CONSTRAINT `double_series_type_ibfk_1` FOREIGN KEY(`data_type_id`) REFERENCES data_type_names(`id`)");
//query_ngafid_db("ALTER TABLE double_series DROP COLUMN `name`, DROP COLUMN `data_type`, LOCK=SHARED");

