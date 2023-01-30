package org.ngafid.accounts;

import java.io.IOException;
import java.io.InputStream;
import java.sql.*;
import java.time.Duration;
import java.time.LocalDateTime;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
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
    private Timestamp lastQueryTime;
    private Duration timeout;

    private static AirSyncFleet [] fleets = null;

    private static final Gson gson = WebServer.gson;

    public AirSyncFleet(int id, String name, AirSyncAuth airSyncAuth, Timestamp lastQueryTime, long timeout) {
        super(id, name);
        this.authCreds = airSyncAuth;
        this.lastQueryTime = lastQueryTime;
        this.timeout = Duration.ofMinutes(timeout);
    }

    private AirSyncFleet(ResultSet resultSet) throws SQLException {
        this(resultSet.getInt(1), resultSet.getString(2), 
            new AirSyncAuth(resultSet.getString(3), resultSet.getString(4)),
            resultSet.getTimestamp(5), resultSet.getInt(6));
    }

    private LocalDateTime getLastQueryTime(Connection connection) throws SQLException {
        String sql = "SELECT last_upload_time FROM airsync_fleet_info WHERE fleet_id = ?";
        PreparedStatement query = connection.prepareStatement(sql);

        ResultSet resultSet = query.executeQuery();

        if (resultSet.next()) {
            return resultSet.getTimestamp(1).toLocalDateTime();
        } else return null;

    } 

    public boolean isQueryOutdated(Connection connection) throws SQLException {
        return (Duration.between(getLastQueryTime(connection), this.lastQueryTime.toLocalDateTime()).compareTo(this.timeout) >= 0);
    }

    public void setLastQueryTime() {
        this.setLastQueryTime(LocalDateTime.now(), Database.getConnection());
    }

    public void setLastQueryTime(LocalDateTime time, Connection connection) {
        String sql = "UPDATE airsync_fleet_info SET last_upload_time WHERE fleet_id = ?";

        try {
            PreparedStatement query = connection.prepareStatement(sql);
            query.executeUpdate();
        } catch (SQLException se) {
            se.printStackTrace();
        }
    }

    public AirSyncAuth getAuth() {
        if (this.authCreds.isOutdated()) {
            this.authCreds.requestAuthorization();
        }

        return this.authCreds;
    }

    public LocalDateTime getLastImportTime(Connection connection, AirSyncAircraft aircraft) throws SQLException {
        String sql = "SELECT MAX(start_time) FROM flights WHERE id IN (SELECT flight_id FROM airsync_imports WHERE fleet_id = ? AND tail = ?)";
        PreparedStatement query = connection.prepareStatement(sql);

        query.setInt(1, this.getId());
        query.setString(2, aircraft.getTailNumber());

        LocalDateTime time = null;
        ResultSet resultSet = query.executeQuery();
        if (resultSet.next()) {
            Timestamp t = resultSet.getTimestamp(1);

            if (t != null) {
                time = t.toLocalDateTime();
            }
        }

        return time;
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
        } else return null;

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

        return fleets;
    }
}
