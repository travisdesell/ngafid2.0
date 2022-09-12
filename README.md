# Steps for running the NGAFID2.0 website

## 1. Clone the repository

Using SSH (reccomended):
```
~/ $ git clone git@github.com:travisdesell/ngafid2.0
```
Using HTTPS:
```
~/ $ git clone https://github.com/travisdesell/ngafid2.0.git
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

To create the tables of the database, you'll need php and the php mysql library installed.
For ubuntu:
```
~/ $ sudo apt install php7.2 php7.2-mysql
```
If you use an older version of PHP it will probably work too.


Then, in the ngafid2.0 repo create the following two files:
```
~/ngafid2.0 $ touch db/db_info db/db_info.php
```

The contents of those files should be:

`db/db_info`
```
<database_name - probably ngafid or ngafid_test>
<database_host - probably localhost>
<database_user - probably ngafid_user>
<database_password - better not be 'password' on production>
```

`db/db_info.php`
```
<?php
$ngafid_db_user = '<database_user>';
$ngafid_db_name = '<database_name>';
$ngafid_db_host = '<probably localhost>';
$ngafid_db_password = '<your password>';
?>
```

Now that we've created these two files, we can create the tables of the database by running the
PHP script `db/create_tables.php`:
```
~/ngafid2.0 $ php db/create_tables.php
(lots of output here)
```

Now load the event definitions SQL to set up all the exceedence events in the database, filling in the appropriate things:

```
mysql -h <hostname> -u <database_user> --password=<your password> <database_name> < db/event_definitions_2020_05_15.sql
```


## 3. Configure the environment
We need to set up some environmental variables.

Create `init_env.sh`, and add the following:
```bash
export NGAFID_REPO=<absolute path to ngafid2.0 repo>
export NGAFID_DATA_FOLDER=<create a ngafid data folder and put the absolute path here>
export NGAFID_PORT=8181 # You can use whatever port you need or want to use
export NGAFID_UPLOAD_DIR=$NGAFID_DATA_FOLDER/uploads
export NGAFID_ARCHIVE_DIR=$NGAFID_DATA_FOLDER/archive
export NGAFID_DB_INFO=$NGAFID_REPO/db/db_info.php
# If you don't have the data to add to the terrain directory, ask for it.
export TERRAIN_DIRECTORY=$NGAFID_DATA_FOLDER/terrain/
# If you don't have the data for the airports directory, ask for it.
export AIRPORTS_FILE=$NGAFID_DATA_FOLDER/airports/airports_parsed.csv
# If you don't have the data for the runways directory, ask for it.
export RUNWAYS_FILE=$NGAFID_DATA_FOLDER/runways/runways_parsed.csv
export MUSTACHE_TEMPLATE_DIR=$NGAFID_REPO/src/main/resources/public/templates/
export SPARK_STATIC_FILES=$NGAFID_REPO/src/main/resources/public/
export NGAFID_EMAIL_INFO=$NGAFID_REPO/email_info.txt
export NGAFID_ADMIN_EMAILS="ritchie@rit.edu"
# Set me to true if you dont want backups being made everytime you fire off the NGAFID
# If you do set this to true the following 3 parameters do not need to be set
export NGAFID_BACKUP_DIR=<path to where backups should be stored>
export NGAFID_BACKUP_TABLES="user fleet airframes airframe_types tails user_preferences user_preferences_metrics double_series_names stored_filters string_series_names data_type_names flight_tags sim_aircraft uploads"
# This is the number of days to wait to perform another backup
```

and run
```
~/ngafid2.0 $ source init_env.sh
```
every time you want to run the website from a new shell.

If you want these variables to be initialized automatically when you launch a new shell,
add the following line to your `~/.bashrc` file:
```bash
source ~/ngafid2.0/init_env.sh
```

## 4. Running the webserver
For java 14:
http://download.opensuse.org/repositories/Java:/Factory/openSUSE_Factory/x86_64/

First we need maven to fetch all of the java dependencies:
```
~/ngafid2.0 $ mvn install
```

Next we need to initialize node. You'll need npm installed for this. For ubuntu:
```
~/ $ sudo apt install npm
```

Then run:
```
~/ngafid2.0 $ npm install
```

This will download the javascript dependencies. 

Then, in order to compile the javascript
and automatically recompile whenever you change one of the files:
```
~/ngafid2.0 $ npm run watch
```

You should then be able to compile and run the webserver by running `run_webserver.sh`
```
~/ngafid2.0 $ sh run_webserver.sh
```

Importing flights and calculating exceedences can be done by running the `run_process_upload.sh` file.


## (Optional) using the backup daemon - works on Linux systems only.
As demonstrated in `init_env.sh`, the NGAFID can be backed up using a configurable set of parameters (i.e. what tables to backup, etc).
First, change the first line of the file in `db/backup_database.sh` so that the argument after `source` is the path to the aformentioned `init_env.sh` file.

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
