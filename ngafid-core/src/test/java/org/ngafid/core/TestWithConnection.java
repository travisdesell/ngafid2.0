package org.ngafid.core;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import java.sql.Connection;
import java.sql.SQLException;

public class TestWithConnection {
    protected Connection connection;

    @BeforeEach
    public void init() throws SQLException {
        connection = H2Database.getConnection();
    }

    @AfterEach
    public void close() throws SQLException {
        connection.close();
    }
}
