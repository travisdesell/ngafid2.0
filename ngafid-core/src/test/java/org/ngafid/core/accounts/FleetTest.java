package org.ngafid.core.accounts;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.ngafid.core.H2Database;
import org.ngafid.core.TestWithConnection;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

public class FleetTest extends TestWithConnection {
    /**
     * A List of all test fleets in the test data
     */
    private static final List<Fleet> targets = List.of(new Fleet(1, "Test Fleet with ID 1"), new Fleet(2, "Test Fleet with ID 2"));
    private static final Fleet fleet1 = new Fleet(1, "Test Fleet");
    private static final Fleet airsyncFleet = new Fleet(2, "Airsync Fleet");
    private static User user1 = null;

    @BeforeAll
    public static void setup() throws SQLException, AccountException {
        try (Connection connection = H2Database.getConnection()) {
            user1 = User.get(connection, 1, 1);
        }
    }

    @Test
    void constructor() {
        assertEquals(1, fleet1.getId());
        assertEquals("Test Fleet", fleet1.getName());
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
    void get() throws SQLException, AccountException {
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

    }

    @Test
    void getAllFleets() throws SQLException {
        List<Fleet> fleets = Fleet.getAllFleets(connection);
        fleets.sort(Comparator.comparingInt(Fleet::getId));

        assertEquals(targets, fleets);
    }

    @Test
    void exists() throws SQLException {
        for (Fleet target : targets) {
            assertTrue(Fleet.exists(connection, target.getName()));
        }

        assertFalse(Fleet.exists(connection, "Fleet that does not exist"));
    }

    @Test
    void populateUsers() throws SQLException {
        assertEquals(3, fleet1.getUsers(connection).size());
    }

    @Test
    void getWaitingUserCount() {
        try {
            Fleet fleet = targets.get(0);
            assertEquals(0, fleet.getWaitingUserCount(connection));

            fleet = targets.get(1);
            assertEquals(2, fleet.getWaitingUserCount(connection));

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void hasAirsync() {
        try {
            assertFalse(fleet1.hasAirsync(connection));
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
