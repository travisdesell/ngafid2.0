package org.ngafid.uploads;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.ngafid.bin.WebServer;
import org.ngafid.common.MD5;
import org.ngafid.flights.Flight;
import org.ngafid.kafka.Configuration;
import org.ngafid.kafka.Topic;
import org.ngafid.uploads.airsync.AirSyncImport;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.*;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public final class Upload {
    private static final Logger LOG = Logger.getLogger(Upload.class.getName());

    public static final String DEFAULT_COLUMNS =
            "id, parent_id, fleet_id, uploader_id, filename, " +
                    "identifier, kind, number_chunks, uploaded_chunks, " +
                    "chunk_status, md5_hash, size_bytes, bytes_uploaded, " +
                    "status, start_time, end_time, n_valid_flights, " +
                    "n_warning_flights, n_error_flights ";

    public enum Kind {
        FILE,
        AIRSYNC,
        DERIVED
    }

    public enum Status {
        UPLOADING,
        UPLOADING_FAILED,
        UPLOADED,
        ENQUEUED,
        PROCESSING,
        PROCESSED_OK,
        PROCESSED_WARNING,
        FAILED_FILE_TYPE,
        FAILED_AIRCRAFT_TYPE,
        FAILED_ARCHIVE_TYPE,
        FAILED_UNKNOWN,
        DERIVED;

        public static Status[] IMPORTED_SET = new Status[]{
                Status.PROCESSED_OK,
                Status.PROCESSED_WARNING,
                Status.FAILED_UNKNOWN,
                Status.FAILED_AIRCRAFT_TYPE,
                Status.FAILED_ARCHIVE_TYPE,
                Status.FAILED_FILE_TYPE,
        };

        public static Status[] NOT_IMPORTED_SET = new Status[]{
                Status.UPLOADING,
                Status.UPLOADED,
                Status.UPLOADING_FAILED,
                Status.ENQUEUED,
        };

        public boolean isProcessed() {
            return this == PROCESSED_OK || this == PROCESSED_WARNING;
        }
    }

    //CHECKSTYLE:OFF
    public int id;
    public Integer parentId;
    public int fleetId;
    public int uploaderId;
    public String filename;
    public String identifier;
    public Kind kind;
    public int numberChunks;
    public int uploadedChunks;
    public String chunkStatus;
    public String md5Hash;
    public long sizeBytes;
    public long bytesUploaded;
    public Status status;
    public String startTime;
    public String endTime;
    public int validFlights;
    public int warningFlights;
    public int errorFlights;

    // For AirSync uploads that are grouped by month.
    String groupString = null;
    String tail = null;
    //CHECKSTYLE:ON

    public Upload(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public Integer getParentId() {
        return parentId;
    }

    public Kind getKind() {
        return kind;
    }

    public String getStartTime() {
        return startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    /**
     * This is used for getting uploads for the GetUpload route. If an upload was
     * incomplete it will
     * be stored as "UPLOADING" in the database, but this needs to be updated to
     * "UPLOAD INCOMPLETE" for
     * the webpage to make it work correctly to distinguish between current uploads
     * and those that need
     * to be restarted.
     *
     * @param newStatus the new status for this upload
     */
    public void setStatus(Status newStatus) {
        status = newStatus;
    }

    /**
     * @return the status of this upload
     */
    public Status getStatus() {
        return status;
    }

    private static String lockNameFor(int uploadId) {
        return "upload_" + uploadId;
    }

    /**
     * Releases or acquires the mysql lock corresponding to this upload, based on parameter acquire.
     * <p>
     * >>>> THE FOLLOWING IS ABSOLUTELY CRITICAL TO NOT BREAKING THE LOCK FUNCTIONALITY:
     * MySQL locks are automatically released when a session is terminated, where a session is effectively a single connection.
     * This means that the same connection MUST be used to acquire and release the connection.
     *
     * @param connection the connection used for the operation -- this must be the same connection used for the inverse operation.
     * @param acquire    whether to acquire the lock (true) or release the lock (false)
     * @return a boolean representing whether the lock was successfully released or acquired.
     * @throws SQLException
     */
    public boolean lock(Connection connection, boolean acquire) throws SQLException {
        String function = acquire ? "GET_LOCK(?, 1)" : "RELEASE_LOCK(?)";
        try (PreparedStatement statement = connection.prepareStatement("SELECT " + function)) {
            statement.setString(1, lockNameFor(id));
            LOG.info(statement.toString());
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt(1) == 1;
                } else {
                    return false;
                }
            }
        }
    }

    private static void deleteDirectory(File folder) {
        File[] files = folder.listFiles();
        if (files != null) { // some JVMs return null for empty dirs
            for (File f : files) {
                if (f.isDirectory()) {
                    deleteDirectory(f);
                } else {
                    f.delete();
                }
            }
        }
        folder.delete();
    }

    /**
     * Removes all other entries in the database related to this upload so
     * it can be reprocessed or deleted
     *
     * @param connection is the database connection
     * @param uploadId   is the id of the upload to delete (in case it does not
     *                   exist for some reason)
     * @throws SQLException if there is an error in the database
     */
    public static void clearUpload(Connection connection, int uploadId) throws SQLException {
        String query = "DELETE FROM upload_errors WHERE upload_id = ?";
        try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            preparedStatement.setInt(1, uploadId);
            LOG.info(preparedStatement.toString());
            preparedStatement.executeUpdate();
        }

        query = "DELETE FROM flight_errors WHERE upload_id = ?";
        try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            preparedStatement.setInt(1, uploadId);
            LOG.info(preparedStatement.toString());
            preparedStatement.executeUpdate();
        }

        ArrayList<Flight> flights = Flight.getFlightsFromUpload(connection, uploadId);

        for (Flight flight : flights) {
            flight.remove(connection);
        }
    }

    /**
     * Removes all other entries in the database related to this upload so
     * it can be reprocessed or deleted
     *
     * @param connection is the database connection
     * @throws SQLException if there is an error in the database
     */
    public void clearUpload(Connection connection) throws SQLException {
        String query = "DELETE FROM upload_errors WHERE upload_id = ?";
        try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            preparedStatement.setInt(1, this.id);
            LOG.info(preparedStatement.toString());
            preparedStatement.executeUpdate();
        }

        query = "DELETE FROM flight_errors WHERE upload_id = ?";
        try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            preparedStatement.setInt(1, this.id);
            LOG.info(preparedStatement.toString());
            preparedStatement.executeUpdate();
        }

        ArrayList<Flight> flights = Flight.getFlightsFromUpload(connection, this.id);

        for (Flight flight : flights) {
            flight.remove(connection);
        }

        if (!md5Hash.contains("DERIVED")) {
            Upload derived = getUploadByUser(connection, uploaderId, getDerivedMd5(md5Hash));
            if (derived != null) {
                derived.remove(connection);
            }
        }

        query = "DELETE FROM uploads WHERE md5_hash = ?";
        try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            preparedStatement.setString(1, getDerivedMd5(md5Hash));
            preparedStatement.executeUpdate();
        }
    }

    static String getDerivedMd5(String md5) {
        return MD5.computeHexHash(md5 + "DERIVED");
    }

    /**
     * Removes all other entries in the database related to this upload
     * and sets its status to 'ENQUEUED' so it can be reprocessed
     *
     * @param connection is the database connection
     * @throws SQLException if there is an error in the database
     */
    public void reset(Connection connection) throws SQLException {
        this.clearUpload(connection);

        if (status != Status.ENQUEUED) {
            String query = "UPDATE uploads SET status = '" + Status.UPLOADED + "' WHERE id = ?";
            try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
                preparedStatement.setInt(1, this.id);
                LOG.info(preparedStatement.toString());
                preparedStatement.executeUpdate();
            }
        }
    }

    /**
     * Removes all other entries in the database related to this upload
     * and then removes the upload from the database, and removes the
     * uploaded files related to it from disk
     *
     * @param connection is the database connection
     * @throws SQLException if there is an error in the database
     */
    public void remove(Connection connection) throws SQLException {
        this.clearUpload(connection);

        if (kind == Kind.AIRSYNC) {
            try (PreparedStatement preparedStatement = connection
                    .prepareStatement("DELETE FROM airsync_imports WHERE upload_id = " + this.id)) {
                preparedStatement.executeUpdate();
            }
        }

        try (PreparedStatement preparedStatement = connection.prepareStatement("DELETE FROM uploads WHERE id = ?")) {
            preparedStatement.setInt(1, this.id);
            LOG.info(preparedStatement.toString());
            preparedStatement.executeUpdate();
        }

        File archiveFile = new File(this.getArchivePath().toUri());
        archiveFile.delete();
    }

    public static Upload getUploadByUser(Connection connection, int uploaderId, String md5Hash) throws SQLException {
        try (PreparedStatement uploadQuery = connection.prepareStatement(
                "SELECT " + DEFAULT_COLUMNS + " FROM uploads WHERE uploader_id = ? AND md5_hash = ?")) {
            uploadQuery.setInt(1, uploaderId);
            uploadQuery.setString(2, md5Hash);

            try (ResultSet resultSet = uploadQuery.executeQuery()) {
                if (resultSet.next()) {
                    return new Upload(resultSet);
                } else {
                    return null;
                }
            }
        }
    }

    /*
     * This gets an upload only by its id. Used by {@link CSVWriter} to get
     * archival files usually.
     *
     * @param connection a connection to the database
     * @param uploadId the id of the upload entry in the database
     *
     * @throws SQLException on a database error
     */
    public static Upload getUploadById(Connection connection, int uploadId) throws SQLException {
        try (PreparedStatement uploadQuery = connection.prepareStatement(
                "SELECT " + DEFAULT_COLUMNS + " FROM uploads WHERE id = ?")) {
            uploadQuery.setInt(1, uploadId);
            try (ResultSet resultSet = uploadQuery.executeQuery()) {
                if (resultSet.next()) {
                    return new Upload(resultSet);
                } else {
                    return null;
                }
            }
        }
    }

    /**
     * Create a new user upload object in the database and return it as a Java
     * object.
     *
     * @param connection   connection to the database
     * @param uploaderId   the id of the user who uploaded the file
     * @param fleetId      the id of the fleet the file belongs to
     * @param filename     the name of the file
     * @param identifier   the identifier of the file
     * @param kind         the kind of the file
     * @param size         the size of the file
     * @param numberChunks the number of chunks the file is split into
     * @param md5hash      the md5 hash of the file
     * @return the upload object
     * @throws SQLException on a database error
     */
    public static Upload createNewUpload(Connection connection, int uploaderId, int fleetId, String filename,
                                         String identifier, Kind kind, long size, int numberChunks, String md5hash) throws SQLException {
        return createUpload(connection, uploaderId, fleetId, null, filename, identifier, kind, size, numberChunks,
                md5hash, 0, "0".repeat(numberChunks), Status.UPLOADING);
    }

    /**
     * Creates a new 'derived' upload - an upload that a user did not upload, and
     * only contains data stemming from transformed versions of the files in a user
     * upload.
     *
     * @param connection connection to the database
     * @param parent     the parent upload that this derived upload is based on
     * @return The upload object
     * @throws SQLException
     */
    public static Upload createDerivedUpload(Connection connection, Upload parent) throws SQLException {
        // Need to create a fake md5 hash for the derived upload.
        return createUpload(connection, parent.uploaderId, parent.fleetId, parent.id, parent.filename,
                parent.identifier + "-DERIVED", Kind.DERIVED, 0, 0, getDerivedMd5(parent.md5Hash), 0, "", Status.DERIVED);
    }

    public static Upload createAirsyncUpload(Connection connection, int fleetId) throws SQLException {
        Timestamp ts = Timestamp.valueOf(LocalDateTime.now());
        return createUpload(connection, 1, fleetId, null, "airsync.zip", "airsync.zip", Kind.AIRSYNC, 0, 1,
                ts.toString(), 1, "1", Status.UPLOADING);
    }

    /**
     * Create a new user upload object in the database and return it as a Java
     *
     * @param connection     connection to the database
     * @param uploaderId     the id of the user who uploaded the file
     * @param fleetId        the id of the fleet the file belongs to
     * @param parentId       the id of the parent upload
     * @param filename       the name of the file
     * @param identifier     the identifier of the file
     * @param kind           the kind of the file
     * @param size           the size of the file
     * @param numberChunks   the number of chunks the file is split into
     * @param md5hash        the md5 hash of the file
     * @param uploadedChunks the number of chunks that have been uploaded
     * @param chunkStatus    the status of each chunk
     * @param status         the status of the upload
     * @return the upload object
     * @throws SQLException on a database error
     */
    // Disable checkstyle for 13 parameters > 10 limit
    //CHECKSTYLE:OFF
    public static Upload createUpload(Connection connection, int uploaderId, int fleetId, Integer parentId,
                                      String filename, String identifier, Kind kind, long size, int numberChunks, String md5hash,
                                      int uploadedChunks, String chunkStatus, Status status) throws SQLException {
        //CHECKSTYLE:OFF
        try (PreparedStatement query = connection.prepareStatement(
                "INSERT INTO uploads SET uploader_id = ?, fleet_id = ?, parent_id = ?, filename = ?, " +
                        "identifier = ?, kind = ?, size_bytes = ?, number_chunks = ?, md5_hash=?, " +
                        "uploaded_chunks = ?, chunk_status = ?, status = ?, start_time = now()")) {
            query.setInt(1, uploaderId);
            query.setInt(2, fleetId);

            if (parentId != null)
                query.setInt(3, parentId);
            else
                query.setNull(3, java.sql.Types.INTEGER);

            query.setString(4, filename);
            query.setString(5, identifier);
            query.setString(6, kind.name());
            query.setLong(7, size);
            query.setInt(8, numberChunks);
            query.setString(9, md5hash);
            query.setInt(10, uploadedChunks);
            query.setString(11, chunkStatus);
            query.setString(12, status.name());

            LOG.info("QUERY: " + query.toString());
            query.executeUpdate();

            return Upload.getUploadByUser(connection, uploaderId, md5hash);
        }
    }

    public static Upload getUploadById(Connection connection, int uploadId, String md5Hash) throws SQLException {
        try (PreparedStatement uploadQuery = connection
                .prepareStatement("SELECT " + DEFAULT_COLUMNS + " FROM uploads WHERE id = ? AND md5_hash = ?")) {
            uploadQuery.setInt(1, uploadId);
            uploadQuery.setString(2, md5Hash);
            try (ResultSet resultSet = uploadQuery.executeQuery()) {
                if (resultSet.next()) {
                    Upload upload = new Upload(resultSet);
                    return upload;
                } else {
                    //CHECKSTYLE:OFF
                    // TODO: maybe need to throw an exception
                    //CHECKSTYLE:ON
                    return null;
                }
            }
        }
    }

    public static List<Upload> getUploads(Connection connection, int fleetId) throws SQLException {
        return getUploads(connection, fleetId, "");
    }

    public static List<Upload> getUploads(Connection connection, int fleetId, String condition) throws SQLException {
        // PreparedStatement uploadQuery = connection.prepareStatement("SELECT id,
        // fleetId, uploaderId, filename, identifier, numberChunks, chunkStatus,
        // md5Hash, sizeBytes, bytesUploaded, status, startTime, endTime, validFlights,
        // warningFlights, errorFlights FROM uploads WHERE fleetId = ?");
        String query = "SELECT " + DEFAULT_COLUMNS
                + " FROM uploads WHERE fleet_id = ? AND uploader_id != ? ORDER BY start_time DESC ";
        if (condition != null) {
            query += " " + condition;
        }

        PreparedStatement uploadQuery = connection.prepareStatement(query);
        uploadQuery.setInt(1, fleetId);
        uploadQuery.setInt(2, AirSyncImport.getUploaderId());

        ResultSet resultSet = uploadQuery.executeQuery();

        ArrayList<Upload> uploads = new ArrayList<Upload>();

        while (resultSet.next()) {
            uploads.add(new Upload(resultSet));
        }

        // resultSet.close();
        uploadQuery.close();

        return uploads;
    }

    public static int getNumUploads(Connection connection, int fleetId, String condition) throws SQLException {
        String query = "SELECT count(id) FROM uploads WHERE uploader_id != ?";
        if (fleetId > 0)
            query += " AND fleet_id = " + fleetId;

        if (condition != null)
            query += " " + condition;

        try (PreparedStatement uploadQuery = connection.prepareStatement(query)) {
            uploadQuery.setInt(1, AirSyncImport.getUploaderId());

            try (ResultSet resultSet = uploadQuery.executeQuery()) {
                resultSet.next();
                int count = resultSet.getInt(1);

                return count;
            }
        }
    }

    public static List<Upload> getUploads(Connection connection, int fleetId, Upload.Status[] types) throws SQLException {
        // String query = "SELECT id, fleetId, uploaderId, filename, identifier,
        // numberChunks, chunkStatus, md5Hash, sizeBytes, bytesUploaded, status,
        // startTime, endTime, validFlights, warningFlights, errorFlights FROM uploads
        // WHERE fleetId = ?";
        String query = "SELECT " + DEFAULT_COLUMNS + " FROM uploads WHERE fleet_id = ? AND uploader_id != ?";

        if (types.length > 0) {
            query += " AND (";

            for (int i = 0; i < types.length; i++) {
                if (i > 0)
                    query += " OR ";
                query += "status = ?";
            }
            query += ")";
        }
        query += " ORDER BY start_time DESC";

        PreparedStatement uploadQuery = connection.prepareStatement(query);

        uploadQuery.setInt(1, fleetId);
        uploadQuery.setInt(2, AirSyncImport.getUploaderId());

        for (int i = 0; i < types.length; i++) {
            uploadQuery.setString(i + 3, types[i].toString());
        }

        ResultSet resultSet = uploadQuery.executeQuery();

        ArrayList<Upload> uploads = new ArrayList<Upload>();

        while (resultSet.next()) {
            uploads.add(new Upload(resultSet));
        }

        resultSet.close();
        uploadQuery.close();

        return uploads;
    }

    public static List<Upload> getUploads(Connection connection, int fleetId, Upload.Status[] types, String sqlLimit)
            throws SQLException {
        String query = "SELECT " + DEFAULT_COLUMNS + " FROM uploads WHERE fleet_id = ? AND uploader_id != ?";

        if (types.length > 0) {
            query += " AND (";

            for (int i = 0; i < types.length; i++) {
                if (i > 0)
                    query += " OR ";
                query += "status = ?";
            }
            query += ")";
        }
        query += " ORDER BY start_time DESC";

        if (!sqlLimit.isEmpty())
            query += sqlLimit;

        PreparedStatement uploadQuery = connection.prepareStatement(query);
        uploadQuery.setInt(1, fleetId);
        uploadQuery.setInt(2, AirSyncImport.getUploaderId());

        for (int i = 0; i < types.length; i++) {
            uploadQuery.setString(i + 3, types[i].toString());
        }
        ResultSet resultSet = uploadQuery.executeQuery();

        ArrayList<Upload> uploads = new ArrayList<Upload>();

        while (resultSet.next()) {
            uploads.add(new Upload(resultSet));
        }

        resultSet.close();
        uploadQuery.close();

        return uploads;

    }

    public Upload(ResultSet resultSet) throws SQLException {
        id = resultSet.getInt(1);

        parentId = resultSet.getInt(2);
        if (resultSet.wasNull())
            parentId = null;

        fleetId = resultSet.getInt(3);
        uploaderId = resultSet.getInt(4);
        filename = resultSet.getString(5);
        identifier = resultSet.getString(6);
        kind = Kind.valueOf(resultSet.getString(7));
        numberChunks = resultSet.getInt(8);
        uploadedChunks = resultSet.getInt(9);
        chunkStatus = resultSet.getString(10);
        md5Hash = resultSet.getString(11);
        sizeBytes = resultSet.getLong(12);
        bytesUploaded = resultSet.getLong(13);
        status = Status.valueOf(resultSet.getString(14));
        startTime = resultSet.getString(15);
        endTime = resultSet.getString(16);
        validFlights = resultSet.getInt(17);
        warningFlights = resultSet.getInt(18);
        errorFlights = resultSet.getInt(19);
    }

    /**
     * Returns the folder which will contain the zip file corresponding to this upload.
     *
     * @return the folder which will contain the zip file corresponding to this upload
     */
    public String getArchiveDirectory() {
        switch (kind) {
            case AIRSYNC:
                return WebServer.NGAFID_ARCHIVE_DIR + "/" + fleetId + "/airsync/" + uploaderId;
            case DERIVED:
                return WebServer.NGAFID_ARCHIVE_DIR + "/" + fleetId + "/" + uploaderId + "/derived";
            case FILE:
                return WebServer.NGAFID_ARCHIVE_DIR + "/" + fleetId + "/" + uploaderId;
            default:
                return ""; // unreachable
        }
    }

    public String getArchiveFilename() {
        switch (kind) {
            case AIRSYNC:
                return id + "__airsync__" + filename;
            case DERIVED:
                return id + "__derived__" + filename;
            case FILE:
                return id + "__" + filename;
            default:
                return ""; // unreachable
        }
    }

    public Path getArchivePath() {
        return Paths.get(getArchiveDirectory(), getArchiveFilename());
    }

    public FileSystem getZipFileSystem() throws IOException {
        return getZipFileSystem(Map.of());
    }

    public FileSystem getZipFileSystem(Map<String, String> env) throws IOException {
        Files.createDirectories(getArchivePath().getParent());
        return FileSystems.newFileSystem(URI.create("jar:" + getArchivePath().toUri()), env);
    }

    public int getFleetId() {
        return fleetId;
    }

    public int getUploaderId() {
        return uploaderId;
    }

    public String getFilename() {
        return filename;
    }

    public String getMd5Hash() {
        return md5Hash;
    }

    public int getNumberChunks() {
        return numberChunks;
    }

    public boolean completed() {
        return uploadedChunks == numberChunks;
    }

    public boolean checkSize() {
        return bytesUploaded == sizeBytes;
    }

    private static KafkaProducer<String, Integer> producer = null;

    public void complete(Connection connection) throws SQLException {
        status = Status.UPLOADED;
        try (PreparedStatement query = connection
                .prepareStatement("UPDATE uploads SET status = ?, end_time = now() WHERE id = ?")) {
            query.setString(1, status.name());
            query.setInt(2, id);
            query.executeUpdate();
        }

        if (producer == null) {
            producer = Configuration.getUploadProducer();
        }

        producer.send(new ProducerRecord<>(Topic.UPLOAD.toString(), id));
    }

    public void chunkUploaded(Connection connection, int chunkNumber, long chunkSize) throws SQLException {
        uploadedChunks++;
        bytesUploaded += chunkSize;

        StringBuilder statusSB = new StringBuilder(chunkStatus);
        statusSB.setCharAt(chunkNumber, '1');

        chunkStatus = statusSB.toString();

        try (PreparedStatement query = connection.prepareStatement(
                "UPDATE uploads SET uploaded_chunks = ?, bytes_uploaded = ?, chunk_status = ? WHERE id = ?")) {
            query.setInt(1, uploadedChunks);
            query.setLong(2, bytesUploaded);
            query.setString(3, chunkStatus);
            query.setInt(4, id);
            query.executeUpdate();
        }
    }

    public void updateStatus(Connection connection) throws SQLException {
        Upload.updateStatus(connection, id, status);
    }

    public static void updateStatus(Connection connection, int id, Status status) throws SQLException {
        try (PreparedStatement query = connection
                .prepareStatement("UPDATE uploads SET status = ? WHERE id = ?")) {
            query.setString(1, status.name());
            query.setInt(2, id);
            query.executeUpdate();
        }
    }

    public void getAirSyncInfo(Connection connection) throws SQLException {
        String[] dateInfo = this.identifier.split("-");
        int month = Integer.parseInt(dateInfo[3]);

        this.groupString = Month.of(month) + " " + dateInfo[2];

        String sql = "SELECT DISTINCT tail FROM airsync_imports WHERE upload_id = " + id;
        try (PreparedStatement query = connection.prepareStatement(sql); ResultSet resultSet = query.executeQuery()) {

            if (resultSet.next()) {
                tail = resultSet.getString(1);

                if (resultSet.next()) {
                    // This should not happen!
                    tail = null;

                    // It indicates that more than one aircraft is grouped into an
                    // AirSync upload, which is not intended!
                    LOG.severe("This should not be happening! Multiple tails in one AirSync upload! "
                            + Thread.currentThread().getStackTrace().toString());
                }
            }
        }
    }
}
