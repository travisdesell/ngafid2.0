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
import org.ngafid.UploadException;
import org.ngafid.WebServer;
import org.ngafid.accounts.AirSyncAircraft;
import org.ngafid.accounts.Fleet;

public class AirSyncImport {
    private int id, uploadId, aircraftId, fleetId;
    private byte [] data;
    private String origin, destination, timeStart, timeEnd, md5Hash, fileUrl, timestampUploaded;
    private LocalDateTime localDateTimeStart, localDateTimeEnd, localDateTimeUpload;
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
        this.localDateTimeUpload = LocalDateTime.parse(this.timestampUploaded.split("\\+")[0], DateTimeFormatter.ISO_LOCAL_DATE_TIME);
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

            query.close();
        }

        return AIRSYNC_UPLOADER_ID;
    }

    public int getId() {
        return this.id;
    }

    public static String getUploadIdentifier(int fleetId, int aircraftId, LocalDateTime time) {
        return "AS-" + fleetId + "." + aircraftId + "-" + time.getYear() + "-" + time.getMonthValue();
    }

    public void process(Connection connection) throws MalformedFlightFileException {
        //Ensure there is data to read before proceeding...
        int count = 0;
        String identifier = getUploadIdentifier(fleetId, aircraftId, this.localDateTimeStart);

        if ((count = readCsvData()) > 0) {
            String csvName = this.aircraftId + "_" + this.localDateTimeStart.getYear() + "_" + this.localDateTimeStart.getMonthValue() + "_" + this.localDateTimeStart.getDayOfMonth() + ".csv";

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
                Flight flight = new Flight(fleetId, csvName, new ByteArrayInputStream(this.data), connection);

                if (connection != null) {
                    //TODO: change the status based on the month!
                    this.insertUpload(connection, STATUS_IMPORTED, zipId, identifier, count, flight);
                }

                flight.updateTail(connection, aircraft.getTailNumber());
                flight.updateDatabase(connection, this.uploadId, getUploaderId(), fleetId);

                this.createImport(connection, flight);

                CalculateExceedences.calculateExceedences(connection, uploadId, null);
			} catch (IOException | FatalFlightFileException | FlightAlreadyExistsException e) {
                UploadException ue = new UploadException(e.getMessage(), e, csvName);
                try {
                    FlightError.insertError(connection, uploadId, ue.getFilename(), ue.getMessage());
                    addErrorFlight(connection, identifier);
                } catch (SQLException se) {
                    AirSync.crashGracefully(se);
                }
            } catch (Exception e) {
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

    public void addErrorFlight(Connection connection, String identifier) throws SQLException {
        String sql = "UPDATE uploads SET n_error_flights = n_error_flights + 1 WHERE identifier = ?";
        PreparedStatement query = connection.prepareStatement(sql);

        query.setString(1, identifier);

        query.executeUpdate();
        query.close();
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
            query.setInt(2, this.fleetId);
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
            Upload u = new Upload(resultSet);
            u.getAirSyncInfo(connection);

            uploads.add(u);
        }

        query.close();
        return uploads;
    }

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


    public int readCsvData() {
        try {
            URL input = new URL(fileUrl);
            InputStream is = input.openStream();

            this.data = is.readAllBytes();
        } catch (Exception e) {
            AirSync.logFile.println("ERROR: Unable to read fileUrl for aircraftId " + this.aircraftId + ": " + fileUrl);
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
