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
import org.ngafid.Database;
import org.ngafid.WebServer;
import org.ngafid.accounts.AirSyncAircraft;
import org.ngafid.accounts.Fleet;

public class AirSyncImport {
    private int id, uploadId, aircraftId, fleetId;
    private byte [] data;
    private String origin, destination, timeStart, timeEnd, md5Hash, fileUrl;
    private LocalDateTime localDateTimeStart, localDateTimeEnd;
    private AirSyncAircraft aircraft;

    private static final String STATUS_IMPORTED = "IMPORTED";
    private static final String STATUS_ERR = "ERROR";
    private static final String STATUS_WARN = "WARNING";

    /* The airsync uploader should be the same across all fleets!
     * There should only be one dummy 'airsync user' in each NGAFID 
     * instance's database! See db/update_tables.php for more info. */
    private static int AIRSYNC_UPLOADER_ID = -1;

    private static final Logger LOG = Logger.getLogger(AirSyncImport.class.getName());

    private AirSyncImport(int id, int uploadId, int fleetId) {
        this.uploadId = uploadId;
        this.id = id;
        this.fleetId = fleetId;
    }

    /**
     * This will act as the constructor as we will be parsing the object
     * from JSON most of the time.
     *
     * It is up to the programmer to ensure this method is called each time a JSON
     * AirSyncImport class is instantiated.
     *
     * @param {fleet} a reference to the fleet that this upload is for
     * @param {aircraft} a reference to the aircraft this import is from {@link AirSyncAircraft}
     */
    public void init(Fleet fleet, AirSyncAircraft aircraft) {
        this.fleetId = fleet.getId();
        this.aircraft = aircraft;

        //This does not include timezones yet
        //TODO: Add time zone support!
        this.localDateTimeStart = LocalDateTime.parse(this.timeStart.split("\\+")[0], DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        this.localDateTimeEnd = LocalDateTime.parse(this.timeEnd.split("\\+")[0], DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }

    public static int getUploaderId() throws SQLException {
        if (AIRSYNC_UPLOADER_ID <= 0) {
            String sql = "SELECT id FROM user WHERE first_name = 'airsync' AND last_name = 'user'";
            PreparedStatement query = Database.getConnection().prepareStatement(sql);

            ResultSet resultSet = query.executeQuery();

            if (resultSet.next()) {
                AIRSYNC_UPLOADER_ID = resultSet.getInt(1);
            }
        }

        return AIRSYNC_UPLOADER_ID;
    }

    public int getId() {
        return this.id;
    }

    public void proccess(Connection connection) throws MalformedFlightFileException {
        //Ensure there is data to read before proceeding...
        int count = 0;
        if ((count = readCsvData()) > 0) {
            try {
                String zipId = aircraftId + ".zip";
                String path = WebServer.NGAFID_ARCHIVE_DIR + "/AirSyncUploader/"/* we will use this instead of the user id */ + this.localDateTimeStart.getYear() + "/" + this.localDateTimeStart.getMonthValue();

                File file = new File(path + "/" + zipId);

                if (file.exists()) {
                    LOG.info(String.format("Found file for aircraft %d and parent (date) %s", aircraftId, path));
                }  else {
                    File dirPath = new File(path);
                    if (!dirPath.exists()) {
                        dirPath.mkdirs();
                    }
                }

                String csvName = this.aircraftId + "_" + this.localDateTimeStart.getYear() + "_" + this.localDateTimeStart.getMonthValue() + "_" + this.localDateTimeStart.getDayOfMonth() + ".csv";
                    
                if (file.exists() || file.createNewFile()) {
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


                // The identifier of any AirSync upload will be unique! 
                // NOTE: multiple imports will reside in one upload.
                // Format: AS-<ngafid_fleet_id>.<airsync_aircraft_id>-<UPLOAD_YYYY>-<UPLOAD_MM>

                String identifier = "AS-" + this.fleetId + "." + this.aircraftId + "-" + this.localDateTimeStart.getYear() + "-" + this.localDateTimeStart.getMonthValue();

                Flight flight = new Flight(fleetId, csvName, new ByteArrayInputStream(this.data), connection);

                if (connection != null) {
                    //TODO: change the status based on the month!
                    this.insertUpload(connection, STATUS_IMPORTED, zipId, identifier, count);
                }

                flight.updateTail(connection, aircraft.getTailNumber());
                flight.updateDatabase(connection, this.uploadId, getUploaderId(), fleetId);

                this.createImport(connection, flight);

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

    //public String getMd5Hash() {
        //if (this.md5Hash == null) {
            //try {
               //MessageDigest md = MessageDigest.getInstance("MD5");
               //md.update(this.data);

               //this.md5Hash = DatatypeConverter.printBase64Binary(this.data).toUpperCase();
               //return this.md5Hash;
            //} catch (Exception e) {
                //e.printStackTrace();
            //}

            //return null;
        //} else {
            //return this.md5Hash;
        //}

    //}

    /**
     * Inserts into the uploads table and returns the id, if it does not already exist
     *
     * @param {connection} the database connection
     * @param {status} the current status of this upload
     * @param {fileName} the location of the source upload (zip) file
     * @param {identifier} how this AirSync upload is identified
     * @param {count} the number of bytes uploaed
     *
     * @throws SQLException
     */
    public void insertUpload(Connection connection, String status, String fileName, String identifier, int count) throws SQLException {
        String sql = "SELECT id, size_bytes, bytes_uploaded FROM uploads WHERE identifier = ?";
        PreparedStatement query = connection.prepareStatement(sql);

        query.setString(1, identifier);

        ResultSet resultSet = query.executeQuery();

        if (resultSet.next() && (this.uploadId = resultSet.getInt(1)) >= 0) {
            int sizeBytes = resultSet.getInt(2);
            int bytesUploaded = resultSet.getInt(3);

            sql = "UPDATE uploads SET size_bytes = ?, bytes_uploaded = ? WHERE id = ?";
            query = connection.prepareStatement(sql);

            int newSize = count + sizeBytes;
            int newUploadedSize = count + bytesUploaded;

            query.setInt(1, newSize);
            query.setInt(2, newUploadedSize);
            query.setInt(3, this.uploadId);

            query.executeUpdate();
        } else {
            sql = "INSERT INTO uploads (status, fleet_id, filename, identifier, size_bytes, start_time, end_time, n_valid_flights, n_warning_flights, n_error_flights, uploader_id, number_chunks, uploaded_chunks, chunk_status, md5_hash) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
            query = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);

            //String hash = this.getMd5Hash();

            query.setString(1, status);
            query.setInt(2, this.fleetId);
            query.setString(3, fileName);
            query.setString(4, identifier);
            query.setInt(5, count);
            query.setTimestamp(6, Timestamp.valueOf(this.localDateTimeStart));
            query.setTimestamp(7, Timestamp.valueOf(this.localDateTimeEnd));
            query.setInt(8, (status.equals(STATUS_IMPORTED) ? 1 : 0));
            query.setInt(9, (status.equals(STATUS_ERR) ? 1 : 0));
            query.setInt(10, (status.equals(STATUS_WARN) ? 1 : 0));
            query.setInt(11, getUploaderId());
            query.setInt(12, -2);
            query.setInt(13, 0);
            query.setInt(14, 0);
        
            //TODO: changeme when we get the MD5 hash from AirSync
            query.setInt(15, this.id);

            System.out.println(query.toString());
            query.executeUpdate();

            resultSet = query.getGeneratedKeys();

            if (resultSet.next()) {
                this.uploadId = resultSet.getInt(1);
            }
        }
    }

    public void createImport(Connection connection, Flight flight) throws SQLException {
        String sql = "INSERT INTO airsync_imports(id, tail, time_received, upload_id, fleet_id, flight_id) VALUES(?,?,?,?,?,?)";
        PreparedStatement query = connection.prepareStatement(sql);

        query.setInt(1, this.id);
        query.setString(2, this.aircraft.getTailNumber());
        query.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now()));
        query.setInt(4, this.uploadId);
        query.setInt(5, this.fleetId);
        query.setInt(6, flight.getId());

        query.executeUpdate();
        query.close();
    }

    public static List<Upload> getUploads(Connection connection, int fleetId) throws SQLException {
        return getUploads(connection, fleetId, new String());
    }

    public static List<Upload> getUploads(Connection connection, int fleetId, String condition) throws SQLException {
        String sql = String.format("SELECT %s FROM uploads WHERE fleet_id = ? AND uploader_id = ? ORDER BY start_time DESC", Upload.DEFAULT_COLUMNS);
        if (condition != null && !condition.isBlank()) sql += " " + condition;

        PreparedStatement query = connection.prepareStatement(sql);

        query.setInt(1, fleetId);
        query.setInt(2, getUploaderId());

        ResultSet resultSet = query.executeQuery();

        List<Upload> uploads = new ArrayList<>();

        while (resultSet.next()) {
            uploads.add(new Upload(resultSet));
        }

        return uploads;
    }

    public static int getNumUploads(Connection connection, int fleetId, String condition) throws SQLException {
        String sql = "SELECT COUNT(*) FROM uploads WHERE fleet_id = ? AND uploader_id = ?";
        if (condition != null && !condition.isBlank()) sql += " " + condition;

        PreparedStatement query = connection.prepareStatement(sql);

        query.setInt(1, fleetId);
        query.setInt(2, getUploaderId());

        ResultSet resultSet = query.executeQuery();

        if (resultSet.next()) {
            return resultSet.getInt(1);
        }

        return -1;
    }

    public static List<AirSyncImportResponse> getImports(Connection connection, int fleetId, String condition) throws SQLException {
        String sql = "SELECT a.id, a.time_received, a.upload_id, f.status, a.flight_id, a.tail FROM airsync_imports AS a INNER JOIN flights AS f ON f.id = a.flight_id WHERE a.fleet_id = ?";
        if (condition != null && !condition.isBlank()) sql += " " + condition;

        PreparedStatement query = connection.prepareStatement(sql);

        query.setInt(1, fleetId);

        System.out.println(query.toString());
        ResultSet resultSet = query.executeQuery();

        List<AirSyncImportResponse> imports = new ArrayList<>();

        while (resultSet.next()) {
            imports.add(new AirSyncImportResponse(fleetId, resultSet));
        }

        return imports;
    }

    public static int getNumImports(Connection connection, int fleetId, String condition) throws SQLException {
        String sql = "SELECT COUNT(*) FROM airsync_imports WHERE fleet_id = ?";
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
        return "AirSyncImport: " + this.uploadId + ", for aircraftId: " + aircraftId + ", origin: " + origin + ", destination: " + destination + ",\n" +
            "url: " + fileUrl + ", start time: " + timeStart + ", end time: " + timeEnd + ";";
    }
}
