package org.ngafid.core.accounts;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.*;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.PreparedStatement;
import java.util.ArrayList;

import org.ngafid.core.H2Database;

public class FleetAccessNamedTest {

    private Connection connection;

    @BeforeEach
    public void setUp() throws SQLException {
        // Get connection from H2Database
        connection = H2Database.getConnection();
        
        // Clean up any existing test data
        try (PreparedStatement stmt = connection.prepareStatement(
                "DELETE FROM fleet_access WHERE user_id IN (999, 998, 997)")) {
            stmt.executeUpdate();
        }
        
        // Create test data to ensure coverage
        createTestData();
    }
    
    private void createTestData() throws SQLException {
        // Create test fleets
        try (PreparedStatement stmt = connection.prepareStatement(
                "INSERT INTO fleet (id, fleet_name) VALUES (999, 'Test Fleet 999'), (998, 'Test Fleet 998') ON DUPLICATE KEY UPDATE fleet_name = VALUES(fleet_name)")) {
            stmt.executeUpdate();
        }
        
        // Create test users
        try (PreparedStatement stmt = connection.prepareStatement(
                "INSERT INTO user (id, email, password_token, first_name, last_name, address, city, country, state, zip_code, phone_number, reset_phrase, registration_time, admin, aggregate_view, last_login_time, fleet_selected, two_factor_enabled, two_factor_secret, backup_codes, two_factor_setup_complete) VALUES (999, 'test999@example.com', 'aaaaaaaaaaaaaaaaaaaa', 'Test', 'User999', '123 Test St', 'Test City', 'Test Country', 'Test State', '12345', '123-456-7890', '', CURRENT_DATE, 0, 0, CURRENT_DATE, -1, FALSE, NULL, NULL, FALSE), (998, 'test998@example.com', 'aaaaaaaaaaaaaaaaaaaa', 'Test', 'User998', '123 Test St', 'Test City', 'Test Country', 'Test State', '12345', '123-456-7890', '', CURRENT_DATE, 0, 0, CURRENT_DATE, -1, FALSE, NULL, NULL, FALSE) ON DUPLICATE KEY UPDATE email = VALUES(email)")) {
            stmt.executeUpdate();
        }
        
        // Create test fleet access entries
        try (PreparedStatement stmt = connection.prepareStatement(
                "INSERT INTO fleet_access (user_id, fleet_id, type) VALUES (999, 999, 'VIEW'), (999, 998, 'MANAGER'), (998, 999, 'UPLOAD') ON DUPLICATE KEY UPDATE type = VALUES(type)")) {
            stmt.executeUpdate();
        }
    }

    @AfterEach
    public void tearDown() throws SQLException {
        // Clean up test data
        if (connection != null) {
            try (PreparedStatement stmt = connection.prepareStatement(
                    "DELETE FROM fleet_access WHERE user_id IN (999, 998, 997)")) {
                stmt.executeUpdate();
            }
            try (PreparedStatement stmt = connection.prepareStatement(
                    "DELETE FROM user WHERE id IN (999, 998, 997)")) {
                stmt.executeUpdate();
            }
            try (PreparedStatement stmt = connection.prepareStatement(
                    "DELETE FROM fleet WHERE id IN (999, 998, 997)")) {
                stmt.executeUpdate();
            }
        }
    }

    @Test
    @DisplayName("Should test getFleetName method")
    public void testGetFleetName() throws SQLException, AccountException {
        // Get a FleetAccessNamed object from the database using existing test data
        ArrayList<FleetAccess> allAccess = FleetAccessNamed.getAllFleetAccessEntries(connection, 1);
        FleetAccessNamed fleetAccessNamed = (FleetAccessNamed) allAccess.get(0);
        
        // Test that getFleetName returns the fleet name from database
        assertNotNull(fleetAccessNamed.getFleetName());
        assertTrue(fleetAccessNamed.getFleetName().contains("Test Fleet"));
    }

    @Test
    @DisplayName("Should test updateFleetName method with real database operations")
    public void testUpdateFleetNameWithRealDatabaseOperations() throws SQLException, AccountException {
        // Get a FleetAccessNamed object from the database using existing test data
        ArrayList<FleetAccess> allAccess = FleetAccessNamed.getAllFleetAccessEntries(connection, 999);
        FleetAccessNamed fleetAccessNamed = (FleetAccessNamed) allAccess.get(0);
        
        // Store the original fleet name
        String originalFleetName = fleetAccessNamed.getFleetName();
        assertNotNull(originalFleetName);
        
        // Update the fleet name in the database to test the updateFleetName method
        try (PreparedStatement stmt = connection.prepareStatement(
                "UPDATE fleet SET fleet_name = ? WHERE id = ?")) {
            stmt.setString(1, "Updated Test Fleet Name");
            stmt.setInt(2, 999);
            stmt.executeUpdate();
        }
        
        // Clear the fleet name to force it to be reloaded
        fleetAccessNamed.updateFleetName(connection);
        
        // Verify the fleet name was updated
        assertNotNull(fleetAccessNamed.getFleetName());
        assertEquals("Updated Test Fleet Name", fleetAccessNamed.getFleetName());
        
        // Restore the original fleet name
        try (PreparedStatement stmt = connection.prepareStatement(
                "UPDATE fleet SET fleet_name = ? WHERE id = ?")) {
            stmt.setString(1, originalFleetName);
            stmt.setInt(2, 999);
            stmt.executeUpdate();
        }
    }

    @Test
    @DisplayName("Should test updateFleetName with real database")
    public void testUpdateFleetNameWithRealDatabase() throws SQLException, AccountException {
        // Get a FleetAccessNamed object from the database using existing test data
        ArrayList<FleetAccess> allAccess = FleetAccessNamed.getAllFleetAccessEntries(connection, 999);
        FleetAccessNamed fleetAccessNamed = (FleetAccessNamed) allAccess.get(0);
        
        // The fleet name should already be populated from getAllFleetAccessEntries
        assertNotNull(fleetAccessNamed.getFleetName());
        assertTrue(fleetAccessNamed.getFleetName().contains("Test Fleet"));
        
        // Test updateFleetName method (should work the same way)
        fleetAccessNamed.updateFleetName(connection);
        
        // Fleet name should still be set correctly
        assertNotNull(fleetAccessNamed.getFleetName());
        assertTrue(fleetAccessNamed.getFleetName().contains("Test Fleet"));
    }
    
    @Test
    @DisplayName("Should test updateFleetName method with different fleet access types")
    public void testUpdateFleetNameWithDifferentAccessTypes() throws SQLException, AccountException {
        // Test with user 999 who has multiple fleet access entries
        ArrayList<FleetAccess> allAccess = FleetAccessNamed.getAllFleetAccessEntries(connection, 999);
        
        // Verify we have the expected number of entries
        assertEquals(2, allAccess.size());
        
        // Test each fleet access entry
        for (FleetAccess access : allAccess) {
            assertTrue(access instanceof FleetAccessNamed);
            FleetAccessNamed namedAccess = (FleetAccessNamed) access;
            
            // Fleet name should be populated
            assertNotNull(namedAccess.getFleetName());
            assertTrue(namedAccess.getFleetName().contains("Test Fleet"));
            
            // Test updateFleetName method
            namedAccess.updateFleetName(connection);
            
            // Fleet name should still be set correctly after update
            assertNotNull(namedAccess.getFleetName());
            assertTrue(namedAccess.getFleetName().contains("Test Fleet"));
        }
    }

    @Test
    @DisplayName("Should test getAllFleetAccessEntries with real database")
    public void testGetAllFleetAccessEntriesWithRealDatabase() throws SQLException, AccountException {
        // Test with user 999 who has existing access
        ArrayList<FleetAccess> allAccess = FleetAccessNamed.getAllFleetAccessEntries(connection, 999);
        
        // Verify results - user 999 should have 2 fleet access entries
        assertNotNull(allAccess);
        assertEquals(2, allAccess.size());
        
        // Verify all entries are FleetAccessNamed instances with fleet names
        for (FleetAccess access : allAccess) {
            assertTrue(access instanceof FleetAccessNamed);
            FleetAccessNamed namedAccess = (FleetAccessNamed) access;
            assertNotNull(namedAccess.getFleetName());
            // Verify fleet names are populated
            assertTrue(namedAccess.getFleetName().contains("Test Fleet"));
        }
    }
    
    @Test
    @DisplayName("Should test getAllFleetAccessEntries with user having no access")
    public void testGetAllFleetAccessEntriesWithNoAccess() throws SQLException, AccountException {
        // Test with user 997 who has no access
        ArrayList<FleetAccess> allAccess = FleetAccessNamed.getAllFleetAccessEntries(connection, 997);
        
        // Verify results - user 997 should have no fleet access entries
        assertNotNull(allAccess);
        assertEquals(0, allAccess.size());
    }
    
    @Test
    @DisplayName("Should test getAllFleetAccessEntries with user having single access")
    public void testGetAllFleetAccessEntriesWithSingleAccess() throws SQLException, AccountException {
        // Test with user 998 who has single access
        ArrayList<FleetAccess> allAccess = FleetAccessNamed.getAllFleetAccessEntries(connection, 998);
        
        // Verify results - user 998 should have 1 fleet access entry
        assertNotNull(allAccess);
        assertEquals(1, allAccess.size());
        
        // Verify the entry is FleetAccessNamed with fleet name
        FleetAccess access = allAccess.get(0);
        assertTrue(access instanceof FleetAccessNamed);
        FleetAccessNamed namedAccess = (FleetAccessNamed) access;
        assertNotNull(namedAccess.getFleetName());
        assertTrue(namedAccess.getFleetName().contains("Test Fleet"));
    }

    @Test
    @DisplayName("Should test getAllFleetAccessEntries with different user scenarios")
    public void testGetAllFleetAccessEntriesWithDifferentUserScenarios() throws SQLException, AccountException {
        // Test with user 999 who has multiple access entries
        ArrayList<FleetAccess> user999Access = FleetAccessNamed.getAllFleetAccessEntries(connection, 999);
        assertEquals(2, user999Access.size());
        
        // Test with user 998 who has single access entry
        ArrayList<FleetAccess> user998Access = FleetAccessNamed.getAllFleetAccessEntries(connection, 998);
        assertEquals(1, user998Access.size());
        
        // Test with user 997 who has no access entries
        ArrayList<FleetAccess> user997Access = FleetAccessNamed.getAllFleetAccessEntries(connection, 997);
        assertEquals(0, user997Access.size());
        
        // Verify all entries are properly populated with fleet names
        for (FleetAccess access : user999Access) {
            assertTrue(access instanceof FleetAccessNamed);
            FleetAccessNamed namedAccess = (FleetAccessNamed) access;
            assertNotNull(namedAccess.getFleetName());
            assertTrue(namedAccess.getFleetName().contains("Test Fleet"));
        }
        
        for (FleetAccess access : user998Access) {
            assertTrue(access instanceof FleetAccessNamed);
            FleetAccessNamed namedAccess = (FleetAccessNamed) access;
            assertNotNull(namedAccess.getFleetName());
            assertTrue(namedAccess.getFleetName().contains("Test Fleet"));
        }
    }
    
    @Test
    @DisplayName("Should test fleet access with real database operations and edge cases")
    public void testFleetAccessWithRealDatabaseOperationsAndEdgeCases() throws SQLException, AccountException {
        // Test with a user who has access to multiple fleets
        ArrayList<FleetAccess> allAccess = FleetAccessNamed.getAllFleetAccessEntries(connection, 999);
        
        // Verify we have the expected number of entries
        assertEquals(2, allAccess.size());
        
        // Test each entry individually
        for (int i = 0; i < allAccess.size(); i++) {
            FleetAccess access = allAccess.get(i);
            assertTrue(access instanceof FleetAccessNamed);
            FleetAccessNamed namedAccess = (FleetAccessNamed) access;
            
            // Verify basic properties
            assertNotNull(namedAccess.getFleetName());
            assertTrue(namedAccess.getFleetName().contains("Test Fleet"));
            assertTrue(namedAccess.getFleetId() > 0);
            assertEquals(999, namedAccess.getUserId());
            
            // Test updateFleetName method
            namedAccess.updateFleetName(connection);
            
            // Verify fleet name is still set after update
            assertNotNull(namedAccess.getFleetName());
            assertTrue(namedAccess.getFleetName().contains("Test Fleet"));
        }
    }



}
