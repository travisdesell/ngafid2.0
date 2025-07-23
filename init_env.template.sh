

    # ------------------------------------------------------------------------------ #
    # NGAFID - Environment Variables Template                                        #
    #                                                                                #
    #                                                                                #
    # Be sure to run  ` ~/ngafid2.0 $ source init_env.sh `  after making changes or  #
    # running from a new shell.                                                      #
    #                                                                                #
    # Alternatively, you can add the following line to your ~/.bashrc                # 
    # file to automatically source this file:                                        #
    #                                                                                #
    #   ` source ~/ngafid2.0/init_env.sh `                                           #
    #                                                                                #
    # Last Updated: 5/8/25                                                           #
    # ------------------------------------------------------------------------------ #
    # ✂ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ < ❗ Remove Here > ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ #
    #                                                                                #
    # This file is a template for setting up the environment variables for NGAFID.   #
    # Please make a copy called 'init_env.sh' and modify it according to your setup. #
    #                                                                                #
    # ------------------------------------------------------------------------------ #


#Absolute path
export NGAFID_REPO=<⚠ /Set/Absolute/Path/Here ⚠>  #<-- ❗ Set this to the absolute path of your ngafid repo. This is the directory where you cloned the ngafid repo.


#Data
export NGAFID_DATA_FOLDER=$NGAFID_REPO/data        #<-- ❗ Create a data folder in the NGAFID root (if it doesn't already exist).
export NGAFID_UPLOAD_DIR=$NGAFID_DATA_FOLDER/uploads
export NGAFID_ARCHIVE_DIR=$NGAFID_DATA_FOLDER/archive
export TERRAIN_DIRECTORY=$NGAFID_DATA_FOLDER/terrain/                       # If you don't have the data to add to the terrain directory, ask for it.
export AIRPORTS_FILE=$NGAFID_DATA_FOLDER/airports/airports_parsed.csv       # If you don't have the data for the airports directory, ask for it.
export RUNWAYS_FILE=$NGAFID_DATA_FOLDER/runways/runways_parsed.csv          # If you don't have the data for the runways directory, ask for it.


#Webserver
export NGAFID_PORT=8181     #<-- You can use whatever port you need or want to use
export MUSTACHE_TEMPLATE_DIR=$NGAFID_REPO/ngafid-static/templates/
export WEBSERVER_STATIC_FILES=ngafid-static/
export DISABLE_PERSISTENT_SESSIONS=false   #<-- To require users to log in again after a restart, set this to true.


#Kafka bootstrap port
export KAFKA_BOOTSTRAP=localhost:9092


#Emails
export NGAFID_EMAIL_INFO=$NGAFID_REPO/email_info.txt
export NGAFID_ADMIN_EMAILS="ritchie@rit.edu"
export NGAFID_EMAIL_ENABLED=false       #<-- If you don't want the webserver to send emails (exceptions, shutdowns, etc.), set this to false.


#Backups
export NGAFID_BACKUP_DIR=$NGAFID_REPO/backups
export NGAFID_BACKUP_TABLES="user fleet airframes airframe_types tails user_preferences user_preferences_metrics double_series_names stored_filters string_series_names data_type_names flight_tags sim_aircraft uploads"


#Output message
if (echo "$0" | grep -q "template"); then
    echo "⚠ This file is intended to be used as a template. Please make a copy called 'init_env.sh' and use that instead."
fi