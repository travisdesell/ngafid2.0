# - Stops the event_stats_cron_start.sh cron job (i.e. Removes the cron job from the crontab)
# - Displays a message to the console indicating that the cron job has been stopped
# - Checks whether or not the cron job was actually running first
# - ⚠ Make sure that this file is run from the event_stats directory, otherwise the source command will not work


#Source the configuration script
. ./event_stats_variables_config.sh


#Stop the cron job
echo "Stopping the cron job for the Event Statistics Fetch Java program..."
if crontab -l | grep -q "$CRON_CMD"; then
    crontab -l | grep -v "$CRON_CMD" | crontab -
    echo "Cron job stopped successfully."
else
    echo "Cron job was not running."
fi
