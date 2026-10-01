package org.ngafid.core;

import java.sql.Connection;
import java.sql.SQLException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

public class TestWithConnection {
    protected Connection connection;

    /**
     * Opens a fresh H2 test connection before each test and stores it in {@link #connection} for the subclass to use.
     *
     * @throws SQLException if a connection cannot be obtained
     */
    @BeforeEach
    public void init() throws SQLException {
        connection = H2Database.getConnection();
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
