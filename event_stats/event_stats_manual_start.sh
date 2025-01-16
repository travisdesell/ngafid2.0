#!/bin/bash

# - Call with 'bash event_stats/event_stats_manual_start.sh'
# - Manually triggers the event stats call


#Source the configuration script 
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
. "$SCRIPT_DIR/event_stats_variables_config.sh"


#Enable the cron job
printf "\n--------------------\nManual Fetch Started\n--------------------\n\n" >> $SCRIPT_DIR/EventStatisticsFetch.log


#Manually trigger the event stats call
MANUAL_CMD="cd $SCRIPT_DIR && cd ../ && . ./init_env.sh && $JAVA_HOME/bin/java -cp ./target/ngafid-1.0-SNAPSHOT.jar org.ngafid.events.EventStatisticsFetch"
echo "Manually starting the Event Statistics Fetch Java program... (Using Java Home: $JAVA_HOME)"

eval "$MANUAL_CMD"