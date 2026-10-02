package org.ngafid.core;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import liquibase.Contexts;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.LiquibaseException;
import liquibase.resource.DirectoryResourceAccessor;
import org.testcontainers.containers.MySQLContainer;

/**
 * Supplies pooled connections to an ephemeral MySQL database for tests.
 *
 * <p>A single MySQL container -- the same {@code mysql:8.0} image used in production -- is started once in a static
 * initializer and shared by every test in the JVM; Testcontainers' Ryuk reaper stops and removes it when the JVM
 * exits. The production Liquibase changelog is then applied to that container, so the test schema matches production
 * exactly. This replaced an in-memory H2 database, which only emulated MySQL and could not evaluate the MySQL-specific
 * migrations (e.g. {@code information_schema} preconditions) in the production changelog.
 *
 * <p>Because it runs a container, a working Docker engine must be available wherever the tests run (developer machines
 * and CI). The container, connection pool, and migrated schema are all created lazily the first time this class is
 * referenced.
 */
public final class TestDatabase {

    /** Production MySQL image; kept in sync with docker-compose so tests mirror production. */
    private static final String MYSQL_IMAGE = "mysql:8.0";

    // JVM-lifetime singleton: never closed explicitly because Testcontainers' Ryuk reaper removes it at JVM exit.
    @SuppressWarnings("resource")
    private static final MySQLContainer<?> MYSQL = new MySQLContainer<>(MYSQL_IMAGE)
            .withDatabaseName("ngafid")
            .withUsername("ngafid")
            .withPassword("ngafid");

    private static HikariDataSource CONNECTION_POOL = null;

    private TestDatabase() {
        // Private constructor to hide the implicit public one
    }

    static {
        MYSQL.start();
        createConnectionPool();
        try {
            populateDatabase();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Borrows a connection from the test database connection pool. The MySQL container, pool, and schema are created
     * once in a static initializer (a MySQL container populated by running the Liquibase test changelog), so tests
     * share one migrated database.
     *
     * @return a pooled connection to the test database
     * @throws SQLException if a connection cannot be obtained from the pool
     */
    public static Connection getConnection() throws SQLException {
        return CONNECTION_POOL.getConnection();
    }

    /**
     * Builds the HikariCP pool pointed at the running MySQL container, using the container's generated JDBC URL and
     * credentials. Stores the pool in {@link #CONNECTION_POOL}.
     */
    private static void createConnectionPool() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(MYSQL.getJdbcUrl());
        config.setUsername(MYSQL.getUsername());
        config.setPassword(MYSQL.getPassword());
        config.setPoolName("TestPool");

        CONNECTION_POOL = new HikariDataSource(config);
    }

    /**
     * Applies the Liquibase test changelog ({@code ngafid-db/src/test-changelog-root.xml}, which includes the
     * production schema changesets plus test seed data) to the container, leaving a fully migrated schema. Runs once
     * from the static initializer.
     *
     * @throws SQLException if a connection to the container cannot be obtained
     */
    private static void populateDatabase() throws SQLException {
        try (Connection connection = CONNECTION_POOL.getConnection()) {
            Database database =
                    DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnection(connection));

            Liquibase liquibase = new Liquibase(
                    "ngafid-db/src/test-changelog-root.xml", new DirectoryResourceAccessor(Path.of("../")), database);

            liquibase.update(new Contexts());
        } catch (LiquibaseException | FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
}
