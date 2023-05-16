# Steps for adding the LOC-I annotations functionality to the NGAFID

## 1. Ensure you have followed all the instructions in README.md and your instance of the NGAFID is running properly.
We will just be adding a few additional tables here to collect more data.

## 2. Next, create the additional 4 tables required for annotations. These include:
```
event_annotations
loci_event_classes
user_group_names
user_groups
```
This can be done by simply running:

```
$ php db/add_annotation_tables.php
```

Of course, you will still want to make sure these tables are in the `ngafid` database.

## 3. Add the classes that users will annotate

This part is up to the data scientist. The current classes used by annotators include "False Positive", "False Positive (forward slip)" and "Stall-like Event".

```
mysql> INSERT INTO loci_event_classes (name, fleet_id) VALUES ('False Positive', 1);

mysql> SELECT * FROM loci_event_classes;
+----+----------------+----------+
| id | name           | fleet_id |
+----+----------------+----------+
|  1 | False Positive |        1 |
+----+----------------+----------+
1 row in set (0.00 sec)
```

## 4. (Optional) User grouping

It may be ideal for the data scientist to sort users into groups for measuring inter-rater reliability. To do so you may add named groups (see below).

### First, create the group

```
mysql> INSERT INTO user_group_names (name) VALUES ('RIT');

mysql> SELECT * FROM user_group_names;
+----+------+
| id | name |
+----+------+
|  1 | RIT  |
+----+------+
1 row in set (0.00 sec)
```

### Finally, associate users with the group

This will allow users of the same group to 'supervise' the annotation process.

```
INSERT INTO user_groups (user_id, group_id) VALUES ((SELECT id FROM user WHERE first_name = 'ritchie'), (SELECT id FROM user_group_names WHERE name = 'RIT'));
```
