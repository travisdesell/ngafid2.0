# About this Repository

The NGAFID2.0 is an open source flight data management tool. The project is broken up into several modules:

- `ngafid-chart-processor`: Python service which downloads and transforms charts from the FAA for use in maps on the
  website.
- `ngafid-core`: Core Java NGAFID code shared among other modules, mostly contains object relational mapping and
  business logic.
- `ngafid-data-processor`: Flight data processing code.
- `ngafid-db`: Database schema, written in liquibase formatted SQL.
- `ngafid-frontend`: React frontend.
- `ngafid-static`: Directory from which static files are served.
- `ngafid-www`: Java backend for the web application.

# Steps for running the NGAFID2.0 website

## 0. Requirements

You will need the following software packages:

1. Mysql
2. Maven
3. Java >= 21
4. Nodejs
5. Kafka

## 1. Clone the repository

```
~/ $ git clone git@github.com:travisdesell/ngafid2.0
```

Afterward, we need to install a JAR file dependency to where Maven fetches your dependencies from.
Running Maven will not be possible without running this script.

```
run/setup_dat_importing
```

## 2. Set up the database

Install MySQL (instructions are system dependent).

Next we'll create the database in mysql:

```
~/ $ sudo mysql
...
Type 'help;' or '\h' for help. Type '\c' to clear the current input statement.

mysql> CREATE DATABASE ngafid;
Query OK, 1 row affected (0.00 sec)
mysql> CREATE USER 'ngafid_user'@'localhost' IDENTIFIED BY 'password';
Query OK, 1 row affected (0.00 sec)
mysql> GRANT ALL PRIVILEGES ON ngafid.* TO 'ngafid_user'@'localhost';
Query OK, 1 row affected (0.00 sec)
mysql> FLUSH PRIVILEGES;
Query OK, 1 row affected (0.00 sec)
mysql> exit
Bye
```

We need to store these credentials in a file called `src/main/resources/liquibase.properties`, along with some other
information:

```
changeLogFile=changelog-root.xml
outputChangeLogFile=changelog.mysql.sql
url=jdbc:mysql://localhost/ngafid
username=ngafid_user
password=password
```

Make a second file `src/main/resources/liquibase.docker.properties` that mirrors this, but change the url as shwon:

```
changeLogFile=changelog-root.xml
outputChangeLogFile=changelog.mysql.sql
url=jdbc:mysql://host.docker.internal/ngafid
username=ngafid_user
password=password
```

You should not modify the first two lines, but lines 3-5 may need to be tweaked depending on your database setup. In
particular, if you are using a database other than mysql you will have to tweak the URL.

Once you have done this, you can create the database tables by running the following:

```
~/ngafid2.0 $ run/liquibase/update
```

This command can also be used to apply changes to the database schema when the changelogs have been updated.

## 3. Download Data Required for Flight Processing

You need to download the data from the following link, and place the files in the following folder
structure: [NGAFID Setup Data](https://drive.google.com/drive/folders/1cPMWpXCQb-I1lraFDY5Snn14UvMqu2SH?usp=drive_link).
Request permissions if you do not have them (if you use an RIT google account you should have access by default).

Make the data folder wherever you like -- you may consider using secondary storage disk for this. The terrain data is
quite large -- you likely want it wherever your data folder is (though you can configure things however you like below).

```
$NGAFID_DATA_FOLDER
├── terrain        # You will have to decompress the terrain data.
│   ├── H11
│   ├── H12
│   ├── H13
...
```

## 4. Configure the environment

We need to set up two configuration files -- one for docker, the other for locally running services.

Copy `template.env` to `.env.host` and modify it to fit your setup, then modify your shell profile to source, e.g.

```shell
source ~/ngafid2.0/.env.host
```

Similarly, copy `template.docker.env` to `.env` and modify it to fit your setup. The `docker-compose.yml` file
is configured to automatically load all environmental variables from this file.

Note that the variables specified in the `.env` file are read by docker compose and available in the docker-compose file
for substitution. Within Dockerfiles, however, variables will have to be manually passed as arguments.

We also pass `.env` as an `env_file` in `docker-compose.yml`, which provides the variables for use within the container.
The `.env` file format used by docker doesn't support any scripting (e.g. source) so rather than splitting the compose
env and container env into two separate files, a single common file is used.

## 5. Build Node Modules

Initialize node. You'll need npm installed for this, then inside the `ngafid-frontend` directory run:

```
~/ngafid2.0/ngafid-frontend $ npm install
```

This will download the javascript dependencies. Then, in order to compile the javascript and automatically recompile
whenever you change one of the files:

```
~/ngafid2.0/ngafid-frontend $ npm run watch
```

## 6. Run Kafka

Kafka is used by the ngafid for simple message passing between processes.
Follow the instructions on the [Kafka quickstart](https://kafka.apache.org/quickstart) to configure kafkas storage,
using `ngafid2.0/resource/reconfig-server.properties`. Now, launch Kafka:

```
# Launch kafka kraft 
~/ngafid2.0 $ kafka-server-start resources/reconfig-server.properties
```

Next, run the following script to create the appropriate kafka topics:

```
~/ngafid2.0 $ run/kafka/create_topics
```

## 7. Services

The NGAFID is essentially composed of several services:

The upload processing service:

```shell
~/ngafid2.0 $ run/kafka/upload_consumer
```

The event processing service:

```shell
~/ngafid2.0 $ run/kafka/event_consumer
```

The event observer service -- used to queue up work for the event consumer when missing events are detected:

```shell
~/ngafid2.0 $ run/kafka/event_observer
```

The webserver:

```shell
~/ngafid2.0 $ run/webserver
```

The email consumer:

```shell
~/ngafid2.0 $ run/kafka/email_consumer
```

The airsync importer (you shouldn't run this locally unless you are working directly on it):

```shell
~/ngafid2.0 $ run/airsync_daemon
```

## 8. Launching with Docker

To build and run all requires services simultaneously, we can use docker. We build the java packages and then inject
them into docker containers:

```shell
~/ngafid2.0 $ run/build
~/ngafid2.0 $ run/package
```

Then, build the docker images. Note, that we define a `base` image from which other service-specific images are
dependent on. The docker build system does not handle dependencies like this properly, so in order to prevent issues you
must run the following commands in-order:

```shell
~/ngafid2.0 $ docker compose build base # create base image
~/ngafid2.0 $ docker compose build
```

## 9. Workflow

Note that these things should work regardless of whether you launching services directly or with docker so long as your
configuration is correct.

If you modify the upload processing code in some way and want to re-add an upload to the processing queue, you may use
the `UploadHelper` utility to add individual uploads, or all uploads from a fleet to the queue:

```
~/ngafid2.0 $ run/kafka/upload_helper --help
```

Similarly, if you modify a custom-event computation you can use the `EventHelper` to remove events from the database.
The event observer will pick up on this and enqueue them for re-computation. You may also delete events and opt for them
not to be recomputed.

```
~/ngafid2.0 $ run/kafka/event_helper --help
```

## 10. Event Statistics

Event statistics are to be computed and cached occasionally. If you import data and want to see it reflected on the
website, you must update these cached tables:

```
$ run/liquibase/daily-materialized-views
$ run/liquibase/hourly-materialized-views
```

You can set up a timer with `cron` or `systemd` to automatically do this on a schedule. Website features that work on
event statistics, frequency, severity, etc. will need to have this data updated to be 100% accurate.

## 11. Chart Processing Service

[Chart Processing Service Documentation](ngafid-chart-processor/README.md)
