package org.ngafid.flights;

import java.util.ArrayList;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.ngafid.WebServer;

import java.io.File;

import java.util.logging.Logger;



public class Upload {
    private static final Logger LOG = Logger.getLogger(Upload.class.getName());

    private int id;
    private int fleetId;
    private int uploaderId;
    private String filename;
    private String identifier;
    private int numberChunks;
    private int uploadedChunks;
    private String chunkStatus;
    private String md5Hash;
    private long sizeBytes;
    private long bytesUploaded;
    private String status;
    private String startTime;
    private String endTime;
    private int validFlights;
    private int warningFlights;
    private int errorFlights;

    public int getId() {
        return id;
    }

    /**
     * This is used for getting uploads for the GetUpload route. If an upload was incomplete it will
     * be stored as "UPLOADING" in the database, but this needs to be updated to "UPLOAD INCOMPLETE" for
     * the webpage to make it work correctly to distinguish between current uploads and those that need
     * to be restarted.
     *
     * @param newStatus the new status for this upload
     */
    public void setStatus(String newStatus) {
        status = newStatus;
    }

    /**
     * @return the status of this upload
     */
    public String getStatus() {
        return status;
    }

    private static void deleteDirectory(File folder) {
        File[] files = folder.listFiles();
        if (files != null) { //some JVMs return null for empty dirs
            for (File f: files) {
                if(f.isDirectory()) {
                    deleteDirectory(f);
                } else {
                    f.delete();
                }
            }
        }
        folder.delete();
    }

    public void remove(Connection connection) throws SQLException {
        String query = "DELETE FROM upload_errors WHERE upload_id = ?";
        PreparedStatement preparedStatement = connection.prepareStatement(query);
        preparedStatement.setInt(1, this.id);
        LOG.info(preparedStatement.toString());
        preparedStatement.executeUpdate();
        preparedStatement.close();


        query = "DELETE FROM flight_errors WHERE upload_id = ?";
        preparedStatement = connection.prepareStatement(query);
        preparedStatement.setInt(1, this.id);
        LOG.info(preparedStatement.toString());
        preparedStatement.executeUpdate();
        preparedStatement.close();

        query = "DELETE FROM uploads WHERE id = ?";
        preparedStatement = connection.prepareStatement(query);
        preparedStatement.setInt(1, this.id);
        LOG.info(preparedStatement.toString());
        preparedStatement.executeUpdate();
        preparedStatement.close();


        String archiveFilename = WebServer.NGAFID_ARCHIVE_DIR + "/" + fleetId + "/" + uploaderId + "/" + filename;

        LOG.info("deleting archive file: '" + archiveFilename + "'");
        File archiveFile = new File(archiveFilename);
        archiveFile.delete();

        String uploadDirectory = WebServer.NGAFID_UPLOAD_DIR + "/" + fleetId + "/" + uploaderId + "/" + identifier;

        LOG.info("deleting directory: '" + uploadDirectory + "'");

        Upload.deleteDirectory(new File(uploadDirectory));
    }

    public static Upload getUploadByUser(Connection connection, int uploaderId, String md5Hash) throws SQLException {
        PreparedStatement uploadQuery = connection.prepareStatement("SELECT id, fleet_id, uploader_id, filename, identifier, number_chunks, uploaded_chunks, chunk_status, md5_hash, size_bytes, bytes_uploaded, status, start_time, end_time, n_valid_flights, n_warning_flights, n_error_flights FROM uploads WHERE uploader_id = ? AND md5_hash = ?");
        uploadQuery.setInt(1, uploaderId);
        uploadQuery.setString(2, md5Hash);
        ResultSet resultSet = uploadQuery.executeQuery();

        if (resultSet.next()) {
            Upload upload = new Upload(resultSet);
            resultSet.close();
            uploadQuery.close();
            return upload;
        } else {
            //TODO: maybe need to throw an exception
            resultSet.close();
            uploadQuery.close();
            return null;
        }
    }

    /**
     * This gets an upload only by it's id. Used by {@link CSVWriter} to get archival
     * files usually.
     *
     * @param connection a connection to the database
     * @param uploadId the id of the upload entry in the database
     * @throws SQLException on a database error
     */
    public static Upload getUploadById(Connection connection, int uploadId) throws SQLException {
        PreparedStatement uploadQuery = connection.prepareStatement("SELECT id, fleet_id, uploader_id, filename, identifier, number_chunks, uploaded_chunks, chunk_status, md5_hash, size_bytes, bytes_uploaded, status, start_time, end_time, n_valid_flights, n_warning_flights, n_error_flights FROM uploads WHERE id = ?");
        uploadQuery.setInt(1, uploadId);
        ResultSet resultSet = uploadQuery.executeQuery();

        if (resultSet.next()) {
            Upload upload = new Upload(resultSet);
            resultSet.close();
            uploadQuery.close();
            return upload;
        } else {
            //TODO: maybe need to throw an exception
            resultSet.close();
            uploadQuery.close();
            return null;
        }
    }


    public static Upload getUploadById(Connection connection, int uploadId, String md5Hash) throws SQLException {
        PreparedStatement uploadQuery = connection.prepareStatement("SELECT id, fleet_id, uploader_id, filename, identifier, number_chunks, uploaded_chunks, chunk_status, md5_hash, size_bytes, bytes_uploaded, status, start_time, end_time, n_valid_flights, n_warning_flights, n_error_flights FROM uploads WHERE id = ? AND md5_hash = ?");
        uploadQuery.setInt(1, uploadId);
        uploadQuery.setString(2, md5Hash);
        ResultSet resultSet = uploadQuery.executeQuery();

        if (resultSet.next()) {
            Upload upload = new Upload(resultSet);
            resultSet.close();
            uploadQuery.close();
            return upload;
        } else {
            //TODO: maybe need to throw an exception
            resultSet.close();
            uploadQuery.close();
            return null;
        }
    }

    public static ArrayList<Upload> getUploads(Connection connection, int fleetId) throws SQLException {
        return getUploads(connection, fleetId, "");
    }


    public static ArrayList<Upload> getUploads(Connection connection, int fleetId, String condition) throws SQLException {
        //PreparedStatement uploadQuery = connection.prepareStatement("SELECT id, fleetId, uploaderId, filename, identifier, numberChunks, chunkStatus, md5Hash, sizeBytes, bytesUploaded, status, startTime, endTime, validFlights, warningFlights, errorFlights FROM uploads WHERE fleetId = ?");
        String query = "SELECT id, fleet_id, uploader_id, filename, identifier, number_chunks, uploaded_chunks, chunk_status, md5_hash, size_bytes, bytes_uploaded, status, start_time, end_time, n_valid_flights, n_warning_flights, n_error_flights FROM uploads WHERE fleet_id = ? ORDER BY start_time DESC";
        if (condition != null) query += " " + condition;

        PreparedStatement uploadQuery = connection.prepareStatement(query);
        uploadQuery.setInt(1, fleetId);
        ResultSet resultSet = uploadQuery.executeQuery();

        ArrayList<Upload> uploads = new ArrayList<Upload>();

        while (resultSet.next()) {
            uploads.add(new Upload(resultSet));
        }

        //resultSet.close();
        uploadQuery.close();

        return uploads;
    }

    public static int getNumUploads(Connection connection, int fleetId, String condition) throws SQLException {
        String query = "SELECT count(id) FROM uploads WHERE fleet_id = ?";
        if (condition != null) query += " " + condition;

        PreparedStatement uploadQuery = connection.prepareStatement(query);

        uploadQuery.setInt(1, fleetId);
        ResultSet resultSet = uploadQuery.executeQuery();

        ArrayList<Upload> uploads = new ArrayList<Upload>();

        resultSet.next();
        int count = resultSet.getInt(1);

        resultSet.close();
        uploadQuery.close();

        return count;
    }

    public static ArrayList<Upload> getUploads(Connection connection, int fleetId, String[] types) throws SQLException {
        //String query = "SELECT id, fleetId, uploaderId, filename, identifier, numberChunks, chunkStatus, md5Hash, sizeBytes, bytesUploaded, status, startTime, endTime, validFlights, warningFlights, errorFlights FROM uploads WHERE fleetId = ?";
        String query = "SELECT id, fleet_id, uploader_id, filename, identifier, number_chunks, uploaded_chunks, chunk_status, md5_hash, size_bytes, bytes_uploaded, status, start_time, end_time, n_valid_flights, n_warning_flights, n_error_flights FROM uploads WHERE fleet_id = ?";

        if (types.length > 0) {
            query += " AND (";

            for (int i = 0; i < types.length; i++) {
                if (i > 0) query += " OR ";
                query += "status = ?";
            }
            query += ")";
        }
        query += " ORDER BY start_time DESC";

        PreparedStatement uploadQuery = connection.prepareStatement(query);
        uploadQuery.setInt(1, fleetId);

        for (int i = 0; i < types.length; i++) {
            uploadQuery.setString(i + 2, types[i]);
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

    public static ArrayList<Upload> getUploads(Connection connection, int fleetId, String [] types, String sqlLimit) throws SQLException{
        String query = "SELECT id, fleet_id, uploader_id, filename, identifier, number_chunks, uploaded_chunks, chunk_status, md5_hash, size_bytes, bytes_uploaded, status, start_time, end_time, n_valid_flights, n_warning_flights, n_error_flights FROM uploads WHERE fleet_id = ?";

        if (types.length > 0) {
            query += " AND (";

            for (int i = 0; i < types.length; i++) {
                if (i > 0) query += " OR ";
                query += "status = ?";
            }
            query += ")";
        }
        query += " ORDER BY start_time DESC";

        if(!sqlLimit.isEmpty())
            query += sqlLimit;

        PreparedStatement uploadQuery = connection.prepareStatement(query);
        uploadQuery.setInt(1, fleetId);

        for (int i = 0; i < types.length; i++) {
            uploadQuery.setString(i + 2, types[i]);
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
        fleetId = resultSet.getInt(2);
        uploaderId = resultSet.getInt(3);
        filename = resultSet.getString(4);
        identifier = resultSet.getString(5);
        numberChunks = resultSet.getInt(6);
        uploadedChunks = resultSet.getInt(7);
        chunkStatus = resultSet.getString(8);
        md5Hash = resultSet.getString(9);
        sizeBytes = resultSet.getLong(10);
        bytesUploaded = resultSet.getLong(11);
        status = resultSet.getString(12);
        startTime = resultSet.getString(13);
        endTime = resultSet.getString(14);
        validFlights = resultSet.getInt(15);
        warningFlights = resultSet.getInt(16);
        errorFlights = resultSet.getInt(17);
    }

    public int getFleetId() {
        return fleetId;
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


    public void complete(Connection connection) throws SQLException {
        status = "UPLOADED";

        PreparedStatement query = connection.prepareStatement("UPDATE uploads SET status = ?, end_time = now() WHERE id = ?");
        query.setString(1, status);
        query.setInt(2, id);
        query.executeUpdate();
        query.close();
    }

    public void chunkUploaded(Connection connection, int chunkNumber, long chunkSize) throws SQLException {
        uploadedChunks++;
        bytesUploaded += chunkSize;

        StringBuilder statusSB = new StringBuilder(chunkStatus);
        statusSB.setCharAt(chunkNumber, '1');

        chunkStatus = statusSB.toString();

        PreparedStatement query = connection.prepareStatement("UPDATE uploads SET uploaded_chunks = ?, bytes_uploaded = ?, chunk_status = ? WHERE id = ?");
        query.setInt(1, uploadedChunks);
        query.setLong(2, bytesUploaded);
        query.setString(3, chunkStatus);
        query.setInt(4, id);
        query.executeUpdate();
        query.close();
    }
}
