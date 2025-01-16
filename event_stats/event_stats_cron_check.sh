#!/bin/bash

# - Call with 'bash event_stats/event_stats_cron_check.sh'
# - Just checks whether or not the cron job from event_stats_cron_start.sh is running


#Source the configuration script 
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
. "$SCRIPT_DIR/event_stats_variables_config.sh"


if crontab -l | grep -q "org.ngafid.events.EventStatisticsFetch"; then
    echo "Cron job IS currently running."
else
    echo "Cron job IS NOT currently running."
fi