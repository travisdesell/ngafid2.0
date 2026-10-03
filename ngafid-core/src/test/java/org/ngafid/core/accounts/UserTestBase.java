package org.ngafid.core.accounts;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.ngafid.core.TestWithConnection;

/**
 * Shared test fixtures for the {@link User} test classes.
 *
 * <p>Seeds the user and {@code fleet_access} rows the {@code User*Test} classes depend on and loads a handful of
 * {@link User} fixtures spanning different user/fleet access combinations (granted, denied, and waiting) before each
 * test, restoring the baseline {@code fleet_access} rows afterward so tests stay independent. Extends
 * {@link TestWithConnection}, so every subclass test still receives a fresh {@code connection} against the
 * Testcontainers MySQL instance.
 */
public class UserTestBase extends TestWithConnection {

    protected User user1Fleet1;
    protected User user1Fleet2;
    protected User user2Fleet1;
    protected User user2Fleet2;
    protected User user3fleet1; // User 3 with DENIED access to fleet 1
    protected User user1Fleet2Waiting; // User 1 with WAITING access to fleet 2

    /**
     * Seeds the shared test data and loads the handful of {@link User} fixtures (various user/fleet access
     * combinations) that the tests reference, before each test runs.
     *
     * @throws SQLException if seeding or loading a user fails
     * @throws AccountException if a fixture user has no valid fleet access
     */
    @BeforeEach
    public void initUsers() throws SQLException, AccountException {
        // Set up test data first
        setupTestData();

        // Then get the users
        user1Fleet1 = User.get(connection, 1, 1);
        user1Fleet2 = User.get(connection, 1, 2);
        user2Fleet1 = User.get(connection, 2, 1);
        user2Fleet2 = User.get(connection, 2, 2);
        user3fleet1 = User.get(connection, 3, 1); // User 3 with DENIED access to fleet 1
        user1Fleet2Waiting = User.get(connection, 1, 2); // User 1 with WAITING access to fleet 2
    }

    private void setupTestData() throws SQLException {

        // Set up additional test users for equals method testing (new range)
        try (PreparedStatement stmt = connection.prepareStatement(
                "INSERT INTO user (id, email, first_name, last_name, address, city, country, state, "
                        + "zip_code, phone_number, reset_phrase, registration_time, admin, aggregate_view, "
                        + "password_token, last_login_time, fleet_selected, two_factor_enabled, "
                        + "two_factor_secret, backup_codes, two_factor_setup_complete) VALUES "
                        + "(1999, 'test1999@example.com', 'Test', 'User1999', '123 Test St', "
                        + "'Test City', 'Test Country', 'TS', '12345', '555-123-4567', 'reset', "
                        + "NOW(), 0, 0, 'nnnnnnnnnnnnnnnnnnnn', NOW(), 0, 0, 'secret', 'codes', 0), "
                        + "(1998, 'test1998@example.com', 'Test', 'User1998', '123 Test St', "
                        + "'Test City', 'Test Country', 'TS', '12345', '555-123-4567', 'reset', "
                        + "NOW(), 0, 0, 'oooooooooooooooooooo', NOW(), 0, 0, 'secret', 'codes', 0), "
                        + "(1997, 'test1997@example.com', 'Test', 'User1997', '123 Test St', "
                        + "'Test City', 'Test Country', 'TS', '12345', '555-123-4567', 'reset', "
                        + "NOW(), 0, 0, 'pppppppppppppppppppp', NOW(), 0, 0, 'secret', 'codes', 0), "
                        + "(1996, 'test1996@example.com', 'Test', 'User1996', '123 Test St', "
                        + "'Test City', 'Test Country', 'TS', '12345', '555-123-4567', 'reset', "
                        + "NOW(), 0, 0, 'qqqqqqqqqqqqqqqqqqqq', NOW(), 0, 0, 'secret', 'codes', 0), "
                        + "(1995, 'test1995@example.com', 'Test', 'User1995', '123 Test St', "
                        + "'Test City', 'Test Country', 'TS', '12345', '555-123-4567', 'reset', "
                        + "NOW(), 0, 0, 'rrrrrrrrrrrrrrrrrrrr', NOW(), 0, 0, 'secret', 'codes', 0), "
                        + "(1994, 'test1994@example.com', 'Test', 'User1994', '123 Test St', "
                        + "'Test City', 'Test Country', 'TS', '12345', '555-123-4567', 'reset', "
                        + "NOW(), 0, 0, 'ssssssssssssssssssss', NOW(), 0, 0, 'secret', 'codes', 0), "
                        + "(1993, 'test1993@example.com', 'Test', 'User1993', '123 Test St', "
                        + "'Test City', 'Test Country', 'TS', '12345', '555-123-4567', 'reset', "
                        + "NOW(), 0, 0, 'tttttttttttttttttttt', NOW(), 0, 0, 'secret', 'codes', 0), "
                        + "(1992, 'test1992@example.com', 'Test', 'User1992', '123 Test St', "
                        + "'Test City', 'Test Country', 'TS', '12345', '555-123-4567', 'reset', "
                        + "NOW(), 0, 0, 'uuuuuuuuuuuuuuuuuuuu', NOW(), 0, 0, 'secret', 'codes', 0), "
                        + "(1991, 'test1991@example.com', 'Test', 'User1991', '123 Test St', "
                        + "'Test City', 'Test Country', 'TS', '12345', '555-123-4567', 'reset', "
                        + "NOW(), 0, 0, 'vvvvvvvvvvvvvvvvvvvv', NOW(), 0, 0, 'secret', 'codes', 0), "
                        + "(1990, 'test1990@example.com', 'Test', 'User1990', '123 Test St', "
                        + "'Test City', 'Test Country', 'TS', '12345', '555-123-4567', 'reset', "
                        + "NOW(), 0, 0, 'wwwwwwwwwwwwwwwwwwww', NOW(), 0, 0, 'secret', 'codes', 0), "
                        + "(1989, 'test1989@example.com', 'Test', 'User1989', '123 Test St', "
                        + "'Test City', 'Test Country', 'TS', '12345', '555-123-4567', 'reset', "
                        + "NOW(), 0, 0, 'xxxxxxxxxxxxxxxxxxxx', NOW(), 0, 0, 'secret', 'codes', 0), "
                        + "(1988, 'test1988@example.com', 'Test', 'User1988', '123 Test St', "
                        + "'Test City', 'Test Country', 'TS', '12345', '555-123-4567', 'reset', "
                        + "NOW(), 0, 0, 'yyyyyyyyyyyyyyyyyyyy', NOW(), 0, 0, 'secret', 'codes', 0), "
                        + "(1987, 'test1987@example.com', 'Test', 'User1987', '123 Test St', "
                        + "'Test City', 'Test Country', 'TS', '12345', '555-123-4567', 'reset', "
                        + "NOW(), 0, 0, 'zzzzzzzzzzzzzzzzzzzz', NOW(), 0, 0, 'secret', 'codes', 0) "
                        + "ON DUPLICATE KEY UPDATE email = VALUES(email)")) {
            stmt.executeUpdate();
        }

        // Set up additional test users for equals method testing (3000+ range)
        try (PreparedStatement stmt = connection.prepareStatement(
                "INSERT INTO user (id, email, first_name, last_name, address, city, country, state, "
                        + "zip_code, phone_number, reset_phrase, registration_time, admin, aggregate_view, "
                        + "password_token, last_login_time, fleet_selected, two_factor_enabled, "
                        + "two_factor_secret, backup_codes, two_factor_setup_complete) VALUES "
                        + "(2999, 'test2999@example.com', 'Test', 'User2999', '123 Test St', "
                        + "'Test City', 'Test Country', 'TS', '12345', '555-123-4567', 'reset', "
                        + "NOW(), 0, 0, 'aaaaaaaaaaaaaaaaaaaa', NOW(), 0, 0, 'secret', 'codes', 0), "
                        + "(2998, 'test2998@example.com', 'Test', 'User2998', '123 Test St', "
                        + "'Test City', 'Test Country', 'TS', '12345', '555-123-4567', 'reset', "
                        + "NOW(), 0, 0, 'bbbbbbbbbbbbbbbbbbbb', NOW(), 0, 0, 'secret', 'codes', 0), "
                        + "(2997, 'test2997@example.com', 'Test', 'User2997', '123 Test St', "
                        + "'Test City', 'Test Country', 'TS', '12345', '555-123-4567', 'reset', "
                        + "NOW(), 0, 0, 'cccccccccccccccccccc', NOW(), 0, 0, 'secret', 'codes', 0), "
                        + "(2996, 'test2996@example.com', 'Test', 'User2996', '123 Test St', "
                        + "'Test City', 'Test Country', 'TS', '12345', '555-123-4567', 'reset', "
                        + "NOW(), 0, 0, 'dddddddddddddddddddd', NOW(), 0, 0, 'secret', 'codes', 0), "
                        + "(2995, 'test2995@example.com', 'Test', 'User2995', '123 Test St', "
                        + "'Test City', 'Test Country', 'TS', '12345', '555-123-4567', 'reset', "
                        + "NOW(), 0, 0, 'eeeeeeeeeeeeeeeeeeee', NOW(), 0, 0, 'secret', 'codes', 0), "
                        + "(2994, 'test2994@example.com', 'Test', 'User2994', '123 Test St', "
                        + "'Test City', 'Test Country', 'TS', '12345', '555-123-4567', 'reset', "
                        + "NOW(), 0, 0, 'ffffffffffffffffffff', NOW(), 0, 0, 'secret', 'codes', 0), "
                        + "(2993, 'test2993@example.com', 'Test', 'User2993', '123 Test St', "
                        + "'Test City', 'Test Country', 'TS', '12345', '555-123-4567', 'reset', "
                        + "NOW(), 0, 0, 'gggggggggggggggggggg', NOW(), 0, 0, 'secret', 'codes', 0), "
                        + "(2992, 'test2992@example.com', 'Test', 'User2992', '123 Test St', "
                        + "'Test City', 'Test Country', 'TS', '12345', '555-123-4567', 'reset', "
                        + "NOW(), 0, 0, 'hhhhhhhhhhhhhhhhhhhh', NOW(), 0, 0, 'secret', 'codes', 0), "
                        + "(2991, 'test2991@example.com', 'Test', 'User2991', '123 Test St', "
                        + "'Test City', 'Test Country', 'TS', '12345', '555-123-4567', 'reset', "
                        + "NOW(), 0, 0, 'iiiiiiiiiiiiiiiiiiii', NOW(), 0, 0, 'secret', 'codes', 0), "
                        + "(2990, 'test2990@example.com', 'Test', 'User2990', '123 Test St', "
                        + "'Test City', 'Test Country', 'TS', '12345', '555-123-4567', 'reset', "
                        + "NOW(), 0, 0, 'jjjjjjjjjjjjjjjjjjjj', NOW(), 0, 0, 'secret', 'codes', 0), "
                        + "(2989, 'test2989@example.com', 'Test', 'User2989', '123 Test St', "
                        + "'Test City', 'Test Country', 'TS', '12345', '555-123-4567', 'reset', "
                        + "NOW(), 0, 0, 'kkkkkkkkkkkkkkkkkkkk', NOW(), 0, 0, 'secret', 'codes', 0), "
                        + "(2988, 'test2988@example.com', 'Test', 'User2988', '123 Test St', "
                        + "'Test City', 'Test Country', 'TS', '12345', '555-123-4567', 'reset', "
                        + "NOW(), 0, 0, 'llllllllllllllllllll', NOW(), 0, 0, 'secret', 'codes', 0), "
                        + "(2987, 'test2987@example.com', 'Test', 'User2987', '123 Test St', "
                        + "'Test City', 'Test Country', 'TS', '12345', '555-123-4567', 'reset', "
                        + "NOW(), 0, 0, 'mmmmmmmmmmmmmmmmmmmm', NOW(), 0, 0, 'secret', 'codes', 0) "
                        + "ON DUPLICATE KEY UPDATE email = VALUES(email)")) {
            stmt.executeUpdate();
        }

        // Set up fleet_access records for main test users
        try (PreparedStatement stmt =
                connection.prepareStatement("INSERT INTO fleet_access (user_id, fleet_id, type) VALUES "
                        + "(1, 1, 'VIEW'), (2, 1, 'MANAGER'), (1, 2, 'WAITING'), "
                        + "(2, 2, 'WAITING'), (3, 1, 'DENIED'), (3, 2, 'DENIED') "
                        + "ON DUPLICATE KEY UPDATE type = VALUES(type)")) {
            stmt.executeUpdate();
        }

        // Set up fleet_access records for test users (all with VIEW access to fleet 1)
        try (PreparedStatement stmt =
                connection.prepareStatement("INSERT INTO fleet_access (user_id, fleet_id, type) VALUES "
                        + "(1999, 1, 'VIEW'), (1998, 1, 'VIEW'), (1997, 1, 'VIEW'), (1996, 1, 'VIEW'),"
                        + " (1995, 1, 'VIEW'), "
                        + "(1994, 1, 'VIEW'), (1993, 1, 'VIEW'), (1992, 1, 'VIEW'), (1991, 1, 'VIEW'),"
                        + " (1990, 1, 'VIEW'), "
                        + "(1989, 1, 'VIEW'), (1988, 1, 'VIEW'), (1987, 1, 'VIEW'), "
                        + "(2999, 1, 'VIEW'), (2998, 1, 'VIEW'), (2997, 1, 'VIEW'), (2996, 1, 'VIEW'),"
                        + " (2995, 1, 'VIEW'), "
                        + "(2994, 1, 'VIEW'), (2993, 1, 'VIEW'), (2992, 1, 'VIEW'), (2991, 1, 'VIEW'),"
                        + " (2990, 1, 'VIEW'), "
                        + "(2989, 1, 'VIEW'), (2988, 1, 'VIEW'), (2987, 1, 'VIEW') "
                        + "ON DUPLICATE KEY UPDATE type = VALUES(type)")) {
            stmt.executeUpdate();
        }
    }

    /**
     * Cleans up fleet-access and other records created during a test after it runs, restoring the baseline test data so
     * tests remain independent.
     *
     * @throws SQLException if a cleanup statement fails
     */
    @AfterEach
    public void cleanupTestData() throws SQLException {
        // Clean up any fleet access records created during tests
        // but preserve the original test data by restoring it
        try (PreparedStatement stmt =
                connection.prepareStatement("DELETE FROM fleet_access WHERE user_id IN (1, 2, 3)")) {
            stmt.executeUpdate();
        }

        // Restore the original test data
        try (PreparedStatement stmt = connection.prepareStatement(
                "INSERT INTO fleet_access (user_id, fleet_id, type) VALUES (1, 1, 'VIEW'), (2, 1, 'MANAGER'), "
                        + "(1, 2, 'WAITING'), (2, 2, 'WAITING'), (3, 1, 'DENIED'), (3, 2, 'DENIED')")) {
            stmt.executeUpdate();
        }
    }
}
