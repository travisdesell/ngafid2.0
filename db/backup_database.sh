#!/bin/bash
# A simple sh script to backup the NGAFID
# Author: Aidan LaBella

source <path_to_ngafid2.0_repo>/init_env.sh

declare DB_PASSWORD
declare DB_USERNAME
declare DB_NAME
declare DB_HOST

cat "${NGAFID_REPO}/db/db_info" | (while read line
do
    if [[ $line == *"password"* ]]; then
        IFS='> ' read -r -a array <<< "$line"
        DB_PASSWORD="${array[2]}"
    fi
    if [[ $line == *"user"* ]]; then
        IFS='> ' read -r -a array <<< "$line"
        DB_USERNAME="${array[2]}"
    fi
    if [[ $line == *"name"* ]]; then
        IFS='> ' read -r -a array <<< "$line"
        DB_NAME="${array[2]}"
    fi
    if [[ $line == *"host"* ]]; then
        IFS='> ' read -r -a array <<< "$line"
        DB_HOST="${array[2]}"
    fi
done

/usr/bin/mysqldump -u$DB_USERNAME -p$DB_PASSWORD -h $DB_HOST $DB_NAME $NGAFID_BACKUP_TABLES > ${NGAFID_BACKUP_DIRECORY}/backup_$(date "+%F-%T").sql

)
