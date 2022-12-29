package org.ngafid.accounts;

import java.io.IOException;
import java.io.InputStream;
import java.sql.*;
import java.time.LocalDateTime;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

import org.ngafid.WebServer;

import com.google.gson.Gson;
import com.google.gson.reflect.*;

public class AirSyncFleet extends Fleet {
    private AirSyncAuth authCreds;
    private List<AirSyncAircraft> aircraft;
    private LocalDateTime lastUploadTime;

    private static AirSyncFleet [] fleets = null;
    private static String AIR_SYNC_AIRCRAFT_ENDPOINT = "https://service-dev.air-sync.com/partner_api/v1/aircraft/";

    private static final Gson gson = WebServer.gson;

    public AirSyncFleet(int id, String name, AirSyncAuth airSyncAuth, LocalDateTime lastUploadTime) {
        super(id, name);
        this.authCreds = airSyncAuth;
        this.lastUploadTime = lastUploadTime;
    }

    private AirSyncFleet(ResultSet resultSet) throws SQLException {
        this(resultSet.getInt(1), resultSet.getString(2), 
            new AirSyncAuth(resultSet.getString(3), resultSet.getString(4)), resultSet.getTimestamp(5).toLocalDateTime());
    }

    public AirSyncAuth getAuth() throws IOException {
        if (this.authCreds.isOutdated()) {
            this.authCreds.requestAuthorization();
        }

        return this.authCreds;
    }

    public List<AirSyncAircraft> getAircraft() throws IOException {
        if (aircraft == null) {
            HttpsURLConnection connection = (HttpsURLConnection) new URL(AIR_SYNC_AIRCRAFT_ENDPOINT).openConnection();

            connection.setRequestMethod("GET");
            connection.setDoOutput(true);
            connection.setRequestProperty("Authorization", this.authCreds.bearerString());     

            InputStream is = connection.getInputStream();
            byte [] respRaw = is.readAllBytes();
            
            String resp = new String(respRaw).replaceAll("tail_number", "tailNumber");
            
            Type target = new TypeToken<List<AirSyncAircraft>>(){}.getType();
            this.aircraft = gson.fromJson(resp, target);
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
            sql = "SELECT fl.id, fl.fleet_name, sync.api_key, sync.api_secret, sync.last_upload_time FROM fleet AS fl INNER JOIN airsync_fleet_info AS sync ON sync.fleet_id = fl.id";
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
