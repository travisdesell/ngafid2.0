export NGAFID_REPO=/ngafid/ngafid2.0
export NGAFID_DATA_FOLDER=/ngafid/ngafid_data
export NGAFID_PORT=8443 # You can use whatever port you need or want to use
export HTTPS_PASSKEY="k31U8rpOU65t@BQVsxK51?%6"
export HTTPS_CERT_PATH=/ngafid/ssl/ngafid_keystore.jks
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
export NGAFID_EMAIL_INFO=$NGAFID_REPO/email_info
export NGAFID_ADMIN_EMAILS="ritchie@rit.edu"
export NGAFID_BACKUP_DIR=/data/d2s2_lab/ngafid_backups/system
export NGAFID_BACKUP_TABLES="user fleet airframes airframe_types tails user_preferences user_preferences_metrics double_series_names stored_filters event_annotations string_series_names data_type_names flight_tags sim_aircraft uploads"
