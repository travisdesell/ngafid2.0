package org.ngafid.flights;

import java.io.*;
import java.net.*;
import java.security.MessageDigest;
import java.sql.*;
import java.sql.Date;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.logging.Logger;
import java.util.*;
import java.util.zip.*;

import javax.xml.bind.DatatypeConverter;

import org.ngafid.CalculateExceedences;
import org.ngafid.WebServer;
import org.ngafid.accounts.AirSyncAircraft;
import org.ngafid.accounts.Fleet;

public class AirSyncUpload extends Upload {
    private int airsyncId;
    private byte [] data;
    private int aircraftId;
    private String origin, destination;
    private String timeStart, timeEnd;
    private LocalDateTime localDateTimeStart, localDateTimeEnd;
    private String fileUrl;

    private static final String AS_COLUMNS = DEFAULT_COLUMNS + ", airsync_id";
    private static final String STATUS_IMPORTED = "IMPORTED";
    private static final String STATUS_ERR = "ERROR";
    private static final String STATUS_WARN = "WARNING";

    private static final Logger LOG = Logger.getLogger(AirSyncUpload.class.getName());

    public AirSyncUpload(ResultSet resultSet) throws SQLException {
        super(resultSet);
        this.airsyncId = resultSet.getInt(18);
    }

    private AirSyncUpload(int id, int fleetId) {
        super(id);
        super.fleetId = fleetId;
    }

    /**
     * This will act as the constructor as we will be parsing the object
     * from JSON most of the time.
     *
     * It is up to the programmer to ensure this method is called each time a JSON
     * AirSyncUpload class is instantiated.
     *
     * @param {fleet} a reference to the fleet that this upload is for
     * @param {connection} a reference to the database {@link Connection}
     */
    public void init(Fleet fleet, Connection connection) {
        super.fleetId = fleet.getId();

        //Change this over from the JSON parse so that the NGAFID id and AirSync id
        //do not get interchanged
        this.airsyncId = id;

        //This does not include timezones yet
        //TODO: Add time zone support!
        this.localDateTimeStart = LocalDateTime.parse(this.timeStart.split("\\+")[0], DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        this.localDateTimeEnd = LocalDateTime.parse(this.timeEnd.split("\\+")[0], DateTimeFormatter.ISO_LOCAL_DATE_TIME);

        try {
            this.setUploaderId(connection);
        } catch (SQLException se) {
            se.printStackTrace();
        }

    }

    public void setUploaderId(Connection connection) throws SQLException {
        String sql = "SELECT id FROM user WHERE first_name = 'airsync' AND last_name = 'user'";
        PreparedStatement query = connection.prepareStatement(sql);

        ResultSet resultSet = query.executeQuery();

        if (resultSet.next()) {
            super.uploaderId = resultSet.getInt(1);
        }
    }

    public void proccess(Connection connection) throws MalformedFlightFileException {
        //Ensure there is data to read before proceeding...
        int count = 0;
        if ((count = readCsvData()) > 0) {
            // Target zip: $NGAFID_ARCHIVE_DIR/<FLEET_ID>/<UPLOADER_ID>/<FLIGHT YYYY>/<FLIGHT MM>/<upload_id>__<upload_name>.zip 
            try {
                String zipId = String.format("%d_%s.zip", id, aircraftId);
                String path = WebServer.NGAFID_ARCHIVE_DIR + "/AirSyncUploader/" + this.localDateTimeStart.getYear() + "/" + this.localDateTimeStart.getMonthValue();

                File file = new File(path + "/" + zipId);

                if (file.exists()) {
                    LOG.severe("Zip file for upload " + path + " already exists! Skipping this file...");
                    return;
                } 

                String csvName = this.aircraftId + "_" + this.localDateTimeStart.getYear() + "_" + this.localDateTimeStart.getMonthValue() + "_" + this.localDateTimeStart.getDayOfMonth() + ".csv";
                    
                File dirPath = new File(path);
                if (!dirPath.exists()) {
                    dirPath.mkdirs();
                }

                if (file.createNewFile()) {
                    ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream(file));
                    ZipEntry csvEntry = new ZipEntry(csvName);

                    zipOutputStream.putNextEntry(csvEntry);
                    zipOutputStream.write(this.data, 0, count);

                    zipOutputStream.closeEntry();
                    zipOutputStream.close();
                } else {
                    LOG.severe("Could not create AirSync zip " + path + ". This should not happen - check your permissions!");
                    return;
                }

                String identifier = this.aircraftId + "AS-" + this.localDateTimeStart.getYear() + "-" + this.localDateTimeStart.getMonthValue() + "-" + this.localDateTimeStart.getDayOfMonth();

                Flight flight = new Flight(fleetId, csvName, new ByteArrayInputStream(this.data), connection);
                int uploadId = -1;
                if (connection != null) {
                    uploadId = createUpload(connection, STATUS_IMPORTED, zipId, identifier, count);
                }

                flight.updateDatabase(connection, uploadId, this.uploaderId, fleetId);
                CalculateExceedences.calculateExceedences(connection, uploadId, null);
			} catch (Exception e) {
                LOG.severe("Exception caught in file processing:");
                LOG.severe(e.getLocalizedMessage());
				e.printStackTrace();
                System.exit(1);
			}
        } else {
            throw new MalformedFlightFileException("Unable to read data from the provided AirSync upload for AirSync Aircraft id: " + aircraftId + " for flight: " + origin + " to " + destination + " at " + timeStart);
        }
    }

    @Override
    public String getMd5Hash() {
        if (super.md5Hash == null) {
            try {
               MessageDigest md = MessageDigest.getInstance("MD5");
               md.update(this.data);

               super.md5Hash = DatatypeConverter.printBase64Binary(this.data).toUpperCase();
               return super.md5Hash;
            } catch (Exception e) {
                e.printStackTrace();
            }

            return null;
        } else {
            return super.md5Hash;
        }

    }

    public int createUpload(Connection connection, String status, String fileName, String identifier, int count) throws SQLException {
        String sql = "INSERT INTO uploads (status, fleet_id, filename, identifier, size_bytes, start_time, end_time, n_valid_flights, n_warning_flights, n_error_flights, airsync_id, uploader_id, number_chunks, uploaded_chunks, chunk_status, md5_hash) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
        PreparedStatement query = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);

        //String hash = this.getMd5Hash();

        query.setString(1, status);
        query.setInt(2, super.fleetId);
        query.setString(3, fileName);
        query.setString(4, identifier);
        query.setInt(5, count);
        query.setTimestamp(6, Timestamp.valueOf(this.localDateTimeStart));
        query.setTimestamp(7, Timestamp.valueOf(this.localDateTimeEnd));
        query.setInt(8, (status.equals(STATUS_IMPORTED) ? 1 : 0));
        query.setInt(9, (status.equals(STATUS_ERR) ? 1 : 0));
        query.setInt(10, (status.equals(STATUS_WARN) ? 1 : 0));
        query.setInt(11, this.airsyncId);
        query.setInt(12, this.uploaderId);
        query.setInt(13, -2);
        query.setInt(14, 0);
        query.setInt(15, 0);

        //TODO: changeme when we get the MD5 hash from AirSync
        query.setInt(16, this.airsyncId);

        System.out.println(query.toString());
        query.executeUpdate();

        ResultSet resultSet = query.getGeneratedKeys();

        if (resultSet.next()) {
            return resultSet.getInt(1);
        }

        return -1;
    }

    public static List<Upload> getUploads(Connection connection, int fleetId) throws SQLException {
        return getUploads(connection, fleetId, new String());
    }

    public static List<Upload> getUploads(Connection connection, int fleetId, String condition) throws SQLException {
        String sql = String.format("SELECT %s FROM uploads WHERE fleet_id = ? AND airsync_id > 0 ORDER BY start_time DESC", AS_COLUMNS);
        if (condition != null && !condition.isBlank()) sql += " " + condition;

        PreparedStatement query = connection.prepareStatement(sql);
        query.setInt(1, fleetId);

        ResultSet resultSet = query.executeQuery();

        List<Upload> uploads = new ArrayList<>();

        while (resultSet.next()) {
            uploads.add(new AirSyncUpload(resultSet));
        }

        return uploads;
    }

    public static int getNumUploads(Connection connection, int fleetId, String condition) throws SQLException {
        String sql = "SELECT COUNT(*) FROM uploads WHERE fleet_id = ? AND airsync_id > 0";
        if (condition != null && !condition.isBlank()) sql += " " + condition;

        PreparedStatement query = connection.prepareStatement(sql);
        query.setInt(1, fleetId);

        ResultSet resultSet = query.executeQuery();

        if (resultSet.next()) {
            return resultSet.getInt(1);
        }

        return -1;
    }


    public int readCsvData() {
        try {
            URL input = new URL(fileUrl);
            InputStream is = input.openStream();

            this.data = is.readAllBytes();
        } catch (Exception e) {
            e.printStackTrace();

            return -1;
        }

        // Return num of bytes read
        return data.length;
    }
    
    @Override
    public String toString() {
        return "AirSyncUpload: " + super.id + ", for aircraftId: " + aircraftId + ", origin: " + origin + ", destination: " + destination + ",\n" +
            "url: " + fileUrl + ", start time: " + timeStart + ", end time: " + timeEnd + ";";
    }
}
