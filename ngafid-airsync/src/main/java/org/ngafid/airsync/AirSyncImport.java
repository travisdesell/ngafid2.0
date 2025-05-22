package org.ngafid.airsync;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.ngafid.core.Database;
import org.ngafid.core.flights.Flight;
import org.ngafid.core.uploads.Upload;

import javax.net.ssl.HttpsURLConnection;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * This class represents an Import from the airsync servers in the NGAFID
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public final class AirSyncImport {
    private static final Logger LOG = Logger.getLogger(AirSyncImport.class.getName());
    public static final Gson GSON = new GsonBuilder().serializeSpecialFloatingPointValues().create();

    @JsonCreator
    public static AirSyncImport create(@JsonProperty("id") int id, @JsonProperty("aircraftId") int aircraftId, @JsonProperty("origin") String origin,
                                       @JsonProperty("destination") String destination, @JsonProperty("timeStart") String timeStart, @JsonProperty("timeEnd") String timeEnd,
                                       @JsonProperty("fileUrl") String fileUrl, @JsonProperty("timestampUploaded") String timestampUploaded) {
        AirSyncImport imp = new AirSyncImport();
        imp.id = id;
        imp.aircraftId = aircraftId;
        imp.origin = origin;
        imp.destination = destination;
        imp.timeStart = timeStart;
        imp.timeEnd = timeEnd;
        imp.fileUrl = fileUrl;
        imp.timestampUploaded = timestampUploaded;

        imp.localDateTimeUpload = LocalDateTime.parse(imp.timestampUploaded, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        imp.localDateTimeStart = LocalDateTime.parse(imp.timeStart, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        imp.localDateTimeEnd = LocalDateTime.parse(imp.timeEnd, DateTimeFormatter.ISO_OFFSET_DATE_TIME);

        return imp;
    }

    private AirSyncImport() {
    }

    /*
     * The airsync uploader should be the same across all fleets!
     * There should only be one dummy 'airsync user' in each NGAFID
     * instance's database! See db/update_tables.php for more info.
     */
    private static int AIRSYNC_UPLOADER_ID = -1;

    // This ID is the ID airsync provides, NOT A FLIGHT ID
    private int id;
    private int uploadId;
    private int aircraftId;
    private String origin;
    private String destination;
    private String timeStart;
    private String timeEnd;
    private String fileUrl;
    private String timestampUploaded;
    private LocalDateTime localDateTimeStart;
    private LocalDateTime localDateTimeEnd;
    private LocalDateTime localDateTimeUpload;
    private AirSyncAircraft aircraft;
    private AirSyncFleet fleet;

    /**
     * Gets the uploader id of the AirSync user
     *
     * @return an int that is the id of the AirSync user from the database
     * @throws SQLException if there is a DBMS issue
     */
    public static int getUploaderId() throws SQLException {
        if (AIRSYNC_UPLOADER_ID <= 0) {
            String sql = "SELECT id FROM user WHERE id = -1";
            try (Connection connection = Database.getConnection(); PreparedStatement query =
                    connection.prepareStatement(sql)) {
                ResultSet resultSet = query.executeQuery();

                if (resultSet.next()) {
                    AIRSYNC_UPLOADER_ID = resultSet.getInt(1);
                }
            }
        }

        return AIRSYNC_UPLOADER_ID;
    }

    public static PreparedStatement createPreparedStatement(Connection connection) throws SQLException {
        String sql = "INSERT INTO airsync_imports(id, tail, time_received, upload_id, fleet_id, flight_id) VALUES " +
                "(?, ?, ?, ?, ?, ?)";
        return connection.prepareStatement(sql);
    }

    public static void batchCreateImport(Connection connection,
                                         List<AirSyncImport> imports, Flight flight) throws SQLException {
        try (PreparedStatement query = createPreparedStatement(connection)) {
            for (var imp : imports)
                imp.addBatch(query, flight);

            query.executeUpdate();
        }
    }

    /**
     * Gets a list of the uploads that belong to this fleet
     *
     * @param connection the DBMS connection
     * @param fleetId    the fleet id
     * @return a list of uploads that belong to this fleet
     * @throws SQLException if there is an issue with the DBMS
     */
    public static List<Upload> getUploads(Connection connection, int fleetId) throws SQLException {
        return getUploads(connection, fleetId, "");
    }

    /**
     * Gets uploads with an extra condition
     *
     * @param connection the DBMS connection
     * @param fleetId    the fleet id
     * @param condition  the extra SQL conditions
     * @return a list of uploads that match the condition
     * @throws SQLException if there is an issue with the DBMS
     */
    public static List<Upload> getUploads(Connection connection, int fleetId, String condition) throws SQLException {
        String sql = String.format("SELECT %s FROM uploads WHERE fleet_id = ? AND kind = ? " +
                "ORDER BY start_time DESC", Upload.DEFAULT_COLUMNS);
        if (condition != null && !condition.isBlank()) sql += " " + condition;

        try (PreparedStatement query = connection.prepareStatement(sql)) {
            query.setInt(1, fleetId);
            query.setString(2, Upload.Kind.AIRSYNC.name());

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
     * @return an int with the number of uploads
     * @throws SQLException if there is an issue with the DBMS
     */
    public static int getNumUploads(Connection connection, int fleetId, String condition) throws SQLException {
        String sql = "SELECT COUNT(*) FROM uploads WHERE fleet_id = ? AND uploader_id = ?";
        if (condition != null && !condition.isBlank()) sql += " " + condition;

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
     * @return a list of AirSyncImportResponses
     * @throws SQLException if there is an issue with the DBMS
     */
    public static List<AirSyncImportResponse> getImports(Connection connection,
                                                         int fleetId, String condition) throws SQLException {
        String sql = "SELECT a.id, a.time_received, a.upload_id, u.status, a.flight_id, a.tail FROM airsync_imports " +
                "AS a INNER JOIN uploads AS u ON u.id = a.upload_id WHERE u.status LIKE 'PROCESSED%' ORDER BY a.time_received";
        if (condition != null && !condition.isBlank()) sql += " " + condition;

        try (PreparedStatement query = connection.prepareStatement(sql)) {
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
     * @return an int with the number of imports
     * @throws SQLException if there is an issue with the DBMS
     */
    public static int getNumImports(Connection connection, int fleetId, String condition) throws SQLException {
        String sql = "SELECT COUNT(*) FROM airsync_imports " +
                "AS a INNER JOIN uploads AS u ON u.id = a.upload_id WHERE u.status LIKE 'PROCESSED%'";
        if (condition != null && !condition.isBlank()) sql += " " + condition;

        try (PreparedStatement query = connection.prepareStatement(sql)) {
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
     * This will act as the constructor as we will be parsing the object
     * from JSON most of the time.
     * <p>
     * It is up to the programmer to ensure this method is called each time a JSON
     * AirSyncImport class is instantiated.
     *
     * @param initFleet    a reference to the fleet that this upload is for
     * @param initAircraft a reference to the aircraft this import is from
     *                     {@link AirSyncAircraft}
     */
    public void init(AirSyncFleet initFleet, AirSyncAircraft initAircraft) {
        this.fleet = initFleet;
        this.aircraft = initAircraft;

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
     * Accessor method for the import id
     *
     * @return an int with the imports database id
     */
    public int getId() {
        return this.id;
    }

    public String getFilename() {
        return String.format("%d_%d_%d_%d_%d_%d.csv", this.aircraftId, this.localDateTimeStart.getYear(),
                this.localDateTimeStart.getMonthValue(), this.localDateTimeStart.getDayOfMonth(),
                this.localDateTimeStart.getHour(), this.localDateTimeStart.getMinute());
    }

    /**
     * Contains the logic for processing the import
     *
     * @return a {@link Flight} object that was created from this import
     */
    public byte[] download() throws IOException {
        HttpsURLConnection connection = (HttpsURLConnection) new URL(String.format(AirSyncEndpoints.SINGLE_LOG,
                this.id)).openConnection();

        connection.setRequestMethod("GET");
        connection.setDoOutput(true);
        connection.setRequestProperty("Authorization", this.fleet.getAuth().bearerString());

        try (InputStream is = connection.getInputStream()) {
            byte[] respRaw = is.readAllBytes();

            String resp = new String(respRaw).replaceAll("file_url", "fileUrl");

            LogResponse log = GSON.fromJson(resp, LogResponse.class);
            URL input = new URL(log.getFileUrl());
            LOG.info("Got URL for logfile " + log.getFileUrl());

            try (InputStream dataStream = input.openStream()) {
                return dataStream.readAllBytes();
            }
        }
    }

    public boolean exists(Connection connection) throws SQLException {
        try (PreparedStatement query =
                     connection.prepareStatement("SELECT 1 FROM airsync_imports WHERE id = " + id + " LIMIT 1");
             ResultSet resultSet = query.executeQuery()) {
            return resultSet.next();
        }
    }

    /**
     * Creates a record of this import in the database
     *
     * @param connection the DBMS connection
     * @param flight     the {@link Flight} that came from this import
     * @throws SQLException if there is an issue with the DBMS
     */
    public void createImport(Connection connection, Flight flight) throws SQLException {
        try (PreparedStatement query = createPreparedStatement(connection)) {
            this.addBatch(query, flight);
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
     * {@inheritDoc}
     */
    public String toString() {
        return "AirSyncImport: " + this.uploadId + ", for aircraftId: " + aircraftId + ", origin: " + origin + ", " +
                "destination: " + destination + ",\n" + "url: " + fileUrl + ", start time: " + timeStart + ", end " +
                "time: " + timeEnd + ";";
    }

    /**
     * This is a static class to represent the response we get from the AirSync
     * servers
     * <p>
     * id      the logfiles id
     * fileUrl the accessible web URL where this import resides (csv data)
     */
    private static class LogResponse {
        private int id;
        private String fileUrl;

        public int getId() {
            return id;
        }

        public String getFileUrl() {
            return fileUrl;
        }
    }
}
