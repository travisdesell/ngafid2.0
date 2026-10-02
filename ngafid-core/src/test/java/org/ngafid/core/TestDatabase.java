package org.ngafid.core;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;
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
 *
 * <p>Some tests exercise production code paths that borrow connections from the production {@link org.ngafid.core.Database}
 * singleton (methods that take no {@code Connection} argument). To support those, {@link #configureProductionSingletons()}
 * generates a throwaway {@code ngafid.properties} pointing {@link org.ngafid.core.Config} and
 * {@link org.ngafid.core.Database} at this same container, so production code reaches the migrated test schema instead of
 * failing to initialize for lack of configuration. This wiring is installed from the static initializer, before any test
 * touches those singletons.
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
        configureProductionSingletons();
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

    /**
     * Generates a throwaway {@code ngafid.properties} in a temp directory and points the production
     * {@link org.ngafid.core.Config} and {@link org.ngafid.core.Database} singletons at it via the
     * {@code -Dngafid.config.file} system property, so production methods that borrow from the production
     * {@code Database} pool reach this container's migrated schema rather than failing to initialize.
     *
     * <p>The generated configuration points {@code ngafid.db.info} at a credentials file holding this container's JDBC
     * URL, user, and password; disables email; supplies writable temp directories for the file-system properties that
     * {@code Config} and {@code SendEmail} require; and deliberately leaves the Kafka config file missing so the email
     * pipeline fails fast with a {@code FileNotFoundException} (which the email-path tests tolerate) rather than
     * attempting real delivery. The email-info file is pre-created with a comment first line so {@code SendEmail}'s
     * static initializer treats email as unconfigured instead of trying (and possibly failing) to create it.
     *
     * <p>The {@code ngafid.config.file} property is always set here: these tests require the throwaway container, so the
     * production singletons must point at it regardless of any ambient configuration. It is set before any test touches
     * {@code Config}/{@code Database} because this runs from {@link TestDatabase}'s static initializer, which every
     * database-backed test triggers first.
     *
     * @throws RuntimeException if the temp configuration files cannot be written
     */
    private static void configureProductionSingletons() {
        try {
            Path baseDir = Files.createTempDirectory("ngafid-test-config");

            // Credentials file consumed by the production Database connection pool.
            Path dbInfo = baseDir.resolve("db-info.properties");
            Files.writeString(
                    dbInfo,
                    "username=" + MYSQL.getUsername() + "\n"
                            + "password=" + MYSQL.getPassword() + "\n"
                            + "url=" + MYSQL.getJdbcUrl() + "\n");

            // Directories that Config-driven code expects to exist.
            Path uploadDir = Files.createDirectories(baseDir.resolve("uploads"));
            Path archiveDir = Files.createDirectories(baseDir.resolve("archive"));
            Path staticDir = Files.createDirectories(baseDir.resolve("static"));
            Path terrainDir = Files.createDirectories(baseDir.resolve("terrain"));

            // A comment first line makes SendEmail treat email as unconfigured (username stays null) rather than
            // trying to create the file, which would System.exit on failure.
            Path emailInfo = baseDir.resolve("email.properties");
            Files.writeString(emailInfo, "# Email intentionally unconfigured for tests\n");

            Properties config = new Properties();
            config.setProperty("ngafid.db.info", dbInfo.toString());
            config.setProperty("ngafid.use.maria.db", "false");
            config.setProperty("ngafid.email.enabled", "false");
            config.setProperty("ngafid.admin.emails", "test@ngafid.org");
            config.setProperty("ngafid.upload.dir", uploadDir.toString());
            config.setProperty("ngafid.archive.dir", archiveDir.toString());
            config.setProperty("ngafid.static.dir", staticDir.toString());
            config.setProperty("ngafid.terrain.dir", terrainDir.toString());
            config.setProperty(
                    "ngafid.airports.file", baseDir.resolve("airports.csv").toString());
            config.setProperty(
                    "ngafid.runways.file", baseDir.resolve("runways.csv").toString());
            config.setProperty("ngafid.email.info", emailInfo.toString());
            // Deliberately absent: Kafka producer creation then fails fast with a FileNotFoundException, which the
            // email-path tests tolerate, instead of attempting real delivery.
            config.setProperty(
                    "ngafid.kafka.config.file",
                    baseDir.resolve("kafka.properties").toString());
            config.setProperty(
                    "ngafid.log.properties.file",
                    baseDir.resolve("log.properties").toString());

            Path configFile = baseDir.resolve("ngafid.properties");
            try (OutputStream out = Files.newOutputStream(configFile)) {
                config.store(out, "Generated test configuration for the NGAFID core test suite");
            }

            System.setProperty("ngafid.config.file", configFile.toString());
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate test configuration for production singletons", e);
        }
    }
}
