# - Just checks whether or not the cron job from event_stats_cron_start.sh is running
# - ⚠ Make sure that this file is run from the event_stats directory, otherwise the source command will not work


#Source the configuration script
. ./event_stats_variables_config.sh


if crontab -l | grep -q "$CRON_CMD"; then
    echo "Cron job IS currently running."
else
    echo "Cron job IS NOT currently running."
fi