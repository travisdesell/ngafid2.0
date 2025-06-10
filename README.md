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
sh setup_dat_importing.sh
```

## 2. Set up the database

Install mysql on your system. For ubuntu:

```
~/ $ sudo apt install mysql-server
```

### NOTE: On most Linux distributions, the MySQL package is provided by MariaDB. MariaDB is essentially the open-source version of MySQL and fully compatible with the MySQL syntax, however make sure you are using the latest version or you may run into problems.

Most distributions (with the exception of Arch and a few others) will alias MySQL to MariaDB
i.e.

```
~/ $ sudo zypper in mysql mysql-server
```

or, for Arch:

```
~/ $ sudo pacman -S mariadb
```

You will also need to run

```
~/ $ sudo mysql_install_db --user=mysql --basedir=/usr --datadir=/var/lib/mysql
```

before starting the systemd service (see below)

```
~/ $ sudo systemctl enable --now mariadb
```

or

```
~/ $ sudo systemctl enable --now mysql
```

**the systemd service name may vary depending on your distro.

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

We need to store these credentials in a file called `ngafid-db/src/liquibase.properties`, along with some other
information:

```
changeLogFile=changelog-root.xml
outputChangeLogFile=changelog.mysql.sql
url=jdbc:mysql://localhost/ngafid
username=ngafid_user
password=password
```

You should not modify the first two lines, but lines 3-5 may need to be tweaked depending on your database setup. In
particular, if you are using a database other than mysql you will have to tweak the URL.

Once you have done this, you can create the database tables by running the following:

```
~/ngafid2.0 $ run/liquibase/update
```

## 3. Configure the environment

We need to set up some environmental variables that point to some important data / directories.

Create a **copy** of the environment variables template file, `init_env.template.sh`, and call it `init_env.sh`. Ensure that you properly configure `init_env.sh`. You **must** address all lines marked with an exclamation point (❗).

Run the command below after making changes or running from a new shell:

```
~/ngafid2.0 $ source init_env.sh
```
If you want these variables to be initialized automatically when you launch a new shell,
add the following line to your `~/.bashrc` file:

```bash
source ~/ngafid2.0/init_env.sh
```

## 4. Download Data Required for Flight Processing

You need to download the data from the following link, and place the files in the following folder
structure: [NGAFID Setup Data](https://drive.google.com/drive/folders/1cPMWpXCQb-I1lraFDY5Snn14UvMqu2SH?usp=drive_link).
Request permissions if you do not have them (if you use an RIT google account you should have access by default).

```
$NGAFID_DATA_FOLDER
├── airports
│   └── airports_parsed.csv
├── archive
├── runways
│   └── runways_parsed.csv
├── terrain        # You will have to decompress the terrain data.
│   ├── H11
│   ├── H12
│   ├── H13
...
```

## 5. Running the webserver

### Initialize Dependencies

You need Maven installed to run the following script which will build the java modules:
From the project root (`~/ngafid2.0`), run:
```
run/build 
```

Next we need to initialize node. You'll need npm installed for this, then inside the `ngafid-frontend` directory run:
From ~/ngafid2.0/ngafid-frontend, run:
```
npm install
```

This will download the javascript dependencies.

Then, in order to compile the javascript and automatically recompile whenever you change one of the files:
From ~/ngafid2.0/ngafid-frontend, run:
```
npm run watch
```

 ### Launch Kafka
Next, to launch the web server we must first initialize and start Kafka. Follow the instructions on the [Kafka quickstart](https://kafka.apache.org/quickstart), using `ngafid2.0/resource/reconfig-server.properties`.
On macOS, you can install Kafka with:
```
 brew install kafka
```


These are the steps to setup kafka from the Kafka quickstart (above).
Before starting the Kafka server, you must format the storage directory using a cluster ID. This step is required only once per cluster.

1. Generate random claster-id:
From the project root (`~/ngafid2.0`), run:
```
kafka-storage random-uuid
```

2. Run the following command to format Kafka’s log directory and generate the required meta.properties file:
From the project root (`~/ngafid2.0`), run:
```
kafka-storage format -t <your-cluster-id> -c resources/reconfig-server.properties
```


3. Add this line to the resources/reconfig-server.properties – required for Kafka in KRaft mode (i.e., no Zookeeper).
It tells Kafka how to form the internal metadata quorum used for coordination.
The 1@localhost:9093 part maps the node.id to the address where the controller listens
```
controller.quorum.voters=1@localhost:9093
```

4. Now, launch Kafka:
From the project root (`~/ngafid2.0`), run:
```
kafka-server-start resources/reconfig-server.properties
```

5. Run the following script to create the appropriate kafka topics:
From the project root (`~/ngafid2.0`), run:
```
run/kafka/create_topics
```

### Launch webserver

You should then be able to compile and run the webserver. 
From the project root (`~/ngafid2.0`), run:
```
run/webserver
```

## 6. Data Processing

The data processing pipeline consists of two Kafka consumers and one database observer -- one processes archives
uploaded to the website, and the other two handle event processing. They should all run persistently in separate
terminals:

The upload consumer simply processes uploaded files from the `upload` topic:

```shell
~/ngafid2.0 $ run/kafka/upload_consumer
```

The event consumer and event observer work in concert: the event observer looks for uncomputed events in fully imported
flights and places them into the `event` topic. Then, the event consumer computes those events.

```shell
~/ngafid2.0 $ run/kafka/event_consumer
```

```shell
~/ngafid2.0 $ run/kafka/event_observer
```

## 7. Workflow

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

## 8. Event Statistics

Event statistics are to be computed and cached occasionally. If you import data and want to see it reflected on the
website, you must update these cached tables:

```
$ run/liquibase/daily-materialized-views
$ run/liquibase/hourly-materialized-views
```

You can set up a timer with `cron` or `systemd` to automatically do this on a schedule. Website features that work on
event statistics, frequency, severity, etc. will need to have this data updated to be 100% accurate.

## (Optional) using the backup daemon - works on Linux systems only.

As demonstrated in `init_env.sh`, the NGAFID can be backed up using a configurable set of parameters (i.e. what tables
to backup, etc).
First, change the first line of the file in `db/backup_database.sh` so that the argument after `source` is the path to
the aformentioned `init_env.sh` file.

Then, you will need to copy the systemd files to your systemd directory and reload the os daemons.

```
~/ngafid2.0 # cp services/backup/ngafid-backup* /usr/lib/systemd/system
~/ngafid2.0 # systemctl daemon-reload
```

To enable the backup service:

```
# systemctl enable ngafid-backup.service ngafid-backup.timer
```

If you desire to change the backup interval (default is weekly), you can override the `OnCalendar=` parameter with:

```
# systemctl edit ngafid-backup.service
# systemctl daemon-reload
```

You may also want to check that everything has been loaded successfully.

```
# systemctl status *timer
```

To run the backup at any given time, you can now simply invoke:

```
# systemctl start ngafid-backup
```

# ngafid2.0

airport database:
http://osav-usdot.opendata.arcgis.com/datasets/a36a509eab4e43b4864fb594a35b90d6_0?filterByExtent=false&geometry=-97.201%2C47.944%2C-97.147%2C47.952

runway database:
http://osav-usdot.opendata.arcgis.com/datasets/d1b43f8a1d474b8c9c24cad4b942b74a_0?uiTab=table&geometry=-97.2%2C47.944%2C-97.146%2C47.953&filterByExtent=false

required for jQuery query-builder:

https://github.com/mistic100/jQuery.extendext.git
https://github.com/olado/doT.git

setting up javascript with react/webpack/babel:
https://www.valentinog.com/blog/react-webpack-babel/

For mySQL on SUSE
https://software.opensuse.org/package/mysql-community-server

For Arch:
https://aur.archlinux.org/packages/mysql57/

#not used anymore
information on using PM2 to start/restart node servers:

https://www.digitalocean.com/community/tutorials/how-to-set-up-a-node-js-application-for-production-on-centos-7

information on setting up apache to use PM2:

https://vedmant.com/setup-node-js-production-application-apache-multiple-virtual-host-server/

if error "service unavailabile":
http://sysadminsjourney.com/content/2010/02/01/apache-modproxy-error-13permission-denied-error-rhel/

to fix:

sudo /usr/sbin/setsebool -P httpd_can_network_connect 1

## Chart Processing Service

[Chart Processing Service Documentation](ngafid-chart-processor/README.md)
