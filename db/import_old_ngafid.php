<?php

$cwd[__FILE__] = __FILE__;
if (is_link($cwd[__FILE__])) $cwd[__FILE__] = readlink($cwd[__FILE__]);
$cwd[__FILE__] = dirname($cwd[__FILE__]);

require_once($cwd[__FILE__] . "/my_query.php");


$fleet_id = $argv[1];
$uploader_id = $argv[2];
$import_files = array_slice($argv, 3);

//print_r($import_files);

$archive_dir = getenv("NGAFID_ARCHIVE_DIR");

$chunkSize = 2097152;

foreach ($import_files as $filepath) {
    echo $filepath . "\n";
    echo "\tpos: " . strrpos($filepath, '/') . "\n";
    $filename = substr($filepath, strrpos($filepath, '/') + 1);
    echo "\t" . $filename . "\n";
    $size_bytes = filesize($filepath);
    $identifier = $size_bytes . "-" . preg_replace('/[^0-9a-zA-Z_-]/', '', $filename);
    echo "\t" .$identifier . "\n";
    $number_chunks = ceil(floatval($size_bytes) / floatval($chunkSize));
    $uploaded_chunks = $number_chunks;
    $chunk_status = '';
    for ($i = 0; $i < $number_chunks; $i++) $chunk_status .= '1';
    echo "\t" . $number_chunks . "\n";
    echo "\t" . $uploaded_chunks . "\n";
    echo "\t" . $chunk_status. "\n";

    $md5_hash = md5_file($filepath);
    echo "\t" . $md5_hash . "\n";
    echo "\t" . $size_bytes . "\n";

    $bytes_uploaded = $size_bytes;
    echo "\t" . $bytes_uploaded . "\n";
    $status = "UPLOADED";

    $start_time = gmdate('Y-m-d h:i:s', time());
    $end_time = gmdate('Y-m-d h:i:s', time());

    echo "\t" . $start_time . "\n";
    echo "\t" . $end_time . "\n";

    $query = "INSERT INTO uploads SET fleet_id = $fleet_id, uploader_id = $uploader_id, filename = '$filename', identifier = '$identifier', number_chunks = $number_chunks, uploaded_chunks = $uploaded_chunks, chunk_status = '$chunk_status', md5_hash = '$md5_hash', size_bytes = $size_bytes, bytes_uploaded = $bytes_uploaded, tiling_progress = 0, status = '$status', start_time = '$start_time', end_time = '$end_time'";
    echo $query . "\n";
    query_ngafid_db($query);
    $insert_id = $ngafid_db->insert_id;

    echo "INSERT ID: $insert_id\n";

    if ($insert_id == 0) {
        $query = "SELECT id FROM uploads WHERE fleet_id = $fleet_id AND size_bytes = $size_bytes AND md5_hash = '$md5_hash'";
        $result = query_ngafid_db($query);
        $row = $result->fetch_assoc();
        $insert_id = $row['id'];
        echo "EXISTING INSERT ID: $insert_id\n";
    }

    $outpath = $archive_dir . "/" . $fleet_id . "/" . $uploader_id . "/" . $insert_id . "__" . $filename;

    echo "\tTARGET: $outpath\n";

    if (!file_exists($outpath)) {
        echo "\tFILE DOES NOT EXIST, COPYING!\n";
        copy($filepath, $outpath);
    } else if ($size_bytes != filesize($outpath)) {
        echo "\tFILE NOT FULLY COPIED!\n";
        copy($filepath, $outpath);
    } else {
        echo "\tFILE EXISTS, NOT COPYING!\n";
    }

    echo "\n\n";
}
