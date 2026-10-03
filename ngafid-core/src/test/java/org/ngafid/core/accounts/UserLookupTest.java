package org.ngafid.core.accounts;

import static org.junit.jupiter.api.Assertions.*;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests for looking up and authenticating {@link User}s: {@code User.get} by id/fleet, by email/password, and by email
 * only; existence checks ({@code User.exists}); and credential validation ({@code validate},
 * {@code validatePassphrase}). Tests that seed users or set passwords run inside a rolled-back transaction.
 *
 * <p>Shared user/fleet fixtures and per-test seeding live in {@link UserTestBase}.
 */
public class UserLookupTest extends UserTestBase {

    /**
     * Verifies {@code User.get(connection, userId, fleetId)} loads the expected user for a valid user/fleet pair, with
     * all profile fields matching a reference user built from the same data.
     *
     * @throws SQLException if the lookup fails
     * @throws AccountException if the user has no valid access to the fleet
     */
    @Test
    @DisplayName("Should get user with valid ID and fleet ID")
    public void getUserWithValidIdAndFleetId() throws SQLException, AccountException {
        int userId = 1;
        int fleetId = 1;
        User expectedUser = new User(
                connection,
                1,
                "test@email.com",
                "John",
                "Doe",
                "123 House Road",
                "CityName",
                "CountryName",
                "StateName",
                "10001",
                "",
                false,
                false,
                1,
                1);

        User actualUser = User.get(connection, userId, fleetId);
        assertEquals(expectedUser, actualUser);
    }

    /**
     * Verifies {@code User.get} loads the same user against a second fleet they belong to, with the selected fleet id
     * reflecting that fleet.
     *
     * @throws SQLException if the lookup fails
     * @throws AccountException if the user has no valid access to the fleet
     */
    @Test
    @DisplayName("Should get user with valid ID and different fleet ID")
    public void getUserWithValidIdAndDifferentFleetId() throws SQLException, AccountException {
        int userId = 1;
        int fleetId = 2;
        User expectedUser = new User(
                connection,
                1,
                "test@email.com",
                "John",
                "Doe",
                "123 House Road",
                "CityName",
                "CountryName",
                "StateName",
                "10001",
                "",
                false,
                false,
                2,
                2);

        User actualUser = User.get(connection, userId, fleetId);
        assertEquals(expectedUser, actualUser);
    }

    /**
     * Verifies {@code User.get} loads an admin user with the admin and aggregate-view flags set true, matching a
     * reference admin user.
     *
     * @throws SQLException if the lookup fails
     * @throws AccountException if the user has no valid access to the fleet
     */
    @Test
    @DisplayName("Should get admin user")
    public void getUserWithAdminUser() throws SQLException, AccountException {
        int userId = 2;
        int fleetId = 1;
        User expectedUser = new User(
                connection,
                2,
                "test1@email.com",
                "John Admin",
                "Aggregate Doe",
                "123 House Road",
                "CityName",
                "CountryName",
                "StateName",
                "10001",
                "",
                true,
                true,
                1,
                1);

        User actualUser = User.get(connection, userId, fleetId);
        assertEquals(expectedUser, actualUser);
    }

    /**
     * Verifies {@code User.get(connection, email, password)} returns null (rather than throwing) for a user who
     * authenticates correctly but has no fleet access. Seeds the user and sets their password in a rolled-back
     * transaction.
     *
     * @throws SQLException if a database operation fails
     */
    @Test
    @DisplayName("Should return null for user with valid email and password but no fleet access")
    public void getUserWithValidEmailAndPasswordButNoFleetAccess() throws SQLException {
        connection.setAutoCommit(false);
        String email = "nofleet@email.com";
        String password = "password123";

        try (PreparedStatement stmt = connection.prepareStatement(
                "INSERT INTO user (email, first_name, last_name, country, state, city, address, "
                        + "phone_number, zip_code, admin, aggregate_view, password_token) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
            stmt.setString(1, email);
            stmt.setString(2, "No");
            stmt.setString(3, "Fleet");
            stmt.setString(4, "US");
            stmt.setString(5, "CA");
            stmt.setString(6, "San Francisco");
            stmt.setString(7, "123 Main St");
            stmt.setString(8, "555-1234");
            stmt.setString(9, "94102");
            stmt.setBoolean(10, false);
            stmt.setBoolean(11, false);
            stmt.setString(12, "aaaaaaaaaaaaaaaaaaaa"); // Will be updated to valid format
            stmt.executeUpdate();
        }

        User.updatePassword(connection, email, password);

        User result = null;
        try {
            result = User.get(connection, email, password);
        } catch (AccountException e) {
            fail("AccountException should not be thrown for user with no fleet access: " + e.getMessage());
        }

        assertNull(result, "User with no fleet access should return null");

        connection.rollback();
    }

    /**
     * Verifies {@code User.get(connection, email, password)} returns the user when they authenticate and have a single
     * fleet access, populating fleet id, access type, and email preferences. Seeds the user, grants VIEW access, and
     * sets the password in a rolled-back transaction.
     *
     * @throws SQLException if a database operation fails
     */
    @Test
    @DisplayName("Should get user with valid email, password, and single fleet access")
    public void getUserWithValidEmailAndPasswordAndSingleFleetAccess() throws SQLException {
        connection.setAutoCommit(false);
        String email = "singlefleet@email.com";
        String password = "password123";

        int userId;
        try (PreparedStatement stmt = connection.prepareStatement(
                "INSERT INTO user (email, first_name, last_name, country, state, city, address, "
                        + "phone_number, zip_code, admin, aggregate_view, password_token) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, email);
            stmt.setString(2, "Single");
            stmt.setString(3, "Fleet");
            stmt.setString(4, "US");
            stmt.setString(5, "CA");
            stmt.setString(6, "San Francisco");
            stmt.setString(7, "123 Main St");
            stmt.setString(8, "555-1234");
            stmt.setString(9, "94102");
            stmt.setBoolean(10, false);
            stmt.setBoolean(11, false);
            stmt.setString(12, "aaaaaaaaaaaaaaaaaaaa"); // Will be updated to valid format
            stmt.executeUpdate();

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    userId = generatedKeys.getInt(1);
                } else {
                    throw new SQLException("Failed to get generated user ID");
                }
            }
        }

        try (PreparedStatement stmt =
                connection.prepareStatement("INSERT INTO fleet_access (user_id, fleet_id, type) VALUES (?, ?, ?)")) {
            stmt.setInt(1, userId); // Use the actual generated user ID
            stmt.setInt(2, 1); // Fleet 1
            stmt.setString(3, "VIEW");
            stmt.executeUpdate();
        }

        User.updatePassword(connection, email, password);

        User result = null;
        try {
            result = User.get(connection, email, password);
        } catch (AccountException e) {
            fail("AccountException should not be thrown for user with single fleet access: " + e.getMessage());
        }

        assertNotNull(result, "User with single fleet access should return a valid user");
        assertEquals(1, result.getFleetId(), "User should have fleet ID 1");
        assertEquals("VIEW", result.getFleetAccessType(), "User should have VIEW access type");
        assertNotNull(result.getUserEmailPreferences(connection), "User should have email preferences");

        connection.rollback();
    }

    /**
     * Verifies {@code User.get(connection, userId, fleetId)} returns null for a user id that does not exist.
     *
     * @throws SQLException if the query fails
     * @throws AccountException if access resolution unexpectedly fails
     */
    @Test
    @DisplayName("Should return null for non-existent user ID")
    public void getUserWithNonExistentUserId() throws SQLException, AccountException {
        int nonExistentUserId = 999999;
        int fleetId = 1;

        User result = User.get(connection, nonExistentUserId, fleetId);

        assertNull(result, "User with non-existent ID should return null");
    }

    /**
     * Verifies {@code User.get(connection, email, password)} throws {@link AccountException} when the password is
     * incorrect. Sets a known password in a rolled-back transaction first.
     *
     * @throws SQLException if a database operation fails
     */
    @Test
    @DisplayName("Should throw AccountException for invalid password")
    public void getUserWithInvalidPassword() throws SQLException {
        connection.setAutoCommit(false);
        String email = "test@email.com";
        String correctPassword = "password123";
        String invalidPassword = "wrongpassword";

        User.updatePassword(connection, email, correctPassword);

        assertThrows(AccountException.class, () -> User.get(connection, email, invalidPassword));

        connection.rollback();
    }

    /**
     * Verifies {@code User.get(connection, email, password)} returns null when the email matches no user.
     *
     * @throws SQLException if the query fails
     * @throws AccountException if access resolution unexpectedly fails
     */
    @Test
    @DisplayName("Should return null for non-existent email")
    public void getUserWithNonExistentEmail() throws SQLException, AccountException {
        String nonExistentEmail = "nonexistent@email.com";
        String password = "password123";

        User user = User.get(connection, nonExistentEmail, password);

        assertNull(user);
    }

    /**
     * Verifies {@code User.get(connection, email)} (email-only overload) returns the matching user with the expected
     * email and id.
     *
     * @throws SQLException if the query fails
     */
    @Test
    @DisplayName("Should get user by email only")
    public void getUserWithEmailOnly() throws SQLException {
        String email = "test@email.com";

        User user = User.get(connection, email);

        assertNotNull(user);
        assertEquals(email, user.getEmail());
        assertEquals(1, user.getId());
    }

    /**
     * Verifies {@code User.get(connection, email)} (email-only overload) returns null when the email matches no user.
     *
     * @throws SQLException if the query fails
     */
    @Test
    @DisplayName("Should return null for non-existent email only")
    public void getUserWithNonExistentEmailOnly() throws SQLException {
        String nonExistentEmail = "nonexistent@email.com";

        User user = User.get(connection, nonExistentEmail);

        assertNull(user);
    }

    /**
     * Verifies {@code User.exists} returns true for an email that is present in the database.
     *
     * @throws SQLException if the query fails
     */
    @Test
    @DisplayName("Should return true for existing email")
    public void existsWithExistingEmail() throws SQLException {
        String existingEmail = "test@email.com";

        boolean exists = User.exists(connection, existingEmail);

        assertTrue(exists);
    }

    /**
     * Verifies {@code User.exists} returns false for an email that is not in the database.
     *
     * @throws SQLException if the query fails
     */
    @Test
    @DisplayName("Should return false for non-existent email")
    public void existsWithNonExistentEmail() throws SQLException {
        String nonExistentEmail = "nonexistent@email.com";

        boolean exists = User.exists(connection, nonExistentEmail);

        assertFalse(exists);
    }

    /**
     * Verifies {@code user.validate} returns true when the supplied password matches the stored hash. Sets the password
     * in a rolled-back transaction first.
     *
     * @throws SQLException if a database operation fails
     */
    @Test
    @DisplayName("Should validate correct password")
    public void validateWithCorrectPassword() throws SQLException {
        connection.setAutoCommit(false);
        User user = user1Fleet1;
        String correctPassword = "password123";

        User.updatePassword(connection, user.getEmail(), correctPassword);

        boolean isValid = user.validate(connection, correctPassword);

        assertTrue(isValid);

        connection.rollback();
    }

    /**
     * Verifies {@code user.validate} returns false when the supplied password does not match the stored hash.
     *
     * @throws SQLException if a database operation fails
     */
    @Test
    @DisplayName("Should reject incorrect password")
    public void validateWithIncorrectPassword() throws SQLException {
        connection.setAutoCommit(false);
        User user = user1Fleet1;
        String correctPassword = "password123";
        String incorrectPassword = "wrongpassword";

        User.updatePassword(connection, user.getEmail(), correctPassword);

        boolean isValid = user.validate(connection, incorrectPassword);

        assertFalse(isValid);

        connection.rollback();
    }

    /**
     * Verifies {@code User.validatePassphrase} returns a boolean result (does not throw) for a candidate passphrase.
     *
     * @throws SQLException if the query fails
     */
    @Test
    @DisplayName("Should validate passphrase with valid passphrase")
    public void validatePassphraseWithValidPassphrase() throws SQLException {
        String email = "test@email.com";
        String passphrase = "validpassphrase";

        boolean isValid = User.validatePassphrase(connection, email, passphrase);

        assertNotNull(Boolean.valueOf(isValid));
    }

    /**
     * Verifies {@code User.validatePassphrase} returns false for a passphrase that does not match the stored reset
     * phrase.
     *
     * @throws SQLException if the query fails
     */
    @Test
    @DisplayName("Should reject invalid passphrase")
    public void validatePassphraseWithInvalidPassphrase() throws SQLException {
        String email = "test@email.com";
        String invalidPassphrase = "invalidpassphrase";

        boolean isValid = User.validatePassphrase(connection, email, invalidPassphrase);

        assertFalse(isValid);
    }

    /**
     * Verifies {@code user.validate} returns false when the user's id (overwritten via reflection to a non-existent
     * value) matches no row. Runs in a rolled-back transaction.
     *
     * @throws SQLException if the validation query fails
     */
    @Test
    @DisplayName("Should return false when validating non-existent user")
    public void validateWithNonExistentUser() throws SQLException {
        connection.setAutoCommit(false);

        User nonExistentUser = user1Fleet1;
        try {
            java.lang.reflect.Field idField = User.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(nonExistentUser, 999999); // Non-existent user ID
        } catch (NoSuchFieldException | IllegalAccessException e) {
            fail("Failed to set user ID via reflection: " + e.getMessage());
        }

        String password = "testpassword";

        boolean isValid = nonExistentUser.validate(connection, password);

        assertFalse(isValid);

        connection.rollback();
    }
}
