package org.ngafid.core.accounts;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.ngafid.core.airsync.AirSync;
import org.ngafid.core.airsync.AirSyncEndpoints;
import org.ngafid.core.airsync.AirSyncImport;

import javax.net.ssl.HttpsURLConnection;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * Represents an Aircraft that is AirSync compatibile
 */
public final class AirSyncAircraft {
    public static final Gson GSON = new GsonBuilder().serializeSpecialFloatingPointValues().create();

    // NOTE: If this code exists in the year 9999, this may want to be adjusted :p
    private static final LocalDateTime MAX_LCL_DATE_TIME = LocalDateTime.of(9999, 12, 31, 10, 10);
    private static final String TIMESTAMP_UPLOADED = "";
    private static final Logger LOG = Logger.getLogger(AirSyncAircraft.class.getName());
    private final int id;
    private final String tailNumber;
    private AirSyncFleet fleet;

    /**
     * Private constructor, for instantiation within this class.
     *
     * @param id         the Aircraft's id
     * @param tailNumber the Aircraft's tail number
     */
    private AirSyncAircraft(int id, String tailNumber) {
        this.id = id;
        this.tailNumber = tailNumber;
    }

    /**
     * Sets a reference to the fleet which this aircraft belongs to
     *
     * @param airsyncFleet the AirSyncFleet that is responsible for this aircraft
     */
    public void initialize(AirSyncFleet airsyncFleet) {
        this.fleet = airsyncFleet;
    }

    /**
     * Public accessor method for this aircraft
     *
     * @return the tail number as a string
     */
    public String getTailNumber() {
        return this.tailNumber;
    }

    /**
     * Gets the aircraft log URL from page number
     *
     * @param page the page number for AirSync servers
     * @return a URL to the logfile
     */
    private URL getAircraftLogURL(int page) throws MalformedURLException {
        return new URL(String.format(AirSyncEndpoints.ALL_LOGS, this.id, page, AirSyncEndpoints.PAGE_SIZE));
    }

    /**
     * Gets the aircraft log URL from page number AFTER
     * the last import time
     *
     * @param page           the page number for AirSync servers
     * @param lastImportTime the last import time
     * @return a URL to the logfile
     */
    private URL getAircraftLogURL(int page, LocalDateTime lastImportTime) throws MalformedURLException {
        return new URL(String.format(AirSyncEndpoints.ALL_LOGS_BY_TIME, this.id, page, AirSyncEndpoints.PAGE_SIZE,
                lastImportTime.toString(), MAX_LCL_DATE_TIME));
    }

    /**
     * Gets the last import time for this fleet.
     *
     * @param connection the dbms connection
     * @return an {@link Optional} of {@link LocalDateTime} representing the last
     * import time
     */
    public Optional<LocalDateTime> getLastImportTime(Connection connection) throws SQLException {
        String sql = "SELECT MAX(start_time) FROM uploads AS u " + "JOIN airsync_imports AS imp ON imp.fleet_id = u" +
                ".fleet_id WHERE imp.tail = ? AND imp.fleet_id = ?";
        try (PreparedStatement query = connection.prepareStatement(sql)) {

            query.setString(1, this.tailNumber);
            query.setInt(2, this.fleet.getId());

            try (ResultSet resultSet = query.executeQuery()) {
                if (resultSet.next()) {
                    Timestamp timestamp = resultSet.getTimestamp(1);
                    if (timestamp != null) {
                        return Optional.of(timestamp.toLocalDateTime());
                    }
                }
            }
        }

        return Optional.empty();
    }

    /**
     * Gets a list of AirSyncImports securely with HTTPS
     *
     * @param netConnection  the connection to the AirSync servers
     * @param authentication the instance of {@link AirSyncAuth} for this fleet
     * @return a {@link List} of AirSyncImports
     * @throws IOException an exception if there is a network or dbms issue
     */
    private List<AirSyncImport> getImportsHTTPS(HttpsURLConnection netConnection, AirSyncAuth authentication)
            throws IOException {
        netConnection.setRequestMethod("GET");
        netConnection.setDoOutput(true);
        netConnection.setRequestProperty("Authorization", authentication.bearerString());

        byte[] respRaw;
        try (InputStream is = netConnection.getInputStream()) {
            respRaw = is.readAllBytes();
        }

        String resp = new String(respRaw).replaceAll("aircraft_id", "aircraftId");
        resp = resp.replaceAll("tail_number", "tailNumber");
        resp = resp.replaceAll("time_start", "timeStart");
        resp = resp.replaceAll("time_end", "timeEnd");
        resp = resp.replaceAll("file_url", "fileUrl");
        resp = resp.replaceAll("timestamp_uploaded", "timestampUploaded");

        Type target = new TypeToken<List<AirSyncImport>>() {
        }.getType();
        List<AirSyncImport> page = GSON.fromJson(resp, target);

        // initialize the imports
        for (AirSyncImport i : page)
            i.init(fleet, this);

        return page;
    }

    public String getAirSyncFleetName() {
        try {
            byte[] respRaw = getBytes();

            List<AirSyncAircraftAccountInfo> info = GSON.fromJson(new String(respRaw),
                    new TypeToken<List<AirSyncAircraftAccountInfo>>() {
                    }.getType());

            if (info.size() != 1) {
                LOG.severe("AirSync aircraft appears for multiple fleets. We do not support this functionality " +
                        "currently...");
                System.exit(1);
            }

            return info.get(0).name;
        } catch (IOException e) {
            return null;
        }
    }

    private byte[] getBytes() throws IOException {
        AirSyncAuth authentication = fleet.getAuth();
        HttpsURLConnection netConnection = (HttpsURLConnection) new URL(AirSyncEndpoints.AIRSYNC_ROOT + "/aircraft" +
                "/accounts").openConnection();
        netConnection.setRequestMethod("GET");
        netConnection.setDoOutput(true);
        netConnection.setRequestProperty("Authorization", authentication.bearerString());

        byte[] respRaw;
        try (InputStream is = netConnection.getInputStream()) {
            respRaw = is.readAllBytes();
        }
        return respRaw;
    }

    /**
     * Gets ALL imports for this Aircraft
     *
     * @param connection   the database connection
     * @param airSyncFleet the fleet this aircraft belongs to
     * @return a {@link List} of AirSyncImports
     */
    public List<AirSyncImport> getImports(Connection connection, AirSyncFleet airSyncFleet) throws IOException {
        AirSyncAuth authentication = airSyncFleet.getAuth();
        List<AirSyncImport> imports = new LinkedList<>();

        boolean continueIteration = true;

        int nPage = 0;
        while (continueIteration) {
            HttpsURLConnection netConnection = (HttpsURLConnection) getAircraftLogURL(nPage++).openConnection();
            List<AirSyncImport> page = getImportsHTTPS(netConnection, airSyncFleet.getAuth());

            continueIteration = page.size() == AirSyncEndpoints.PAGE_SIZE;
            imports.addAll(page);
        }

        return imports;
    }

    /**
     * Gets a List of imports after a certian date
     *
     * @param connection     the database connection
     * @param airSyncFleet   the AirSyncFleet that these imports belong to
     * @param lastImportTime the last import time recorded in the database
     * @return a {@link List} of AirSyncImports
     */
    public List<AirSyncImport> getImportsAfterDate(Connection connection, AirSyncFleet airSyncFleet,
                                                   LocalDateTime lastImportTime) throws IOException {
        AirSyncAuth authentication = airSyncFleet.getAuth();
        List<AirSyncImport> imports = new LinkedList<>();

        boolean continueIteration = true;

        int nPage = 0;

        while (continueIteration) {
            try {
                HttpsURLConnection netConnection =
                        (HttpsURLConnection) getAircraftLogURL(nPage++, lastImportTime).openConnection();
                List<AirSyncImport> page = getImportsHTTPS(netConnection, authentication);
                continueIteration = page.size() == AirSyncEndpoints.PAGE_SIZE;
                imports.addAll(page);
            } catch (Exception e) {
                AirSync.handleAirSyncAPIException(e, authentication);
            }
        }

        return imports;
    }

    public List<AirSyncImport> getImportsForUpdate(Connection connection, AirSyncFleet airSyncFleet)
            throws IOException, SQLException {
        var lastImportTime = getLastImportTime(connection);
        if (lastImportTime.isPresent()) {
            // We must make the interval exclusive when asking the server for flights
            LocalDateTime importTime = lastImportTime.get().plusSeconds(1);
            return getImportsAfterDate(connection, airSyncFleet, importTime);
        } else {
            return getImports(connection, airSyncFleet);
        }
    }

    //CHECKSTYLE:OFF
    static class AirSyncAircraftAccountInfo {
        public String accountToken;
        public String name;
    }
    //CHECKSTYLE:ON
}
