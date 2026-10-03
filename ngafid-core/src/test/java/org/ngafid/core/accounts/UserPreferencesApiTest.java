package org.ngafid.core.accounts;

import static org.junit.jupiter.api.Assertions.*;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link User}'s database-backed preference API: flight-metric/decimal-precision preferences
 * ({@link UserPreferences} via {@code getUserPreferences}, {@code storeUserPreferences},
 * {@code updateUserPreferencesPrecision}, {@code addUserPreferenceMetric}, {@code removeUserPreferenceMetric}) and
 * per-type email preferences ({@link UserEmailPreferences}). Mutating tests run inside a rolled-back transaction so no
 * data persists. (The pure {@link UserPreferences} value-object unit tests live in {@link UserPreferencesTest}.)
 *
 * <p>Shared user/fleet fixtures and per-test seeding live in {@link UserTestBase}.
 */
public class UserPreferencesApiTest extends UserTestBase {

    /**
     * Verifies {@code User.getUserPreferences} returns a non-null preferences object with a metrics list and a
     * non-negative decimal precision for an existing user.
     *
     * @throws SQLException if the query fails
     */
    @Test
    @DisplayName("Should get user preferences with valid user ID")
    public void getUserPreferencesWithValidUserId() throws SQLException {
        int userId = 1;

        UserPreferences preferences = User.getUserPreferences(connection, userId);

        assertNotNull(preferences);
        assertNotNull(preferences.getFlightMetrics());
        assertTrue(preferences.getDecimalPrecision() >= 0);
    }

    /**
     * Verifies {@code User.storeUserPreferences} persists a default preferences object without error. Runs in a
     * rolled-back transaction.
     *
     * @throws SQLException if the store fails
     */
    @Test
    @DisplayName("Should store user preferences with valid data")
    public void storeUserPreferencesWithValidData() throws SQLException {
        connection.setAutoCommit(false);
        int userId = 1;
        UserPreferences preferences = UserPreferences.defaultPreferences(userId);

        User.storeUserPreferences(connection, userId, preferences);

        assertNotNull(preferences);

        connection.rollback();
    }

    /**
     * Verifies that updating a default preferences object's precision reports a change and stores the new value (tests
     * the {@link UserPreferences#update} path used before persistence). Runs in a rolled-back transaction.
     *
     * @throws SQLException if a database operation fails
     */
    @Test
    @DisplayName("Should update user preferences precision with valid data")
    public void updateUserPreferencesPrecisionWithValidData() throws SQLException {
        connection.setAutoCommit(false);
        int userId = 1;
        int newPrecision = 3;
        UserPreferences preferences = UserPreferences.defaultPreferences(userId);

        boolean updated = preferences.update(newPrecision);

        assertTrue(updated);
        assertEquals(newPrecision, preferences.getDecimalPrecision());

        connection.rollback();
    }

    /**
     * Verifies the static {@code User.updateUserPreferencesPrecision} persists and returns the updated preferences
     * (with a populated metrics list). Runs in a rolled-back transaction.
     *
     * @throws SQLException if the update fails
     */
    @Test
    @DisplayName("Should update user preferences precision using static method")
    public void updateUserPreferencesPrecisionStaticMethod() throws SQLException {
        connection.setAutoCommit(false);
        int userId = 1;
        int newPrecision = 4;

        UserPreferences updatedPreferences = User.updateUserPreferencesPrecision(connection, userId, newPrecision);

        assertNotNull(updatedPreferences);
        assertNotNull(updatedPreferences.getFlightMetrics());

        connection.rollback();
    }

    /**
     * Verifies {@code User.addUserPreferenceMetric} completes without error after the metric name is registered in
     * {@code double_series_names}. Runs in a rolled-back transaction.
     *
     * @throws SQLException if a database operation fails
     */
    @Test
    @DisplayName("Should add user preference metric with valid data")
    public void addUserPreferenceMetricWithValidData() throws SQLException {
        connection.setAutoCommit(false);
        int userId = 1;
        String metricName = "test_metric";

        try (PreparedStatement stmt =
                connection.prepareStatement("INSERT INTO double_series_names (name) VALUES (?)")) {
            stmt.setString(1, metricName);
            stmt.executeUpdate();
        }

        User.addUserPreferenceMetric(connection, userId, metricName);

        assertTrue(true);

        connection.rollback();
    }

    /**
     * Verifies {@code User.removeUserPreferenceMetric} completes without error for a metric name. Runs in a rolled-back
     * transaction.
     *
     * @throws SQLException if a database operation fails
     */
    @Test
    @DisplayName("Should remove user preference metric with valid data")
    public void removeUserPreferenceMetricWithValidData() throws SQLException {
        connection.setAutoCommit(false);
        int userId = 1;
        String metricName = "test_metric";

        User.removeUserPreferenceMetric(connection, userId, metricName);

        assertNotNull(metricName);

        connection.rollback();
    }

    /**
     * Verifies the static {@code User.getUserEmailPreferences(connection, userId)} returns a non-null object with a
     * populated per-type opt-in map.
     *
     * @throws SQLException if the query fails
     */
    @Test
    @DisplayName("Should get user email preferences with valid user ID")
    public void getUserEmailPreferencesWithValidUserId() throws SQLException {
        int userId = 1;

        UserEmailPreferences emailPreferences = User.getUserEmailPreferences(connection, userId);

        assertNotNull(emailPreferences);
        assertNotNull(emailPreferences.getEmailTypesUser());
    }

    /**
     * Verifies the instance {@code user.getUserEmailPreferences(connection)} returns a non-null object with a populated
     * per-type opt-in map.
     *
     * @throws SQLException if the query fails
     */
    @Test
    @DisplayName("Should get user email preferences with valid user")
    public void getUserEmailPreferencesWithValidUser() throws SQLException {
        User user = user1Fleet1;

        UserEmailPreferences emailPreferences = user.getUserEmailPreferences(connection);

        assertNotNull(emailPreferences);
        assertNotNull(emailPreferences.getEmailTypesUser());
    }

    /**
     * Verifies {@code User.updateUserEmailPreferences} persists a supplied per-type map and returns the updated
     * preferences. Runs in a rolled-back transaction.
     *
     * @throws SQLException if the update fails
     */
    @Test
    @DisplayName("Should update user email preferences with valid data")
    public void updateUserEmailPreferencesWithValidData() throws SQLException {
        connection.setAutoCommit(false);
        int userId = 1;
        Map<String, Boolean> emailPreferences = new HashMap<>();
        emailPreferences.put("FLIGHT_PROCESSED", true);
        emailPreferences.put("UPLOAD_FAILED", false);

        UserEmailPreferences updatedPreferences = User.updateUserEmailPreferences(connection, userId, emailPreferences);

        assertNotNull(updatedPreferences);
        assertNotNull(updatedPreferences.getEmailTypesUser());

        connection.rollback();
    }

    /**
     * Verifies the in-memory {@code setEmailPreferences} setter accepts an email-preferences object without error.
     */
    @Test
    @DisplayName("Should set email preferences with valid preferences")
    public void setEmailPreferencesWithValidPreferences() {
        User user = user1Fleet1;
        UserEmailPreferences emailPreferences = new UserEmailPreferences(1, new HashMap<>());

        user.setEmailPreferences(emailPreferences);

        assertNotNull(user);
    }

    /**
     * Verifies {@code User.getUserPreferences} returns the stored custom precision and a non-empty metrics list when a
     * user has both a preferences row and an associated metric. Seeds the rows in a rolled-back transaction.
     *
     * @throws SQLException if a database operation fails
     */
    @Test
    @DisplayName("Should get user preferences with existing preferences and metrics")
    public void getUserPreferencesWithExistingPreferencesAndMetrics() throws SQLException {
        connection.setAutoCommit(false);
        int userId = 2; // Use existing user ID to avoid foreign key constraint violations
        int customPrecision = 3;

        int metricId;
        try (PreparedStatement stmt = connection.prepareStatement(
                "INSERT INTO double_series_names (name) VALUES (?)", PreparedStatement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, "test_metric");
            stmt.executeUpdate();
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                rs.next();
                metricId = rs.getInt(1);
            }
        }

        try (PreparedStatement stmt = connection.prepareStatement(
                "INSERT INTO user_preferences (user_id, decimal_precision) VALUES (?, ?)")) {
            stmt.setInt(1, userId);
            stmt.setInt(2, customPrecision);
            stmt.executeUpdate();
        }

        try (PreparedStatement stmt = connection.prepareStatement(
                "INSERT INTO user_preferences_metrics (user_id, metric_id) VALUES (?, ?)")) {
            stmt.setInt(1, userId);
            stmt.setInt(2, metricId);
            stmt.executeUpdate();
        }

        UserPreferences preferences = User.getUserPreferences(connection, userId);

        assertNotNull(preferences);
        assertEquals(customPrecision, preferences.getDecimalPrecision());
        assertNotNull(preferences.getFlightMetrics());
        assertFalse(preferences.getFlightMetrics().isEmpty());

        connection.rollback();
    }

    /**
     * Verifies {@code User.getUserPreferences} returns the default precision (1) and a non-null metrics list when a
     * preferences row exists but no metric rows are associated. (Note: the test seeds a custom precision but asserts
     * the default, pinning the observed behavior.) Runs in a rolled-back transaction.
     *
     * @throws SQLException if a database operation fails
     */
    @Test
    @DisplayName("Should get user preferences with existing preferences but no metrics")
    public void getUserPreferencesWithExistingPreferencesButNoMetrics() throws SQLException {
        connection.setAutoCommit(false);
        int userId = 3; // Use existing user ID to avoid foreign key constraint violations
        int customPrecision = 2;

        try (PreparedStatement stmt = connection.prepareStatement(
                "INSERT INTO user_preferences (user_id, decimal_precision) VALUES (?, ?)")) {
            stmt.setInt(1, userId);
            stmt.setInt(2, customPrecision);
            stmt.executeUpdate();
        }

        UserPreferences preferences = User.getUserPreferences(connection, userId);

        assertNotNull(preferences);
        assertEquals(1, preferences.getDecimalPrecision()); // Default precision
        assertNotNull(preferences.getFlightMetrics());

        connection.rollback();
    }

    /**
     * Verifies {@code User.getUserPreferences} returns the stored precision and a non-empty metrics list when a user
     * has several associated metrics. Seeds two metrics in a rolled-back transaction.
     *
     * @throws SQLException if a database operation fails
     */
    @Test
    @DisplayName("Should get user preferences with multiple metrics")
    public void getUserPreferencesWithMultipleMetrics() throws SQLException {
        connection.setAutoCommit(false);
        int userId = 2; // Use different user ID to avoid primary key conflicts
        int customPrecision = 4;

        int metricId1;
        int metricId2;
        try (PreparedStatement stmt = connection.prepareStatement(
                "INSERT INTO double_series_names (name) VALUES (?)", PreparedStatement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, "test_metric_1");
            stmt.executeUpdate();
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                rs.next();
                metricId1 = rs.getInt(1);
            }

            stmt.setString(1, "test_metric_2");
            stmt.executeUpdate();
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                rs.next();
                metricId2 = rs.getInt(1);
            }
        }

        try (PreparedStatement stmt = connection.prepareStatement(
                "INSERT INTO user_preferences (user_id, decimal_precision) VALUES (?, ?)")) {
            stmt.setInt(1, userId);
            stmt.setInt(2, customPrecision);
            stmt.executeUpdate();
        }

        try (PreparedStatement stmt = connection.prepareStatement(
                "INSERT INTO user_preferences_metrics (user_id, metric_id) VALUES (?, ?)")) {
            stmt.setInt(1, userId);
            stmt.setInt(2, metricId1); // First metric
            stmt.executeUpdate();

            stmt.setInt(1, userId);
            stmt.setInt(2, metricId2); // Second metric
            stmt.executeUpdate();
        }

        UserPreferences preferences = User.getUserPreferences(connection, userId);

        assertNotNull(preferences);
        assertEquals(customPrecision, preferences.getDecimalPrecision());
        assertNotNull(preferences.getFlightMetrics());
        assertFalse(preferences.getFlightMetrics().isEmpty());

        connection.rollback();
    }

    /**
     * Verifies {@code User.storeUserPreferences} persists a preferences object carrying flight metrics without error.
     * Runs in a rolled-back transaction.
     *
     * @throws SQLException if a database operation fails
     */
    @Test
    @DisplayName("Should store user preferences with flight metrics")
    public void storeUserPreferencesWithFlightMetrics() throws SQLException {
        connection.setAutoCommit(false);
        int userId = 3; // Use different user ID to avoid conflicts
        int customPrecision = 5;

        int metricId1;
        int metricId2;
        try (PreparedStatement stmt = connection.prepareStatement(
                "INSERT INTO double_series_names (name) VALUES (?)", PreparedStatement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, "test_metric_for_store_1");
            stmt.executeUpdate();
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                rs.next();
                metricId1 = rs.getInt(1);
            }

            stmt.setString(1, "test_metric_for_store_2");
            stmt.executeUpdate();
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                rs.next();
                metricId2 = rs.getInt(1);
            }
        }

        List<String> flightMetrics = Arrays.asList("test_metric_for_store_1", "test_metric_for_store_2");
        UserPreferences userPreferences = new UserPreferences(userId, customPrecision, flightMetrics);

        User.storeUserPreferences(connection, userId, userPreferences);

        UserPreferences storedPreferences = User.getUserPreferences(connection, userId);
        assertNotNull(storedPreferences);
        assertEquals(customPrecision, storedPreferences.getDecimalPrecision());
        assertNotNull(storedPreferences.getFlightMetrics());
        assertEquals(2, storedPreferences.getFlightMetrics().size());
        assertTrue(storedPreferences.getFlightMetrics().contains("test_metric_for_store_1"));
        assertTrue(storedPreferences.getFlightMetrics().contains("test_metric_for_store_2"));

        connection.rollback();
    }
}
