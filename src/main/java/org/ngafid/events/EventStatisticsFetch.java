package org.ngafid.events;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import org.ngafid.Database;
import org.ngafid.events.EventStatistics.EventCounts;

import com.google.gson.Gson;


@SuppressWarnings("LoggerStringConcat")
public class EventStatisticsFetch {

    /*
        Uses a cron jobs (via the 'event_stats' directory)
        to repeatedly fetch the event statistics from the database
        and cache them in a JSON file in the same directory.

        A backup of the JSON cache file is created before each fetch.

        Information is logged in the EventStatisticsFetch.log file
        located in the same directory as the cron scripts.
    */

    protected static final String EVENT_STATS_DIRECTORY = "event_stats/";
    private static final Logger LOG = Logger.getLogger(EventStatisticsFetch.class.getName());
    private static final String LOG_FILE_NAME = (EVENT_STATS_DIRECTORY + "EventStatisticsFetch.log");
    private static final boolean LOG_DO_APPEND = true;
    
    protected static final String JSON_CACHE_FILE_NAME = (EVENT_STATS_DIRECTORY + "EventStatisticsCache.json");

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");



    public static void main(String[] args) {

        LOG.info("EventStatisticsFetch.java -- Starting...");

        //Initialize File Handler
        try {

            //Initialize File Handler & Formatting
            final FileHandler fh = new FileHandler(LOG_FILE_NAME, LOG_DO_APPEND);
            LOG.addHandler(fh);
            SimpleFormatter formatter = new SimpleFormatter();
            fh.setFormatter(formatter);
            
            //Record and log the current date/time
            String currentDate = DATE_FORMATTER.format(java.time.LocalDate.now());
            LOG.info("EventStatisticsFetch started at " + currentDate);

            //Try to connect to the database
            Connection connection = Database.resetConnection(LOG);
            LOG.info("EventStatisticsFetch established database connection...");

            //Check if the JSON cache file exists
            boolean cacheExists = (Files.exists(Paths.get(JSON_CACHE_FILE_NAME)));

            //Cache exists, create temporary copy as a backup in case of an error
            if (cacheExists) {

                boolean backupSuccess = false;
                try {

                    LOG.info("Attempting to create backup of JSON cache file...");
                    Files.copy(
                        Paths.get(JSON_CACHE_FILE_NAME),
                        Paths.get(JSON_CACHE_FILE_NAME + ".bak"),
                        StandardCopyOption.REPLACE_EXISTING
                    );

                    backupSuccess = true;

                } catch (IOException e) {
                    LOG.severe("Error creating backup of JSON cache file:\n\t" + e.getMessage());
                }

                //Backup failed, exit
                if (!backupSuccess) {
                    LOG.severe("Backup of JSON cache file failed, stopping process...");
                    return;
                }

            } else {
                LOG.info("JSON cache file does not exist, a new file will be created");
            }

            //Clear/create the JSON cache file
            try (FileWriter file = new FileWriter(JSON_CACHE_FILE_NAME)) {
                file.write("{}");
                file.flush();
            } catch (IOException e) {
                LOG.severe("Error clearing JSON cache file:\n\t" + e.getMessage());
            }

            //Fetch the event statistics
            fetchEventStatistics(connection);
            LOG.info("EventStatisticsFetch finished!\n\n");

            //Remove File Handler from Logger
            LOG.removeHandler(fh);

        } catch (Exception e) {

            LOG.severe("Error in EventStatisticsFetch:\n\t" + e.getMessage());

            //Restore the backup of the JSON cache file
            try {
                LOG.info("Attempting to restore backup of JSON cache file...");
                Files.copy(Paths.get(JSON_CACHE_FILE_NAME + ".bak"), Paths.get(JSON_CACHE_FILE_NAME));
            } catch (IOException e2) {
                LOG.severe("Error in trying to restore backup of JSON cache file:\n\t" + e2.getMessage());
            }

        }

        LOG.info("EventStatisticsFetch.java -- ...Finished!");

    }


    private static int fetchResultSetSize(ResultSet resultSet) throws SQLException {

        //Set is null, return 0
        if (resultSet == null)
            return 0;

        //Move to the last row
        resultSet.last();

        //Get the row number of the last row
        int rowCountOut = resultSet.getRow();

        //Move back to the first row
        resultSet.beforeFirst();

        return rowCountOut;

    }

    private static void fetchEventStatistics(Connection connection) throws SQLException, IOException {

        LOG.info("Established database connection, fetching event statistics and clearing cached statistics...");
        
        final String ALL_FLEETS_QUERY = "SELECT * FROM fleet";
        
        try (
            PreparedStatement statement = connection.prepareStatement(ALL_FLEETS_QUERY, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
            ResultSet resultSet = statement.executeQuery()
        ) {

            final int resultSetSize = fetchResultSetSize(resultSet);
            LOG.info("Fetched " + resultSetSize + " fleets from the database.");
            
            //Aggregate statistics for all fleets
            Map<Integer, CacheObject> fleetStatisticsMap = new HashMap<>();
            
            while (resultSet.next()) {

                int fleetId = resultSet.getInt("id");
                String fleetName = resultSet.getString("fleet_name");
                LOG.info("Fleet Data -- ID: " + fleetId + ", Name: '" + fleetName + "'");

                //Fetch statistics for this fleet
                ArrayList<EventStatistics> eventStatisticsList = EventStatistics.getAll(connection, fleetId);
                Map<String, EventStatistics.EventCounts> eventCountsMap = EventStatistics.getEventCounts(connection, fleetId, null, null);

                //Store in aggregated data structure
                fleetStatisticsMap.put(fleetId, new CacheObject(eventStatisticsList, eventCountsMap));
            }
            
            //Save aggregated data to JSON cache
            saveAggregatedToJsonCache(fleetStatisticsMap);
            
        }

    }

    //protected static class CacheObject {
    public static class CacheObject {

        ArrayList<EventStatistics> statsList;
        Map<String, EventCounts> eventCounts;
        
        public CacheObject() {
            statsList = new ArrayList<>();
            eventCounts = null;
        }

        public CacheObject(ArrayList<EventStatistics> statsList, Map<String, EventCounts> eventCounts) {
            this.statsList = statsList;
            this.eventCounts = eventCounts;
        }

        public ArrayList<EventStatistics> getStatsList() {
            return statsList;
        }

        public void setStatsList(ArrayList<EventStatistics> statsList) {
            this.statsList = statsList;
        }

        public Map<String, EventCounts> getEventCounts() {
            return eventCounts;
        }

        public void setEventCounts(Map<String, EventCounts> eventCounts) {
            this.eventCounts = eventCounts;
        }
        
    }

    private static void saveAggregatedToJsonCache(Map<Integer, CacheObject> fleetStatisticsMap) throws IOException {

        Gson gson = new Gson();
        try (FileWriter file = new FileWriter(JSON_CACHE_FILE_NAME)) {

            file.write(gson.toJson(fleetStatisticsMap));
            file.flush();
            
        } catch (IOException e) {
            LOG.severe("Error saving aggregated event statistics to JSON cache file:\n\t" + e.getMessage());
        }
        
    }

}