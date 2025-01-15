CRON_TIME="* * * * *"    #Every minute (For testing)
#CRON_TIME="0 0 * * *"    #Every day at midnight
CRON_PATH="$( cd ./ "$( dirname "$0" )" >/dev/null 2>&1 && pwd )"
CRON_CMD="$CRON_TIME cd $CRON_PATH && . ./init_env.sh && $JAVA_HOME/bin/java -cp target/ngafid-1.0-SNAPSHOT.jar org.ngafid.events.EventStatisticsFetch"