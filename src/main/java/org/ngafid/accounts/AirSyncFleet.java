package org.ngafid.accounts;

import java.io.IOException;
import java.io.InputStream;
import java.sql.*;
import java.time.Duration;
import java.time.LocalDateTime;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collections;
import java.util.stream.Collectors;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.Files;

import javax.net.ssl.HttpsURLConnection;

import org.ngafid.Database;
import org.ngafid.WebServer;
import org.ngafid.flights.AirSync;
import org.ngafid.flights.AirSyncEndpoints;
import org.ngafid.flights.AirSyncImport;
import org.ngafid.flights.Upload;
import org.ngafid.flights.MalformedFlightFileException;

import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.gson.reflect.*;

/**
 * This is a representation of an AirSync enabled fleet in the NGAFID
 */
public class AirSyncFleet extends Fleet {
    private AirSyncAuth authCreds;
    private String airsyncFleetName;
    private List<AirSyncAircraft> aircraft;
    private transient LocalDateTime lastQueryTime;

    // timeout in minutes
    private int timeout = -1;

    // 1 Day
    private static final int DEFAULT_TIMEOUT = 1440;

    private static AirSyncFleet[] fleets = null;

    private static final Logger LOG = Logger.getLogger(AirSyncFleet.class.getName());

    private static final Gson gson = WebServer.gson;

    /**
     * Default constructor
     *
     * @param id               the fleet id
     * @param fleetName        the name of this fleet in the NGAFID
     * @param airsyncFleetName the name of the fleet on Airsyncs service
     * @param airSyncAuth      the credentials for this fleet
     * @param lastQueryTime    the last time this fleet was synced with AirSync
     * @param timeout          how long the fleet is set to wait before checking for updates again
     */
    public AirSyncFleet(int id, String fleetName, String airsyncFleetName, AirSyncAuth airSyncAuth,
            LocalDateTime lastQueryTime, int timeout) {
        super(id, fleetName);
        this.authCreds = airSyncAuth;
        this.airsyncFleetName = airsyncFleetName;
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
     * @param resultSet a SQL ResultSet that has information from a query for the
     *                  fleet requested.
     */
    private AirSyncFleet(ResultSet resultSet) throws SQLException {
        super(resultSet.getInt(1), resultSet.getString(2));
        this.airsyncFleetName = resultSet.getString(3);
        this.authCreds = new AirSyncAuth(resultSet.getString(4), resultSet.getString(5));

        Timestamp timestamp = resultSet.getTimestamp(6);
        if (timestamp == null) {
            this.lastQueryTime = LocalDateTime.MIN;
        } else {
            this.lastQueryTime = timestamp.toLocalDateTime();
        }

        int timeout = resultSet.getInt(7);
        if (timeout <= 0) {
            this.timeout = DEFAULT_TIMEOUT;
        } else {
            this.timeout = timeout;
        }
    }

    public boolean getOverride(Connection connection) throws SQLException {
        String query = """
                    SELECT override from airsync_fleet_info WHERE
                        fleet_id = ?
                """;

        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, getId());

            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("override") != 0;
                } else {
                    return false;
                }
            }
        }
    }

    public void setOverride(Connection connection, boolean value) throws SQLException {
        String query = """
                    UPDATE airsync_fleet_info SET override = ? WHERE fleet_id = ?
                """;

        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, value ? 1 : 0);
            statement.setInt(2, getId());

            statement.executeUpdate();
        }
    }

    private LocalDateTime getLastQueryTime(Connection connection) throws SQLException {
        if (this.lastQueryTime == null) {
            String sql = "SELECT last_upload_time FROM airsync_fleet_info WHERE fleet_id = " + super.getId();
            try (PreparedStatement query = connection.prepareStatement(sql);
                    ResultSet resultSet = query.executeQuery()) {

                LocalDateTime lastQueryTime = null;
                if (resultSet.next()) {
                    this.lastQueryTime = resultSet.getTimestamp(1).toLocalDateTime();
                }
            }
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
            String sql = "SELECT timeout FROM airsync_fleet_info WHERE fleet_id = " + super.getId();
            try (PreparedStatement query = connection.prepareStatement(sql);
                    ResultSet resultSet = query.executeQuery()) {
                if (resultSet.next()) {
                    int timeout = resultSet.getInt(1);

                    if (timeout > 0) {
                        this.timeout = timeout;
                    }
                }
            }
        }

        return timeout;
    }

    /**
     * Gets the timeout this fleet chose in a human-readable form
     *
     * @param connection the DBMS connection
     * @param fleetId    the Fleets's id
     *
     * @return the timeout as "X hours" or "XX minutes"
     *
     * @throws SQLException is there is a DBMS error
     */
    public static String getTimeout(Connection connection, int fleetId) throws SQLException {
        String sql = "SELECT timeout FROM airsync_fleet_info WHERE fleet_id = " + fleetId;
        try (PreparedStatement query = connection.prepareStatement(sql); ResultSet resultSet = query.executeQuery()) {
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

            return formattedString;
        }
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
        return (Duration.between(getLastQueryTime(connection), LocalDateTime.now())
                .toMinutes() >= getTimeout(connection));
    }

    /**
     * This gets an existing AirSyncFleet by its fleet id
     *
     * @param connection is the DBMS connection
     * @param fleetId    is the id of the fleet to get
     *
     * @return an instance of an AirSyncFleet with the given id
     *
     * @throws SQLException if the DBMS has an error
     */
    public static AirSyncFleet getAirSyncFleet(Connection connection, int fleetId) throws SQLException {
        String sql = "SELECT fl.id, fl.fleet_name, sync.airsync_fleet_name, sync.api_key, sync.api_secret, sync.last_upload_time, sync.timeout FROM fleet AS fl INNER JOIN airsync_fleet_info AS sync ON sync.fleet_id = fl.id WHERE fl.id = "
                + fleetId;

        try (PreparedStatement query = connection.prepareStatement(sql);
                ResultSet resultSet = query.executeQuery()) {

            if (resultSet.next()) {
                AirSyncFleet fleet = new AirSyncFleet(resultSet);
                return fleet;
            }

            return null;
        }
    }

    /**
     * Updates the fleets timeout for AirSync (how long to wait between checks)
     *
     * @param connection the DBMS connection
     * @param user       the User that is making the request
     *
     * @throws SQLException if the DBMS has an issue
     */
    public void updateTimeout(Connection connection, User user, String newTimeout) throws SQLException {
        // It is assumed that the UI will prevent non-admins from doing this,
        // but just to be safe we will have this check.
        if (user.isAdmin()) {
            String[] timeoutTok = newTimeout.split("\\s");

            int duration = Integer.parseInt(timeoutTok[0]);

            int timeoutMinutes = -1;

            if (timeoutTok[1].equalsIgnoreCase("Hours")) {
                timeoutMinutes = duration * 60;
            } else if (timeoutTok[1].equalsIgnoreCase("Minutes")) {
                timeoutMinutes = duration;
            } else {
                // This should not happen as long as the options for on the UI contain "minutes"
                // or "hours"
                return;
            }

            String sql = "UPDATE airsync_fleet_info SET timeout = ? WHERE fleet_id = ?";
            try (PreparedStatement query = connection.prepareStatement(sql)) {
                query.setInt(1, timeoutMinutes);
                query.setInt(2, super.getId());
                query.executeUpdate();
            }
        } else {
            LOG.severe("Non-admin user attempted to change AirSync settings! This should not happen! Offending user: "
                    + user.getFullName());
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
     * @param time       the time that the fleet was last queried
     * @param connection the DBMS connection
     *
     * @throws SQLException if there is a DBMS issue
     */
    public void setLastQueryTime(LocalDateTime time, Connection connection) throws SQLException {
        String sql = "UPDATE airsync_fleet_info SET last_upload_time = CURRENT_TIMESTAMP WHERE fleet_id = " + getId();

        try (PreparedStatement query = connection.prepareStatement(sql)) {
            query.executeUpdate();
        }

        // Force updating these variables the next time there
        // is a check
        this.timeout = -1;
        this.lastQueryTime = null;
    }

    /**
     * Checks to see if the authentication string os out of date and requests a new
     * one if it is.
     * This should always be used to access the credentials of the fleet for the
     * AirSync server.
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
    public List<AirSyncAircraft> getAircraft() throws IOException {
        if (aircraft == null) {
            HttpsURLConnection connection = (HttpsURLConnection) new URL(AirSyncEndpoints.AIRCRAFT).openConnection();

            connection.setRequestMethod("GET");
            connection.setDoOutput(true);
            connection.setRequestProperty("Authorization", this.authCreds.bearerString());

            byte[] respRaw;
            try (InputStream is = connection.getInputStream()) {
                respRaw = is.readAllBytes();
            }

            String resp = new String(respRaw).replaceAll("tail_number", "tailNumber");

            Type target = new TypeToken<List<AirSyncAircraft>>() {
            }.getType();
            System.out.println(resp);

            List<AirSyncAircraft> aircraft = gson.fromJson(resp, target);
            for (AirSyncAircraft a : aircraft)
                a.initialize(this);

            this.aircraft = aircraft.stream().filter(a -> a.getAirSyncFleetName().equals(airsyncFleetName))
                    .collect(Collectors.toList());
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
    public static AirSyncFleet[] getAll(Connection connection) throws SQLException {
        String sql = "SELECT COUNT(*) FROM airsync_fleet_info";
        int asFleetCount = 0;
        try (PreparedStatement query = connection.prepareStatement(sql);
                ResultSet resultSet = query.executeQuery()) {

            if (resultSet.next()) {
                asFleetCount = resultSet.getInt(1);
            } else {
                return null;
            }
        }

        if (fleets == null || fleets.length != asFleetCount) {
            sql = "SELECT fl.id, fl.fleet_name, sync.airsync_fleet_name, sync.api_key, sync.api_secret, sync.last_upload_time, sync.timeout FROM fleet AS fl INNER JOIN airsync_fleet_info AS sync ON sync.fleet_id = fl.id";
            try (PreparedStatement query = connection.prepareStatement(sql);
                    ResultSet resultSet = query.executeQuery()) {
                fleets = new AirSyncFleet[asFleetCount];
                int i = 0;
                while (resultSet.next()) {
                    fleets[i++] = new AirSyncFleet(resultSet);
                }
            }
        }

        return fleets;
    }

    /**
     * Gets a list of the processed flight ids for this fleet that are from AirSync
     *
     * @param connection the dbms connection
     *
     * @return a {@link List} of {@link Integer} with the ids of the processed
     *         imports
     *
     * @throws SQLException if there is a DBMS issue
     */
    private List<Integer> getProcessedIds(Connection connection) throws SQLException {
        String sql = "SELECT id FROM airsync_imports WHERE fleet_id = " + getId();
        try (PreparedStatement query = connection.prepareStatement(sql); ResultSet resultSet = query.executeQuery()) {
            List<Integer> ids = new LinkedList<>();

            while (resultSet.next()) {
                ids.add(resultSet.getInt(1));
            }

            return ids;
        }
    }

    /**
     * Gets the last update/upload time for an AirSyncFleet
     *
     * @param connection a DBMS connection
     *
     * @return the timestamp as a string form.
     */
    public String getLastUpdateTime(Connection connection) throws SQLException {
        String sql = "SELECT last_upload_time FROM airsync_fleet_info WHERE fleet_id = " + getId();
        try (PreparedStatement query = connection.prepareStatement(sql); ResultSet resultSet = query.executeQuery()) {
            if (resultSet.next()) {
                Timestamp timestamp = resultSet.getTimestamp(1);

                if (timestamp != null) {
                    return timestamp.toString();
                }
            }

            return "N/A";
        }
    }

    class AirSyncFleetUpdater implements AutoCloseable {
        private static final int DOWNLOAD_BATCH_SIZE = 32;
        private static final int ARCHIVE_MAX_SIZE = 128;

        Upload upload = null;
        FileSystem zipFile = null;
        private int filesAdded = 0;

        List<AirSyncAircraft> aircraft;

        AirSyncFleetUpdater() throws IOException {
            aircraft = getAircraft();
        }

        Upload getUpload(Connection connection) throws SQLException {
            if (upload == null) {
                upload = Upload.createAirsyncUpload(connection, getId());
            }

            return upload;
        }

        FileSystem getZipFile() throws IOException {
            if (zipFile == null) {
                zipFile = upload.getZipFileSystem(Map.of("create", "true"));
            }

            return zipFile;
        }

        void addFileToUpload(Connection connection, AirSyncImport imp, byte[] data) throws IOException, SQLException {
            var upload = getUpload(connection);
            imp.setUploadId(upload.id);
            Files.write(getZipFile().getPath("/" + imp.getFilename()), data);
            filesAdded += 1;

            if (filesAdded >= 128) {
                upload.complete(connection);
                upload = Upload.createAirsyncUpload(connection, getId());
                zipFile.close();
                zipFile = null;
                getZipFile();
                filesAdded = 0;
            }
        }

        void run() throws IOException, SQLException {
            for (var ac : aircraft) {
                updateAircraft(ac);
            }

            try (Connection connection = Database.getConnection()) {
                AirSyncFleet.this.setLastQueryTime(connection);
            }
        }

        void updateAircraft(AirSyncAircraft aircraft) throws IOException, SQLException {
            List<AirSyncImport> allImports;
            try (Connection connection = Database.getConnection()) {
                allImports = aircraft.getImportsForUpdate(connection, AirSyncFleet.this);
                allImports.sort(new Comparator<AirSyncImport>() {
                    public int compare(AirSyncImport left, AirSyncImport right) {
                        return left.getUploadTime().compareTo(right.getUploadTime());
                    }
                });

                if (allImports.size() == 0)
                    return;
            }

            for (var chunk : Lists.partition(allImports, 32)) {
                var errors = new ArrayList<IOException>();
                var downloads = chunk.parallelStream()
                        .map(imp -> {
                            try {
                                return imp.download();
                            } catch (IOException e) {
                                synchronized (errors) {
                                    errors.add(e);
                                }
                                return null;
                            }
                        })
                        .collect(Collectors.toList());

                if (errors.size() != 0)
                    throw errors.get(0);

                try (Connection connection = Database.getConnection()) {
                    for (int i = 0; i < chunk.size(); i++) {
                        var imp = chunk.get(i);
                        var data = downloads.get(i);

                        addFileToUpload(connection, imp, data);
                    }

                    AirSyncImport.batchCreateImport(connection, chunk, null);
                }
            }
        }

        public void close() throws IOException, SQLException {
            if (upload != null) {
                try (Connection connection = Database.getConnection()) {
                    upload.complete(connection);
                }
            }
            if (zipFile != null)
                zipFile.close();
        }

    }

    /**
     * Updates this fleet with the AirSync servers
     *
     * @param connection the DBMS connection
     *
     * @return a "status" string that is human readable
     * 
     * @throws SQLException if the DBMS has an issue
     */
    public String update(Connection connection) throws IOException, SQLException {
        try (AirSyncFleetUpdater updater = new AirSyncFleetUpdater()) {
            updater.run();
        }

        return "OK";
    }
}
