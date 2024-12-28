package org.ngafid.flights;

import java.io.*;
import java.net.*;
import java.sql.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.logging.Logger;

import javax.net.ssl.HttpsURLConnection;

import org.ngafid.Database;
import org.ngafid.WebServer;
import org.ngafid.accounts.AirSyncAircraft;
import org.ngafid.accounts.AirSyncFleet;

/**
 * This class represents an Import from the airsync servers in the NGAFID
 */
public class AirSyncImport {
    private int id, uploadId, aircraftId;
    private byte[] data;
    private String origin, destination, timeStart, timeEnd, md5Hash, fileUrl, timestampUploaded;
    private LocalDateTime localDateTimeStart, localDateTimeEnd, localDateTimeUpload;
    private AirSyncAircraft aircraft;
    private AirSyncFleet fleet;

    private static final String STATUS_UPLOADING = "UPLOADING";
    private static final String STATUS_UPLOADED = "UPLOADED";
    private static final String STATUS_IMPORTED = "IMPORTED";
    private static final String STATUS_ERR = "ERROR";
    private static final String STATUS_WARN = "WARNING";

    /*
     * The airsync uploader should be the same across all fleets!
     * There should only be one dummy 'airsync user' in each NGAFID
     * instance's database! See db/update_tables.php for more info.
     */
    private static int AIRSYNC_UPLOADER_ID = -1;

    private static final Logger LOG = Logger.getLogger(AirSyncImport.class.getName());

    /**
     * This is a static class to represent the response we get from the AirSync
     * servers
     *
     * @param id      the logfiles id
     * @param fileUrl the accessible web URL where this import resides (csv data)
     */
    private static class LogResponse {
        int id;
        String fileUrl;
    }

    /**
     * Private contructor for instaniation within this class
     *
     * @param id       the id of this import in the database
     * @param uploadId the id of the upload this import belongs to in the database
     * @param fleet    the fleet that this import belongs to
     */
    private AirSyncImport(int id, int uploadId, AirSyncFleet fleet) {
        this.uploadId = uploadId;
        this.id = id;
        this.fleet = fleet;
    }

    /**
     * This will act as the constructor as we will be parsing the object
     * from JSON most of the time.
     *
     * It is up to the programmer to ensure this method is called each time a JSON
     * AirSyncImport class is instantiated.
     *
     * @param {fleet}    a reference to the fleet that this upload is for
     * @param {aircraft} a reference to the aircraft this import is from
     *                   {@link AirSyncAircraft}
     */
    public void init(AirSyncFleet fleet, AirSyncAircraft aircraft) {
        this.fleet = fleet;
        this.aircraft = aircraft;

        // This does not include timezones yet
        // TODO: Add time zone support!
        this.localDateTimeUpload = LocalDateTime.parse(this.timestampUploaded.split("\\+")[0],
                DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        this.localDateTimeStart = LocalDateTime.parse(this.timeStart.split("\\+")[0],
                DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        this.localDateTimeEnd = LocalDateTime.parse(this.timeEnd.split("\\+")[0],
                DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }

    public LocalDateTime getUploadTime() {
        return localDateTimeUpload;
    }

    public void setUploadId(int uploadId) {
        this.uploadId = uploadId;
    }

    /**
     * Gets the uploader id of the AirSync user
     *
     * @return an int that is the id of the AirSync user from the database
     *
     * @throws SQLException if there is a DBMS issue
     */
    public static int getUploaderId() throws SQLException {
        if (AIRSYNC_UPLOADER_ID <= 0) {
            String sql = "SELECT id FROM user WHERE id = -1";
            try (Connection connection = Database.getConnection();
                    PreparedStatement query = connection.prepareStatement(sql)) {
                ResultSet resultSet = query.executeQuery();

                if (resultSet.next()) {
                    AIRSYNC_UPLOADER_ID = resultSet.getInt(1);
                }
            }
        }

        return AIRSYNC_UPLOADER_ID;
    }

    /**
     * Accessor method for the import id
     *
     * @return an int with the imports database id
     */
    public int getId() {
        return this.id;
    }

    /**
     * Formats a string that will server as an identified for the uploads table
     *
     * @param fleetId    the id of the fleet that this import belongs to
     * @param aircraftId the id of the aircraft this import is coming from
     * @param time       the time of this import
     *
     * @return a formatted string that will serve as a unique indetifier in the
     *         database (uploads table)
     */
    public static String getUploadIdentifier(int fleetId, int aircraftId, LocalDateTime time) {
        return "AS-" + fleetId + "." + aircraftId + "-" + time.getYear() + "-" + time.getMonthValue();
    }

    public String getFilename() {
        return String.format("%d_%d_%d_%d_%d_%d.csv",
                this.aircraftId,
                this.localDateTimeStart.getYear(),
                this.localDateTimeStart.getMonthValue(),
                this.localDateTimeStart.getDayOfMonth(),
                this.localDateTimeStart.getHour(),
                this.localDateTimeStart.getMinute());
    }

    /**
     * Contains the logic for processing the import
     *
     * @throws MalformedFlightFileException if we get a bad file from AirSync
     */
    public byte[] download() throws IOException {
        HttpsURLConnection connection = (HttpsURLConnection) new URL(
                String.format(AirSyncEndpoints.SINGLE_LOG, this.id)).openConnection();

        connection.setRequestMethod("GET");
        connection.setDoOutput(true);
        connection.setRequestProperty("Authorization", this.fleet.getAuth().bearerString());

        try (InputStream is = connection.getInputStream()) {
            byte[] respRaw = is.readAllBytes();

            String resp = new String(respRaw).replaceAll("file_url", "fileUrl");

            LogResponse log = WebServer.gson.fromJson(resp, LogResponse.class);
            URL input = new URL(log.fileUrl);
            LOG.info("Got URL for logfile " + log.fileUrl);

            try (InputStream dataStream = input.openStream()) {
                return dataStream.readAllBytes();
            }
        }
    }

    public boolean exists(Connection connection) throws SQLException {
        try (PreparedStatement query = connection
                .prepareStatement("SELECT 1 FROM airsync_imports WHERE id = " + id + " LIMIT 1");
                ResultSet resultSet = query.executeQuery()) {
            return resultSet.next();
        }
    }

    /**
     * Creates a record of this import in the database
     * 
     * @param connection the DBMS connection
     * @param flight     the {@link Flight} that came from this import
     *
     * @throws SQLException if there is an issue with the DBMS
     */
    public void createImport(Connection connection, Flight flight) throws SQLException {
        try (PreparedStatement query = createPreparedStatement(connection)) {
            this.addBatch(query, flight);
            query.executeUpdate();
        }
    }

    public static PreparedStatement createPreparedStatement(Connection connection) throws SQLException {
        String sql = "INSERT INTO airsync_imports(id, tail, time_received, upload_id, fleet_id, flight_id) VALUES (?, ?, ?, ?, ?, ?)";
        return connection.prepareStatement(sql);
    }

    public static void batchCreateImport(Connection connection, List<AirSyncImport> imports, Flight flight)
            throws SQLException {
        try (PreparedStatement query = createPreparedStatement(connection)) {
            for (var imp : imports)
                imp.addBatch(query, flight);

            query.executeUpdate();
        }
    }

    public void addBatch(PreparedStatement query, Flight flight) throws SQLException {
        query.setInt(1, this.id);
        query.setString(2, this.aircraft.getTailNumber());

        // NOTE: this is the time that we recieve the CSV, not the time
        // that AirSync recieves it.
        query.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now()));
        query.setInt(4, this.uploadId);
        query.setInt(5, this.fleet.getId());

        if (flight != null) {
            query.setInt(6, flight.getId());
        } else {
            query.setNull(6, java.sql.Types.INTEGER);
        }

        query.addBatch();
    }

    /**
     * Gets a list of the uploads that belong to this fleet
     *
     * @param connection the DBMS connection
     * @param fleetId    the fleet id
     *
     * @throws SQLException if there is an issue with the DBMS
     */
    public static List<Upload> getUploads(Connection connection, int fleetId) throws SQLException {
        return getUploads(connection, fleetId, new String());
    }

    /**
     * Gets uploads with an extra condition
     *
     * @param connection the DBMS connection
     * @param fleetId    the fleet id
     * @param condition  the extra SQL conditions
     *
     * @throws SQLException if there is an issue with the DBMS
     */
    public static List<Upload> getUploads(Connection connection, int fleetId, String condition) throws SQLException {
        String sql = String.format(
                "SELECT %s FROM uploads WHERE fleet_id = ? AND uploader_id = ? ORDER BY start_time DESC",
                Upload.DEFAULT_COLUMNS);
        if (condition != null && !condition.isBlank())
            sql += " " + condition;

        try (PreparedStatement query = connection.prepareStatement(sql)) {
            query.setInt(1, fleetId);
            query.setInt(2, -1);

            try (ResultSet resultSet = query.executeQuery()) {
                List<Upload> uploads = new ArrayList<>();

                while (resultSet.next()) {
                    Upload u = new Upload(resultSet);
                    u.getAirSyncInfo(connection);

                    uploads.add(u);
                }

                return uploads;
            }

        }
    }

    /**
     * Gets the COUNT of uploads with a condition
     *
     * @param connection the DBMS connection
     * @param fleetId    the fleet id
     * @param condition  the extra SQL conditions
     *
     * @throws SQLException if there is an issue with the DBMS
     */
    public static int getNumUploads(Connection connection, int fleetId, String condition) throws SQLException {
        String sql = "SELECT COUNT(*) FROM uploads WHERE fleet_id = ? AND uploader_id = ?";
        if (condition != null && !condition.isBlank())
            sql += " " + condition;

        try (PreparedStatement query = connection.prepareStatement(sql)) {
            query.setInt(1, fleetId);
            query.setInt(2, getUploaderId());

            try (ResultSet resultSet = query.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt(1);
                } else {
                    return -1;
                }
            }

        }
    }

    /**
     * Gets a list of AirSyncImportResponses that can be used to be populated on a
     * webpage.
     *
     * @param connection the DBMS connection
     * @param fleetId    the fleet id
     * @param condition  the extra SQL conditions
     *
     * @throws SQLException if there is an issue with the DBMS
     */
    public static List<AirSyncImportResponse> getImports(Connection connection, int fleetId, String condition)
            throws SQLException {
        String sql = "SELECT a.id, a.time_received, a.upload_id, f.status, a.flight_id, a.tail FROM airsync_imports AS a INNER JOIN flights AS f ON f.id = a.flight_id WHERE a.fleet_id = ? ORDER BY a.time_received";
        if (condition != null && !condition.isBlank())
            sql += " " + condition;

        try (PreparedStatement query = connection.prepareStatement(sql)) {
            query.setInt(1, fleetId);

            try (ResultSet resultSet = query.executeQuery()) {
                List<AirSyncImportResponse> imports = new ArrayList<>();

                while (resultSet.next()) {
                    imports.add(new AirSyncImportResponse(fleetId, resultSet));
                }

                return imports;
            }
        }
    }

    /**
     * Gets the COUNT of imports with an extra condition
     *
     * @param connection the DBMS connection
     * @param fleetId    the fleet id
     * @param condition  the extra SQL conditions
     *
     * @throws SQLException if there is an issue with the DBMS
     */
    public static int getNumImports(Connection connection, int fleetId, String condition) throws SQLException {
        String sql = "SELECT COUNT(*) FROM airsync_imports WHERE fleet_id = ?";
        if (condition != null && !condition.isBlank())
            sql += " " + condition;

        try (PreparedStatement query = connection.prepareStatement(sql)) {
            query.setInt(1, fleetId);

            try (ResultSet resultSet = query.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt(1);
                } else {
                    return -1;
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public String toString() {
        return "AirSyncImport: " + this.uploadId + ", for aircraftId: " + aircraftId + ", origin: " + origin
                + ", destination: " + destination + ",\n" +
                "url: " + fileUrl + ", start time: " + timeStart + ", end time: " + timeEnd + ";";
    }
}
