package org.ngafid.core.accounts;

import static org.junit.jupiter.api.Assertions.*;

import java.sql.SQLException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link User}'s access and permission checks: per-flight access ({@code hasFlightAccess}), simple profile
 * getters, and the fleet-permission predicates ({@code isAdmin}, {@code hasAggregateView}, {@code managesFleet},
 * {@code hasUploadAccess}, {@code hasViewAccess}).
 *
 * <p>Shared user/fleet fixtures and per-test seeding live in {@link UserTestBase}.
 */
public class UserAccessTest extends UserTestBase {

    /**
     * Verifies {@code hasFlightAccess} returns false for an invalid flight id (0), which matches no flight.
     *
     * @throws SQLException if the access check query fails
     */
    @Test
    @DisplayName("Should deny flight access with invalid flight ID")
    public void hasFlightAccessWithInvalidFlightId() throws SQLException {
        int invalidFlightId = 0;

        boolean hasAccess = user1Fleet1.hasFlightAccess(connection, invalidFlightId);
        assertFalse(hasAccess);
    }

    /**
     * Verifies {@code hasFlightAccess} returns false for a user whose fleet access is DENIED.
     *
     * @throws SQLException if the access check query fails
     */
    @Test
    @DisplayName("Should deny flight access for denied user")
    public void hasFlightAccessWithDeniedUser() throws SQLException {
        int flightId = 0;

        boolean hasAccess = user3fleet1.hasFlightAccess(connection, flightId);
        assertFalse(hasAccess);
    }

    /**
     * Verifies {@code hasFlightAccess} returns false for a user whose fleet access is still WAITING (not yet approved).
     *
     * @throws SQLException if the access check query fails
     */
    @Test
    @DisplayName("Should deny flight access for waiting user")
    public void hasFlightAccessWithWaitingUser() throws SQLException {
        int flightId = 1;

        boolean hasAccess = user1Fleet2Waiting.hasFlightAccess(connection, flightId);

        assertFalse(hasAccess);
    }

    /**
     * Verifies {@code hasFlightAccess} returns false when the flight belongs to a fleet other than the user's selected
     * fleet.
     *
     * @throws SQLException if the access check query fails
     */
    @Test
    @DisplayName("Should deny flight access for user from different fleet")
    public void hasFlightAccessWithDifferentFleet() throws SQLException {
        int flightId = 1;

        boolean hasAccess = user1Fleet2.hasFlightAccess(connection, flightId);

        assertFalse(hasAccess);
    }

    /**
     * Verifies {@code hasFlightAccess} returns true for a flight in the user's own fleet to which they have access.
     *
     * @throws SQLException if the access check query fails
     */
    @Test
    @DisplayName("Should grant flight access for valid flight in same fleet")
    public void hasFlightAccessWithValidFlightInSameFleet() throws SQLException {
        int flightId = 1;

        boolean hasAccess = user1Fleet1.hasFlightAccess(connection, flightId);

        assertTrue(hasAccess);
    }

    /**
     * Verifies {@code hasFlightAccess} returns true for each of several flights in the user's own fleet.
     *
     * @throws SQLException if an access check query fails
     */
    @Test
    @DisplayName("Should grant flight access for multiple valid flights")
    public void hasFlightAccessWithMultipleValidFlights() throws SQLException {
        int flightId1 = 1;
        int flightId2 = 2;

        boolean hasAccess1 = user1Fleet1.hasFlightAccess(connection, flightId1);
        boolean hasAccess2 = user1Fleet1.hasFlightAccess(connection, flightId2);

        assertTrue(hasAccess1);
        assertTrue(hasAccess2);
    }

    /**
     * Verifies {@code hasFlightAccess} returns true for an admin user accessing a flight in their fleet.
     *
     * @throws SQLException if the access check query fails
     */
    @Test
    @DisplayName("Should grant flight access for admin user")
    public void hasFlightAccessWithAdminUser() throws SQLException {
        int flightId = 1;

        boolean hasAccess = user2Fleet1.hasFlightAccess(connection, flightId);

        assertTrue(hasAccess);
    }

    /**
     * Verifies {@code getId} returns the user's id.
     */
    @Test
    @DisplayName("Should get user ID")
    public void getIdWithValidUser() {
        User user = user1Fleet1;

        int userId = user.getId();

        assertEquals(1, userId);
    }

    /**
     * Verifies {@code getEmail} returns the user's email address.
     */
    @Test
    @DisplayName("Should get user email")
    public void getEmailWithValidUser() {
        User user = user1Fleet1;

        String email = user.getEmail();

        assertEquals("test@email.com", email);
    }

    /**
     * Verifies {@code getFullName} returns the user's first and last name joined (e.g. "John Doe").
     */
    @Test
    @DisplayName("Should get user full name")
    public void getFullNameWithValidUser() {
        User user = user1Fleet1;

        String fullName = user.getFullName();

        assertEquals("John Doe", fullName);
    }

    /**
     * Verifies {@code getFleetId} returns the user's fleet id.
     */
    @Test
    @DisplayName("Should get fleet ID")
    public void getFleetIdWithValidUser() {
        User user = user1Fleet1;

        int fleetId = user.getFleetId();

        assertEquals(1, fleetId);
    }

    /**
     * Verifies {@code getFleetAccessType} returns the user's access level for their fleet (e.g. "VIEW").
     */
    @Test
    @DisplayName("Should get fleet access type")
    public void getFleetAccessTypeWithValidUser() {
        User user = user1Fleet1;

        String accessType = user.getFleetAccessType();

        assertEquals("VIEW", accessType);
    }

    /**
     * Verifies {@code getWaitingUserCount} returns the number of users with WAITING access to the user's fleet.
     *
     * @throws SQLException if the count query fails
     */
    @Test
    @DisplayName("Should get waiting user count")
    public void getWaitingUserCountWithValidUser() throws SQLException {
        User user = user1Fleet2;

        int waitingCount = user.getWaitingUserCount(connection);

        assertEquals(2, waitingCount);
    }

    /**
     * Verifies {@code isAdmin} returns false for a non-admin user.
     */
    @Test
    @DisplayName("Should return false for admin status of regular user")
    public void isAdminWithRegularUser() {
        User user = user1Fleet1;

        boolean isAdmin = user.isAdmin();

        assertFalse(isAdmin);
    }

    /**
     * Verifies {@code hasAggregateView} returns false for a regular (non-aggregate) user.
     */
    @Test
    @DisplayName("Should return false for aggregate view status of regular user")
    public void hasAggregateViewWithRegularUser() {
        User user = user1Fleet1;

        boolean hasAggregateView = user.hasAggregateView();

        assertFalse(hasAggregateView);
    }

    /**
     * Verifies {@code managesFleet} returns false for a user who is not a manager of the given fleet.
     */
    @Test
    @DisplayName("Should return false for fleet management of non-manager user")
    public void managesFleetWithNonManagerUser() {
        User user = user1Fleet1;
        int fleetId = 1;

        boolean managesFleet = user.managesFleet(fleetId);

        assertFalse(managesFleet);
    }

    /**
     * Verifies {@code managesFleet} returns true for a user with MANAGER access to the given fleet.
     */
    @Test
    @DisplayName("Should return true for fleet management of manager user")
    public void managesFleetWithManagerUser() {
        User user = user2Fleet1;
        int fleetId = 1;

        boolean managesFleet = user.managesFleet(fleetId);

        assertTrue(managesFleet);
    }

    /**
     * Verifies {@code hasUploadAccess} returns false for a user without upload rights to the given fleet.
     */
    @Test
    @DisplayName("Should return false for upload access of non-upload user")
    public void hasUploadAccessWithNonUploadUser() {
        User user = user1Fleet1;
        int fleetId = 1;

        boolean hasUploadAccess = user.hasUploadAccess(fleetId);

        assertFalse(hasUploadAccess);
    }

    /**
     * Verifies {@code hasUploadAccess} returns true for a user with upload (or higher) rights to the given fleet.
     */
    @Test
    @DisplayName("Should return true for upload access of upload user")
    public void hasUploadAccessWithUploadUser() {
        User user = user2Fleet1;
        int fleetId = 1;

        boolean hasUploadAccess = user.hasUploadAccess(fleetId);

        assertTrue(hasUploadAccess);
    }

    /**
     * Verifies {@code hasViewAccess} returns true for a user with view access to their own fleet.
     */
    @Test
    @DisplayName("Should return true for view access of valid user")
    public void hasViewAccessWithValidUser() {
        User user = user1Fleet1;
        int fleetId = 1;

        boolean hasViewAccess = user.hasViewAccess(fleetId);

        assertTrue(hasViewAccess);
    }

    /**
     * Verifies {@code hasViewAccess} returns true for a manager (view access is implied by management rights).
     */
    @Test
    @DisplayName("Should return true for view access of manager user")
    public void hasViewAccessWithManagerUser() {
        User user = user2Fleet1;
        int fleetId = 1;

        boolean hasViewAccess = user.hasViewAccess(fleetId);

        assertTrue(hasViewAccess);
    }

    /**
     * Verifies {@code hasViewAccess} returns false when the queried fleet is not the user's accessible fleet.
     */
    @Test
    @DisplayName("Should return false for view access of user from different fleet")
    public void hasViewAccessWithDifferentFleet() {
        User user = user1Fleet2;
        int fleetId = 2;

        boolean hasViewAccess = user.hasViewAccess(fleetId);

        assertFalse(hasViewAccess);
    }

    /**
     * Verifies {@code hasViewAccess} returns false even for an admin when querying a fleet that is not their own
     * (per-fleet access is not granted by the admin flag alone).
     */
    @Test
    @DisplayName("Should return false for view access of admin user from different fleet")
    public void hasViewAccessWithAdminUserFromDifferentFleet() {
        User user = user2Fleet2;
        int fleetId = 2;

        boolean hasViewAccess = user.hasViewAccess(fleetId);

        assertFalse(hasViewAccess);
    }
}
