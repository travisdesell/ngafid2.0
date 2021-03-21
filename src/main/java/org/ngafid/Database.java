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


public class Database {
    private static Connection connection;

    public static Connection getConnection() { 
        try {
            if (connection.isClosed()) {
                setConnection();
            } 
        } catch (SQLException e) {
            e.printStackTrace();
            System.exit(1);
        }

        return connection;
    }

    private static void setConnection() {
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
            Class.forName("com.mysql.cj.jdbc.Driver");

            java.util.Properties connProperties = new java.util.Properties();
            connProperties.put("user", dbUser);
            connProperties.put("password", dbPassword);

            // set additional connection properties:
            // if connection stales, then make automatically
            // reconnect; make it alive again;
            // if connection stales, then try for reconnection;
            connProperties.put("autoReconnect", "true");
            connProperties.put("maxReconnects", "5");
            connection = DriverManager.getConnection("jdbc:mysql://" + dbHost + "/" + dbName, connProperties);

            // Setup the connection with the DB
            //connection = DriverManager.getConnection("jdbc:mysql://" + dbHost + "/" + dbName, dbUser, dbPassword);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    static {
        setConnection();
    }
}
