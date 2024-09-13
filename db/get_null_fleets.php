<?php

$cwd[__FILE__] = __FILE__;
if (is_link($cwd[__FILE__])) $cwd[__FILE__] = readlink($cwd[__FILE__]);
$cwd[__FILE__] = dirname($cwd[__FILE__]);

require_once($cwd[__FILE__] . "/my_query.php");

$result = query_ngafid_db("SELECT id FROM fleet WHERE fleet_name is NULL");

$user_emails = [];

while (NULL != ($row = $result->fetch_assoc())) {
    $fleet_id = $row['id'];

    echo "$fleet_id\n";

    $access_result = query_ngafid_db("SELECT user_id, type FROM fleet_access WHERE fleet_id = $fleet_id");

    while (NULL != ($access_row = $access_result->fetch_assoc())) {
        $user_id = $access_row['user_id'];
        $user_type = $access_row['type'];

        $user_result = query_ngafid_db("SELECT email, first_name, last_name FROM user WHERE id = $user_id");
        $user_row = $user_result->fetch_assoc();

        $email = $user_row['email'];
        $first_name = $user_row['first_name'];
        $last_name = $user_row['last_name'];

        echo "\t$user_id - $user_type : $email $first_name $last_name\n";

        $user_emails[] = $email;
    }
    echo "\n";
}

foreach ($user_emails as $user_email) {
    print("; $user_email");
}

print("\n");
