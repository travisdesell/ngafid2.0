package org.ngafid.accounts;

import java.io.IOException;
import java.io.InputStream;
import java.sql.*;
import java.time.Duration;
import java.time.LocalDateTime;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

import org.ngafid.Database;
import org.ngafid.WebServer;
import org.ngafid.flights.AirSync;
import org.ngafid.flights.AirSyncEndpoints;
import org.ngafid.flights.AirSyncImport;

import com.google.gson.Gson;
import com.google.gson.reflect.*;

/**
 * This is a representation of an AirSync enabled fleet in the NGAFID
 */
public class AirSyncFleet extends Fleet {
    private AirSyncAuth authCreds;
    private List<AirSyncAircraft> aircraft;
    private LocalDateTime lastQueryTime;

    //timeout in minutes
    private int timeout = -1;

    // 1 Day
    private static final int DEFAULT_TIMEOUT = 1440;

    private static AirSyncFleet [] fleets = null;
        
    private static final Logger LOG = Logger.getLogger(AirSyncFleet.class.getName());

    private static final Gson gson = WebServer.gson;

    /**
     * Default constructor 
     *
     * @param id the fleet id
     * @param name this fleet's name
     * @param airSyncAuth the credentials for this fleet
     * @param lastQueryTime the last time this fleet was synced with AirSync
     * @param timeout how long the fleet is set to wait before checking for updates again
     */
    public AirSyncFleet(int id, String name, AirSyncAuth airSyncAuth, LocalDateTime lastQueryTime, int timeout) {
        super(id, name);
        this.authCreds = airSyncAuth;
        this.lastQueryTime = lastQueryTime;

        if (timeout <= 0) {
            this.timeout = DEFAULT_TIMEOUT;
        } else {
            this.timeout = timeout;
        }
    }

    /**
     * Private constructor for instantiation within this class
     *
     * @param resultSet a SQL ResultSet that has information from a query for the fleet requested.
     */
    private AirSyncFleet(ResultSet resultSet) throws SQLException {
        super(resultSet.getInt(1), resultSet.getString(2));
        this.authCreds = new AirSyncAuth(resultSet.getString(3), resultSet.getString(4));

        Timestamp timestamp = resultSet.getTimestamp(5);
        if (timestamp == null) {
            this.lastQueryTime = LocalDateTime.MIN;
        } else {
            this.lastQueryTime = timestamp.toLocalDateTime();
        }

        int timeout = resultSet.getInt(6);
        if (timeout <= 0) {
            this.timeout = DEFAULT_TIMEOUT;
        } else {
            this.timeout = timeout;
        }
    }

    /**
     * Semaphore-style (P) mutex that allows for an entity (i.e. the daemon or user) to 
     * ask AirSync for updates
     *
     * In this case, the database holds the "signal" or lock flag
     *
     * @param connection the DBMS connection
     *
     * @return true if able to enter the critical section, false otherwise. This
     * can allow for a busy-wait elsewhere
     *
     * @throws SQLException if the DMBS has an issue
     */
    public boolean lock(Connection connection) throws SQLException {
        String sql = "SELECT mutex FROM airsync_fleet_info WHERE fleet_id = ?";
        PreparedStatement query = connection.prepareStatement(sql);

        query.setInt(1, super.getId());
        ResultSet resultSet = query.executeQuery();

        if (resultSet.next() && !resultSet.getBoolean(1)) {
            sql = "UPDATE airsync_fleet_info SET mutex = 1 WHERE fleet_id = ?";
            query = connection.prepareStatement(sql);

            query.setInt(1, super.getId());
            query.executeUpdate();
            query.close();

            return true;
        }

        query.close();
        return false;
    }

    /**
     * Semaphore-style (V) mutex that allows for an entity (i.e. the daemon or user) to 
     * ask AirSync for updates
     *
     * In this case, the database holds the "signal" or lock flag
     *
     * @param connection the DBMS connection
     *
     * @throws SQLException if the DMBS has an issue
     */
    public void unlock(Connection connection) throws SQLException {
       String sql = "UPDATE airsync_fleet_info SET mutex = 0 WHERE fleet_id = ?";
       PreparedStatement query = connection.prepareStatement(sql);

       query.setInt(1, super.getId());
       query.executeUpdate();

       query.close();
    }

    private LocalDateTime getLastQueryTime(Connection connection) throws SQLException {
        if (this.lastQueryTime == null) {
            String sql = "SELECT last_upload_time FROM airsync_fleet_info WHERE fleet_id = ?";
            PreparedStatement query = connection.prepareStatement(sql);

            query.setInt(1, super.getId());

            ResultSet resultSet = query.executeQuery();

            LocalDateTime lastQueryTime = null;
            if (resultSet.next()) {
                lastQueryTime = resultSet.getTimestamp(1).toLocalDateTime();
            } 
            
            query.close();
            return lastQueryTime;
        }

        return this.lastQueryTime;
    } 

    /**
     * Gets the timeout of the AirSyncFleet
     *
     * @param connection the DBMS connection
     *
     * @return the timeout in minutes as an int
     *
     * @throws SQLException if there is an error with the DBMS
     */
    public int getTimeout(Connection connection) throws SQLException {
        if (this.timeout <= 0) {
            String sql = "SELECT timeout FROM airsync_fleet_info WHERE fleet_id = ?";
            PreparedStatement query = connection.prepareStatement(sql);

            query.setInt(1, super.getId());

            ResultSet resultSet = query.executeQuery();

            if (resultSet.next()) {
                int timeout = resultSet.getInt(1);

                if (timeout > 0) {
                    this.timeout = timeout;
                }
            }

            query.close();
        }

        return timeout;
    }

    /**
     * Gets the timeout this fleet chose in a human-readable form
     *
     * @param connection the DBMS connection
     * @param fleetId the Fleets's id
     *
     * @return the timeout as "X hours" or "XX minutes"
     *
     * @throws SQLException is there is a DBMS error
     */
    public static String getTimeout(Connection connection, int fleetId) throws SQLException {
        String sql = "SELECT timeout FROM airsync_fleet_info WHERE fleet_id = ?";
        PreparedStatement query = connection.prepareStatement(sql);

        query.setInt(1, fleetId);

        ResultSet resultSet = query.executeQuery();
        int timeout = 0;

        if (resultSet.next()) {
            timeout = resultSet.getInt(1);
        } 

        String formattedString = null;

        if (timeout > 0) {
            if (timeout >= 60) {
                formattedString = String.format("%d Hours", (timeout / 60));
            } else {
                formattedString = String.format("%d Minutes", timeout); 
            }
        }

        query.close();

        return formattedString;
    }

    /**
     * Determines if the fleet is "out of data" i.e., is the last time we checked with airsync longer 
     * ago than the timeout specified?
     *
     * @param connection the DBMS connection
     *
     * @throws SQLException if there is a DBMS issue
     */
    public boolean isQueryOutdated(Connection connection) throws SQLException {
        return (Duration.between(getLastQueryTime(connection), LocalDateTime.now()).toMinutes() >= getTimeout(connection));
    }

    /**
     * This gets an existing AirSyncFleet by its fleet id
     *
     * @param connection is the DBMS connection
     * @param fleetId is the id of the fleet to get
     *
     * @return an instance of an AirSyncFleet with the given id
     *
     * @throws SQLException if the DBMS has an error
     */
    public static AirSyncFleet getAirSyncFleet(Connection connection, int fleetId) throws SQLException {
        String sql = "SELECT fl.id, fl.fleet_name, sync.api_key, sync.api_secret, sync.last_upload_time, sync.timeout FROM fleet AS fl INNER JOIN airsync_fleet_info AS sync ON sync.fleet_id = fl.id WHERE fl.id = ?";

        PreparedStatement query = connection.prepareStatement(sql);
        query.setInt(1, fleetId);

        ResultSet resultSet = query.executeQuery();

        if (resultSet.next()) {
            AirSyncFleet fleet = new AirSyncFleet(resultSet);

            query.close();
            return fleet;
        }

        query.close();
        return null;
    }

    /**
     * Updates the fleets timeout for AirSync (how long to wait between checks)
     *
     * @param connection the DBMS connection
     * @param user the User that is making the request
     *
     * @throws SQLException if the DBMS has an issue
     */
    public void updateTimeout(Connection connection, User user, String newTimeout) throws SQLException {
        // It is assumed that the UI will prevent non-admins from doing this, 
        // but just to be safe we will have this check.
        if (user.isAdmin()) {
            String [] timeoutTok = newTimeout.split("\\s");

            int duration = Integer.parseInt(timeoutTok[0]);

            int timeoutMinutes = -1;

            if (timeoutTok[1].equalsIgnoreCase("Hours")) {
                timeoutMinutes = duration * 60;
            } else if (timeoutTok[1].equalsIgnoreCase("Minutes")) {
                timeoutMinutes = duration;
            } else {
                // This should not happen as long as the options for on the UI contain "minutes" or "hours"
                return;
            }

            String sql = "UPDATE airsync_fleet_info SET timeout = ? WHERE fleet_id = ?";
            PreparedStatement query = connection.prepareStatement(sql);
            
            query.setInt(1, timeoutMinutes);
            query.setInt(2, super.getId());

            query.executeUpdate();
            query.close();
        } else {
            LOG.severe("Non-admin user attempted to change AirSync settings! This should not happen! Offending user: " + user.getFullName());
        }
    }

    /**
     * Sets the last query time of the fleet to now
     *
     * @param connection the DBMS connection
     *
     * @throws SQLException if there is a DBMS issue
     */
    public void setLastQueryTime(Connection connection) throws SQLException {
        this.setLastQueryTime(LocalDateTime.now(), connection);
    }

    /**
     * Sets the last query time for this fleet
     *
     * @param time the time that the fleet was last queried
     * @param connection the DBMS connection
     *
     * @throws SQLException if there is a DBMS issue
     */
    public void setLastQueryTime(LocalDateTime time, Connection connection) throws SQLException {
        String sql = "UPDATE airsync_fleet_info SET last_upload_time = CURRENT_TIMESTAMP WHERE fleet_id = ?";

        PreparedStatement query = connection.prepareStatement(sql);
        query.setInt(1, super.getId());

        query.executeUpdate();
        query.close();

        //Force updating these variables the next time there
        //is a check
        this.timeout = -1;
        this.lastQueryTime = null;
    }

    /**
     * Checks to see if the authentication string os out of date and requests a new one if it is.
     * This should always be used to access the credentials of the fleet for the AirSync server.
     * Otherwise, exceptions may be thrown when using an out of data auth object.
     *
     * @return an unexpired {@link AirSyncAuth} instance
     */
    public AirSyncAuth getAuth() {
        if (this.authCreds.isOutdated()) {
            LOG.info("Bearer token is out of date. Requesting a new one.");
            this.authCreds.requestAuthorization();
        }

        return this.authCreds;
    }

    /**
     * Gets all the AirSync aircraft that belong to this fleet
     *
     * @return a {@link List} of the {@link AirSyncAircraft} in this fleet.
     */
    public List<AirSyncAircraft> getAircraft() {
        if (aircraft == null) {
            try {
                HttpsURLConnection connection = (HttpsURLConnection) new URL(AirSyncEndpoints.AIRCRAFT).openConnection();

                connection.setRequestMethod("GET");
                connection.setDoOutput(true);
                connection.setRequestProperty("Authorization", this.authCreds.bearerString());     

                InputStream is = connection.getInputStream();
                byte [] respRaw = is.readAllBytes();
                
                String resp = new String(respRaw).replaceAll("tail_number", "tailNumber");
                
                Type target = new TypeToken<List<AirSyncAircraft>>(){}.getType();
                this.aircraft = gson.fromJson(resp, target);

                for (AirSyncAircraft a : aircraft) a.initialize(this);
            } catch (IOException ie) {
                AirSync.handleAirSyncAPIException(ie, this.authCreds);
            }
        }
        
        return this.aircraft;
    }

    /**
     * Gets all AirSync fleets in an array
     *
     * @param connection the DBMS connection
     *
     * @return an array of {@link AirSyncFleet}
     *
     * @throws SQLException if there is a DBMS issue
     */
    public static AirSyncFleet [] getAll(Connection connection) throws SQLException {
        String sql = "SELECT COUNT(*) FROM airsync_fleet_info";
        PreparedStatement query = connection.prepareStatement(sql);

        ResultSet resultSet = query.executeQuery();

        int asFleetCount = 0;
        if (resultSet.next()) {
            asFleetCount = resultSet.getInt(1);
        } else {
            query.close();
            return null;
        }

        query.close();

        if (fleets == null || fleets.length != asFleetCount) {
            sql = "SELECT fl.id, fl.fleet_name, sync.api_key, sync.api_secret, sync.last_upload_time, sync.timeout FROM fleet AS fl INNER JOIN airsync_fleet_info AS sync ON sync.fleet_id = fl.id";
            query = connection.prepareStatement(sql);

            resultSet = query.executeQuery();

            fleets = new AirSyncFleet[asFleetCount];

            int i = 0;
            while (resultSet.next()) {
                fleets[i++] = new AirSyncFleet(resultSet);
            }

            query.close();
        }

        return fleets;
    }

    /**
     * Gets a list of the processed flight ids for this fleet that are from AirSync
     *
     * @param connection the dbms connection
     *
     * @return a {@link List} of {@link Integer} with the ids of the processed imports
     *
     * @throws SQLException if there is a DBMS issue
     */
    private List<Integer> getProcessedIds(Connection connection) throws SQLException {
        String sql = "SELECT id FROM airsync_imports WHERE fleet_id = ?";
        PreparedStatement query = connection.prepareStatement(sql);

        query.setInt(1, super.getId());

        ResultSet resultSet = query.executeQuery();
        List<Integer> ids = new LinkedList<>();

        while (resultSet.next()) {
            ids.add(resultSet.getInt(1));
        }

        return ids;
    }

    /**
     * Gets the last update/upload time for an AirSyncFleet
     *
     * @param connection a DBMS connection
     *
     * @return the timestamp as a string form.
     */
    public String getLastUpdateTime(Connection connection) throws SQLException {
        String sql = "SELECT last_upload_time FROM airsync_fleet_info WHERE fleet_id = ?";
        PreparedStatement query = connection.prepareStatement(sql);

        query.setInt(1, super.getId());

        ResultSet resultSet = query.executeQuery();

        if (resultSet.next()) {
            Timestamp timestamp = resultSet.getTimestamp(1);

            if (timestamp != null) {
                return timestamp.toString();
            }
        }

        return "N/A";
    }

    /**
     * Updates this fleet with the AirSync servers
     * This is not thread-safe and should be guarded with a mutex!
     *
     * @param connection the DBMS connection
     *
     * @return a "status" string that is human readable
     * 
     * @throws SQLException if the DBMS has an issue
     */
    public String update(Connection connection) throws Exception {
        int nImports = 0;

        List<AirSyncAircraft> aircraft = this.getAircraft();
        for (AirSyncAircraft a : aircraft) {
            List<Integer> processedIds = getProcessedIds(connection);

            Optional<LocalDateTime> aircraftLastImportTime = a.getLastImportTime(connection);

            List<AirSyncImport> imports;

            if (aircraftLastImportTime.isPresent()) {
                // We must make the interval exclusive when asking the server for flights
                LocalDateTime importTime = aircraftLastImportTime.get().plusSeconds(1);
                imports = a.getImportsAfterDate(connection, this, importTime);
                LOG.info(String.format("Getting imports for fleet %s after %s.", super.getName(), importTime.toString()));
            } else {
                imports = a.getImports(connection, this);
                LOG.info(String.format("Getting all imports for fleet %s, as there are no other uploads waiting for this fleet.", super.getName()));
            }

            if (imports != null && !imports.isEmpty()) {
                for (AirSyncImport i : imports) {
                    if (processedIds.contains(i.getId())) {
                        LOG.info("Skipping AirSync with upload id: " + i.getId() + " as it already exists in the database");
                    } else {
                        i.process(connection);
                        System.out.println("Done processing " + i.getId());
                        nImports++;
                    }
                }
            } else {
                LOG.info("No imports found for aircraft: " + a.getTailNumber() + " in fleet " + super.getName() + ", continuing.");
            }
        }

        this.setLastQueryTime(connection);

        if (nImports > 0) {
            return String.format("AirSync update complete! %d new flights imported.", nImports);
        } else {
            return "No new imports found from AirSync.";
        }
    }
}
