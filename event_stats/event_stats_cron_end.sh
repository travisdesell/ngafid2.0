#!/bin/bash

# - Call with 'bash event_stats/event_stats_cron_end.sh'
# - Stops the event_stats_cron_start.sh cron job (i.e. Removes the cron job from the crontab)
# - Displays a message to the console indicating that the cron job has been stopped
# - Checks whether or not the cron job was actually running first


#Source the configuration script 
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
. "$SCRIPT_DIR/event_stats_variables_config.sh"


#Stop the cron job
echo "Stopping the cron job for the Event Statistics Fetch Java program..."
if crontab -l | grep -q "org.ngafid.events.EventStatisticsFetch"; then
    crontab -l | grep -v "org.ngafid.events.EventStatisticsFetch" | crontab -
    echo "Cron job stopped successfully."
else
    echo "Cron job was not running."
fi
