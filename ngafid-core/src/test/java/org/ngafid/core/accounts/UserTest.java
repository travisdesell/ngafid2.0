package org.ngafid.core.accounts;

import java.sql.SQLException;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.ngafid.core.TestWithConnection;

public class UserTest extends TestWithConnection {
    private static final Logger LOG = Logger.getLogger(UserTest.class.getName());

    private User user1Fleet1;
    private User user1Fleet2;
    private User user2Fleet1;
    private User user2Fleet2;
    private User user3fleet1;

    @BeforeEach
    public void initUsers() throws SQLException, AccountException {
        user1Fleet1 = User.get(connection, 1, 1);
        user1Fleet2 = User.get(connection, 1, 2);
        user2Fleet1 = User.get(connection, 2, 1);
        user2Fleet2 = User.get(connection, 2, 2);
        user3fleet1 = User.get(connection, 2, 1);
    }

    @Test
    public void get() throws SQLException, AccountException {
        assertEquals(User.get(connection, 1, 1), new User(connection, 1, "test@email.com", "John", "Doe", "123 House Road", "CityName", "CountryName",
                "StateName", "10001", "", false, false, 1, -1));
        assertEquals(User.get(connection, 1, 2), new User(connection, 1, "test@email.com", "John", "Doe", "123 House Road", "CityName", "CountryName",
                "StateName", "10001", "", false, false, 2, -1));

        assertEquals(User.get(connection, 2, 1), new User(connection, 2, "test1@email.com", "John Admin", "Aggregate Doe", "123 House Road", "CityName", "CountryName",
                "StateName", "10001", "", true, true, 1, 1));
        assertEquals(User.get(connection, 2, 2), new User(connection, 2, "test1@email.com", "John Admin", "Aggregate Doe", "123 House Road", "CityName", "CountryName",
                "StateName", "10001", "", true, true, 2, 1));
    }

    @Test
    public void hasFlightAccess() throws SQLException {
        // Invalid flight id -- should return false
        assertFalse(user1Fleet1.hasFlightAccess(connection, 0));

        // User denied access to fleet
        assertFalse(user3fleet1.hasFlightAccess(connection, 0));

        // user.fleet_id != flight.fleet_id
        assertFalse(user1Fleet2.hasFlightAccess(connection, 1));

        // valid
        assertTrue(user1Fleet1.hasFlightAccess(connection, 1));
        assertTrue(user1Fleet1.hasFlightAccess(connection, 2));

        assertTrue(user2Fleet1.hasFlightAccess(connection, 1));
        assertTrue(user2Fleet1.hasFlightAccess(connection, 2));
    }

    @Test
    public void createNewFleetUser() throws SQLException, AccountException {
        connection.setAutoCommit(false);

        // Duplicate email and fleet
        assertThrows(AccountException.class, () -> User.createNewFleetUser(
                connection, user1Fleet1.getEmail(), "pass", "first", "last", "country", "state", "city", "address", "phone", "zip", "Test Fleet with ID 1"
        ));

        // Duplicate email
        assertThrows(AccountException.class, () -> User.createNewFleetUser(
                connection, user1Fleet1.getEmail(), "pass", "first", "last", "country", "state", "city", "address", "phone", "zip", "Fleet that doesn't exist 1000"
        ));

        // Duplicate fleet
        assertThrows(AccountException.class, () -> User.createNewFleetUser(
                connection, "coolemail@mail.com", "pass", "first", "last", "country", "state", "city", "address", "phone", "zip", "Test Fleet with ID 1"
        ));

        // Unique fleet and email -- should work
        User.createNewFleetUser(connection, "coolemail@mail.com", "pass", "first", "last", "country", "state", "city", "address", "phone", "zip", "cool fleet 100");

        connection.rollback();
    }

    @Test
    public void getSet() throws SQLException {
        assertEquals(1, user1Fleet1.getId());
        assertEquals("test@email.com", user1Fleet1.getEmail());
        assertEquals("John Doe", user1Fleet1.getFullName());
        assertEquals(1, user1Fleet1.getFleetId());
        assertEquals("VIEW", user1Fleet1.getFleetAccessType());

        assertEquals(2, user1Fleet2.getWaitingUserCount(connection));
        assertEquals(0, user1Fleet1.getUnconfirmedTailsCount(connection));
    }

    @Test
    public void permissions() {
        assertFalse(user1Fleet1.isAdmin());
        assertFalse(user1Fleet1.hasAggregateView());

        assertFalse(user1Fleet1.managesFleet(1));
        assertTrue(user2Fleet1.managesFleet(1));

        assertFalse(user1Fleet1.hasUploadAccess(1));
        assertTrue(user2Fleet1.hasUploadAccess(1));

        assertTrue(user1Fleet1.hasViewAccess(1));
        assertTrue(user2Fleet1.hasViewAccess(1));
        assertFalse(user1Fleet2.hasViewAccess(2));
        assertFalse(user2Fleet2.hasViewAccess(2));
    }

}
