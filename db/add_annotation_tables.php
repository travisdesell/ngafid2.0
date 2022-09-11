<?php

$cwd[__FILE__] = __FILE__;
if (is_link($cwd[__FILE__])) $cwd[__FILE__] = readlink($cwd[__FILE__]);
$cwd[__FILE__] = dirname($cwd[__FILE__]);

require_once($cwd[__FILE__] . "/my_query.php");

$query =
"CREATE TABLE loci_event_classes
(
    id    INT AUTO_INCREMENT
        PRIMARY KEY,
    name VARCHAR(2048) NOT NULL,
    fleet_id INT           NULL,
    CONSTRAINT loci_event_classes_fleet_id_fk
    FOREIGN KEY (fleet_id) REFERENCES fleet (id)
);";

query_ngafid_db($query);

$query = 
"CREATE TABLE event_annotations
(
    fleet_id  INT           NOT NULL,
    user_id   INT           NOT NULL,
    event_id  INT           NOT NULL,
    class_id  INT           NOT NULL,
    timestamp TIMESTAMP     NULL,
    notes     VARCHAR(4096) NULL,
    PRIMARY KEY (fleet_id, user_id, event_id),
    CONSTRAINT event_annotations_events_id_fk
        FOREIGN KEY (event_id) REFERENCES events (id),
    CONSTRAINT event_annotations_fleet_id_fk
        FOREIGN KEY (fleet_id) REFERENCES fleet (id),
    CONSTRAINT event_annotations_loci_event_classes_id_fk
        FOREIGN KEY (class_id) REFERENCES loci_event_classes (id),
    CONSTRAINT event_annotations_user_id_fk
        FOREIGN KEY (user_id) REFERENCES user (id)
);";

query_ngafid_db($query);

$query =
"CREATE TABLE user_group_names
(
    id   INT AUTO_INCREMENT
        PRIMARY KEY,
    name VARCHAR(2048) NULL
);";

query_ngafid_db($query);

$query = 
"CREATE TABLE user_groups
(
    user_id  INT NOT NULL,
    group_id INT NOT NULL,
    CONSTRAINT user_groups_user_group_names_null_fk
        FOREIGN KEY (group_id) REFERENCES user_group_names (id),
    CONSTRAINT user_groups_user_null_fk
        FOREIGN KEY (user_id) REFERENCES user (id)
);";

query_ngafid_db($query);

?>
