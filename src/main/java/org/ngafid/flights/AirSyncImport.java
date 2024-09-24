package org.ngafid.flights;

import java.io.*;
import java.net.*;
import java.sql.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.logging.Logger;
import java.util.zip.*;

import javax.net.ssl.HttpsURLConnection;

import org.ngafid.CalculateExceedences;
import org.ngafid.Database;
import org.ngafid.UploadException;
import org.ngafid.WebServer;
import org.ngafid.accounts.AirSyncAircraft;
import org.ngafid.accounts.AirSyncFleet;

/**
 * This class represents an Import from the airsync servers in the NGAFID
 */
public class AirSyncImport {
    private int id, uploadId, aircraftId;
    private byte [] data;
    private String origin, destination, timeStart, timeEnd, md5Hash, fileUrl, timestampUploaded;
    private LocalDateTime localDateTimeStart, localDateTimeEnd, localDateTimeUpload;
    private AirSyncAircraft aircraft;
    private AirSyncFleet fleet;

    private static final String STATUS_IMPORTED = "IMPORTED";
    private static final String STATUS_ERR = "ERROR";
    private static final String STATUS_WARN = "WARNING";

    /* The airsync uploader should be the same across all fleets!
     * There should only be one dummy 'airsync user' in each NGAFID 
     * instance's database! See db/update_tables.php for more info. */
    private static int AIRSYNC_UPLOADER_ID = -1;

    private static final Logger LOG = Logger.getLogger(AirSyncImport.class.getName());

    /**
     * This is a static class to represent the response we get from the AirSync servers
     *
     * @param id the logfiles id
     * @param fileUrl the accessible web URL where this import resides (csv data)
     */
    private static class LogResponse {
        int id;
        String fileUrl;
    }

    /**
     * Private contructor for instaniation within this class
     *
     * @param id the id of this import in the database
     * @param uploadId the id of the upload this import belongs to in the database
     * @param fleet the fleet that this import belongs to
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
     * @param {fleet} a reference to the fleet that this upload is for
     * @param {aircraft} a reference to the aircraft this import is from {@link AirSyncAircraft}
     */
    public void init(AirSyncFleet fleet, AirSyncAircraft aircraft) {
        this.fleet = fleet;
        this.aircraft = aircraft;

        //This does not include timezones yet
        //TODO: Add time zone support!
        this.localDateTimeUpload = LocalDateTime.parse(this.timestampUploaded.split("\\+")[0], DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        this.localDateTimeStart = LocalDateTime.parse(this.timeStart.split("\\+")[0], DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        this.localDateTimeEnd = LocalDateTime.parse(this.timeEnd.split("\\+")[0], DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }

    public LocalDateTime getUploadTime() {
        return localDateTimeUpload;
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
            PreparedStatement query = Database.getConnection().prepareStatement(sql);

            ResultSet resultSet = query.executeQuery();

            if (resultSet.next()) {
                AIRSYNC_UPLOADER_ID = resultSet.getInt(1);
            }

            query.close();
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
     * @param fleetId the id of the fleet that this import belongs to
     * @param aircraftId the id of the aircraft this import is coming from
     * @param time the time of this import
     *
     * @return a formatted string that will serve as a unique indetifier in the database (uploads table)
     */
    public static String getUploadIdentifier(int fleetId, int aircraftId, LocalDateTime time) {
        return "AS-" + fleetId + "." + aircraftId + "-" + time.getYear() + "-" + time.getMonthValue();
    }

    /**
     * Contains the logic for processing the import
     *
     * @param connection the DBMS connection
     *
     * @throws MalformedFlightFileException if we get a bad file from AirSync
     */
    public void process(Connection connection) throws IOException {
        //Ensure there is data to read before proceeding...
        int count = 0;
        String identifier = getUploadIdentifier(this.fleet.getId(), aircraftId, this.localDateTimeStart);

        if ((count = readCsvData()) > 0) {
            String csvName = String.format("%d_%d_%d_%d_%d_%d.csv",
                    this.aircraftId,
                    this.localDateTimeStart.getYear(),
                    this.localDateTimeStart.getMonthValue(),
                    this.localDateTimeStart.getDayOfMonth(),
                    this.localDateTimeStart.getHour(),
                    this.localDateTimeStart.getMinute());

            try {
                String zipId = aircraftId + ".zip";
                String path = WebServer.NGAFID_ARCHIVE_DIR + "/AirSyncUploader/"/* we will use this instead of the user id */ + this.localDateTimeStart.getYear() + "/" + this.localDateTimeStart.getMonthValue();

                String fileName = path + "/" + zipId;
                File file = new File(fileName);

                if (file.exists()) {
                    LOG.info(String.format("Found file for aircraft %d and parent (date) %s", aircraftId, path));
                }  else {
                    File dirPath = new File(path);
                    if (!dirPath.exists()) {
                        dirPath.mkdirs();
                    }
                }

                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                ZipOutputStream zipOutputStream = new ZipOutputStream(bos);

                if (file.exists()) {
                    //If the zip archive already exists for this aircraft 
                    //we must take the exisitng archive and append to it
                    //File tempInput = new File(fileName + ".temp");
                    //if (bos.)  {
                    ZipFile input = new ZipFile(file);
                    
                    Enumeration<? extends ZipEntry> entries = input.entries();

                    while (entries.hasMoreElements()) {
                        ZipEntry entry = entries.nextElement();

                        zipOutputStream.putNextEntry(entry);

                        byte [] data = input.getInputStream(entry).readAllBytes();

                        zipOutputStream.write(data, 0, data.length);
                        zipOutputStream.closeEntry();
                    }

                    // We don't need this file anymore, destroy it.
                    input.close();
                    //}

                } else if (file.createNewFile()) {
                    //We can write directly to the output stream here since there
                    //is nothing to overwrite
                    //zipOutputStream = new ZipOutputStream(new FileOutputStream(file));
                    LOG.info("Creating new zip file: " + file.toURI());
                } else {
                    LOG.severe("Could not create AirSync zip " + path + ". This should not happen - check your permissions!");
                    return;
                }

                // Put the new entry from this import into the ZIP archive.
                ZipEntry csvEntry = new ZipEntry(csvName);

                zipOutputStream.putNextEntry(csvEntry);
                zipOutputStream.write(this.data, 0, count);

                zipOutputStream.closeEntry();
                zipOutputStream.close();

                //Write to the new file from memory
                byte [] buf = bos.toByteArray();
                FileOutputStream fos = new FileOutputStream(file);

                fos.write(buf);
                fos.close();

                // The identifier of any AirSync upload will be unique! 
                // NOTE: multiple imports will reside in one upload.
                // Format: AS-<ngafid_fleet_id>.<airsync_aircraft_id>-<UPLOAD_YYYY>-<UPLOAD_MM>
                Flight flight = new Flight(this.fleet.getId(), csvName, new ByteArrayInputStream(this.data), connection);

                if (connection != null) {
                    this.insertUpload(connection, STATUS_IMPORTED, zipId, identifier, count, flight);
                }

                flight.updateTail(connection, aircraft.getTailNumber());
                flight.updateDatabase(connection, this.uploadId, getUploaderId(), this.fleet.getId());

                this.createImport(connection, flight);

                CalculateExceedences.calculateExceedences(connection, uploadId, null);
			} catch (FatalFlightFileException | FlightAlreadyExistsException e) {
                UploadException ue = new UploadException(e.getMessage(), e, csvName);
                try {
                    FlightError.insertError(connection, uploadId, ue.getFilename(), ue.getMessage());
                    addErrorFlight(connection, identifier);
                } catch (SQLException se) {
                    AirSync.crashGracefully(se);
                }
            } catch (SQLException e) {
                AirSync.crashGracefully(e);
			}
        } else {
            try {
                addErrorFlight(connection, identifier);
            } catch (SQLException se) {
                se.printStackTrace();
            }
        }
    }

    /**
     * Adds an error flight to the db
     *
     * @param connection the DBMS connection
     * @param identifier the unique identifier of the import
     *
     * @throws SQLException if there is an issue with the DBMS
     */
    public void addErrorFlight(Connection connection, String identifier) throws SQLException {
        String sql = "UPDATE uploads SET n_error_flights = n_error_flights + 1 WHERE identifier = ?";
        PreparedStatement query = connection.prepareStatement(sql);

        query.setString(1, identifier);

        query.executeUpdate();
        query.close();
    }

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
    public void insertUpload(Connection connection, String status, String fileName, String identifier, int count, Flight flight) throws SQLException {
        String sql = "SELECT id, size_bytes, bytes_uploaded, n_valid_flights, n_error_flights, n_warning_flights, start_time FROM uploads WHERE identifier = ?";
        PreparedStatement query = connection.prepareStatement(sql);

        query.setString(1, identifier);

        ResultSet resultSet = query.executeQuery();

        if (resultSet.next() && (this.uploadId = resultSet.getInt(1)) >= 0) {
            int sizeBytes = resultSet.getInt(2);
            int bytesUploaded = resultSet.getInt(3);

            int nValid = resultSet.getInt(4);
            int nErr = resultSet.getInt(5);
            int nWarn = resultSet.getInt(6);

            // This represents the "timestamp_uploaded" on the AirSync 
            // side of things
            LocalDateTime latestStartTime = resultSet.getTimestamp(7).toLocalDateTime();

            if (latestStartTime.compareTo(this.localDateTimeUpload) < 0) {
                latestStartTime = this.localDateTimeUpload;
            }

            query.close();

            sql = "UPDATE uploads SET size_bytes = ?, bytes_uploaded = ?, n_valid_flights = ?, n_error_flights = ?, n_warning_flights = ?, start_time = ? WHERE id = ?";
            query = connection.prepareStatement(sql);

            int newSize = count + sizeBytes;
            int newUploadedSize = count + bytesUploaded;

            query.setInt(1, newSize);
            query.setInt(2, newUploadedSize);

            // Update respective statuses
            query.setInt(3, (flight.getStatus().equals(Flight.SUCCESS) ? nValid + 1 : nValid));
            query.setInt(4, (flight.getStatus().equals(Flight.ERROR) ? nErr + 1 : nErr));
            query.setInt(5, (flight.getStatus().equals(Flight.WARNING) ? nWarn + 1 :nWarn));
            query.setTimestamp(6, Timestamp.valueOf(latestStartTime));

            query.setInt(7, this.uploadId);

            query.executeUpdate();
        } else {
            sql = "INSERT INTO uploads (status, fleet_id, filename, identifier, size_bytes, start_time, end_time, n_valid_flights, n_warning_flights, n_error_flights, uploader_id, number_chunks, uploaded_chunks, chunk_status, md5_hash) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
            
            if (!query.isClosed()) {
                // Make sure we close our resources before 
                // we proceed with another query.
                query.close();
            }

            query = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);

            //String hash = this.getMd5Hash();

            query.setString(1, status);
            query.setInt(2, this.fleet.getId());
            query.setString(3, fileName);
            query.setString(4, identifier);
            query.setInt(5, count);
            query.setTimestamp(6, Timestamp.valueOf(this.localDateTimeUpload));
            query.setTimestamp(7, Timestamp.valueOf(this.localDateTimeStart));
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

        query.close();
    }

    /**
     * Creates a record of this import in the database
     * 
     * @param connection the DBMS connection
     * @param flight the {@link Flight} that came from this import
     *
     * @throws SQLException if there is an issue with the DBMS
     */
    public void createImport(Connection connection, Flight flight) throws SQLException {
        String sql = "INSERT INTO airsync_imports(id, tail, time_received, upload_id, fleet_id, flight_id) VALUES(?,?,?,?,?,?)";
        PreparedStatement query = connection.prepareStatement(sql);

        query.setInt(1, this.id);
        query.setString(2, this.aircraft.getTailNumber());

        //NOTE: this is the time that we recieve the CSV, not the time 
        //that AirSync recieves it. That will be denoted in the start_time
        //column in `uploads`
        query.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now()));
        query.setInt(4, this.uploadId);
        query.setInt(5, this.fleet.getId());
        query.setInt(6, flight.getId());

        query.executeUpdate();
        query.close();
    }

    /**
     * Gets a list of the uploads that belong to this fleet
     *
     * @param connection the DBMS connection
     * @param fleetId the fleet id
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
     * @param fleetId the fleet id
     * @param condition the extra SQL conditions
     *
     * @throws SQLException if there is an issue with the DBMS
     */
    public static List<Upload> getUploads(Connection connection, int fleetId, String condition) throws SQLException {
        String sql = String.format("SELECT %s FROM uploads WHERE fleet_id = ? AND uploader_id = ? ORDER BY start_time DESC", Upload.DEFAULT_COLUMNS);
        if (condition != null && !condition.isBlank()) sql += " " + condition;

        PreparedStatement query = connection.prepareStatement(sql);

        query.setInt(1, fleetId);
        query.setInt(2, getUploaderId());

        ResultSet resultSet = query.executeQuery();

        List<Upload> uploads = new ArrayList<>();

        while (resultSet.next()) {
            Upload u = new Upload(resultSet);
            u.getAirSyncInfo(connection);

            uploads.add(u);
        }

        query.close();
        return uploads;
    }

    /**
     * Gets the COUNT of uploads with a condition
     *
     * @param connection the DBMS connection
     * @param fleetId the fleet id
     * @param condition the extra SQL conditions
     *
     * @throws SQLException if there is an issue with the DBMS
     */
    public static int getNumUploads(Connection connection, int fleetId, String condition) throws SQLException {
        String sql = "SELECT COUNT(*) FROM uploads WHERE fleet_id = ? AND uploader_id = ?";
        if (condition != null && !condition.isBlank()) sql += " " + condition;

        PreparedStatement query = connection.prepareStatement(sql);

        query.setInt(1, fleetId);
        query.setInt(2, getUploaderId());

        ResultSet resultSet = query.executeQuery();

        int numUploads = -1;
        if (resultSet.next()) {
            numUploads = resultSet.getInt(1);
        }

        query.close();
        return numUploads;
    }

    /**
     * Gets a list of AirSyncImportResponses that can be used to be populated on a webpage.
     *
     * @param connection the DBMS connection
     * @param fleetId the fleet id
     * @param condition the extra SQL conditions
     *
     * @throws SQLException if there is an issue with the DBMS
     */
    public static List<AirSyncImportResponse> getImports(Connection connection, int fleetId, String condition) throws SQLException {
        String sql = "SELECT a.id, a.time_received, a.upload_id, f.status, a.flight_id, a.tail FROM airsync_imports AS a INNER JOIN flights AS f ON f.id = a.flight_id WHERE a.fleet_id = ? ORDER BY a.time_received";
        if (condition != null && !condition.isBlank()) sql += " " + condition;

        PreparedStatement query = connection.prepareStatement(sql);

        query.setInt(1, fleetId);

        System.out.println(query.toString());
        ResultSet resultSet = query.executeQuery();

        List<AirSyncImportResponse> imports = new ArrayList<>();

        while (resultSet.next()) {
            imports.add(new AirSyncImportResponse(fleetId, resultSet));
        }

        query.close();
        return imports;
    }

    /**
     * Gets the COUNT of imports with an extra condition
     *
     * @param connection the DBMS connection
     * @param fleetId the fleet id
     * @param condition the extra SQL conditions
     *
     * @throws SQLException if there is an issue with the DBMS
     */
    public static int getNumImports(Connection connection, int fleetId, String condition) throws SQLException {
        String sql = "SELECT COUNT(*) FROM airsync_imports WHERE fleet_id = ?";
        if (condition != null && !condition.isBlank()) sql += " " + condition;

        PreparedStatement query = connection.prepareStatement(sql);

        query.setInt(1, fleetId);

        ResultSet resultSet = query.executeQuery();

        int numImports = -1;
        if (resultSet.next()) {
            numImports = resultSet.getInt(1);
        }

        query.close();
        return numImports;
    }

    /**
     * Gets the file input stream of the CSV data from AirSync
     *
     * @return an InputStream instance with the import's CSV data
     */
    private InputStream getFileInputStream() throws IOException {
        InputStream is = null;

        HttpsURLConnection connection = (HttpsURLConnection) new URL(String.format(AirSyncEndpoints.SINGLE_LOG, this.id)).openConnection();

        connection.setRequestMethod("GET");
        connection.setDoOutput(true);
        connection.setRequestProperty("Authorization", this.fleet.getAuth().bearerString());     

        is = connection.getInputStream();
        byte [] respRaw = is.readAllBytes();
        
        String resp = new String(respRaw).replaceAll("file_url", "fileUrl");

        LogResponse log = WebServer.gson.fromJson(resp, LogResponse.class);
        URL input = new URL(log.fileUrl);
        LOG.info("Got URL for logfile " + log.fileUrl);

        is = input.openStream();

        return is;
    }

    /**
     * Reads the csv data into a local buffer and returns the amount of bytes (chars) read.
     *
     * @param the size of the CSV buffer, in bytes
     */
    public int readCsvData() throws IOException {
        try (InputStream is = getFileInputStream()) {
            if (is == null) {
                return -1;
            } else {
                this.data = is.readAllBytes();
            }
        }
        
        return data.length;
    }

    @Override
    /**
     * {@inheritDoc}
     */
    public String toString() {
        return "AirSyncImport: " + this.uploadId + ", for aircraftId: " + aircraftId + ", origin: " + origin + ", destination: " + destination + ",\n" +
            "url: " + fileUrl + ", start time: " + timeStart + ", end time: " + timeEnd + ";";
    }
}
