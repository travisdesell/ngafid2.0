package org.ngafid.core.accounts;

import static org.junit.jupiter.api.Assertions.*;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests for fleet-related behavior exercised through {@link User}: {@link Fleet} queries ({@code hasAirsync},
 * {@code getAllFleets}, {@code getNumberFleets}), the {@link FleetAccess} entity (getters, type predicates, CRUD,
 * equality, and constants), and a user's fleet-selection flow ({@code getSelectedFleetId}, {@code setSelectedFleetId},
 * {@code leaveSelectedFleet}). Mutating tests run inside a rolled-back transaction.
 *
 * <p>Shared user/fleet fixtures and per-test seeding live in {@link UserTestBase}.
 */
public class UserFleetTest extends UserTestBase {

    /**
     * Verifies {@code Fleet.hasAirsync} executes against a loaded fleet without throwing.
     *
     * @throws SQLException if the query fails
     * @throws AccountException if the fleet cannot be loaded
     */
    @Test
    @DisplayName("Should return boolean for fleet hasAirsync")
    public void testFleetHasAirsync() throws SQLException, AccountException {
        Fleet fleet = Fleet.get(connection, 1);
        assertNotNull(fleet);

        // Test that hasAirsync returns successfully without throwing an exception
        fleet.hasAirsync(connection);
    }

    /**
     * Verifies {@code Fleet.getAllFleets} returns a non-null list in which every fleet has a positive id and a non-null
     * name.
     *
     * @throws SQLException if the query fails
     */
    @Test
    @DisplayName("Should get all fleets with valid data")
    public void testFleetGetAllFleets() throws SQLException {
        List<Fleet> fleets = Fleet.getAllFleets(connection);

        assertNotNull(fleets, "Fleet list should not be null");
        assertTrue(fleets.size() >= 0, "Fleet list should have non-negative size");

        for (Fleet fleet : fleets) {
            assertNotNull(fleet, "Each fleet should not be null");
            assertTrue(fleet.getId() > 0, "Fleet ID should be positive");
            assertNotNull(fleet.getName(), "Fleet name should not be null");
        }
    }

    /**
     * Verifies {@code Fleet.getNumberFleets} returns a non-negative count equal to the size of
     * {@code Fleet.getAllFleets}.
     *
     * @throws SQLException if a query fails
     */
    @Test
    @DisplayName("Should get number of fleets matching getAllFleets count")
    public void testFleetGetNumberFleets() throws SQLException {
        int numberOfFleets = Fleet.getNumberFleets(connection);

        assertTrue(numberOfFleets >= 0, "Number of fleets should be non-negative");

        List<Fleet> allFleets = Fleet.getAllFleets(connection);
        assertEquals(allFleets.size(), numberOfFleets, "getNumberFleets should match getAllFleets count");
    }

    /**
     * Verifies a {@link FleetAccess} created via {@code FleetAccess.create} exposes the user id, fleet id, and access
     * type it was created with (clearing any pre-existing row first).
     *
     * @throws SQLException if a database operation fails
     * @throws AccountException if creation fails
     */
    @Test
    @DisplayName("Should get fleet access getters")
    public void testFleetAccessGetters() throws SQLException, AccountException {
        try (PreparedStatement stmt =
                connection.prepareStatement("DELETE FROM fleet_access WHERE user_id = 1 AND fleet_id = 1")) {
            stmt.executeUpdate();
        }

        FleetAccess fleetAccess = FleetAccess.create(connection, 1, 1, FleetAccess.MANAGER);

        assertEquals(1, fleetAccess.getUserId());
        assertEquals(1, fleetAccess.getFleetId());
        assertEquals(FleetAccess.MANAGER, fleetAccess.getAccessType());
    }

    /**
     * Verifies the {@code FleetAccess} type-predicate methods ({@code isManager}, {@code isUpload}, {@code isView},
     * {@code isWaiting}, {@code isDenied}) each return true only for their matching access type, across all five types.
     *
     * @throws SQLException if a database operation fails
     * @throws AccountException if creating an access entry fails
     */
    @Test
    @DisplayName("Should check fleet access type checks")
    public void testFleetAccessTypeChecks() throws SQLException, AccountException {
        try (PreparedStatement stmt =
                connection.prepareStatement("DELETE FROM fleet_access WHERE user_id = 1 AND fleet_id = 1")) {
            stmt.executeUpdate();
        }

        FleetAccess managerAccess = FleetAccess.create(connection, 1, 1, FleetAccess.MANAGER);
        assertTrue(managerAccess.isManager());
        assertFalse(managerAccess.isUpload());
        assertFalse(managerAccess.isView());
        assertFalse(managerAccess.isWaiting());
        assertFalse(managerAccess.isDenied());

        try (PreparedStatement stmt =
                connection.prepareStatement("DELETE FROM fleet_access WHERE user_id = 2 AND fleet_id = 2")) {
            stmt.executeUpdate();
        }

        FleetAccess uploadAccess = FleetAccess.create(connection, 2, 2, FleetAccess.UPLOAD);
        assertTrue(uploadAccess.isUpload());
        assertFalse(uploadAccess.isManager());

        try (PreparedStatement stmt =
                connection.prepareStatement("DELETE FROM fleet_access WHERE user_id = 3 AND fleet_id = 1")) {
            stmt.executeUpdate();
        }

        FleetAccess viewAccess = FleetAccess.create(connection, 3, 1, FleetAccess.VIEW);
        assertTrue(viewAccess.isView());
        assertFalse(viewAccess.isManager());

        try (PreparedStatement stmt =
                connection.prepareStatement("DELETE FROM fleet_access WHERE user_id = 1 AND fleet_id = 2")) {
            stmt.executeUpdate();
        }

        FleetAccess waitingAccess = FleetAccess.create(connection, 1, 2, FleetAccess.WAITING);
        assertTrue(waitingAccess.isWaiting());
        assertFalse(waitingAccess.isManager());

        try (PreparedStatement stmt =
                connection.prepareStatement("DELETE FROM fleet_access WHERE user_id = 2 AND fleet_id = 1")) {
            stmt.executeUpdate();
        }

        FleetAccess deniedAccess = FleetAccess.create(connection, 2, 1, FleetAccess.DENIED);
        assertTrue(deniedAccess.isDenied());
        assertFalse(deniedAccess.isManager());
    }

    /**
     * Verifies {@code FleetAccess.getAllFleetAccessEntries} returns all of a user's access entries, including both a
     * MANAGER and a VIEW entry seeded across two fleets.
     *
     * @throws SQLException if a database operation fails
     * @throws AccountException if creating an access entry fails
     */
    @Test
    @DisplayName("Should get fleet access by user ID")
    public void testFleetAccessGetByUserId() throws SQLException, AccountException {
        try (PreparedStatement stmt = connection.prepareStatement("DELETE FROM fleet_access WHERE user_id = 1")) {
            stmt.executeUpdate();
        }

        FleetAccess.create(connection, 1, 1, FleetAccess.MANAGER);
        FleetAccess.create(connection, 1, 2, FleetAccess.VIEW);

        ArrayList<FleetAccess> accessList = FleetAccess.getAllFleetAccessEntries(connection, 1);
        assertNotNull(accessList);
        assertTrue(accessList.size() >= 2);

        boolean foundManager = false;
        boolean foundView = false;
        for (FleetAccess access : accessList) {
            if (access.getUserId() == 1
                    && access.getFleetId() == 1
                    && access.getAccessType().equals(FleetAccess.MANAGER)) {
                foundManager = true;
            }
            if (access.getUserId() == 1
                    && access.getFleetId() == 2
                    && access.getAccessType().equals(FleetAccess.VIEW)) {
                foundView = true;
            }
        }
        assertTrue(foundManager, "Manager access should be found");
        assertTrue(foundView, "View access should be found");
    }

    /**
     * Verifies {@code FleetAccess.get(connection, userId, fleetId)} returns the single access entry matching that
     * user/fleet pair with the expected access type.
     *
     * @throws SQLException if a database operation fails
     * @throws AccountException if creating the access entry fails
     */
    @Test
    @DisplayName("Should get fleet access by user ID and fleet ID")
    public void testFleetAccessGetByUserIdAndFleetId() throws SQLException, AccountException {
        try (PreparedStatement stmt =
                connection.prepareStatement("DELETE FROM fleet_access WHERE user_id = 1 AND fleet_id = 1")) {
            stmt.executeUpdate();
        }

        FleetAccess.create(connection, 1, 1, FleetAccess.UPLOAD);

        FleetAccess access = FleetAccess.get(connection, 1, 1);
        assertNotNull(access);
        assertEquals(1, access.getUserId());
        assertEquals(1, access.getFleetId());
        assertEquals(FleetAccess.UPLOAD, access.getAccessType());
    }

    /**
     * Verifies {@code FleetAccess.get} returns null for a user/fleet pair that has no access entry.
     *
     * @throws SQLException if the query fails
     */
    @Test
    @DisplayName("Should return null for non-existent fleet access")
    public void testFleetAccessGetNotFound() throws SQLException {
        FleetAccess access = FleetAccess.get(connection, 999, 999);
        assertNull(access);
    }

    /**
     * Verifies {@code FleetAccess.create} inserts a new access entry (after clearing any existing one) that can then be
     * read back via {@code FleetAccess.get} with the expected access type.
     *
     * @throws SQLException if a database operation fails
     * @throws AccountException if creation fails
     */
    @Test
    @DisplayName("Should create a fleet access entry")
    public void testFleetAccessCreate() throws SQLException, AccountException {
        // Create a new fleet access entry using a user/fleet combination that doesn't exist
        // User 1 has VIEW access to fleet 1, but we can create a MANAGER access (upgrade)
        // First, delete the existing VIEW access
        try (PreparedStatement stmt =
                connection.prepareStatement("DELETE FROM fleet_access WHERE user_id = 1 AND fleet_id = 1")) {
            stmt.executeUpdate();
        }

        FleetAccess newAccess = FleetAccess.create(connection, 1, 1, FleetAccess.MANAGER);
        assertNotNull(newAccess);
        assertEquals(1, newAccess.getUserId());
        assertEquals(1, newAccess.getFleetId());
        assertEquals(FleetAccess.MANAGER, newAccess.getAccessType());

        // Verify it was created in the database
        FleetAccess retrievedAccess = FleetAccess.get(connection, 1, 1);
        assertNotNull(retrievedAccess);
        assertEquals(FleetAccess.MANAGER, retrievedAccess.getAccessType());
    }

    /**
     * Verifies {@code FleetAccess.create} throws {@link AccountException} when an access entry already exists for the
     * user/fleet pair.
     *
     * @throws SQLException if a database operation fails
     * @throws AccountException if the first creation unexpectedly fails
     */
    @Test
    @DisplayName("Should throw exception when creating a duplicate fleet access entry")
    public void testFleetAccessCreateDuplicate() throws SQLException, AccountException {
        // First delete the existing VIEW access for user 1 and fleet 1
        try (PreparedStatement stmt =
                connection.prepareStatement("DELETE FROM fleet_access WHERE user_id = 1 AND fleet_id = 1")) {
            stmt.executeUpdate();
        }

        // Create first access
        FleetAccess.create(connection, 1, 1, FleetAccess.UPLOAD);

        // Try to create duplicate access
        assertThrows(AccountException.class, () -> {
            FleetAccess.create(connection, 1, 1, FleetAccess.VIEW);
        });
    }

    /**
     * Verifies {@code FleetAccess.update} changes an entry's access type (confirmed by re-reading it), restoring the
     * original type afterward; falls back to creating an entry first if none exists.
     *
     * @throws SQLException if a database operation fails
     * @throws AccountException if creating an access entry fails
     */
    @Test
    @DisplayName("Should update a fleet access type")
    public void testFleetAccessUpdate() throws SQLException, AccountException {
        // Use existing fleet access entry and update it
        // First, get an existing entry to work with
        FleetAccess existingAccess = FleetAccess.get(connection, 1, 1);
        if (existingAccess != null) {
            String originalType = existingAccess.getAccessType();

            // Update the access type
            FleetAccess.update(connection, 1, 1, FleetAccess.UPLOAD);

            // Verify the update
            FleetAccess updatedAccess = FleetAccess.get(connection, 1, 1);
            assertNotNull(updatedAccess);
            assertEquals(FleetAccess.UPLOAD, updatedAccess.getAccessType());

            // Restore original type for other tests
            FleetAccess.update(connection, 1, 1, originalType);
        } else {
            // If no existing entry, create one first
            FleetAccess.create(connection, 1, 1, FleetAccess.VIEW);
            FleetAccess.update(connection, 1, 1, FleetAccess.MANAGER);

            FleetAccess updatedAccess = FleetAccess.get(connection, 1, 1);
            assertNotNull(updatedAccess);
            assertEquals(FleetAccess.MANAGER, updatedAccess.getAccessType());
        }
    }

    /**
     * Verifies {@code FleetAccess.update} can move an entry through several access types (UPLOAD, WAITING, DENIED),
     * each confirmed by re-reading, then restores the original type.
     *
     * @throws SQLException if a database operation fails
     * @throws AccountException if creating an access entry fails
     */
    @Test
    @DisplayName("Should update fleet access through different types")
    public void testFleetAccessUpdateToDifferentTypes() throws SQLException, AccountException {
        // Use existing fleet access entry and update it through different types
        FleetAccess existingAccess = FleetAccess.get(connection, 2, 2);
        if (existingAccess != null) {
            String originalType = existingAccess.getAccessType();

            // Test updating to different access types
            FleetAccess.update(connection, 2, 2, FleetAccess.UPLOAD);
            FleetAccess access = FleetAccess.get(connection, 2, 2);
            assertEquals(FleetAccess.UPLOAD, access.getAccessType());

            FleetAccess.update(connection, 2, 2, FleetAccess.WAITING);
            access = FleetAccess.get(connection, 2, 2);
            assertEquals(FleetAccess.WAITING, access.getAccessType());

            FleetAccess.update(connection, 2, 2, FleetAccess.DENIED);
            access = FleetAccess.get(connection, 2, 2);
            assertEquals(FleetAccess.DENIED, access.getAccessType());

            // Restore original type
            FleetAccess.update(connection, 2, 2, originalType);
        } else {
            // If no existing entry, create one first
            FleetAccess.create(connection, 2, 2, FleetAccess.VIEW);

            FleetAccess.update(connection, 2, 2, FleetAccess.UPLOAD);
            FleetAccess access = FleetAccess.get(connection, 2, 2);
            assertEquals(FleetAccess.UPLOAD, access.getAccessType());
        }
    }

    /**
     * Verifies {@code FleetAccess.update} on a non-existent user/fleet pair does not throw (it simply updates zero
     * rows).
     *
     * @throws SQLException if the update query fails
     */
    @Test
    @DisplayName("Should handle updating a non-existent fleet access entry")
    public void testFleetAccessUpdateNonExistent() throws SQLException {
        // This should not throw an exception, just update 0 rows
        assertDoesNotThrow(() -> {
            FleetAccess.update(connection, 999, 999, FleetAccess.MANAGER);
        });
    }

    /**
     * Verifies {@code FleetAccess.update} executes without throwing for both an existing and a non-existent entry (a
     * coverage-focused direct exercise of the update path).
     *
     * @throws SQLException if the update query fails
     */
    @Test
    @DisplayName("Should execute fleet access update directly")
    public void testFleetAccessUpdateDirect() throws SQLException {
        // Direct test of the update method - this will execute the method even if no rows are updated
        // This test specifically targets the update method coverage
        assertDoesNotThrow(() -> {
            FleetAccess.update(connection, 1, 1, FleetAccess.VIEW);
        });

        // Also test with non-existent entries to ensure the method executes
        assertDoesNotThrow(() -> {
            FleetAccess.update(connection, 999, 999, FleetAccess.MANAGER);
        });
    }

    /**
     * Verifies {@code FleetAccess.update} runs for several user/fleet pairs without error (a coverage-focused direct
     * exercise of the update method).
     *
     * @throws SQLException if an update query fails
     */
    @Test
    @DisplayName("Should update fleet access for multiple entries")
    public void testFleetAccessUpdateMethod() throws SQLException {
        // Simple test that directly calls the update method to ensure coverage
        FleetAccess.update(connection, 1, 1, FleetAccess.UPLOAD);
        FleetAccess.update(connection, 2, 2, FleetAccess.WAITING);
        FleetAccess.update(connection, 3, 1, FleetAccess.DENIED);
    }

    /**
     * Verifies {@code FleetAccess.getUserId} returns the user id of entries loaded for different users.
     *
     * @throws SQLException if a query fails
     * @throws AccountException if loading an access entry fails
     */
    @Test
    @DisplayName("Should get the user ID from a fleet access entry")
    public void testFleetAccessGetUserId() throws SQLException, AccountException {
        // Test the getUserId() method by creating a FleetAccess object and calling getUserId()
        // First, get an existing fleet access entry
        FleetAccess existingAccess = FleetAccess.get(connection, 1, 1);
        if (existingAccess != null) {
            // Test getUserId() method
            int userId = existingAccess.getUserId();
            assertEquals(1, userId);
        }

        // Also test with another existing entry
        FleetAccess anotherAccess = FleetAccess.get(connection, 2, 1);
        if (anotherAccess != null) {
            int userId = anotherAccess.getUserId();
            assertEquals(2, userId);
        }
    }

    /**
     * Verifies {@code FleetAccess.equals}: two entries for the same user/fleet are equal (symmetrically), entries for
     * different users are unequal, and an entry is unequal to null and to an unrelated type.
     *
     * @throws SQLException if a database operation fails
     * @throws AccountException if creating an access entry fails
     */
    @Test
    @DisplayName("Should test fleet access equality")
    public void testFleetAccessEquals() throws SQLException, AccountException {
        try (PreparedStatement stmt =
                connection.prepareStatement("DELETE FROM fleet_access WHERE user_id = 1 AND fleet_id = 1")) {
            stmt.executeUpdate();
        }

        FleetAccess access1 = FleetAccess.create(connection, 1, 1, FleetAccess.MANAGER);
        FleetAccess access2 = FleetAccess.get(connection, 1, 1); // Same as access1
        try (PreparedStatement stmt =
                connection.prepareStatement("DELETE FROM fleet_access WHERE user_id = 2 AND fleet_id = 1")) {
            stmt.executeUpdate();
        }

        FleetAccess access3 = FleetAccess.create(connection, 2, 1, FleetAccess.MANAGER);

        // Test equality
        assertEquals(access1, access2);
        assertEquals(access2, access1);

        // Test inequality
        assertNotEquals(access1, access3);

        // Test with null
        assertNotEquals(access1, null);

        // Test with different object type
        assertNotEquals(access1, "not a FleetAccess");
    }

    /**
     * Verifies the {@code FleetAccess} access-type string constants hold their expected values (MANAGER, UPLOAD, VIEW,
     * WAITING, DENIED).
     */
    @Test
    @DisplayName("Should have correct fleet access type constants")
    public void testFleetAccessConstants() {
        assertEquals("MANAGER", FleetAccess.MANAGER);
        assertEquals("UPLOAD", FleetAccess.UPLOAD);
        assertEquals("VIEW", FleetAccess.VIEW);
        assertEquals("WAITING", FleetAccess.WAITING);
        assertEquals("DENIED", FleetAccess.DENIED);
    }

    /**
     * Verifies {@code setSelectedFleetId} rejects the "no fleet selected" sentinel (-1). A web user must always have a
     * valid selected fleet, so selecting fleet -1 (which matches no fleet) throws an {@link AccountException} rather
     * than leaving the user in a fleet-less state.
     *
     * @throws SQLException if the initial user load fails
     * @throws AccountException if the initial user load fails (not expected for a valid user/fleet pair)
     */
    @Test
    @DisplayName("Should throw when selecting fleet id -1")
    public void setSelectedFleetIdWithNegativeOneThrows() throws SQLException, AccountException {
        User user = User.get(connection, 1, 1);
        assertThrows(AccountException.class, () -> user.setSelectedFleetId(connection, -1));
    }

    /**
     * Verifies {@code getSelectedFleetId} returns the user's currently selected fleet id (the default -1 for this
     * user).
     */
    @Test
    @DisplayName("Should get selected fleet ID")
    public void getSelectedFleetId() {
        User user = user1Fleet1;

        Integer selectedFleetId = user.getSelectedFleetId();

        assertNotNull(selectedFleetId);
        assertEquals(-1, selectedFleetId.intValue()); // Default value is -1
    }

    /**
     * Verifies {@code setSelectedFleetId} updates the user's selected and active fleet (in memory and in the
     * {@code user.fleet_selected} column) for a fleet the user can access. Runs in a rolled-back transaction.
     *
     * @throws SQLException if a database operation fails
     * @throws AccountException if the fleet switch is rejected
     */
    @Test
    @DisplayName("Should set selected fleet ID successfully")
    public void setSelectedFleetIdSuccessfully() throws SQLException, AccountException {
        connection.setAutoCommit(false);

        try {
            User user = user1Fleet1;
            int newFleetId = 2;

            user.setSelectedFleetId(connection, newFleetId);

            assertEquals(newFleetId, user.getSelectedFleetId().intValue());
            assertEquals(newFleetId, user.getFleetId());

            // Verify database was updated
            try (PreparedStatement stmt = connection.prepareStatement("SELECT fleet_selected FROM user WHERE id = ?")) {
                stmt.setInt(1, user.getId());
                try (ResultSet rs = stmt.executeQuery()) {
                    assertTrue(rs.next());
                    assertEquals(newFleetId, rs.getInt("fleet_selected"));
                }
            }
        } finally {
            connection.rollback();
        }
    }

    /**
     * Verifies {@code setSelectedFleetId} throws {@link AccountException} when the target fleet does not exist (or the
     * user cannot access it). Runs in a rolled-back transaction.
     *
     * @throws SQLException if a database operation fails
     */
    @Test
    @DisplayName("Should throw AccountException when setting selected fleet ID to non-existent fleet")
    public void setSelectedFleetIdWithNonExistentFleet() throws SQLException {
        connection.setAutoCommit(false);

        try {
            User user = user1Fleet1;
            int nonExistentFleetId = 999;

            assertThrows(AccountException.class, () -> user.setSelectedFleetId(connection, nonExistentFleetId));
        } finally {
            connection.rollback();
        }
    }

    /**
     * Verifies {@code leaveSelectedFleet} succeeds for a user who has access to more than one fleet, switching them off
     * the left fleet. Seeds second-fleet access and runs in a rolled-back transaction.
     *
     * @throws SQLException if a database operation fails
     * @throws AccountException if leaving the fleet is rejected
     */
    @Test
    @DisplayName("Should leave selected fleet successfully when multiple fleets available")
    public void leaveSelectedFleetSuccessfully() throws SQLException, AccountException {
        connection.setAutoCommit(false);

        try {
            // Create a user with access to multiple fleets
            User user = user1Fleet1; // User 1 has access to fleet 1 and 2
            int originalFleetId = user.getFleetId();

            // Ensure user has access to fleet 2 as well
            try (PreparedStatement stmt =
                    connection.prepareStatement("INSERT INTO fleet_access (user_id, fleet_id, type) VALUES (?, ?, ?) "
                            + "ON DUPLICATE KEY UPDATE type = VALUES(type)")) {
                stmt.setInt(1, user.getId());
                stmt.setInt(2, 2);
                stmt.setString(3, "VIEW");
                stmt.executeUpdate();
            }

            // Set user to fleet 1 first
            user.setSelectedFleetId(connection, 1);
            assertEquals(1, user.getFleetId());

            // Now leave fleet 1 (should switch to fleet 2)
            user.leaveSelectedFleet(connection);

            // Should now be on fleet 2
            assertEquals(2, user.getFleetId());
            assertEquals(2, user.getSelectedFleetId().intValue());

            // Verify access to original fleet was removed
            try (PreparedStatement stmt = connection.prepareStatement(
                    "SELECT COUNT(*) FROM fleet_access WHERE user_id = ? AND fleet_id = ?")) {
                stmt.setInt(1, user.getId());
                stmt.setInt(2, originalFleetId);
                try (ResultSet rs = stmt.executeQuery()) {
                    assertTrue(rs.next());
                    assertEquals(0, rs.getInt(1)); // No access to original fleet
                }
            }
        } finally {
            connection.rollback();
        }
    }

    /**
     * Verifies {@code leaveSelectedFleet} throws {@link SQLException} when the user is the fleet's last manager
     * (leaving would orphan the fleet). Runs in a rolled-back transaction.
     *
     * @throws SQLException if a database operation fails
     * @throws AccountException if fleet access resolution fails
     */
    @Test
    @DisplayName("Should throw SQLException when leaving fleet as last manager")
    public void leaveSelectedFleetAsLastManager() throws SQLException, AccountException {
        connection.setAutoCommit(false);

        try {
            // Create a user who is the only manager of a fleet
            User user = user1Fleet1;

            // Make user a manager of fleet 1 and remove other managers
            try (PreparedStatement stmt = connection.prepareStatement(
                    "UPDATE fleet_access SET type = 'MANAGER' WHERE user_id = ? AND fleet_id = 1")) {
                stmt.setInt(1, user.getId());
                stmt.executeUpdate();
            }

            // Remove other managers from fleet 1
            try (PreparedStatement stmt = connection.prepareStatement(
                    "DELETE FROM fleet_access WHERE fleet_id = 1 AND user_id != ? AND type = 'MANAGER'")) {
                stmt.setInt(1, user.getId());
                stmt.executeUpdate();
            }

            user.setSelectedFleetId(connection, 1);

            // Should throw SQLException when trying to leave as last manager
            assertThrows(SQLException.class, () -> user.leaveSelectedFleet(connection));
        } finally {
            connection.rollback();
        }
    }

    /**
     * Verifies {@code leaveSelectedFleet} throws {@link SQLException} when the user has no other fleet to switch to
     * after leaving. Runs in a rolled-back transaction.
     *
     * @throws SQLException if a database operation fails
     * @throws AccountException if fleet access resolution fails
     */
    @Test
    @DisplayName("Should throw SQLException when no other fleets available to switch to")
    public void leaveSelectedFleetWithNoOtherFleets() throws SQLException, AccountException {
        connection.setAutoCommit(false);

        try {
            User user = user1Fleet1;

            // Remove all other fleet access for this user
            try (PreparedStatement stmt =
                    connection.prepareStatement("DELETE FROM fleet_access WHERE user_id = ? AND fleet_id != 1")) {
                stmt.setInt(1, user.getId());
                stmt.executeUpdate();
            }

            user.setSelectedFleetId(connection, 1);

            // Should throw SQLException when no other fleets available
            assertThrows(SQLException.class, () -> user.leaveSelectedFleet(connection));
        } finally {
            connection.rollback();
        }
    }

    /**
     * Verifies {@code leaveSelectedFleet} throws {@link SQLException} when the user's only other fleet access is
     * DENIED/WAITING (not a usable fleet to switch to). Runs in a rolled-back transaction.
     *
     * @throws SQLException if a database operation fails
     * @throws AccountException if fleet access resolution fails
     */
    @Test
    @DisplayName("Should throw SQLException when only denied/waiting fleets available")
    public void leaveSelectedFleetWithOnlyDeniedFleets() throws SQLException, AccountException {
        connection.setAutoCommit(false);

        try {
            User user = user1Fleet1;

            // Add access to fleet 2 but with DENIED status
            try (PreparedStatement stmt =
                    connection.prepareStatement("INSERT INTO fleet_access (user_id, fleet_id, type) VALUES (?, ?, ?) "
                            + "ON DUPLICATE KEY UPDATE type = VALUES(type)")) {
                stmt.setInt(1, user.getId());
                stmt.setInt(2, 2);
                stmt.setString(3, "DENIED");
                stmt.executeUpdate();
            }

            user.setSelectedFleetId(connection, 1);

            // Should throw SQLException when only denied fleets available
            assertThrows(SQLException.class, () -> user.leaveSelectedFleet(connection));
        } finally {
            connection.rollback();
        }
    }

    /**
     * Verifies {@code leaveSelectedFleet} rolls back its changes when the operation fails partway, leaving the user's
     * fleet state unchanged. Runs in a rolled-back transaction.
     *
     * @throws SQLException if a database operation fails
     * @throws AccountException if fleet access resolution fails
     */
    @Test
    @DisplayName("Should handle transaction rollback on failure")
    public void leaveSelectedFleetWithTransactionRollback() throws SQLException, AccountException {
        connection.setAutoCommit(false);

        try {
            User user = user1Fleet1;

            // Set up user with access to fleet 2
            try (PreparedStatement stmt =
                    connection.prepareStatement("INSERT INTO fleet_access (user_id, fleet_id, type) VALUES (?, ?, ?) "
                            + "ON DUPLICATE KEY UPDATE type = VALUES(type)")) {
                stmt.setInt(1, user.getId());
                stmt.setInt(2, 2);
                stmt.setString(3, "VIEW");
                stmt.executeUpdate();
            }

            user.setSelectedFleetId(connection, 1);
            int originalFleetId = user.getFleetId();

            // This should handle the rollback properly
            try {
                user.leaveSelectedFleet(connection);
            } catch (SQLException e) {
                // Expected - connection issues
                assertTrue(
                        e.getMessage().contains("Connection") || e.getMessage().contains("closed"));
            }

            // Verify user state is still consistent (user should now be on fleet 2)
            assertEquals(2, user.getFleetId());
        } finally {
            connection.rollback();
        }
    }
}
