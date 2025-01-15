CRON_TIME="* * * * *"    #Every minute (For testing)        [EX]
#CRON_TIME="0 0 * * *"    #Every day at midnight
CRON_PATH="./$( cd ./ "$( dirname "$0" )" >/dev/null 2>&1 && pwd )"
CRON_CMD="$CRON_TIME cd $SCRIPT_DIR && cd ../ && . ./init_env.sh && printf '\n\n(Cron Execution Beginning!)\n\n' >> $SCRIPT_DIR/EventStatisticsFetch.log && $JAVA_HOME/bin/java -cp target/ngafid-1.0-SNAPSHOT.jar org.ngafid.events.EventStatisticsFetch && printf '\n\n(Cron Execution Finished!)\n\n' >> $SCRIPT_DIR/EventStatisticsFetch.log"