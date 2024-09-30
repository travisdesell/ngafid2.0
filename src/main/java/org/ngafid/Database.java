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

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

public class Database {

    private static HikariDataSource CONNECTION_POOL = null;
    private static boolean connectionInitiated;
    private static String dbHost = null, dbName = null, dbUser = null, dbPassword = null;

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

    public static boolean dbInfoExists() {
        return dbHost != null || dbName != null || dbUser != null || dbPassword != null;
    }

    public static String getDatabaseImplementation() {
        if (System.getenv("NGAFID_USE_MARIA_DB") != null)
            return "mariadb";
        else
            return "mysql";
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

    private static void initializeConnectionPool() {
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
        CONNECTION_POOL = new HikariDataSource(config);
    }
}