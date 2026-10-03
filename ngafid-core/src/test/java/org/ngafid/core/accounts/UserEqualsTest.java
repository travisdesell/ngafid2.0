package org.ngafid.core.accounts;

import static org.junit.jupiter.api.Assertions.*;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link User#equals}: equality with self, null, and non-User objects, plus inequality when any single field
 * differs (id, email, name, location fields, admin/aggregate-view flags, or fleet access). Each field-difference case
 * seeds a comparison user in a rolled-back transaction. Covers both the original and the rollback-safe
 * ({@code try/finally}) variants of these checks.
 *
 * <p>Shared user/fleet fixtures and per-test seeding live in {@link UserTestBase}.
 */
public class UserEqualsTest extends UserTestBase {

    /**
     * Verifies {@code User.equals} returns true when comparing a user instance with itself.
     */
    @Test
    @DisplayName("Should return true when comparing same user")
    public void equalsWithSameUser() {
        User user1 = user1Fleet1;
        User user2 = user1Fleet1;

        boolean isEqual = user1.equals(user2);

        assertTrue(isEqual);
    }

    /**
     * Verifies {@code User.equals} returns false when comparing two distinct users.
     */
    @Test
    @DisplayName("Should return false when comparing different users")
    public void equalsWithDifferentUser() {
        User user1 = user1Fleet1;
        User user2 = user2Fleet1;

        boolean isEqual = user1.equals(user2);

        assertFalse(isEqual);
    }

    /**
     * Verifies {@code User.equals(null)} returns false.
     */
    @Test
    @DisplayName("Should return false when comparing with null object")
    public void equalsWithNullObject() {
        User user = user1Fleet1;
        Object nullObject = null;

        boolean isEqual = user.equals(nullObject);

        assertFalse(isEqual);
    }

    /**
     * Verifies {@code User.equals} returns false when compared against an object that is not a User.
     */
    @Test
    @DisplayName("Should return false when comparing with non-user object")
    public void equalsWithNonUserObject() {
        User user = user1Fleet1;
        String nonUserObject = "not a user";

        boolean isEqual = user.equals(nonUserObject);

        assertFalse(isEqual);
    }

    /**
     * Verifies {@code User.equals} returns false when two users differ in their id. Seeds a second user and compares,
     * in a rolled-back transaction.
     *
     * @throws SQLException if a database operation fails
     * @throws AccountException if loading the comparison user fails
     */
    @Test
    @DisplayName("Should return false when comparing users with different IDs")
    public void equalsWithDifferentId() throws SQLException, AccountException {
        connection.setAutoCommit(false);

        try (PreparedStatement stmt =
                connection.prepareStatement("INSERT INTO user (id, email, first_name, last_name, country, state, city, "
                        + "address, phone_number, zip_code, admin, aggregate_view, password_token) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
            stmt.setInt(1, 999); // Different ID
            stmt.setString(2, "test999@email.com"); // Different email to avoid unique constraint
            stmt.setString(3, "John");
            stmt.setString(4, "Doe");
            stmt.setString(5, "USA");
            stmt.setString(6, "CA");
            stmt.setString(7, "San Francisco");
            stmt.setString(8, "123 Main St");
            stmt.setString(9, "555-1234");
            stmt.setString(10, "94105");
            stmt.setBoolean(11, false);
            stmt.setBoolean(12, false);
            stmt.setString(13, "aaaaaaaaaaaaaaaaaaaa");
            stmt.executeUpdate();
        }

        User user1 = user1Fleet1;
        User user2 = User.get(connection, 2999, 1); // Different ID

        boolean result = user1.equals(user2);

        assertFalse(result);

        connection.rollback();
    }

    /**
     * Verifies {@code User.equals} returns false when two users differ in their email. Seeds a second user and
     * compares, in a rolled-back transaction.
     *
     * @throws SQLException if a database operation fails
     * @throws AccountException if loading the comparison user fails
     */
    @Test
    @DisplayName("Should return false when comparing users with different emails")
    public void equalsWithDifferentEmail() throws SQLException, AccountException {
        connection.setAutoCommit(false);

        try (PreparedStatement stmt =
                connection.prepareStatement("INSERT INTO user (id, email, first_name, last_name, country, state, city, "
                        + "address, phone_number, zip_code, admin, aggregate_view, password_token) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
            stmt.setInt(1, 998); // Different ID
            stmt.setString(2, "different@email.com"); // Different email
            stmt.setString(3, "John");
            stmt.setString(4, "Doe");
            stmt.setString(5, "USA");
            stmt.setString(6, "CA");
            stmt.setString(7, "San Francisco");
            stmt.setString(8, "123 Main St");
            stmt.setString(9, "555-1234");
            stmt.setString(10, "94105");
            stmt.setBoolean(11, false);
            stmt.setBoolean(12, false);
            stmt.setString(13, "aaaaaaaaaaaaaaaaaaaa");
            stmt.executeUpdate();
        }

        User user1 = user1Fleet1;
        User user2 = User.get(connection, 2998, 1); // Different email

        boolean result = user1.equals(user2);

        assertFalse(result);

        connection.rollback();
    }

    /**
     * Verifies {@code User.equals} returns false when two users differ in their first name. Seeds a second user and
     * compares, in a rolled-back transaction.
     *
     * @throws SQLException if a database operation fails
     * @throws AccountException if loading the comparison user fails
     */
    @Test
    @DisplayName("Should return false when comparing users with different first names")
    public void equalsWithDifferentFirstName() throws SQLException, AccountException {
        connection.setAutoCommit(false);

        try (PreparedStatement stmt =
                connection.prepareStatement("INSERT INTO user (id, email, first_name, last_name, country, state, city, "
                        + "address, phone_number, zip_code, admin, aggregate_view, password_token) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
            stmt.setInt(1, 997); // Different ID
            stmt.setString(2, "test997@email.com"); // Different email to avoid unique constraint
            stmt.setString(3, "Jane"); // Different first name
            stmt.setString(4, "Doe");
            stmt.setString(5, "USA");
            stmt.setString(6, "CA");
            stmt.setString(7, "San Francisco");
            stmt.setString(8, "123 Main St");
            stmt.setString(9, "555-1234");
            stmt.setString(10, "94105");
            stmt.setBoolean(11, false);
            stmt.setBoolean(12, false);
            stmt.setString(13, "aaaaaaaaaaaaaaaaaaaa");
            stmt.executeUpdate();
        }

        User user1 = user1Fleet1;
        User user2 = User.get(connection, 2997, 1); // Different first name

        boolean result = user1.equals(user2);

        assertFalse(result);

        connection.rollback();
    }

    /**
     * Verifies {@code User.equals} returns false when two users differ in their last name. Seeds a second user and
     * compares, in a rolled-back transaction.
     *
     * @throws SQLException if a database operation fails
     * @throws AccountException if loading the comparison user fails
     */
    @Test
    @DisplayName("Should return false when comparing users with different last names")
    public void equalsWithDifferentLastName() throws SQLException, AccountException {
        connection.setAutoCommit(false);

        try (PreparedStatement stmt =
                connection.prepareStatement("INSERT INTO user (id, email, first_name, last_name, country, state, city, "
                        + "address, phone_number, zip_code, admin, aggregate_view, password_token) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
            stmt.setInt(1, 996); // Different ID
            stmt.setString(2, "test996@email.com"); // Different email to avoid unique constraint
            stmt.setString(3, "John");
            stmt.setString(4, "Smith"); // Different last name
            stmt.setString(5, "USA");
            stmt.setString(6, "CA");
            stmt.setString(7, "San Francisco");
            stmt.setString(8, "123 Main St");
            stmt.setString(9, "555-1234");
            stmt.setString(10, "94105");
            stmt.setBoolean(11, false);
            stmt.setBoolean(12, false);
            stmt.setString(13, "aaaaaaaaaaaaaaaaaaaa");
            stmt.executeUpdate();
        }

        User user1 = user1Fleet1;
        User user2 = User.get(connection, 2996, 1); // Different last name

        boolean result = user1.equals(user2);

        assertFalse(result);

        connection.rollback();
    }

    /**
     * Verifies {@code User.equals} returns false when two users differ in their country. Seeds a second user and
     * compares, in a rolled-back transaction.
     *
     * @throws SQLException if a database operation fails
     * @throws AccountException if loading the comparison user fails
     */
    @Test
    @DisplayName("Should return false when comparing users with different countries")
    public void equalsWithDifferentCountry() throws SQLException, AccountException {
        connection.setAutoCommit(false);

        try (PreparedStatement stmt =
                connection.prepareStatement("INSERT INTO user (id, email, first_name, last_name, country, state, city, "
                        + "address, phone_number, zip_code, admin, aggregate_view, password_token) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
            stmt.setInt(1, 995); // Different ID
            stmt.setString(2, "test995@email.com"); // Different email to avoid unique constraint
            stmt.setString(3, "John");
            stmt.setString(4, "Doe");
            stmt.setString(5, "Canada"); // Different country
            stmt.setString(6, "CA");
            stmt.setString(7, "San Francisco");
            stmt.setString(8, "123 Main St");
            stmt.setString(9, "555-1234");
            stmt.setString(10, "94105");
            stmt.setBoolean(11, false);
            stmt.setBoolean(12, false);
            stmt.setString(13, "aaaaaaaaaaaaaaaaaaaa");
            stmt.executeUpdate();
        }

        User user1 = user1Fleet1;
        User user2 = User.get(connection, 2995, 1); // Different country

        boolean result = user1.equals(user2);

        assertFalse(result);

        connection.rollback();
    }

    /**
     * Verifies {@code User.equals} returns false when two users differ in their state. Seeds a second user and
     * compares, in a rolled-back transaction.
     *
     * @throws SQLException if a database operation fails
     * @throws AccountException if loading the comparison user fails
     */
    @Test
    @DisplayName("Should return false when comparing users with different states")
    public void equalsWithDifferentState() throws SQLException, AccountException {
        connection.setAutoCommit(false);

        try (PreparedStatement stmt =
                connection.prepareStatement("INSERT INTO user (id, email, first_name, last_name, country, state, city, "
                        + "address, phone_number, zip_code, admin, aggregate_view, password_token) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
            stmt.setInt(1, 994); // Different ID
            stmt.setString(2, "test994@email.com"); // Different email to avoid unique constraint
            stmt.setString(3, "John");
            stmt.setString(4, "Doe");
            stmt.setString(5, "USA");
            stmt.setString(6, "NY"); // Different state
            stmt.setString(7, "San Francisco");
            stmt.setString(8, "123 Main St");
            stmt.setString(9, "555-1234");
            stmt.setString(10, "94105");
            stmt.setBoolean(11, false);
            stmt.setBoolean(12, false);
            stmt.setString(13, "aaaaaaaaaaaaaaaaaaaa");
            stmt.executeUpdate();
        }

        User user1 = user1Fleet1;
        User user2 = User.get(connection, 2994, 1); // Different state

        boolean result = user1.equals(user2);

        assertFalse(result);

        connection.rollback();
    }

    /**
     * Verifies {@code User.equals} returns false when two users differ in their city. Seeds a second user and compares,
     * in a rolled-back transaction.
     *
     * @throws SQLException if a database operation fails
     * @throws AccountException if loading the comparison user fails
     */
    @Test
    @DisplayName("Should return false when comparing users with different cities")
    public void equalsWithDifferentCity() throws SQLException, AccountException {
        connection.setAutoCommit(false);

        try (PreparedStatement stmt =
                connection.prepareStatement("INSERT INTO user (id, email, first_name, last_name, country, state, city, "
                        + "address, phone_number, zip_code, admin, aggregate_view, password_token) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
            stmt.setInt(1, 993); // Different ID
            stmt.setString(2, "test993@email.com"); // Different email to avoid unique constraint
            stmt.setString(3, "John");
            stmt.setString(4, "Doe");
            stmt.setString(5, "USA");
            stmt.setString(6, "CA");
            stmt.setString(7, "Los Angeles"); // Different city
            stmt.setString(8, "123 Main St");
            stmt.setString(9, "555-1234");
            stmt.setString(10, "94105");
            stmt.setBoolean(11, false);
            stmt.setBoolean(12, false);
            stmt.setString(13, "aaaaaaaaaaaaaaaaaaaa");
            stmt.executeUpdate();
        }

        User user1 = user1Fleet1;
        User user2 = User.get(connection, 2993, 1); // Different city

        boolean result = user1.equals(user2);

        assertFalse(result);

        connection.rollback();
    }

    /**
     * Verifies {@code User.equals} returns false when two users differ in their address. Seeds a second user and
     * compares, in a rolled-back transaction.
     *
     * @throws SQLException if a database operation fails
     * @throws AccountException if loading the comparison user fails
     */
    @Test
    @DisplayName("Should return false when comparing users with different addresses")
    public void equalsWithDifferentAddress() throws SQLException, AccountException {
        connection.setAutoCommit(false);

        try (PreparedStatement stmt =
                connection.prepareStatement("INSERT INTO user (id, email, first_name, last_name, country, state, city, "
                        + "address, phone_number, zip_code, admin, aggregate_view, password_token) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
            stmt.setInt(1, 992); // Different ID
            stmt.setString(2, "test992@email.com"); // Different email to avoid unique constraint
            stmt.setString(3, "John");
            stmt.setString(4, "Doe");
            stmt.setString(5, "USA");
            stmt.setString(6, "CA");
            stmt.setString(7, "San Francisco");
            stmt.setString(8, "456 Oak Ave"); // Different address
            stmt.setString(9, "555-1234");
            stmt.setString(10, "94105");
            stmt.setBoolean(11, false);
            stmt.setBoolean(12, false);
            stmt.setString(13, "aaaaaaaaaaaaaaaaaaaa");
            stmt.executeUpdate();
        }

        User user1 = user1Fleet1;
        User user2 = User.get(connection, 2992, 1); // Different address

        boolean result = user1.equals(user2);

        assertFalse(result);

        connection.rollback();
    }

    /**
     * Verifies {@code User.equals} returns false when two users differ in their phone number. Seeds a second user and
     * compares, in a rolled-back transaction.
     *
     * @throws SQLException if a database operation fails
     * @throws AccountException if loading the comparison user fails
     */
    @Test
    @DisplayName("Should return false when comparing users with different phone numbers")
    public void equalsWithDifferentPhoneNumber() throws SQLException, AccountException {
        connection.setAutoCommit(false);

        try (PreparedStatement stmt =
                connection.prepareStatement("INSERT INTO user (id, email, first_name, last_name, country, state, city, "
                        + "address, phone_number, zip_code, admin, aggregate_view, password_token) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
            stmt.setInt(1, 991); // Different ID
            stmt.setString(2, "test991@email.com"); // Different email to avoid unique constraint
            stmt.setString(3, "John");
            stmt.setString(4, "Doe");
            stmt.setString(5, "USA");
            stmt.setString(6, "CA");
            stmt.setString(7, "San Francisco");
            stmt.setString(8, "123 Main St");
            stmt.setString(9, "555-5678"); // Different phone number
            stmt.setString(10, "94105");
            stmt.setBoolean(11, false);
            stmt.setBoolean(12, false);
            stmt.setString(13, "aaaaaaaaaaaaaaaaaaaa");
            stmt.executeUpdate();
        }

        User user1 = user1Fleet1;
        User user2 = User.get(connection, 2991, 1); // Different phone number

        boolean result = user1.equals(user2);

        assertFalse(result);

        connection.rollback();
    }

    /**
     * Verifies {@code User.equals} returns false when two users differ in their zip code. Seeds a second user and
     * compares, in a rolled-back transaction.
     *
     * @throws SQLException if a database operation fails
     * @throws AccountException if loading the comparison user fails
     */
    @Test
    @DisplayName("Should return false when comparing users with different zip codes")
    public void equalsWithDifferentZipCode() throws SQLException, AccountException {
        connection.setAutoCommit(false);

        try (PreparedStatement stmt =
                connection.prepareStatement("INSERT INTO user (id, email, first_name, last_name, country, state, city, "
                        + "address, phone_number, zip_code, admin, aggregate_view, password_token) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
            stmt.setInt(1, 990); // Different ID
            stmt.setString(2, "test990@email.com"); // Different email to avoid unique constraint
            stmt.setString(3, "John");
            stmt.setString(4, "Doe");
            stmt.setString(5, "USA");
            stmt.setString(6, "CA");
            stmt.setString(7, "San Francisco");
            stmt.setString(8, "123 Main St");
            stmt.setString(9, "555-1234");
            stmt.setString(10, "90210"); // Different zip code
            stmt.setBoolean(11, false);
            stmt.setBoolean(12, false);
            stmt.setString(13, "aaaaaaaaaaaaaaaaaaaa");
            stmt.executeUpdate();
        }

        User user1 = user1Fleet1;
        User user2 = User.get(connection, 2990, 1); // Different zip code

        boolean result = user1.equals(user2);

        assertFalse(result);

        connection.rollback();
    }

    /**
     * Verifies {@code User.equals} returns false when two users differ in their admin flag. Seeds a second user and
     * compares, in a rolled-back transaction.
     *
     * @throws SQLException if a database operation fails
     * @throws AccountException if loading the comparison user fails
     */
    @Test
    @DisplayName("Should return false when comparing users with different admin status")
    public void equalsWithDifferentAdminStatus() throws SQLException, AccountException {
        connection.setAutoCommit(false);

        try (PreparedStatement stmt =
                connection.prepareStatement("INSERT INTO user (id, email, first_name, last_name, country, state, city, "
                        + "address, phone_number, zip_code, admin, aggregate_view, password_token) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
            stmt.setInt(1, 989); // Different ID
            stmt.setString(2, "test989@email.com"); // Different email to avoid unique constraint
            stmt.setString(3, "John");
            stmt.setString(4, "Doe");
            stmt.setString(5, "USA");
            stmt.setString(6, "CA");
            stmt.setString(7, "San Francisco");
            stmt.setString(8, "123 Main St");
            stmt.setString(9, "555-1234");
            stmt.setString(10, "94105");
            stmt.setBoolean(11, true); // Different admin status
            stmt.setBoolean(12, false);
            stmt.setString(13, "aaaaaaaaaaaaaaaaaaaa");
            stmt.executeUpdate();
        }

        User user1 = user1Fleet1;
        User user2 = User.get(connection, 2989, 1); // Different admin status

        boolean result = user1.equals(user2);

        assertFalse(result);

        connection.rollback();
    }

    /**
     * Verifies {@code User.equals} returns false when two users differ in their aggregate-view flag. Seeds a second
     * user and compares, in a rolled-back transaction.
     *
     * @throws SQLException if a database operation fails
     * @throws AccountException if loading the comparison user fails
     */
    @Test
    @DisplayName("Should return false when comparing users with different aggregate view")
    public void equalsWithDifferentAggregateView() throws SQLException, AccountException {
        connection.setAutoCommit(false);

        try (PreparedStatement stmt =
                connection.prepareStatement("INSERT INTO user (id, email, first_name, last_name, country, state, city, "
                        + "address, phone_number, zip_code, admin, aggregate_view, password_token) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
            stmt.setInt(1, 988); // Different ID
            stmt.setString(2, "test988@email.com"); // Different email to avoid unique constraint
            stmt.setString(3, "John");
            stmt.setString(4, "Doe");
            stmt.setString(5, "USA");
            stmt.setString(6, "CA");
            stmt.setString(7, "San Francisco");
            stmt.setString(8, "123 Main St");
            stmt.setString(9, "555-1234");
            stmt.setString(10, "94105");
            stmt.setBoolean(11, false);
            stmt.setBoolean(12, true); // Different aggregate view
            stmt.setString(13, "aaaaaaaaaaaaaaaaaaaa");
            stmt.executeUpdate();
        }

        User user1 = user1Fleet1;
        User user2 = User.get(connection, 2988, 1); // Different aggregate view

        boolean result = user1.equals(user2);

        assertFalse(result);

        connection.rollback();
    }

    /**
     * Verifies {@code User.equals} returns false for the same user loaded against two different fleets (differing fleet
     * access makes the instances unequal).
     *
     * @throws SQLException if a database operation fails
     * @throws AccountException if loading a comparison user fails
     */
    @Test
    @DisplayName("Should return false when comparing users with different fleet access")
    public void equalsWithDifferentFleetAccess() throws SQLException, AccountException {
        User user1 = user1Fleet1; // User 1 in fleet 1
        User user2 = user1Fleet2; // User 1 in fleet 2 (different fleet access)

        boolean result = user1.equals(user2);

        assertFalse(result);
    }

    /**
     * Verifies {@code User.equals} returns false for two different users in different fleets.
     *
     * @throws SQLException if a database operation fails
     * @throws AccountException if loading a comparison user fails
     */
    @Test
    @DisplayName("Should return false when comparing users with different fleet")
    public void equalsWithDifferentFleet() throws SQLException, AccountException {
        User user1 = user1Fleet1; // User 1 in fleet 1
        User user2 = user2Fleet2; // User 2 in fleet 2 (different user, different fleet)

        boolean result = user1.equals(user2);

        assertFalse(result);
    }

    /**
     * Verifies {@code User.equals} returns true for the same user instance (identical fields).
     *
     * @throws SQLException if a database operation fails
     * @throws AccountException if loading the user fails
     */
    @Test
    @DisplayName("Should return true when comparing identical users")
    public void equalsWithIdenticalUsers() throws SQLException, AccountException {
        User user1 = user1Fleet1;
        User user2 = user1Fleet1; // Same user object

        boolean result = user1.equals(user2);

        assertTrue(result);
    }

    /**
     * Verifies {@code User.equals} returns true when a user instance is compared with itself (a second variant of the
     * same-user equality check).
     */
    @Test
    @DisplayName("Should return true when comparing a user to itself (second variant)")
    public void testUserEqualsWithSameUser() {
        User user1 = user1Fleet1;
        User user2 = user1Fleet1;

        boolean isEqual = user1.equals(user2);

        assertTrue(isEqual);
    }

    /**
     * Verifies {@code User.equals(null)} returns false (a second variant of the null-comparison check).
     */
    @Test
    @DisplayName("Should return false when comparing a user with null (second variant)")
    public void testUserEqualsWithNullObject() {
        User user = user1Fleet1;
        Object nullObject = null;

        boolean isEqual = user.equals(nullObject);

        assertFalse(isEqual);
    }

    /**
     * Verifies {@code User.equals} returns false against a non-User object (a second variant of the wrong-type check).
     */
    @Test
    @DisplayName("Should return false when comparing user with non-user object")
    public void testUserEqualsWithNonUserObject() {
        User user = user1Fleet1;
        String nonUserObject = "not a user";

        boolean isEqual = user.equals(nonUserObject);

        assertFalse(isEqual);
    }

    /**
     * Verifies {@code User.equals} returns false when two users differ in their id (rollback-safe variant: the seeded
     * row is cleaned up in a try/finally).
     *
     * @throws SQLException if a database operation fails
     * @throws AccountException if loading the comparison user fails
     */
    @Test
    @DisplayName("Should return false when comparing users with different IDs (rollback-safe)")
    public void testUserEqualsWithDifferentId() throws SQLException, AccountException {
        connection.setAutoCommit(false);

        try {
            try (PreparedStatement stmt = connection.prepareStatement(
                    "INSERT INTO user (id, email, first_name, last_name, country, state, city, "
                            + "address, phone_number, zip_code, admin, aggregate_view, password_token) "
                            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
                stmt.setInt(1, 999); // Different ID
                stmt.setString(2, "test999@email.com"); // Different email to avoid unique constraint
                stmt.setString(3, "John");
                stmt.setString(4, "Doe");
                stmt.setString(5, "USA");
                stmt.setString(6, "CA");
                stmt.setString(7, "San Francisco");
                stmt.setString(8, "123 Main St");
                stmt.setString(9, "555-1234");
                stmt.setString(10, "94105");
                stmt.setBoolean(11, false);
                stmt.setBoolean(12, false);
                stmt.setString(13, "aaaaaaaaaaaaaaaaaaaa");
                stmt.executeUpdate();
            }

            User user1 = user1Fleet1;
            User user2 = User.get(connection, 2999, 1); // Different ID

            boolean result = user1.equals(user2);

            assertFalse(result);
        } finally {
            connection.rollback();
        }
    }

    /**
     * Verifies {@code User.equals} returns false when two users differ in their email (rollback-safe variant).
     *
     * @throws SQLException if a database operation fails
     * @throws AccountException if loading the comparison user fails
     */
    @Test
    @DisplayName("Should return false when comparing users with different emails (rollback-safe)")
    public void testUserEqualsWithDifferentEmail() throws SQLException, AccountException {
        connection.setAutoCommit(false);

        try {
            try (PreparedStatement stmt = connection.prepareStatement(
                    "INSERT INTO user (id, email, first_name, last_name, country, state, city, "
                            + "address, phone_number, zip_code, admin, aggregate_view, password_token) "
                            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
                stmt.setInt(1, 998); // Different ID
                stmt.setString(2, "different@email.com"); // Different email
                stmt.setString(3, "John");
                stmt.setString(4, "Doe");
                stmt.setString(5, "USA");
                stmt.setString(6, "CA");
                stmt.setString(7, "San Francisco");
                stmt.setString(8, "123 Main St");
                stmt.setString(9, "555-1234");
                stmt.setString(10, "94105");
                stmt.setBoolean(11, false);
                stmt.setBoolean(12, false);
                stmt.setString(13, "aaaaaaaaaaaaaaaaaaaa");
                stmt.executeUpdate();
            }

            User user1 = user1Fleet1;
            User user2 = User.get(connection, 2998, 1); // Different email

            boolean result = user1.equals(user2);

            assertFalse(result);
        } finally {
            connection.rollback();
        }
    }

    /**
     * Verifies {@code User.equals} returns false when two users differ in their first name (rollback-safe variant).
     *
     * @throws SQLException if a database operation fails
     * @throws AccountException if loading the comparison user fails
     */
    @Test
    @DisplayName("Should return false when comparing users with different first names (rollback-safe)")
    public void testUserEqualsWithDifferentFirstName() throws SQLException, AccountException {
        connection.setAutoCommit(false);

        try {
            try (PreparedStatement stmt = connection.prepareStatement(
                    "INSERT INTO user (id, email, first_name, last_name, country, state, city, "
                            + "address, phone_number, zip_code, admin, aggregate_view, password_token) "
                            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
                stmt.setInt(1, 997); // Different ID
                stmt.setString(2, "test997@email.com"); // Different email to avoid unique constraint
                stmt.setString(3, "Jane"); // Different first name
                stmt.setString(4, "Doe");
                stmt.setString(5, "USA");
                stmt.setString(6, "CA");
                stmt.setString(7, "San Francisco");
                stmt.setString(8, "123 Main St");
                stmt.setString(9, "555-1234");
                stmt.setString(10, "94105");
                stmt.setBoolean(11, false);
                stmt.setBoolean(12, false);
                stmt.setString(13, "aaaaaaaaaaaaaaaaaaaa");
                stmt.executeUpdate();
            }

            User user1 = user1Fleet1;
            User user2 = User.get(connection, 2997, 1); // Different first name

            boolean result = user1.equals(user2);

            assertFalse(result);
        } finally {
            connection.rollback();
        }
    }

    /**
     * Verifies {@code User.equals} returns false when two users differ in their last name (rollback-safe variant).
     *
     * @throws SQLException if a database operation fails
     * @throws AccountException if loading the comparison user fails
     */
    @Test
    @DisplayName("Should return false when comparing users with different last names (rollback-safe)")
    public void testUserEqualsWithDifferentLastName() throws SQLException, AccountException {
        connection.setAutoCommit(false);

        try {
            try (PreparedStatement stmt = connection.prepareStatement(
                    "INSERT INTO user (id, email, first_name, last_name, country, state, city, "
                            + "address, phone_number, zip_code, admin, aggregate_view, password_token) "
                            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
                stmt.setInt(1, 996); // Different ID
                stmt.setString(2, "test996@email.com"); // Different email to avoid unique constraint
                stmt.setString(3, "John");
                stmt.setString(4, "Smith"); // Different last name
                stmt.setString(5, "USA");
                stmt.setString(6, "CA");
                stmt.setString(7, "San Francisco");
                stmt.setString(8, "123 Main St");
                stmt.setString(9, "555-1234");
                stmt.setString(10, "94105");
                stmt.setBoolean(11, false);
                stmt.setBoolean(12, false);
                stmt.setString(13, "aaaaaaaaaaaaaaaaaaaa");
                stmt.executeUpdate();
            }

            User user1 = user1Fleet1;
            User user2 = User.get(connection, 2996, 1); // Different last name

            boolean result = user1.equals(user2);

            assertFalse(result);
        } finally {
            connection.rollback();
        }
    }

    /**
     * Verifies {@code User.equals} returns false when two users differ in their country (rollback-safe variant).
     *
     * @throws SQLException if a database operation fails
     * @throws AccountException if loading the comparison user fails
     */
    @Test
    @DisplayName("Should return false when comparing users with different countries (rollback-safe)")
    public void testUserEqualsWithDifferentCountry() throws SQLException, AccountException {
        connection.setAutoCommit(false);

        try {
            try (PreparedStatement stmt = connection.prepareStatement(
                    "INSERT INTO user (id, email, first_name, last_name, country, state, city, "
                            + "address, phone_number, zip_code, admin, aggregate_view, password_token) "
                            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
                stmt.setInt(1, 995); // Different ID
                stmt.setString(2, "test995@email.com"); // Different email to avoid unique constraint
                stmt.setString(3, "John");
                stmt.setString(4, "Doe");
                stmt.setString(5, "Canada"); // Different country
                stmt.setString(6, "CA");
                stmt.setString(7, "San Francisco");
                stmt.setString(8, "123 Main St");
                stmt.setString(9, "555-1234");
                stmt.setString(10, "94105");
                stmt.setBoolean(11, false);
                stmt.setBoolean(12, false);
                stmt.setString(13, "aaaaaaaaaaaaaaaaaaaa");
                stmt.executeUpdate();
            }

            User user1 = user1Fleet1;
            User user2 = User.get(connection, 2995, 1); // Different country

            boolean result = user1.equals(user2);

            assertFalse(result);
        } finally {
            connection.rollback();
        }
    }

    /**
     * Verifies {@code User.equals} returns false when two users differ in their state (rollback-safe variant).
     *
     * @throws SQLException if a database operation fails
     * @throws AccountException if loading the comparison user fails
     */
    @Test
    @DisplayName("Should return false when comparing users with different states (rollback-safe)")
    public void testUserEqualsWithDifferentState() throws SQLException, AccountException {
        connection.setAutoCommit(false);

        try {
            // Create a user with different state
            try (PreparedStatement stmt = connection.prepareStatement(
                    "INSERT INTO user (id, email, first_name, last_name, country, state, city, "
                            + "address, phone_number, zip_code, admin, aggregate_view, password_token) "
                            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
                stmt.setInt(1, 994); // Different ID
                stmt.setString(2, "test994@email.com"); // Different email to avoid unique constraint
                stmt.setString(3, "John");
                stmt.setString(4, "Doe");
                stmt.setString(5, "USA");
                stmt.setString(6, "NY"); // Different state
                stmt.setString(7, "San Francisco");
                stmt.setString(8, "123 Main St");
                stmt.setString(9, "555-1234");
                stmt.setString(10, "94105");
                stmt.setBoolean(11, false);
                stmt.setBoolean(12, false);
                stmt.setString(13, "aaaaaaaaaaaaaaaaaaaa");
                stmt.executeUpdate();
            }

            User user1 = user1Fleet1;
            User user2 = User.get(connection, 2994, 1); // Different state

            boolean result = user1.equals(user2);

            assertFalse(result);
        } finally {
            connection.rollback();
        }
    }

    /**
     * Verifies {@code User.equals} returns false when two users differ in their city (rollback-safe variant).
     *
     * @throws SQLException if a database operation fails
     * @throws AccountException if loading the comparison user fails
     */
    @Test
    @DisplayName("Should return false when comparing users with different cities (rollback-safe)")
    public void testUserEqualsWithDifferentCity() throws SQLException, AccountException {
        connection.setAutoCommit(false);

        try {
            try (PreparedStatement stmt = connection.prepareStatement(
                    "INSERT INTO user (id, email, first_name, last_name, country, state, city, "
                            + "address, phone_number, zip_code, admin, aggregate_view, password_token) "
                            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
                stmt.setInt(1, 993); // Different ID
                stmt.setString(2, "test993@email.com"); // Different email to avoid unique constraint
                stmt.setString(3, "John");
                stmt.setString(4, "Doe");
                stmt.setString(5, "USA");
                stmt.setString(6, "CA");
                stmt.setString(7, "Los Angeles"); // Different city
                stmt.setString(8, "123 Main St");
                stmt.setString(9, "555-1234");
                stmt.setString(10, "94105");
                stmt.setBoolean(11, false);
                stmt.setBoolean(12, false);
                stmt.setString(13, "aaaaaaaaaaaaaaaaaaaa");
                stmt.executeUpdate();
            }

            User user1 = user1Fleet1;
            User user2 = User.get(connection, 2993, 1); // Different city

            boolean result = user1.equals(user2);

            assertFalse(result);
        } finally {
            connection.rollback();
        }
    }

    /**
     * Verifies {@code User.equals} returns false when two users differ in their address (rollback-safe variant).
     *
     * @throws SQLException if a database operation fails
     * @throws AccountException if loading the comparison user fails
     */
    @Test
    @DisplayName("Should return false when comparing users with different addresses (rollback-safe)")
    public void testUserEqualsWithDifferentAddress() throws SQLException, AccountException {
        connection.setAutoCommit(false);

        try {
            try (PreparedStatement stmt = connection.prepareStatement(
                    "INSERT INTO user (id, email, first_name, last_name, country, state, city, "
                            + "address, phone_number, zip_code, admin, aggregate_view, password_token) "
                            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
                stmt.setInt(1, 992); // Different ID
                stmt.setString(2, "test992@email.com"); // Different email to avoid unique constraint
                stmt.setString(3, "John");
                stmt.setString(4, "Doe");
                stmt.setString(5, "USA");
                stmt.setString(6, "CA");
                stmt.setString(7, "San Francisco");
                stmt.setString(8, "456 Oak Ave"); // Different address
                stmt.setString(9, "555-1234");
                stmt.setString(10, "94105");
                stmt.setBoolean(11, false);
                stmt.setBoolean(12, false);
                stmt.setString(13, "aaaaaaaaaaaaaaaaaaaa");
                stmt.executeUpdate();
            }

            User user1 = user1Fleet1;
            User user2 = User.get(connection, 2992, 1); // Different address

            boolean result = user1.equals(user2);

            assertFalse(result);
        } finally {
            connection.rollback();
        }
    }

    /**
     * Verifies {@code User.equals} returns false when two users differ in their phone number (rollback-safe variant).
     *
     * @throws SQLException if a database operation fails
     * @throws AccountException if loading the comparison user fails
     */
    @Test
    @DisplayName("Should return false when comparing users with different phone numbers (rollback-safe)")
    public void testUserEqualsWithDifferentPhoneNumber() throws SQLException, AccountException {
        connection.setAutoCommit(false);

        try {
            try (PreparedStatement stmt = connection.prepareStatement(
                    "INSERT INTO user (id, email, first_name, last_name, country, state, city, "
                            + "address, phone_number, zip_code, admin, aggregate_view, password_token) "
                            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
                stmt.setInt(1, 991); // Different ID
                stmt.setString(2, "test991@email.com"); // Different email to avoid unique constraint
                stmt.setString(3, "John");
                stmt.setString(4, "Doe");
                stmt.setString(5, "USA");
                stmt.setString(6, "CA");
                stmt.setString(7, "San Francisco");
                stmt.setString(8, "123 Main St");
                stmt.setString(9, "555-5678"); // Different phone number
                stmt.setString(10, "94105");
                stmt.setBoolean(11, false);
                stmt.setBoolean(12, false);
                stmt.setString(13, "aaaaaaaaaaaaaaaaaaaa");
                stmt.executeUpdate();
            }

            User user1 = user1Fleet1;
            User user2 = User.get(connection, 2991, 1); // Different phone number

            boolean result = user1.equals(user2);

            assertFalse(result);
        } finally {
            connection.rollback();
        }
    }

    /**
     * Verifies {@code User.equals} returns false when two users differ in their zip code (rollback-safe variant).
     *
     * @throws SQLException if a database operation fails
     * @throws AccountException if loading the comparison user fails
     */
    @Test
    @DisplayName("Should return false when comparing users with different zip codes (rollback-safe)")
    public void testUserEqualsWithDifferentZipCode() throws SQLException, AccountException {
        connection.setAutoCommit(false);

        try {
            try (PreparedStatement stmt = connection.prepareStatement(
                    "INSERT INTO user (id, email, first_name, last_name, country, state, city, "
                            + "address, phone_number, zip_code, admin, aggregate_view, password_token) "
                            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
                stmt.setInt(1, 990); // Different ID
                stmt.setString(2, "test990@email.com"); // Different email to avoid unique constraint
                stmt.setString(3, "John");
                stmt.setString(4, "Doe");
                stmt.setString(5, "USA");
                stmt.setString(6, "CA");
                stmt.setString(7, "San Francisco");
                stmt.setString(8, "123 Main St");
                stmt.setString(9, "555-1234");
                stmt.setString(10, "90210"); // Different zip code
                stmt.setBoolean(11, false);
                stmt.setBoolean(12, false);
                stmt.setString(13, "aaaaaaaaaaaaaaaaaaaa");
                stmt.executeUpdate();
            }

            User user1 = user1Fleet1;
            User user2 = User.get(connection, 2990, 1); // Different zip code

            boolean result = user1.equals(user2);

            assertFalse(result);
        } finally {
            connection.rollback();
        }
    }

    /**
     * Verifies {@code User.equals} returns false when two users differ in their admin flag (rollback-safe variant).
     *
     * @throws SQLException if a database operation fails
     * @throws AccountException if loading the comparison user fails
     */
    @Test
    @DisplayName("Should return false when comparing users with different admin status (rollback-safe)")
    public void testUserEqualsWithDifferentAdminStatus() throws SQLException, AccountException {
        connection.setAutoCommit(false);

        try {
            try (PreparedStatement stmt = connection.prepareStatement(
                    "INSERT INTO user (id, email, first_name, last_name, country, state, city, "
                            + "address, phone_number, zip_code, admin, aggregate_view, password_token) "
                            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
                stmt.setInt(1, 989); // Different ID
                stmt.setString(2, "test989@email.com"); // Different email to avoid unique constraint
                stmt.setString(3, "John");
                stmt.setString(4, "Doe");
                stmt.setString(5, "USA");
                stmt.setString(6, "CA");
                stmt.setString(7, "San Francisco");
                stmt.setString(8, "123 Main St");
                stmt.setString(9, "555-1234");
                stmt.setString(10, "94105");
                stmt.setBoolean(11, true); // Different admin status
                stmt.setBoolean(12, false);
                stmt.setString(13, "aaaaaaaaaaaaaaaaaaaa");
                stmt.executeUpdate();
            }

            User user1 = user1Fleet1;
            User user2 = User.get(connection, 2989, 1); // Different admin status

            boolean result = user1.equals(user2);

            assertFalse(result);
        } finally {
            connection.rollback();
        }
    }

    /**
     * Verifies {@code User.equals} returns false when two users differ in their aggregate-view flag (rollback-safe
     * variant).
     *
     * @throws SQLException if a database operation fails
     * @throws AccountException if loading the comparison user fails
     */
    @Test
    @DisplayName("Should return false when comparing users with different aggregate view (rollback-safe)")
    public void testUserEqualsWithDifferentAggregateView() throws SQLException, AccountException {
        connection.setAutoCommit(false);

        try {
            try (PreparedStatement stmt = connection.prepareStatement(
                    "INSERT INTO user (id, email, first_name, last_name, country, state, city, "
                            + "address, phone_number, zip_code, admin, aggregate_view, password_token) "
                            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
                stmt.setInt(1, 988); // Different ID
                stmt.setString(2, "test988@email.com"); // Different email to avoid unique constraint
                stmt.setString(3, "John");
                stmt.setString(4, "Doe");
                stmt.setString(5, "USA");
                stmt.setString(6, "CA");
                stmt.setString(7, "San Francisco");
                stmt.setString(8, "123 Main St");
                stmt.setString(9, "555-1234");
                stmt.setString(10, "94105");
                stmt.setBoolean(11, false);
                stmt.setBoolean(12, true); // Different aggregate view
                stmt.setString(13, "aaaaaaaaaaaaaaaaaaaa");
                stmt.executeUpdate();
            }

            User user1 = user1Fleet1;
            User user2 = User.get(connection, 2988, 1); // Different aggregate view

            boolean result = user1.equals(user2);

            assertFalse(result);
        } finally {
            connection.rollback();
        }
    }

    /**
     * Verifies {@code User.equals} returns true for the same user instance with identical fields (rollback-safe
     * variant).
     *
     * @throws SQLException if a database operation fails
     * @throws AccountException if loading the user fails
     */
    @Test
    @DisplayName("Should return true when comparing identical users (rollback-safe)")
    public void testUserEqualsWithIdenticalUsers() throws SQLException, AccountException {
        connection.setAutoCommit(false);

        try {
            try (PreparedStatement stmt = connection.prepareStatement(
                    "INSERT INTO user (id, email, first_name, last_name, country, state, city, "
                            + "address, phone_number, zip_code, admin, aggregate_view, password_token) "
                            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
                stmt.setInt(1, 987); // Different ID
                stmt.setString(2, "test987@email.com"); // Different email to avoid unique constraint
                stmt.setString(3, "John");
                stmt.setString(4, "Doe");
                stmt.setString(5, "USA");
                stmt.setString(6, "CA");
                stmt.setString(7, "San Francisco");
                stmt.setString(8, "123 Main St");
                stmt.setString(9, "555-1234");
                stmt.setString(10, "94105");
                stmt.setBoolean(11, false);
                stmt.setBoolean(12, false);
                stmt.setString(13, "aaaaaaaaaaaaaaaaaaaa");
                stmt.executeUpdate();
            }

            User user1 = User.get(connection, 2987, 1);
            User user2 = User.get(connection, 2987, 1); // Same user, same fleet

            boolean result = user1.equals(user2);

            assertTrue(result);
        } finally {
            connection.rollback();
        }
    }
}
