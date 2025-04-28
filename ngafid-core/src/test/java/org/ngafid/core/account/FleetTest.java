package org.ngafid.core.account;

import org.junit.jupiter.api.*;
import org.ngafid.core.H2Database;
import org.ngafid.core.accounts.AccountException;
import org.ngafid.core.accounts.Fleet;
import org.ngafid.core.accounts.User;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

public class FleetTest {
    /**
     * A List of all test fleets in the test data
     */
    private static final List<Fleet> targets = List.of(new Fleet(0, "Test Fleet with ID 0"), new Fleet(1, "Test Fleet with ID 1"));
    private static final Fleet fleet0 = new Fleet(0, "Test Fleet");
    private static final Fleet airsyncFleet = new Fleet(1, "Airsync Fleet");
    private static User user0 = null;

    private Connection connection = null;

    @BeforeEach
    public void setupConnection() {
        try {
            connection = H2Database.getConnection();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @AfterEach
    public void closeConnection() {
        try {
            connection.close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @BeforeAll
    public static void setup() {
        try (Connection connection = H2Database.getConnection()) {
            user0 = User.get(connection, 0, 0);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void cosntructor() {
        assertEquals(0, fleet0.getId());
        assertEquals("Test Fleet", fleet0.getName());
    }

    @Test
    void getNumberFleets() {
        try {
            assertEquals(2, Fleet.getNumberFleets(connection));
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void get() {
        try {
            for (Fleet target : targets) {
                List<Fleet> flts = List.of(
                        Objects.requireNonNull(Fleet.get(connection, target.getId())),
                        Objects.requireNonNull(Fleet.get(connection, target.getName()))
                );
                for (Fleet f : flts) {
                    assertEquals(target.getId(), f.getId());
                    assertEquals(target.getName(), f.getName());
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void getAllFleets() {
        try {
            List<Fleet> fleets = Fleet.getAllFleets(connection);
            fleets.sort(Comparator.comparingInt(Fleet::getId));

            assertEquals(targets, fleets);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void exists() {
        try {
            for (Fleet target : targets) {
                assertTrue(Fleet.exists(connection, target.getName()));
            }

            assertFalse(Fleet.exists(connection, "Fleet that does not exist"));
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void populateUsers() {
        try {
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
    void getWaitingUserCount() {
        try {
            Fleet fleet = targets.get(0);
            fleet.populateUsers(connection, -1);
            assertEquals(0, fleet.getWaitingUserCount());

            fleet = targets.get(1);
            fleet.populateUsers(connection, -1);
            assertEquals(2, fleet.getWaitingUserCount());

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void hasAirsync() {
        try {
            assertFalse(fleet0.hasAirsync(connection));
            assertTrue(airsyncFleet.hasAirsync(connection));
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @AfterAll
    public static void create() {
        try (Connection connection = H2Database.getConnection()) {
            Fleet newFleet = Fleet.create(connection, "Cool Fleet");
            assertFalse(targets.stream().anyMatch(f -> f.getId() == newFleet.getId()));
            assertEquals("Cool Fleet", newFleet.getName());
        } catch (SQLException | AccountException e) {
            throw new RuntimeException(e);
        }
    }

    @AfterAll
    public static void create2() {
        for (Fleet fleet : targets) {
            assertThrows(AccountException.class, () -> {
                try (Connection connection = H2Database.getConnection()) {
                    Fleet.create(connection, fleet.getName());
                }
            });
        }
    }

}
