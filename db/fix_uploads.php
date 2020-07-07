<?php

$cwd[__FILE__] = __FILE__;
if (is_link($cwd[__FILE__])) $cwd[__FILE__] = readlink($cwd[__FILE__]);
$cwd[__FILE__] = dirname($cwd[__FILE__]);

require_once($cwd[__FILE__] . "/my_query.php");

$drop_tables = false;

//delete and reset events
$result = query_ngafid_db("SELECT id, fleet_id, uploader_id, filename, size_bytes, md5_hash FROM uploads");

$archive_dir = getenv("NGAFID_ARCHIVE_DIR");

$count = 0;
$error_count = 0;
$valid_count = 0;

$fleet_errors = array();

while (NULL != ($row = $result->fetch_assoc())) {
    //print_r($row);

    $id = $row['id'];
    $fleet_id = $row['fleet_id'];
    $uploader_id = $row['uploader_id'];
    $filename = $row['filename'];
    $size_bytes = $row['size_bytes'];
    $md5_hash = $row['md5_hash'];

    $input_filename = $archive_dir . "/" . $fleet_id . "/" . $uploader_id . "/" . $filename;
    if (!file_exists($input_filename)) {
        echo "ERROR! trying to fix input file which does not exist!\n";
        continue;
    }

    $input_bytes = filesize($input_filename);
    $input_md5 = md5_file($input_filename);
    echo "input filename: '$input_filename'\n";
    echo "input bytes: '$input_bytes'\n";
    echo "input md5: '$input_md5'\n";

    if ($input_md5 == $md5_hash) {
        if ($input_bytes == $size_bytes) {
            $valid_count++;

            $output_filename = $archive_dir . "/" . $fleet_id . "/" . $uploader_id . "/" . $id . "__" . $filename;
            echo "output filename: '$output_filename'\n";

            if (file_exists($output_filename)) {
                echo "ERROR! trying to move an uploaded file to a file that already exists, this should never happen!\n";
                exit(1);
            }

            rename($input_filename, $output_filename);
        } else {
            echo "ERROR! md5 hashes were the same but size in bytes was not! this should not happen!\n";
            exit(1);
        }
    } else {
        $error_count++;
        if (array_key_exists($fleet_id, $fleet_errors)) {
            $fleet_errors[$fleet_id]++;
        } else {
            $fleet_errors[$fleet_id] = 1;
        }
    }
    $count++;

    echo "$count files fixed, $valid_count valid, $error_count errors.\n";
    print_r($fleet_errors);
}
