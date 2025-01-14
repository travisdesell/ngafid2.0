package org.ngafid;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.logging.Logger;

public final class Database {
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
        return List.of(dbHost, dbName, dbUser, dbPassword).stream().anyMatch(Objects::isNull);
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
            Properties prop = new Properties();
            prop.load(bufferedReader);

            dbUser = prop.getProperty("username");
            dbName = prop.getProperty("name");
            dbPassword = prop.getProperty("password");
            dbHost = prop.getProperty("url");
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
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "512");
        config.setMaximumPoolSize(32);
        config.setMaxLifetime(60000);
        return config;
    }

    private static void initializeConnectionPool() {
        String dbInfoPath = "src/main/resources/liquibase.properties";

        if (!dbInfoExists()) {
            try {
                readDatabaseCredentials(dbInfoPath);
            } catch (IOException e) {
                System.err.println("Error reading from NGAFID_DB_INFO: '" + dbInfoPath + "'");
                e.printStackTrace();
                System.exit(1);
            }
        }

        HikariConfig config = getHikariConfig();
        CONNECTION_POOL = new HikariDataSource(config);
    }
}
