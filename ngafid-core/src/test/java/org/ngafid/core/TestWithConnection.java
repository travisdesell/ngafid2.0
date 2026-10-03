package org.ngafid.core;

import java.sql.Connection;
import java.sql.SQLException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

/**
 * Base class for tests that need a database connection, managing one against the Testcontainers MySQL instance.
 *
 * <p>It opens a fresh {@link TestDatabase} connection before each test and closes it afterward, exposing it to
 * subclasses via the protected {@code connection} field.
 */
public class TestWithConnection {
    protected Connection connection;

    /**
     * Opens a fresh test connection before each test and stores it in {@link #connection} for the subclass to use.
     *
     * @throws SQLException if a connection cannot be obtained
     */
    @BeforeEach
    public void init() throws SQLException {
        connection = TestDatabase.getConnection();
    }

    /**
     * Closes the per-test connection after each test, returning it to the pool.
     *
     * @throws SQLException if closing the connection fails
     */
    @AfterEach
    public void close() throws SQLException {
        connection.close();
    }
}
