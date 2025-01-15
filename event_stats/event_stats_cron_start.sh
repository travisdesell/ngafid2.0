# - Enables a cron job to for the Event Statistics Fetch Java program
# - Cron job executes every day at midnight (00:00)
# - ⚠ Make sure that this file is run from the event_stats directory, otherwise the source command will not work


#Source the configuration script 
. ./event_stats_variables_config.sh


#Enable the cron job
echo "\n--------------------\nCron Service Started\n--------------------\n" >> $CRON_PATH/EventStatisticsFetch.log
                         
                         
echo "Cron Path: $CRON_PATH"
echo "Enabling the cron job for the Event Statistics Fetch Java program... (Using Java Home: $JAVA_HOME)"
echo "$CRON_CMD" | crontab -


#Check if the cron job started
if crontab -l | grep -q "org.ngafid.events.EventStatisticsFetch"; then
    echo "Cron job started successfully!"
else
    echo "Cron job failed to start!"
fi