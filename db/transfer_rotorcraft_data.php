<?php

//NOTE: need to download phpseclib to use SFTP from here:
// http://phpseclib.sourceforge.net
//
// Can install with PEAR:
//
// sudo pear channel-discover phpseclib.sourceforge.net
// sudo pear remote-list -c phpseclib
// sudo pear install phpseclib/Net_SSH2
// sudo pear install phpseclib/Net_SFTP

set_include_path(get_include_path() . ";" . 'phpseclib');

include('Net/SFTP.php');


/*
 * We can run this periodically to transfer rotorcraft data over into RAIS
 */

$cwd[__FILE__] = __FILE__;
if (is_link($cwd[__FILE__])) $cwd[__FILE__] = readlink($cwd[__FILE__]);
$cwd[__FILE__] = dirname($cwd[__FILE__]);

require_once($cwd[__FILE__] . "/my_query.php");

$raise_password = getenv("RAISE_PASSWORD");

//$sftp = new Net_SFTP('sftp.asias.info');
//if (!$sftp->login('service_ngafid_dto_upload@ngafid.org', $raise_password)) {

$sftp = new Net_SFTP('sftp.rotorcraft.asias.info');
if (!$sftp->login('service_ngafid_dto_upload@ngafid.org', $raise_password)) {
    echo "Login failed!\n";
    exit(1);
}
echo "Login succeded!\n";

$result = query_ngafid_db("SELECT fleet_id, system_id, tail, confirmed FROM tails");
$source_tail_filename = "./system_ids_to_tails__" . $date = date('Y-m-d') . ".csv";
$target_tail_filename = "system_ids_to_tails__" . $date = date('Y-m-d') . ".csv";

echo "system ids to tails source (local) filename is   '$source_tail_filename'\n";
echo "system ids to tails target (on sftp) filename is '$target_tail_filename'\n";

$file_contents = "#fleet_id, system_id, tail number, confirmed\n";
while (NULL != ($row = $result->fetch_assoc())) {
    $fleet_id = $row['fleet_id'];
    $system_id = $row['system_id'];
    $tail = $row['tail'];
    $confirmed = $row['confirmed'];

    //echo "$fleet_id, $system_id, $tail, $confirmed\n";
    $file_contents .= "$fleet_id, $system_id, $tail, $confirmed\n";
}

$bytes_written = file_put_contents($source_tail_filename, $file_contents);
echo "wrote $bytes_written bytes for the system ids to tail file\n";

//transfer the system_ids_to_tails filename over first
$res = $sftp->put($target_tail_filename, $source_tail_filename, NET_SFTP_LOCAL_FILE | NET_SFTP_RESUME);
echo "result from sftp->put was '$res'\n";



$result = query_ngafid_db("SELECT DISTINCT(upload_id) FROM flights WHERE airframe_type_id = (SELECT id FROM airframe_types WHERE name = 'Rotorcraft')");

$count = 0;

while (NULL != ($row = $result->fetch_assoc())) {
    $upload_id = $row['upload_id'];
    echo "upload id: $upload_id\n";

    $upload_result = query_ngafid_db("SELECT uploader_id, fleet_id, filename, sent_to_raise FROM uploads WHERE id = $upload_id");
    while (NULL != ($upload_row = $upload_result->fetch_assoc())) {
        $uploader_id = $upload_row['uploader_id'];
        $fleet_id = $upload_row['fleet_id'];
        $filename = $upload_row['filename'];
        $sent_to_raise = $upload_row['sent_to_raise'];

        $archive_base = getenv("NGAFID_ARCHIVE_DIR");

        echo "\tupload_id: $upload_id, fleet_id: $fleet_id, uploader_id: $uploader_id, filename: '$filename'\n";

        $source_file = $archive_base . "/" . $fleet_id . "/" . $uploader_id . "/" . $upload_id . "__" . $filename;
        $target_file = $fleet_id . "__" . $upload_id . "__" . $filename;
        echo "\tsource file: '$source_file'\n";
        echo "\ttarget file: '$target_file'\n";
        echo "\tsent to raise? $sent_to_raise\n";

        $res = $sftp->put($target_file, $source_file, NET_SFTP_LOCAL_FILE | NET_SFTP_RESUME);
        echo "result from sftp->put was '$res'\n";

        query_ngafid_db("UPDATE uploads SET sent_to_raise = 1, contains_rotorcraft = 1 WHERE id = $upload_id");

        $count += 1;
    }
    echo "\n";

}

