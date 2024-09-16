package org.ngafid;

import java.io.*;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.lang.Thread;
import java.lang.Runnable;

import java.util.logging.LogManager;
import java.util.logging.Logger;


public class Database {
    private static ThreadLocal<Connection> connection = new ThreadLocal<>();
    private static boolean connectionInitiated;
    private static String dbHost = null, dbName = null, dbUser = null, dbPassword = null;

    private static final Logger LOG = Logger.getLogger(Database.class.getName());

    public static Connection getConnection() { 
        try {
            Connection c = connection.get();
            if (c == null || c.isClosed()) { //investigate further here 
                setConnection();
            } 
        } catch (SQLException e) {
            e.printStackTrace();
            System.exit(1);
        }

        return connection.get();
    }

    public static boolean dbInfoExists() {
        return dbHost != null || dbName != null || dbUser != null || dbPassword != null;
    }

    public static Connection resetConnection() {
        try {
            Connection c = connection.get();
            if (c != null) c.close();
            setConnection();
        } catch (SQLException e) {
            e.printStackTrace();
            System.exit(1);
        }
        return connection.get();
    }

    public static Connection createConnection(String dbUser, String dbName, String dbHost, String dbPassword) {
        String db_impl = "mysql";
        if (System.getenv("NGAFID_USE_MARIA_DB") != null)
            db_impl = "mariadb";

        try {
            java.util.Properties connProperties = new java.util.Properties();
            connProperties.put("user", dbUser);
            connProperties.put("password", dbPassword);

            // set additional connection properties:
            // if connection stales, then make automatically
            // reconnect; make it alive again;
            // if connection stales, then try for reconnection;
            connProperties.put("autoReconnect", "true");
            connProperties.put("maxReconnects", "5");
            var connection = DriverManager.getConnection("jdbc:" + db_impl + "://" + dbHost + "/" + dbName + "?useServerPrepStmts=false&rewriteBatchedStatements=true", connProperties);
            return connection;
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
            return null;
        }
    }
    
    private static String DB_NAME = null;
    private static String DB_HOST = null;
    private static String DB_USER = null;
    private static String DB_PASS = null;

    private static void readDatabaseCredentials(String path) throws IOException {
        try(BufferedReader bufferedReader = new BufferedReader(new FileReader(path))) {
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

            //Don't remove this!
            bufferedReader.close();
        }
    }

    private static void setConnection() {
        if (System.getenv("NGAFID_DB_INFO") == null) {
            System.err.println("ERROR: 'NGAFID_DB_INFO' environment variable not specified at runtime.");
            System.err.println("Please add the following to your ~/.bash_rc or ~/.profile file:");
            System.err.println("export NGAFID_DB_INFO=<path/to/db_info_file>");
            System.exit(1);
        }

        String dbInfoPath = System.getenv("NGAFID_DB_INFO");

        if (!dbInfoExists()) {
            try {
                readDatabaseCredentials(dbInfoPath);
            } catch (IOException e) {
                System.err.println("Error reading from NGAFID_DB_INFO: '" + dbInfoPath + "'");
                e.printStackTrace();
                System.exit(1);
            }
        }

        connection.set(createConnection(dbUser, dbName, dbHost, dbPassword));
    }
}
