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
import java.util.Scanner;

public class Database {
    private static Connection connection;
    private static boolean connectionInitiated;
    private static String dbHost = "", dbName = "", dbUser = "", dbPassword = "";

    private static final Logger LOG = Logger.getLogger(Database.class.getName());

    private static final long HOUR = 3600000;
    private static final long DAY = 24 * HOUR;

    private static final boolean isDevInstance = Boolean.parseBoolean(System.getenv("NGAFID_DEV_INSTANCE"));

    public static Connection getConnection() { 
        try {
            if (connection.isClosed()) { //investigate further here 
                setConnection();
            } 
        } catch (SQLException e) {
            e.printStackTrace();
            System.exit(1);
        }

        return connection;
    }

    public static boolean dbInfoExists() {
        return !dbHost.isBlank() && !dbName.isBlank() && !dbUser.isBlank() && !dbPassword.isBlank();
    }

    public static Connection resetConnection() {
        try {
            if (connection != null) connection.close();
            setConnection();
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


        try {
            if (!dbInfoExists()) {
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

                //Don't remove this!
                bufferedReader.close();
            }

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

        if (!connectionInitiated) {
            connectionInitiated = true; 

            LOG.info("Log for SQL server poker, starting at: " + LocalDateTime.now().toString());

            new Thread(() -> {
                while (true) {
                    try {
                        Thread.sleep(HOUR); 

                        String dummyQuery = "SELECT id FROM event_definitions WHERE id <= 1";
                        PreparedStatement preparedStatement = getConnection().prepareStatement(dummyQuery);

                        ResultSet rs = preparedStatement.executeQuery();
                        rs.close();

                        LOG.info("Performed dummy query: " + dummyQuery + " at " + LocalDateTime.now().toString());
                    } catch (Exception e) {
                        LOG.severe(e.getMessage());
                    }
                }
            }).start();

            LOG.info("Log for SQL server backups, starting at: " + LocalDateTime.now().toString());

            if (!isDevInstance) {
                final int BACKUP_INTERVAL = Integer.parseInt(System.getenv("NGAFID_BACKUP_INTERVAL"));

                final String BACKUP_DIRECTORY = System.getenv("NGAFID_BACKUP_DIR");
                final String BACKUP_TABLES = System.getenv("NGAFID_BACKUP_TABLES");

                // Only perform backups on prod!
                new Thread(() -> {
                    while (true) {
                        try {
                            Runtime rt = Runtime.getRuntime();

                            LocalDateTime now = LocalDateTime.now();

                            String backupSubDir = "ngafid_backup_" + now.getDayOfMonth() + "_" + now.getMonthValue() + "_" + now.getYear();
                            String backupPrefix = BACKUP_DIRECTORY + "/" + backupSubDir;
                            File backupFile = new File(backupPrefix + "/backup.sql");

                            if (!backupFile.exists()) {
                                new File(backupPrefix).mkdir();

                                String dumpQueryString = "/usr/bin/mysqldump -u\'" + dbUser + "\' -p\'" + dbPassword + "\' -h\'" + dbHost + "\' " + dbName + " " + BACKUP_TABLES + " > " + backupFile.getPath();

                                String [] cmdArr = {"/bin/sh", "-c", dumpQueryString};
                                Process backup = rt.exec(cmdArr);

                                backup.waitFor();

                                LOG.info("Performed NGAFID backup with exit status " + backup.exitValue() + ", to file " + backupFile.getPath());
                            }

                            //Sleep for n days
                            Thread.sleep(BACKUP_INTERVAL * DAY);
                        } catch (Exception e) {
                            LOG.severe(e.getMessage());
                        }
                    }
                }).start();
            }
        }
    }

    static {
        setConnection();
    }
}
