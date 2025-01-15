package org.ngafid;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.logging.Logger;


public class Database {
    private static ThreadLocal<Connection> connection = new ThreadLocal<>();
    private static boolean connectionInitiated;
    private static String dbHost = "", dbName = "", dbUser = "", dbPassword = "";

    private static final Logger LOG = Logger.getLogger(Database.class.getName());

    private static final long HOUR = 3600000;

    public static Connection getConnection() { 

        try {
            
            Connection c = connection.get();

            /* Investigate further here */ 
            if (c == null || c.isClosed())
                setConnection();

        } catch (SQLException e) {
            e.printStackTrace();
            System.exit(1);
        }

        return connection.get();
        
    }

    public static boolean dbInfoExists() {
        return !dbHost.isBlank() && !dbName.isBlank() && !dbUser.isBlank() && !dbPassword.isBlank();
    }

    public static Connection resetConnection() {
        
        try {

            Connection c = connection.get();

            //Connection exists, force close it
            if (c != null)
                c.close();

            setConnection();

        } catch (SQLException e) {
            e.printStackTrace();
            System.exit(1);
        }

        return connection.get();

    }

    public static Connection resetConnection(Logger loggerIn) {

        loggerIn.info("Database.java -- Resetting connection...");

        try {
            Connection c = connection.get();
            loggerIn.info("Database.java -- Finished 'connection.get' (A)");

            if (c != null) {
                loggerIn.info("Database.java -- Connection was not null, closing...");
                c.close();
            }
            setConnection(loggerIn);
            loggerIn.info("Database.java -- Finished 'setConnection'...");

        } catch (SQLException e) {
            loggerIn.severe("Database.java -- " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }

        Connection c = connection.get();
        loggerIn.info("Database.java -- Finished 'connection.get' (B)");

        return c;
    }


    private static void setConnection(Logger loggerIn) {
        if (System.getenv("NGAFID_DB_INFO") == null) {
            loggerIn.severe("ERROR: 'NGAFID_DB_INFO' environment variable not specified at runtime.");
            loggerIn.severe("Please add the following to your ~/.bash_rc or ~/.profile file:");
            loggerIn.severe("export NGAFID_DB_INFO=<path/to/db_info_file>");
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

                loggerIn.info("dbHost: '" + dbHost + "'");
                loggerIn.info("dbName: '" + dbName + "'");
                loggerIn.info("dbUser: '" + dbUser + "'");
                loggerIn.info("dbPassword: '" + dbPassword + "'");

                //Don't remove this!
                bufferedReader.close();
            }

        } catch (IOException e) {
            loggerIn.severe("Error reading from NGAFID_DB_INFO: '" + NGAFID_DB_INFO + "'");
            e.printStackTrace();
            System.exit(1);
        }

        try {

            loggerIn.info("Database.java -- Loading MySQL driver...");

            // This will load the MySQL driver, each DB has its own driver
            Class.forName("com.mysql.cj.jdbc.Driver");

            loggerIn.info("Database.java -- MySQL driver loaded, creating connection properties...");

            java.util.Properties connProperties = new java.util.Properties();
            connProperties.put("user", dbUser);
            connProperties.put("password", dbPassword);

            // set additional connection properties:
            // if connection stales, then make automatically
            // reconnect; make it alive again;
            // if connection stales, then try for reconnection;
            connProperties.put("autoReconnect", "true");
            connProperties.put("maxReconnects", "5");
            connection.set(DriverManager.getConnection("jdbc:mysql://" + dbHost + "/" + dbName, connProperties));
        } catch (Exception e) {
            loggerIn.severe(e.getMessage());
            System.exit(1);
        }

        /*
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
                    } catch (Throwable t) {
                        t.printStackTrace();
                    }
                }
            }).start();
        }
        */
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
            connection.set(DriverManager.getConnection("jdbc:mysql://" + dbHost + "/" + dbName, connProperties));
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }

        /*
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
                    } catch (Throwable t) {
                        t.printStackTrace();
                    }
                }
            }).start();
        }
        */
    }
}
