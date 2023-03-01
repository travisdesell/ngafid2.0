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
    private long timeout = -1;

    // 1 Day
    private static final long DEFAULT_TIMEOUT = 1440;

    private static AirSyncFleet [] fleets = null;
        
    private static final Logger LOG = Logger.getLogger(AirSyncFleet.class.getName());

    private static final Gson gson = WebServer.gson;

    public AirSyncFleet(int id, String name, AirSyncAuth airSyncAuth, LocalDateTime lastQueryTime, long timeout) {
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

        long timeout = resultSet.getLong(6);
        if (timeout <= 0) {
            this.timeout = DEFAULT_TIMEOUT;
        } else {
            this.timeout = timeout;
        }
    }

    private LocalDateTime getLastQueryTime(Connection connection) throws SQLException {
        if (lastQueryTime == null) {
            String sql = "SELECT last_upload_time FROM airsync_fleet_info WHERE fleet_id = ?";
            PreparedStatement query = connection.prepareStatement(sql);

            query.setInt(1, super.getId());

            ResultSet resultSet = query.executeQuery();

            if (resultSet.next()) {
                return resultSet.getTimestamp(1).toLocalDateTime();
            } else return null;
        }

        return lastQueryTime;
    } 

    public long getTimeout(Connection connection) throws SQLException {
        if (timeout <= 0) {
            String sql = "SELECT timeout FROM airsync_fleet_info WHERE fleet_id = ?";
            PreparedStatement query = connection.prepareStatement(sql);

            query.setInt(1, super.getId());

            ResultSet resultSet = query.executeQuery();

            if (resultSet.next()) {
                long timeout = resultSet.getLong(1);

                if (timeout > 0) {
                    this.timeout = timeout;
                }
            }

            query.close();
        }

        return timeout;
    }

    public boolean isQueryOutdated(Connection connection) throws SQLException {
        return (Duration.between(getLastQueryTime(connection), LocalDateTime.now()).toMinutes() >= getTimeout(connection));
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

        if (fleets == null || fleets.length != asFleetCount) {
            sql = "SELECT fl.id, fl.fleet_name, sync.api_key, sync.api_secret, sync.last_upload_time, sync.timeout FROM fleet AS fl INNER JOIN airsync_fleet_info AS sync ON sync.fleet_id = fl.id";
            query = connection.prepareStatement(sql);

            resultSet = query.executeQuery();

            fleets = new AirSyncFleet[asFleetCount];

            int i = 0;
            while (resultSet.next()) {
                fleets[i++] = new AirSyncFleet(resultSet);
            }
        }

        query.close();

        return fleets;
    }
}
