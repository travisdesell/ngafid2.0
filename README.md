# Steps for running the NGAFID2.0 website

## 1. Clone the repository
```
~/ $ git clone git@github.com:travisdesell/ngafid2.0
```

## 2. Set up the database
Install mysql on your system. For ubuntu:
```
~/ $ sudo apt install mysql-server
```
### NOTE: On some Linux disributions, such as RHEL, SUSE and Arch, the mysql package is provided by mariadb.
In this case you will need to run (on openSUSE):
```
~/ $ sudo zypper in mariadb
```
or, for arch:
```
~/ $ sudo pacman -S mariadb
```
and Fedora, RedHat/CentOS:
```
~/ $ sudo dnf install mariadb mariadb-server 
```
You will also need to run
```
~/ $ sudo mysql_install_db --user=mysql --ldata=/var/lib/mysql
```
and
```
~/ $ sudo systemctl enable --now mariadb
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
mysql -h <hostname> -u <database_user> --password=<your password> <database_name> < db/event_definitions_2019_09_25.sql
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

This will download the javascript dependencies. Then, in order to compile the javascript
and automatically recompile whenever you change one of the files:
```
~/ngafid2.0 $ npm run watch
```

You should then be able to compile and run the webserver by running `run_webserver.sh`
```
~/ngafid2.0 $ sh run_webserver.sh
```

Importing flights and calculating exceedences can be done by running the `run_process_flights.sh`
and `run_exceedences.sh` files.

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



#not used anymore
information on using PM2 to start/restart node servers:

https://www.digitalocean.com/community/tutorials/how-to-set-up-a-node-js-application-for-production-on-centos-7

information on setting up apache to use PM2:

https://vedmant.com/setup-node-js-production-application-apache-multiple-virtual-host-server/

if error "service unavailabile":
http://sysadminsjourney.com/content/2010/02/01/apache-modproxy-error-13permission-denied-error-rhel/

to fix:

sudo /usr/sbin/setsebool -P httpd_can_network_connect 1
