package org.ngafid;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.InputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.PreparedStatement;
import java.sql.SQLException;


import java.util.ArrayList;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.ngafid.flights.FlightAlreadyExistsException;
import org.ngafid.flights.FatalFlightFileException;

import org.ngafid.flights.Flight;

public class ProcessFlights {
    private static Connection connection;

    static {
        if (System.getenv("NGAFID_DB_INFO") == null) {
            System.err.println("ERROR: 'NGAFID_DB_INFO' environment variable not specified at runtime.");
            System.err.println("Please add the following to your ~/.bash_rc or ~/.profile file:");
            System.err.println("export NGAFID_DB_INFO=<path/to/db_info_file>");
            System.exit(1);
        }
        String NGAFID_DB_INFO = System.getenv("NGAFID_DB_INFO");

        String dbHost = "";
        String dbName = "";
        String dbUser = "";
        String dbPassword = "";

        try {
            BufferedReader bufferedReader = new BufferedReader(new FileReader(NGAFID_DB_INFO));
            bufferedReader.readLine();

            dbUser = bufferedReader.readLine();
            dbUser = dbUser.substring(dbUser.indexOf("'") + 1);
            dbUser = dbUser.substring(0, dbUser.indexOf("'"));

            dbName = bufferedReader.readLine();
            dbName = dbName.substring(dbName.indexOf("'") + 1);
            dbName = dbName.substring(0, dbName.indexOf("'"));

            dbHost = bufferedReader.readLine();
            dbHost = dbHost.substring(dbHost.indexOf("'") + 1);
            dbHost = dbHost.substring(0, dbHost.indexOf("'"));

            dbPassword = bufferedReader.readLine();
            dbPassword = dbPassword.substring(dbPassword.indexOf("'") + 1);
            dbPassword = dbPassword.substring(0, dbPassword.indexOf("'"));

            System.out.println("dbHost: '" + dbHost + "'");
            System.out.println("dbName: '" + dbName + "'");
            System.out.println("dbUser: '" + dbUser + "'");
            System.out.println("dbPassword: '" + dbPassword + "'");

        } catch (IOException e) {
            System.err.println("Error reading from NGAFID_DB_INFO: '" + NGAFID_DB_INFO + "'");
            e.printStackTrace();
            System.exit(1);
        }

        try {
            // This will load the MySQL driver, each DB has its own driver
            Class.forName("com.mysql.jdbc.Driver");
            // Setup the connection with the DB
            connection = DriverManager.getConnection("jdbc:mysql://" + dbHost + "/" + dbName, dbUser, dbPassword);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    public static void main(String[] arguments) {
        try {
            PreparedStatement uploadsPreparedStatement = connection.prepareStatement("SELECT id, uploader_id, fleet_id, filename FROM uploads WHERE status = ?");
            uploadsPreparedStatement.setString(1, "UPLOADED");
            ResultSet resultSet = uploadsPreparedStatement.executeQuery();

            while (resultSet.next()) {
                ArrayList<UploadException> flightErrors = new ArrayList<UploadException>();

                int uploadId = resultSet.getInt(1);
                int uploaderId = resultSet.getInt(2);
                int fleetId = resultSet.getInt(3);
                String filename = resultSet.getString(4);

                filename = "/ngafid/archives/" + fleetId + "/" + uploaderId + "/" + filename;
                System.err.println("processing: '" + filename + "'");

                String extension = filename.substring(filename.length() - 4);
                System.err.println("extension: '" + extension + "'");

                String status = "IMPORTED";

                Exception uploadException = null;

                int validFlights = 0;
                int warningFlights = 0;
                int errorFlights = 0;
                if (extension.equals(".zip")) {
                    try {
                        System.err.println("processing zip file: '" + filename + "'");
                        ZipFile zipFile = new ZipFile(filename);

                        Enumeration<? extends ZipEntry> entries = zipFile.entries();

                        while (entries.hasMoreElements()) {
                            ZipEntry entry = entries.nextElement();
                            String name = entry.getName();

                            if (entry.isDirectory()) {
                                //System.err.println("SKIPPING: " + entry.getName());
                                continue;
                            }

                            if (name.contains("__MACOSX")) {
                                //System.err.println("SKIPPING: " + entry.getName());
                                continue;
                            }

                            System.err.println("PROCESSING: " + name);

                            if (entry.getName().contains(".csv")) {
                                try {
                                    InputStream stream = zipFile.getInputStream(entry);
                                    Flight flight = new Flight(entry.getName(), stream, connection);

                                    if (connection != null) {
                                        flight.updateDatabase(connection, uploadId, uploaderId, fleetId);
                                    }

                                    if (flight.getStatus().equals("WARNING")) warningFlights++;

                                    validFlights++;
                                } catch (IOException e) {
                                    System.err.println(e.getMessage());
                                    flightErrors.add(new UploadException(e.getMessage(), e, entry.getName()));
                                    errorFlights++;
                                } catch (FatalFlightFileException e) {
                                    System.err.println(e.getMessage());
                                    flightErrors.add(new UploadException(e.getMessage(), e, entry.getName()));
                                    errorFlights++;
                                } catch (FlightAlreadyExistsException e) {
                                    System.err.println(e.getMessage());
                                    flightErrors.add(new UploadException(e.getMessage(), e, entry.getName()));
                                    errorFlights++;
                                }
                            } else {
                                flightErrors.add(new UploadException("Unknown file type contained in zip file (flight logs should be .csv files).", entry.getName()));
                                errorFlights++;
                            }
                        } 
                    } catch (IOException e) {
                        status = "ERROR";
                        uploadException = e;
                    }
                } else {
                    //insert an upload error for this upload
                    status = "ERROR";
                }

                if (status == "ERROR") {
                    PreparedStatement errorStatement = connection.prepareStatement("INSERT INTO upload_errors SET upload_id = ?, message = ?, stack_trace = ?");
                    errorStatement.setInt(1, uploadId);
                    errorStatement.setString(2, "Uploaded file was not a zip file.");

                    if (uploadException != null) {
                        StringWriter sw = new StringWriter();
                        PrintWriter pw = new PrintWriter(sw);
                        uploadException.printStackTrace(pw);
                        String sStackTrace = sw.toString(); // stack trace as a string

                        errorStatement.setString(3, sStackTrace);
                    } else {
                        errorStatement.setString(3, "");
                    }

                    errorStatement.executeUpdate();
                    errorStatement.close();
                }

                //update upload in database, add upload exceptions if there are any
                PreparedStatement updateStatement = connection.prepareStatement("UPDATE uploads SET status = ?, n_valid_flights = ?, n_warning_flights = ?, n_error_flights = ? WHERE id = ?");
                updateStatement.setString(1, status);
                updateStatement.setInt(2, validFlights);
                updateStatement.setInt(3, warningFlights);
                updateStatement.setInt(4, errorFlights);
                updateStatement.setInt(5, uploadId);
                updateStatement.executeUpdate();
                updateStatement.close();

                for (UploadException exception : flightErrors) {
                    PreparedStatement exceptionStatement = connection.prepareStatement("INSERT INTO flight_errors SET upload_id = ?, filename = ?, message = ?, stack_trace = ?");

                    exceptionStatement.setInt(1, uploadId);
                    exceptionStatement.setString(2, exception.getFilename());
                    exceptionStatement.setString(3, exception.getMessage());

                    StringWriter sw = new StringWriter();
                    PrintWriter pw = new PrintWriter(sw);
                    exception.printStackTrace(pw);
                    String sStackTrace = sw.toString(); // stack trace as a string

                    exceptionStatement.setString(4, sStackTrace);
                    exceptionStatement.executeUpdate();

                    exceptionStatement.close();
                }

            }
            resultSet.close();
            uploadsPreparedStatement.close();
            connection.close();

        } catch (SQLException e) {
            e.printStackTrace();
            System.exit(1);
        }

        System.err.println("finished!");
    }
}
