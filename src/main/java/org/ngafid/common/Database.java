package org.ngafid.common;

import java.io.*;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;
import java.util.logging.Logger;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;

public class Database {

    private static HikariDataSource CONNECTION_POOL = null;
    private static String dbUser = null;
    private static String dbPassword = null;
    private static String dbUrl = null;

    private static final Logger LOG = Logger.getLogger(Database.class.getName());

    static {
        initializeConnectionPool();
    }

    public static Connection getConnection() throws SQLException {
        var info = CONNECTION_POOL.getHikariPoolMXBean();
        LOG.info("Connection stats: " + info.getIdleConnections() + " idle / " + info.getActiveConnections()
                + " active / " + info.getTotalConnections() + " total");
        // new Throwable().printStackTrace();
        return CONNECTION_POOL.getConnection();
    }

    public static DataSource getDataSource() {
        return CONNECTION_POOL;
    }

    public static boolean dbInfoExists() {
        return dbUrl != null && dbUser != null && dbPassword != null;
    }

    public static String getDatabaseImplementation() {
        if (System.getenv("NGAFID_USE_MARIA_DB") != null)
            return "mariadb";
        else
            return "mysql";
    }

    private static void readDatabaseCredentials(String path) throws IOException {
        try (BufferedReader bufferedReader = new BufferedReader(new FileReader(path))) {
            Properties prop = new Properties();
            prop.load(bufferedReader);

            dbUser = prop.getProperty("username");
            dbPassword = prop.getProperty("password");
            dbUrl = prop.getProperty("url");
        }
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

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(dbUrl);
        config.setUsername(dbUser);
        config.setPassword(dbPassword);
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "64");
        config.addDataSourceProperty("prepStmtCacheSize", "64");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "512");
        config.setMaximumPoolSize(32);
        config.setMaxLifetime(60000);
        CONNECTION_POOL = new HikariDataSource(config);
    }
}