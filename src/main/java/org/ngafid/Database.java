package org.ngafid;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Logger;

public class Database {
    private static final Logger LOG = Logger.getLogger(Database.class.getName());

    private static HikariDataSource CONNECTION_POOL = null;
    private static String dbHost = null;
    private static String dbName = null;
    private static String dbUser = null;
    private static String dbPassword = null;


    static {
        initializeConnectionPool();
    }

    /**
     * Get a connection to the database
     *
     * @return a connection to the database
     * @throws SQLException SQL exception
     */
    public static Connection getConnection() throws SQLException {
        var info = CONNECTION_POOL.getHikariPoolMXBean();
        LOG.info("Connection stats: " + info.getIdleConnections() +
                " idle / " + info.getActiveConnections() +
                " active / " + info.getTotalConnections() + " total");
        // new Throwable().printStackTrace();
        return CONNECTION_POOL.getConnection();
    }

    /**
     * Check if the database information exists
     *
     * @return true if the database information exists
     */
    public static boolean dbInfoExists() {
        return dbHost != null || dbName != null || dbUser != null || dbPassword != null;
    }

    /**
     * Get the database implementation
     *
     * @return the database implementation
     */
    public static String getDatabaseImplementation() {
        if (System.getenv("NGAFID_USE_MARIA_DB") != null) {
            return "mariadb";
        } else {
            return "mysql";
        }
    }

    private static void readDatabaseCredentials(String path) throws IOException {
        try (BufferedReader bufferedReader = new BufferedReader(new FileReader(path))) {
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

            // Don't remove this!
            bufferedReader.close();
        }
    }

    /**
     * Get the Hikari configuration
     *
     * @return the Hikari configuration
     */
    private static HikariConfig getHikariConfig() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:" + getDatabaseImplementation() + "://" + dbHost + "/" + dbName);
        config.setUsername(dbUser);
        config.setPassword(dbPassword);
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "64");
        config.addDataSourceProperty("prepStmtCacheSize", "64");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        config.setMaximumPoolSize(32);
        config.setMaxLifetime(60000);
        return config;
    }

    /**
     * Initialize the connection pool
     */
    private static void initializeConnectionPool() {
        if (System.getenv("NGAFID_DB_INFO") == null) {
            LOG.severe("ERROR: 'NGAFID_DB_INFO' environment variable not specified at runtime.");
            LOG.severe("Please add the following to your ~/.bash_rc or ~/.profile file:");
            LOG.severe("export NGAFID_DB_INFO=<path/to/db_info_file>");
            System.exit(1);
        }

        String dbInfoPath = System.getenv("NGAFID_DB_INFO");

        if (!dbInfoExists()) {
            try {
                readDatabaseCredentials(dbInfoPath);
            } catch (IOException e) {
                LOG.severe("Error reading from NGAFID_DB_INFO: '" + dbInfoPath + "'");
                e.printStackTrace();
                System.exit(1);
            }
        }

        HikariConfig config = getHikariConfig();
        CONNECTION_POOL = new HikariDataSource(config);
    }
}
