package org.ngafid.core.account;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.ngafid.core.H2Database;
import org.ngafid.core.accounts.Fleet;
import org.ngafid.core.accounts.User;

import java.sql.Connection;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

public class FleetTest {
    private Fleet fleet0 = new Fleet(0, "Test Fleet");
    private Fleet airsyncFleet = new Fleet(1, "Airsync Fleet");
    private static User user0 = null;


    @BeforeAll
    public static void setup() {
        try (Connection connection = H2Database.getConnection()) {
            user0 = User.get(connection, 0, 0);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void testConstructor() {
        assertEquals(0, fleet0.getId());
        assertEquals("Test Fleet", fleet0.getName());
    }

    @Test
    void testPopulateUsers() {
        try (Connection connection = H2Database.getConnection()) {
            // Bogus user id -- should yield every added user.
            fleet0.populateUsers(connection, 12312345);
            assertEquals(2, fleet0.getUsers().size());

            fleet0.populateUsers(connection, 0);
            assertEquals(1, fleet0.getUsers().size());

        } catch (SQLException e) {
            fail("SQLException thrown: " + e.getMessage());
        }
    }

    @Test
    void testHasAirsync() {
        try (Connection connection = H2Database.getConnection()) {
            assertFalse(fleet0.hasAirsync(connection));
            assertTrue(airsyncFleet.hasAirsync(connection));
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

}
