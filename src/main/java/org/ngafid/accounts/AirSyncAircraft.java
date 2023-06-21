package org.ngafid.accounts;

import java.net.*;
import java.sql.*;
import javax.net.ssl.HttpsURLConnection;

import org.ngafid.WebServer;
import org.ngafid.accounts.AirSyncAuth.AccessToken;
import org.ngafid.flights.AirSync;
import org.ngafid.flights.AirSyncEndpoints;
import org.ngafid.flights.AirSyncImport;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedList;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;
import java.io.IOException;
import java.io.InputStream;

public class AirSyncAircraft {
    private int id;
    private String tailNumber;
    private AirSyncFleet fleet;
    private static final Gson gson = WebServer.gson;

    // NOTE: If this code exists in the year 9999, this may want to be adjusted :p
    private static LocalDateTime MAX_LCL_DATE_TIME = LocalDateTime.of(9999, 12, 31, 10, 10);

    private static final String TIMESTAMP_UPLOADED = "";

    private static final Logger LOG = Logger.getLogger(AirSyncAircraft.class.getName());

    private AirSyncAircraft(int id, String tailNumber) {
        this.id = id;
        this.tailNumber = tailNumber;
    }

    public void initialize(AirSyncFleet fleet) {
        this.fleet = fleet;
    }

    public String getTailNumber() {
        return this.tailNumber;
    }

    private URL getAircraftLogURL(int page) throws MalformedURLException {
        return new URL(String.format(AirSyncEndpoints.ALL_LOGS, this.id, page, AirSyncEndpoints.PAGE_SIZE));
    }

    private URL getAircraftLogURL(int page, LocalDateTime lastImportTime) throws MalformedURLException {
        return new URL(String.format(AirSyncEndpoints.ALL_LOGS_BY_TIME, this.id, page, AirSyncEndpoints.PAGE_SIZE, lastImportTime.toString(), MAX_LCL_DATE_TIME.toString()));
    }

    public Optional<LocalDateTime> getLastImportTime(Connection connection) throws SQLException {
        String sql = "SELECT MAX(start_time) FROM uploads AS u JOIN airsync_imports AS imp ON imp.fleet_id = u.fleet_id WHERE imp.tail = ? AND imp.fleet_id = ?";
        PreparedStatement query = connection.prepareStatement(sql);

        query.setString(1, this.tailNumber);
        query.setInt(2, this.fleet.getId());
            
        ResultSet resultSet = query.executeQuery();
        if (resultSet.next()) {
            Timestamp timestamp = resultSet.getTimestamp(1);
            if (timestamp != null) {
                return Optional.of(timestamp.toLocalDateTime());
            } 
        }

        return Optional.empty();
    }

    private List<AirSyncImport> getImportsHTTPS(HttpsURLConnection netConnection, AirSyncAuth authentication) throws Exception {
        netConnection.setRequestMethod("GET");
        netConnection.setDoOutput(true);
        netConnection.setRequestProperty("Authorization", authentication.bearerString());     

        InputStream is = netConnection.getInputStream();
        byte [] respRaw = is.readAllBytes();

        String resp = new String(respRaw).replaceAll("aircraft_id", "aircraftId");
        resp = resp.replaceAll("tail_number", "tailNumber");
        resp = resp.replaceAll("time_start", "timeStart");
        resp = resp.replaceAll("time_end", "timeEnd");
        resp = resp.replaceAll("file_url", "fileUrl");
        resp = resp.replaceAll("timestamp_uploaded", "timestampUploaded");

        Type target = new TypeToken<List<AirSyncImport>>(){}.getType();
        List<AirSyncImport> page = gson.fromJson(resp, target);


        // initialize the imports
        for (AirSyncImport i : page) i.init(fleet, this);

        return page;
    }

    public List<AirSyncImport> getImports(Connection connection, AirSyncFleet fleet) {
        AirSyncAuth authentication = fleet.getAuth();
        List<AirSyncImport> imports = new LinkedList<>();

        boolean continueIteration = true;

        int nPage = 0;
        while (continueIteration) {
            try {
                HttpsURLConnection netConnection = (HttpsURLConnection) getAircraftLogURL(nPage++).openConnection();
                List<AirSyncImport> page = getImportsHTTPS(netConnection, fleet.getAuth());

                continueIteration = page.size() == AirSyncEndpoints.PAGE_SIZE;
                imports.addAll(page);
            } catch (Exception e) {
                AirSync.handleAirSyncAPIException(e, authentication);
            }
        }
        
        return imports;
    }

     public List<AirSyncImport> getImportsAfterDate(Connection connection, AirSyncFleet fleet, LocalDateTime lastImportTime) {
        AirSyncAuth authentication = fleet.getAuth();
        List<AirSyncImport> imports = new LinkedList<>();

        boolean continueIteration = true;

        int nPage = 0;
        
        while (continueIteration) {
            try {
                HttpsURLConnection netConnection = (HttpsURLConnection) getAircraftLogURL(nPage++, lastImportTime).openConnection();
                List<AirSyncImport> page = getImportsHTTPS(netConnection, authentication);

                continueIteration = page.size() == AirSyncEndpoints.PAGE_SIZE;
                imports.addAll(page);
            } catch (Exception e) {
                AirSync.handleAirSyncAPIException(e, authentication);
            }
        }

        return imports;
     }
    
}
