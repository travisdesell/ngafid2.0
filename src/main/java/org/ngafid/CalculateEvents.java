package org.ngafid;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.FileReader;
import java.io.InputStream;
import java.io.IOException;

import java.sql.Blob;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;


import java.util.ArrayList;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.ngafid.flights.DoubleTimeSeries;
import org.ngafid.flights.MalformedFlightFileException;

import org.ngafid.airframes.Airframe;
import org.ngafid.airframes.C172;
import org.ngafid.airframes.C182;
import org.ngafid.airframes.PA28;
import org.ngafid.airframes.PA44;
import org.ngafid.airframes.SR20;

import org.ngafid.events.Event;

import org.ngafid.flights.Flight;

public class CalculateEvents {
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

    public static void main(String[] arguments) throws Exception {
        // We need to provide file path as the parameter:
        // double backquote is to avoid compiler interpret words
        // like \test as \t (ie. as a escape sequence)

        String columnName = "Pitch";
        double minValue = -30.0;
        double maxValue = 30.0;
        String query = "SELECT id, flight_id, name, length, valid_length, min, avg, max, `values` FROM double_series WHERE name = ? AND (min <= ? OR max >= ?)";

        PreparedStatement preparedStatement = connection.prepareStatement(query);

        System.out.println(preparedStatement);
        preparedStatement.setString(1, columnName);
        preparedStatement.setDouble(2, minValue);
        preparedStatement.setDouble(3, maxValue);
        System.out.println(preparedStatement);

        ResultSet resultSet = preparedStatement.executeQuery();

        while (resultSet.next()) {
            DoubleTimeSeries timeSeries = new DoubleTimeSeries(resultSet);

            System.out.println(timeSeries);
            System.out.println();
            System.out.println();
        }

        System.err.println("finished!");
    }
}
