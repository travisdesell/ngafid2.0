package org.ngafid.core;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;
import java.util.logging.Logger;
import javax.sql.DataSource;

/**
 * Central access point for the NGAFID relational database, wrapping a shared HikariCP connection pool.
 *
 * <p>The pool is configured from {@link Config} and initialized once in a static block; callers borrow connections
 * via {@link #getConnection()} and are responsible for closing them so they return to the pool.
 */
public final class Database {

    private static HikariDataSource CONNECTION_POOL = null;
    private static String dbUser = null;
    private static String dbPassword = null;
    private static String dbUrl = null;

    private static final Logger LOG = Logger.getLogger(Database.class.getName());

    static {
        initializeConnectionPool();
    }

    private Database() {}

    /**
     * Borrows a connection from the shared HikariCP pool. The caller is responsible for closing it (which returns it to
     * the pool rather than physically closing it).
     *
     * @return a pooled database connection
     * @throws SQLException if a connection cannot be obtained from the pool
     */
    public static Connection getConnection() throws SQLException {
        var info = CONNECTION_POOL.getHikariPoolMXBean();
        // LOG.info("Connection stats: " + info.getIdleConnections() + " idle / " + info.getActiveConnections()
        //         + " active / " + info.getTotalConnections() + " total");
        return CONNECTION_POOL.getConnection();
    }

    public static DataSource getDataSource() {
        return CONNECTION_POOL;
    }

    /**
     * Reports whether the database credentials (URL, username, and password) have all been loaded.
     *
     * @return true if the URL, username, and password are all set
     */
    public static boolean dbInfoExists() {
        return dbUrl != null && dbUser != null && dbPassword != null;
    }

    /**
     * Returns the name of the configured database implementation, used to select dialect-specific behavior.
     *
     * @return {@code "mariadb"} when MariaDB is configured, otherwise {@code "mysql"}
     */
    public static String getDatabaseImplementation() {
        if (Config.NGAFID_USE_MARIA_DB) return "mariadb";
        else return "mysql";
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
        String dbInfoPath = Config.NGAFID_DB_INFO;

        if (!dbInfoExists()) {
            LOG.info("db path = " + dbInfoPath);
            try {
                readDatabaseCredentials(dbInfoPath);
            } catch (IOException e) {
                System.err.println("Error reading from NGAFID_DB_INFO: '" + dbInfoPath + "'");
                System.exit(1);
            }
        }

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(dbUrl);
        config.setUsername(dbUser);
        config.setPassword(dbPassword);
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "256");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        config.setMaximumPoolSize(32);
        config.setMinimumIdle(1);
        config.setMaxLifetime(1_800_000);
        config.setIdleTimeout(600_000);
        config.setKeepaliveTime(300_000);
        config.setValidationTimeout(5_000);
        CONNECTION_POOL = new HikariDataSource(config);
    }
}
