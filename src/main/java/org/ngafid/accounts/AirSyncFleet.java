package org.ngafid.accounts;

import java.io.IOException;
import java.io.InputStream;
import java.sql.*;
import java.time.Duration;
import java.time.LocalDateTime;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

import org.ngafid.Database;
import org.ngafid.WebServer;
import org.ngafid.flights.AirSync;
import org.ngafid.flights.AirSyncEndpoints;

import com.google.gson.Gson;
import com.google.gson.reflect.*;

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

    public void setLastQueryTime(Connection connection) throws SQLException {
        this.setLastQueryTime(LocalDateTime.now(), connection);
    }

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

    public AirSyncAuth getAuth() {
        if (this.authCreds.isOutdated()) {
            LOG.info("Bearer token is out of date. Requesting a new one.");
            this.authCreds.requestAuthorization();
        }

        return this.authCreds;
    }

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
}
