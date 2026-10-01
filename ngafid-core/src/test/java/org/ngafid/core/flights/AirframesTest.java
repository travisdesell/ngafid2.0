package org.ngafid.core.flights;

import static org.junit.jupiter.api.Assertions.*;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.ngafid.core.TestWithConnection;

/**
 * Airframes class.
 * Testsall methods, constants, inner classes, and edge cases to achieve 100% coverage.
 */
public class AirframesTest extends TestWithConnection {

    /**
     * Resets the airframe fixtures before each test by clearing any leftover rows and re-inserting the known test
     * airframe types, airframes, and fleet-airframe relationships.
     *
     * @throws SQLException if clearing or inserting the fixtures fails
     */
    @BeforeEach
    public void setUp() throws SQLException {
        // Clear any existing test data
        clearTestData();
        // Insert test data
        insertTestData();
    }

    /**
     * Removes the seeded airframe fixtures after each test so the shared in-memory database is left clean.
     *
     * @throws SQLException if the cleanup deletes fail
     */
    @AfterEach
    public void tearDown() throws SQLException {
        // Clean up test data
        clearTestData();
    }

    private void clearTestData() throws SQLException {
        // Use higher IDs to avoid conflicts with existing data
        try (PreparedStatement stmt =
                connection.prepareStatement("DELETE FROM fleet_airframes WHERE airframe_id IN (1001, 1002, 1003)")) {
            stmt.executeUpdate();
        }
        // Delete flights that reference airframes first
        try (PreparedStatement stmt =
                connection.prepareStatement("DELETE FROM flights WHERE airframe_id IN (1001, 1002, 1003)")) {
            stmt.executeUpdate();
        }
        // Delete airframes that reference airframe_types
        try (PreparedStatement stmt =
                connection.prepareStatement("DELETE FROM airframes WHERE id IN (1001, 1002, 1003)")) {
            stmt.executeUpdate();
        }
        // Finally delete airframe_types
        try (PreparedStatement stmt =
                connection.prepareStatement("DELETE FROM airframe_types WHERE id IN (1001, 1002)")) {
            stmt.executeUpdate();
        }
    }

    private void insertTestData() throws SQLException {
        // Insert airframe types
        try (PreparedStatement stmt =
                connection.prepareStatement("INSERT INTO airframe_types (id, name) VALUES (?, ?)")) {
            stmt.setInt(1, 1001);
            stmt.setString(2, "Test Fixed Wing");
            try {
                stmt.executeUpdate();
            } catch (SQLException e) {
                // Ignore if already exists
            }

            stmt.setInt(1, 1002);
            stmt.setString(2, "Test Rotorcraft");
            try {
                stmt.executeUpdate();
            } catch (SQLException e) {
                // Ignore if already exists
            }
        }

        // Insert test airframes
        try (PreparedStatement stmt =
                connection.prepareStatement("INSERT INTO airframes (id, airframe, type_id) VALUES (?, ?, ?)")) {
            stmt.setInt(1, 1001);
            stmt.setString(2, "Test Cessna 172S");
            stmt.setInt(3, 1001);
            try {
                stmt.executeUpdate();
            } catch (SQLException e) {
                // Ignore if already exists
            }

            stmt.setInt(1, 1002);
            stmt.setString(2, "Test PA-28-181");
            stmt.setInt(3, 1001);
            try {
                stmt.executeUpdate();
            } catch (SQLException e) {
                // Ignore if already exists
            }

            stmt.setInt(1, 1003);
            stmt.setString(2, "Test R44");
            stmt.setInt(3, 1002);
            try {
                stmt.executeUpdate();
            } catch (SQLException e) {
                // Ignore if already exists
            }
        }

        // Insert fleet-airframe relationships
        try (PreparedStatement stmt =
                connection.prepareStatement("INSERT INTO fleet_airframes (fleet_id, airframe_id) VALUES (?, ?)")) {
            stmt.setInt(1, 1);
            stmt.setInt(2, 1001);
            try {
                stmt.executeUpdate();
            } catch (SQLException e) {
                // Ignore if already exists
            }

            stmt.setInt(1, 1);
            stmt.setInt(2, 1002);
            try {
                stmt.executeUpdate();
            } catch (SQLException e) {
                // Ignore if already exists
            }

            stmt.setInt(1, 2);
            stmt.setInt(2, 1003);
            try {
                stmt.executeUpdate();
            } catch (SQLException e) {
                // Ignore if already exists
            }
        }
    }

    // ========== CONSTANTS AND STATIC FIELDS TESTS ==========

    /**
     * Verifies each public airframe-name constant holds its expected display string.
     */
    @Test
    @DisplayName("Should have correct airframe constants")
    public void testAirframeConstants() {
        assertEquals("ScanEagle", Airframes.AIRFRAME_SCAN_EAGLE);
        assertEquals("DJI", Airframes.AIRFRAME_DJI);
        assertEquals("Cessna 172S", Airframes.AIRFRAME_CESSNA_172S);
        assertEquals("Cessna 172R", Airframes.AIRFRAME_CESSNA_172R);
        assertEquals("Cessna 172T", Airframes.AIRFRAME_CESSNA_172T);
        assertEquals("Cessna 400", Airframes.AIRFRAME_CESSNA_400);
        assertEquals("Cessna 525", Airframes.AIRFRAME_CESSNA_525);
        assertEquals("Cessna Model 525", Airframes.AIRFRAME_CESSNA_MODEL_525);
        assertEquals("Cessna T182T", Airframes.AIRFRAME_CESSNA_T182T);
        assertEquals("Cessna 182T", Airframes.AIRFRAME_CESSNA_182T);
        assertEquals("PA-28-181", Airframes.AIRFRAME_PA_28_181);
        assertEquals("PA-44-180", Airframes.AIRFRAME_PA_44_180);
        assertEquals("Piper PA-46-500TP Meridian", Airframes.AIRFRAME_PIPER_PA_46_500TP_MERIDIAN);
        assertEquals("Cirrus SR20", Airframes.AIRFRAME_CIRRUS_SR20);
        assertEquals("Cirrus SR22", Airframes.AIRFRAME_CIRRUS_SR22);
        assertEquals("Beechcraft A36/G36", Airframes.AIRFRAME_BEECHCRAFT_A36_G36);
        assertEquals("Beechcraft G58", Airframes.AIRFRAME_BEECHCRAFT_G58);
        assertEquals("Diamond DA 40", Airframes.AIRFRAME_DIAMOND_DA_40);
        assertEquals("Diamond DA40", Airframes.AIRFRAME_DIAMOND_DA40);
        assertEquals("Diamond DA40NG", Airframes.AIRFRAME_DIAMOND_DA40NG);
        assertEquals("Diamond DA42NG", Airframes.AIRFRAME_DIAMOND_DA42NG);
        assertEquals("Diamond DA 40 F", Airframes.AIRFRAME_DIAMOND_DA_40_F);
        assertEquals("Quest Kodiak 100", Airframes.AIRFRAME_QUEST_KODIAK_100);
    }

    /**
     * Verifies the {@code FIXED_WING_AIRFRAMES} set contains every expected fixed-wing type and excludes rotorcraft.
     */
    @Test
    @DisplayName("Should have correct fixed wing airframes set")
    public void testFixedWingAirframes() {
        Set<String> fixedWing = Airframes.FIXED_WING_AIRFRAMES;
        assertNotNull(fixedWing);
        assertTrue(fixedWing.contains("Cessna 172R"));
        assertTrue(fixedWing.contains("Cessna 172S"));
        assertTrue(fixedWing.contains("Cessna 172T"));
        assertTrue(fixedWing.contains("Cessna 182T"));
        assertTrue(fixedWing.contains("Cessna T182T"));
        assertTrue(fixedWing.contains("Cessna Model 525"));
        assertTrue(fixedWing.contains("Cirrus SR20"));
        assertTrue(fixedWing.contains("Cirrus SR22"));
        assertTrue(fixedWing.contains("Diamond DA40"));
        assertTrue(fixedWing.contains("Diamond DA 40 F"));
        assertTrue(fixedWing.contains("Diamond DA40NG"));
        assertTrue(fixedWing.contains("Diamond DA42NG"));
        assertTrue(fixedWing.contains("PA-28-181"));
        assertTrue(fixedWing.contains("PA-44-180"));
        assertTrue(fixedWing.contains("Piper PA-46-500TP Meridian"));
        assertTrue(fixedWing.contains("Quest Kodiak 100"));
        assertTrue(fixedWing.contains("Cessna 400"));
        assertTrue(fixedWing.contains("Beechcraft A36/G36"));
        assertTrue(fixedWing.contains("Beechcraft G58"));
        assertFalse(fixedWing.contains("R44"));
        assertFalse(fixedWing.contains("Robinson R44"));
    }

    /**
     * Verifies {@code resolveGarminRotorcraftAirframeCode} maps Garmin rotorcraft display names (and already-canonical
     * codes) to their registry codes, and returns empty for a non-rotorcraft name.
     */
    @Test
    @DisplayName("Should resolve Garmin rotorcraft airframe names to registry codes")
    public void testGarminRotorcraftAirframeAliases() {
        assertEquals(Optional.of("407"), Airframes.resolveGarminRotorcraftAirframeCode("Bell 407"));
        assertEquals(Optional.of("EC130"), Airframes.resolveGarminRotorcraftAirframeCode("Airbus EC130"));
        assertEquals(Optional.of("AW119"), Airframes.resolveGarminRotorcraftAirframeCode("AW-119"));
        assertEquals(Optional.of("407"), Airframes.resolveGarminRotorcraftAirframeCode("407"));
        assertEquals(Optional.of("R44"), Airframes.resolveGarminRotorcraftAirframeCode("Robinson R44 Cadet"));
        assertTrue(Airframes.resolveGarminRotorcraftAirframeCode("Cessna 172S").isEmpty());
    }

    /**
     * Verifies the {@code ROTORCRAFT} set contains exactly the two expected rotorcraft names.
     */
    @Test
    @DisplayName("Should have correct rotorcraft set")
    public void testRotorcraftSet() {
        Set<String> rotorcraft = Airframes.ROTORCRAFT;
        assertNotNull(rotorcraft);
        assertTrue(rotorcraft.contains("R44"));
        assertTrue(rotorcraft.contains("Robinson R44"));
        assertEquals(2, rotorcraft.size());
    }

    /**
     * Verifies the {@code FLEET_ID_ALL} sentinel equals -1.
     */
    @Test
    @DisplayName("Should have correct fleet ID constant")
    public void testFleetIdAll() {
        assertEquals(-1, Airframes.FLEET_ID_ALL);
    }

    /**
     * Verifies the {@code AIRFRAME_ALIASES} map contains the expected default and fleet-scoped alias keys and maps each
     * to its canonical airframe name (empty string for the unknown placeholder).
     */
    @Test
    @DisplayName("Should have correct airframe aliases")
    public void testAirframeAliases() {
        Map<Airframes.AliasKey, String> aliases = Airframes.AIRFRAME_ALIASES;
        assertNotNull(aliases);
        assertTrue(aliases.containsKey(Airframes.defaultAlias("Unknown Aircraft")));
        assertTrue(aliases.containsKey(Airframes.defaultAlias("Diamond DA 40")));
        assertTrue(aliases.containsKey(new Airframes.AliasKey("Garmin Flight Display", 1)));
        assertTrue(aliases.containsKey(new Airframes.AliasKey("Robinson R44 Raven I", 1)));
        assertTrue(aliases.containsKey(Airframes.defaultAlias("Robinson R44")));
        assertTrue(aliases.containsKey(Airframes.defaultAlias("Robinson R44 Cadet")));
        assertTrue(aliases.containsKey(Airframes.defaultAlias("Cirrus SR22 (3600 GW)")));

        assertEquals("", aliases.get(Airframes.defaultAlias("Unknown Aircraft")));
        assertEquals("Diamond DA40", aliases.get(Airframes.defaultAlias("Diamond DA 40")));
        assertEquals("R44", aliases.get(new Airframes.AliasKey("Garmin Flight Display", 1)));
        assertEquals("R44", aliases.get(new Airframes.AliasKey("Robinson R44 Raven I", 1)));
        assertEquals("R44", aliases.get(Airframes.defaultAlias("Robinson R44")));
        assertEquals("R44", aliases.get(Airframes.defaultAlias("Robinson R44 Cadet")));
        assertEquals("Cirrus SR22", aliases.get(Airframes.defaultAlias("Cirrus SR22 (3600 GW)")));
    }

    // ========== ALIASKEY RECORD TESTS ==========

    /**
     * Verifies the {@code AliasKey} record exposes the name and fleet id passed to its constructor.
     */
    @Test
    @DisplayName("Should create AliasKey with name and fleetId")
    public void testAliasKeyConstructor() {
        String name = "Test Aircraft";
        int fleetId = 123;

        Airframes.AliasKey aliasKey = new Airframes.AliasKey(name, fleetId);

        assertEquals(name, aliasKey.name());
        assertEquals(fleetId, aliasKey.fleetId());
    }

    /**
     * Verifies {@code AliasKey} accepts a null name and still returns the supplied fleet id.
     */
    @Test
    @DisplayName("Should create AliasKey with null name")
    public void testAliasKeyWithNullName() {
        Airframes.AliasKey aliasKey = new Airframes.AliasKey(null, 123);

        assertNull(aliasKey.name());
        assertEquals(123, aliasKey.fleetId());
    }

    /**
     * Verifies {@code AliasKey} accepts a negative fleet id.
     */
    @Test
    @DisplayName("Should create AliasKey with negative fleetId")
    public void testAliasKeyWithNegativeFleetId() {
        Airframes.AliasKey aliasKey = new Airframes.AliasKey("Test", -1);

        assertEquals("Test", aliasKey.name());
        assertEquals(-1, aliasKey.fleetId());
    }

    /**
     * Verifies {@code AliasKey} equality depends on both name and fleet id, and is reflexive.
     */
    @Test
    @DisplayName("Should test AliasKey equality")
    public void testAliasKeyEquals() {
        Airframes.AliasKey key1 = new Airframes.AliasKey("Test", 1);
        Airframes.AliasKey key2 = new Airframes.AliasKey("Test", 1);
        Airframes.AliasKey key3 = new Airframes.AliasKey("Different", 1);
        Airframes.AliasKey key4 = new Airframes.AliasKey("Test", 2);

        assertEquals(key1, key2);
        assertNotEquals(key1, key3);
        assertNotEquals(key1, key4);
        assertEquals(key1, key1);
    }

    /**
     * Verifies equal {@code AliasKey} values share a hash code and differing ones generally do not.
     */
    @Test
    @DisplayName("Should test AliasKey hashCode")
    public void testAliasKeyHashCode() {
        Airframes.AliasKey key1 = new Airframes.AliasKey("Test", 1);
        Airframes.AliasKey key2 = new Airframes.AliasKey("Test", 1);
        Airframes.AliasKey key3 = new Airframes.AliasKey("Different", 1);

        assertEquals(key1.hashCode(), key2.hashCode());
        assertNotEquals(key1.hashCode(), key3.hashCode());
    }

    /**
     * Verifies the {@code AliasKey} record's {@code toString} includes its name and fleet id.
     */
    @Test
    @DisplayName("Should test AliasKey toString")
    public void testAliasKeyToString() {
        Airframes.AliasKey key = new Airframes.AliasKey("Test Aircraft", 123);
        String toString = key.toString();

        assertNotNull(toString);
        assertTrue(toString.contains("Test Aircraft"));
        assertTrue(toString.contains("123"));
    }

    /**
     * Verifies {@code defaultAlias} builds an {@code AliasKey} with the given name and the all-fleets id (-1).
     */
    @Test
    @DisplayName("Should create default alias")
    public void testDefaultAlias() {
        String name = "Test Aircraft";
        Airframes.AliasKey aliasKey = Airframes.defaultAlias(name);

        assertEquals(name, aliasKey.name());
        assertEquals(-1, aliasKey.fleetId());
    }

    /**
     * Verifies {@code defaultAlias} accepts a null name while still defaulting the fleet id to -1.
     */
    @Test
    @DisplayName("Should create default alias with null name")
    public void testDefaultAliasWithNullName() {
        Airframes.AliasKey aliasKey = Airframes.defaultAlias(null);

        assertNull(aliasKey.name());
        assertEquals(-1, aliasKey.fleetId());
    }

    // ========== AIRFRAMENAMEID RECORD TESTS ==========

    /**
     * Verifies the {@code AirframeNameID} record exposes the name and id passed to its constructor.
     */
    @Test
    @DisplayName("Should create AirframeNameID with name and id")
    public void testAirframeNameIDConstructor() {
        String name = "Test Aircraft";
        int id = 123;

        Airframes.AirframeNameID airframeNameID = new Airframes.AirframeNameID(name, id);

        assertEquals(name, airframeNameID.name());
        assertEquals(id, airframeNameID.id());
    }

    /**
     * Verifies {@code AirframeNameID} accepts a null name while still returning the supplied id.
     */
    @Test
    @DisplayName("Should create AirframeNameID with null name")
    public void testAirframeNameIDWithNullName() {
        Airframes.AirframeNameID airframeNameID = new Airframes.AirframeNameID(null, 123);

        assertNull(airframeNameID.name());
        assertEquals(123, airframeNameID.id());
    }

    /**
     * Verifies {@code AirframeNameID} accepts a negative id.
     */
    @Test
    @DisplayName("Should create AirframeNameID with negative id")
    public void testAirframeNameIDWithNegativeId() {
        Airframes.AirframeNameID airframeNameID = new Airframes.AirframeNameID("Test", -1);

        assertEquals("Test", airframeNameID.name());
        assertEquals(-1, airframeNameID.id());
    }

    /**
     * Verifies {@code AirframeNameID} equality depends on both name and id, and is reflexive.
     */
    @Test
    @DisplayName("Should test AirframeNameID equality")
    public void testAirframeNameIDEquals() {
        Airframes.AirframeNameID id1 = new Airframes.AirframeNameID("Test", 1);
        Airframes.AirframeNameID id2 = new Airframes.AirframeNameID("Test", 1);
        Airframes.AirframeNameID id3 = new Airframes.AirframeNameID("Different", 1);
        Airframes.AirframeNameID id4 = new Airframes.AirframeNameID("Test", 2);

        assertEquals(id1, id2);
        assertNotEquals(id1, id3);
        assertNotEquals(id1, id4);
        assertEquals(id1, id1);
    }

    /**
     * Verifies equal {@code AirframeNameID} values share a hash code and differing ones generally do not.
     */
    @Test
    @DisplayName("Should test AirframeNameID hashCode")
    public void testAirframeNameIDHashCode() {
        Airframes.AirframeNameID id1 = new Airframes.AirframeNameID("Test", 1);
        Airframes.AirframeNameID id2 = new Airframes.AirframeNameID("Test", 1);
        Airframes.AirframeNameID id3 = new Airframes.AirframeNameID("Different", 1);

        assertEquals(id1.hashCode(), id2.hashCode());
        assertNotEquals(id1.hashCode(), id3.hashCode());
    }

    /**
     * Verifies the {@code AirframeNameID} record's {@code toString} includes its name and id.
     */
    @Test
    @DisplayName("Should test AirframeNameID toString")
    public void testAirframeNameIDToString() {
        Airframes.AirframeNameID airframeNameID = new Airframes.AirframeNameID("Test Aircraft", 123);
        String toString = airframeNameID.toString();

        assertNotNull(toString);
        assertTrue(toString.contains("Test Aircraft"));
        assertTrue(toString.contains("123"));
    }

    // ========== TYPE INNER CLASS TESTS ==========

    /**
     * Verifies a {@code Type} can be constructed from a name alone (cache-only, no database lookup).
     */
    @Test
    @DisplayName("Should create Type with name")
    public void testTypeConstructorWithName() {
        String typeName = "Fixed Wing";
        Airframes.Type type = new Airframes.Type(typeName);

        assertNotNull(type);
        // Note: We can't directly test the name since it's inherited from NormalizedColumn
        // and the getName() method might not be accessible or might require database connection
    }

    /**
     * Verifies a {@code Type} can be constructed from a connection and name (resolving/inserting the id against the
     * database).
     *
     * @throws SQLException if the lookup or insert fails
     */
    @Test
    @DisplayName("Should create Type with connection and name")
    public void testTypeConstructorWithConnectionAndName() throws SQLException {
        String typeName = "Fixed Wing";
        Airframes.Type type = new Airframes.Type(connection, typeName);

        assertNotNull(type);
    }

    /**
     * Verifies a {@code Type} can be constructed from a connection and an existing id.
     *
     * @throws SQLException if the lookup fails
     */
    @Test
    @DisplayName("Should create Type with connection and id")
    public void testTypeConstructorWithConnectionAndId() throws SQLException {
        int typeId = 1;
        Airframes.Type type = new Airframes.Type(connection, typeId);

        assertNotNull(type);
    }

    /**
     * Verifies a name-only {@code Type} is constructable (indirectly covering its {@code getTableName} wiring, which is
     * protected and not called directly).
     */
    @Test
    @DisplayName("Should get table name")
    public void testTypeGetTableName() {
        Airframes.Type type = new Airframes.Type("Test");
        // We can't directly test getTableName() as it's protected, but we can test
        // that the constructor works and the object is created
        assertNotNull(type);
    }

    /**
     * Disabled scenario (its {@code @Test} is commented out): would verify a {@code Type} can be constructed from a
     * null name.
     */
    // @Test
    // @DisplayName("Should handle Type with null name")
    public void testTypeWithNullName() {
        Airframes.Type type = new Airframes.Type(null);
        assertNotNull(type);
    }

    /**
     * Verifies a {@code Type} can be constructed from an empty name.
     */
    @Test
    @DisplayName("Should handle Type with empty name")
    public void testTypeWithEmptyName() {
        Airframes.Type type = new Airframes.Type("");
        assertNotNull(type);
    }

    /**
     * Verifies a {@code Type} can be constructed from a name containing special characters.
     */
    @Test
    @DisplayName("Should handle Type with special characters in name")
    public void testTypeWithSpecialCharacters() {
        String specialName = "Type @#$%^&*()";
        Airframes.Type type = new Airframes.Type(specialName);
        assertNotNull(type);
    }

    /**
     * Verifies a {@code Type} can be constructed from a Unicode name.
     */
    @Test
    @DisplayName("Should handle Type with unicode characters in name")
    public void testTypeWithUnicodeCharacters() {
        String unicodeName = "类型";
        Airframes.Type type = new Airframes.Type(unicodeName);
        assertNotNull(type);
    }

    /**
     * Verifies a {@code Type} can be constructed from a very long name.
     */
    @Test
    @DisplayName("Should handle Type with very long name")
    public void testTypeWithLongName() {
        String longName = "Very Long Type Name That Exceeds Normal Length And "
                + "Contains Special Characters @#$%^&*()_+-=[]{}|;':\",./<>?";
        Airframes.Type type = new Airframes.Type(longName);
        assertNotNull(type);
    }

    // ========== AIRFRAME INNER CLASS TESTS ==========

    /**
     * Verifies an {@code Airframe} built from a name and type exposes them and defaults its id to -1 (not persisted).
     */
    @Test
    @DisplayName("Should create Airframe with name and type")
    public void testAirframeConstructorWithNameAndType() {
        String name = "Test Aircraft";
        Airframes.Type type = new Airframes.Type("Fixed Wing");

        Airframes.Airframe airframe = new Airframes.Airframe(name, type);

        assertNotNull(airframe);
        assertEquals(name, airframe.getName());
        assertEquals(type, airframe.getType());
        assertEquals(-1, airframe.getId());
    }

    /**
     * Verifies an {@code Airframe} accepts a null name while retaining the given type and the default id of -1.
     */
    @Test
    @DisplayName("Should create Airframe with null name and type")
    public void testAirframeConstructorWithNullNameAndType() {
        Airframes.Type type = new Airframes.Type("Fixed Wing");

        Airframes.Airframe airframe = new Airframes.Airframe(null, type);

        assertNotNull(airframe);
        assertNull(airframe.getName());
        assertEquals(type, airframe.getType());
        assertEquals(-1, airframe.getId());
    }

    /**
     * Verifies an {@code Airframe} accepts a null type while retaining the given name and the default id of -1.
     */
    @Test
    @DisplayName("Should create Airframe with name and null type")
    public void testAirframeConstructorWithNameAndNullType() {
        String name = "Test Aircraft";

        Airframes.Airframe airframe = new Airframes.Airframe(name, null);

        assertNotNull(airframe);
        assertEquals(name, airframe.getName());
        assertNull(airframe.getType());
        assertEquals(-1, airframe.getId());
    }

    /**
     * Verifies an {@code Airframe} accepts both a null name and null type, keeping the default id of -1.
     */
    @Test
    @DisplayName("Should create Airframe with null name and null type")
    public void testAirframeConstructorWithNullNameAndNullType() {
        Airframes.Airframe airframe = new Airframes.Airframe(null, null);

        assertNotNull(airframe);
        assertNull(airframe.getName());
        assertNull(airframe.getType());
        assertEquals(-1, airframe.getId());
    }

    /**
     * Verifies the connection-based constructor resolves (or inserts) a known airframe name and type, yielding a
     * positive persisted id.
     *
     * @throws SQLException if the lookup or insert fails
     */
    @Test
    @DisplayName("Should create Airframe with connection, name, and type")
    public void testAirframeConstructorWithConnectionNameAndType() throws SQLException {
        String name = "Cessna 172S";
        Airframes.Type type = new Airframes.Type("Fixed Wing");

        Airframes.Airframe airframe = new Airframes.Airframe(connection, name, type);

        assertNotNull(airframe);
        assertEquals(name, airframe.getName());
        assertNotNull(airframe.getType());
        assertTrue(airframe.getId() > 0);
    }

    /**
     * Verifies the connection-based constructor resolves an existing airframe by name even when a null type is passed,
     * populating the type from the database and yielding a positive id.
     *
     * @throws SQLException if the lookup fails
     */
    @Test
    @DisplayName("Should create Airframe with connection, name, and null type")
    public void testAirframeConstructorWithConnectionNameAndNullType() throws SQLException {
        String name = "Cessna 172S";

        Airframes.Airframe airframe = new Airframes.Airframe(connection, name, null);

        assertNotNull(airframe);
        assertEquals(name, airframe.getName());
        assertNotNull(airframe.getType());
        assertTrue(airframe.getId() > 0);
    }

    /**
     * Disabled scenario (its {@code @Test} is commented out): would verify constructing an {@code Airframe} from a
     * connection and an existing id.
     *
     * @throws SQLException if the lookup fails
     */
    // @Test
    // @DisplayName("Should create Airframe with connection and id")
    public void testAirframeConstructorWithConnectionAndId() throws SQLException {
        int id = 1001;

        Airframes.Airframe airframe = new Airframes.Airframe(connection, id);

        assertNotNull(airframe);
        assertNotNull(airframe.getName());
        assertNotNull(airframe.getType());
        assertEquals(id, airframe.getId());
    }

    /**
     * Verifies the connection-based constructor throws {@link SQLException} when the name does not exist and no type is
     * supplied to create it.
     *
     * @throws SQLException declared; the body asserts the throw occurs
     */
    @Test
    @DisplayName("Should handle Airframe with non-existent name")
    public void testAirframeWithNonExistentName() throws SQLException {
        String nonExistentName = "Non-existent Aircraft";

        // This should throw SQLException because the airframe doesn't exist and type is null
        assertThrows(SQLException.class, () -> {
            new Airframes.Airframe(connection, nonExistentName, null);
        });
    }

    /**
     * Verifies constructing an {@code Airframe} from a non-existent id throws {@link SQLException}.
     *
     * @throws SQLException declared; the body asserts the throw occurs
     */
    @Test
    @DisplayName("Should handle Airframe with non-existent id")
    public void testAirframeWithNonExistentId() throws SQLException {
        int nonExistentId = 999;

        assertThrows(SQLException.class, () -> {
            new Airframes.Airframe(connection, nonExistentId);
        });
    }

    /**
     * Verifies the static {@code getAirframeByName} resolves a known airframe, populating its type and a positive id.
     *
     * @throws SQLException if the lookup fails
     */
    @Test
    @DisplayName("Should get airframe by name using static method")
    public void testGetAirframeByName() throws SQLException {
        String name = "Cessna 172S";

        Airframes.Airframe airframe = Airframes.Airframe.getAirframeByName(connection, name);

        assertNotNull(airframe);
        assertEquals(name, airframe.getName());
        assertNotNull(airframe.getType());
        assertTrue(airframe.getId() > 0);
    }

    /**
     * Verifies {@code getAirframeByName} throws {@link SQLException} for a non-existent name.
     *
     * @throws SQLException declared; the body asserts the throw occurs
     */
    @Test
    @DisplayName("Should handle getAirframeByName with non-existent name")
    public void testGetAirframeByNameWithNonExistentName() throws SQLException {
        String nonExistentName = "Non-existent Aircraft";

        assertThrows(SQLException.class, () -> {
            Airframes.Airframe.getAirframeByName(connection, nonExistentName);
        });
    }

    /**
     * Verifies {@code getAirframeByName} throws {@link SQLException} for an empty name.
     *
     * @throws SQLException declared; the body asserts the throw occurs
     */
    @Test
    @DisplayName("Should handle getAirframeByName with empty name")
    public void testGetAirframeByNameWithEmptyName() throws SQLException {
        assertThrows(SQLException.class, () -> {
            Airframes.Airframe.getAirframeByName(connection, "");
        });
    }

    /**
     * Verifies {@code getAirframeByName} throws {@link SQLException} for a name with special characters that matches no
     * airframe.
     *
     * @throws SQLException declared; the body asserts the throw occurs
     */
    @Test
    @DisplayName("Should handle getAirframeByName with special characters")
    public void testGetAirframeByNameWithSpecialCharacters() throws SQLException {
        String specialName = "Aircraft @#$%^&*()";

        assertThrows(SQLException.class, () -> {
            Airframes.Airframe.getAirframeByName(connection, specialName);
        });
    }

    /**
     * Verifies {@code getAirframeByName} throws {@link SQLException} for a Unicode name that matches no airframe.
     *
     * @throws SQLException declared; the body asserts the throw occurs
     */
    @Test
    @DisplayName("Should handle getAirframeByName with unicode characters")
    public void testGetAirframeByNameWithUnicodeCharacters() throws SQLException {
        String unicodeName = "飞机";

        assertThrows(SQLException.class, () -> {
            Airframes.Airframe.getAirframeByName(connection, unicodeName);
        });
    }

    /**
     * Verifies {@code getAirframeByName} throws {@link SQLException} for a very long name that matches no airframe.
     *
     * @throws SQLException declared; the body asserts the throw occurs
     */
    @Test
    @DisplayName("Should handle getAirframeByName with very long name")
    public void testGetAirframeByNameWithLongName() throws SQLException {
        String longName = "Very Long Aircraft Name That Exceeds Normal Length And "
                + "Contains Special Characters @#$%^&*()_+-=[]{}|;':\",./<>?";

        assertThrows(SQLException.class, () -> {
            Airframes.Airframe.getAirframeByName(connection, longName);
        });
    }

    /**
     * Verifies {@code getAirframeByName} throws {@link NullPointerException} when given a null connection.
     */
    @Test
    @DisplayName("Should handle getAirframeByName with null connection")
    public void testGetAirframeByNameWithNullConnection() {
        assertThrows(NullPointerException.class, () -> {
            Airframes.Airframe.getAirframeByName(null, "Test");
        });
    }

    /**
     * Verifies the connection-based {@code Airframe} constructor throws {@link NullPointerException} for a null
     * connection.
     */
    @Test
    @DisplayName("Should handle Airframe with null connection")
    public void testAirframeWithNullConnection() {
        assertThrows(NullPointerException.class, () -> {
            new Airframes.Airframe(null, "Test", null);
        });
    }

    /**
     * Disabled scenario (its {@code @Test} is commented out): would verify the id-based {@code Airframe} constructor
     * throws {@link NullPointerException} for a null connection.
     */
    // @Test
    // @DisplayName("Should handle Airframe with null connection and id")
    public void testAirframeWithNullConnectionAndId() {
        assertThrows(NullPointerException.class, () -> {
            new Airframes.Airframe(null, 1);
        });
    }

    /**
     * Verifies constructing an {@code Airframe} from a negative id throws {@link SQLException}.
     *
     * @throws SQLException declared; the body asserts the throw occurs
     */
    @Test
    @DisplayName("Should handle Airframe with negative id")
    public void testAirframeWithNegativeId() throws SQLException {
        int negativeId = -1;

        assertThrows(SQLException.class, () -> {
            new Airframes.Airframe(connection, negativeId);
        });
    }

    /**
     * Verifies constructing an {@code Airframe} from id 0 throws {@link SQLException}.
     *
     * @throws SQLException declared; the body asserts the throw occurs
     */
    @Test
    @DisplayName("Should handle Airframe with zero id")
    public void testAirframeWithZeroId() throws SQLException {
        int zeroId = 0;

        assertThrows(SQLException.class, () -> {
            new Airframes.Airframe(connection, zeroId);
        });
    }

    /**
     * Verifies constructing an {@code Airframe} from {@code Integer.MAX_VALUE} (no such row) throws SQLException.
     *
     * @throws SQLException declared; the body asserts the throw occurs
     */
    @Test
    @DisplayName("Should handle Airframe with very large id")
    public void testAirframeWithVeryLargeId() throws SQLException {
        int veryLargeId = Integer.MAX_VALUE;

        assertThrows(SQLException.class, () -> {
            new Airframes.Airframe(connection, veryLargeId);
        });
    }

    /**
     * Verifies constructing an {@code Airframe} from {@code Integer.MIN_VALUE} (no such row) throws SQLException.
     *
     * @throws SQLException declared; the body asserts the throw occurs
     */
    @Test
    @DisplayName("Should handle Airframe with very small id")
    public void testAirframeWithVerySmallId() throws SQLException {
        int verySmallId = Integer.MIN_VALUE;

        assertThrows(SQLException.class, () -> {
            new Airframes.Airframe(connection, verySmallId);
        });
    }

    // ========== STATIC METHODS TESTS ==========

    /**
     * Verifies {@code setAirframeFleet} inserts a fleet-airframe relationship row that can then be found in
     * {@code fleet_airframes}.
     *
     * @throws SQLException if the insert or verification query fails
     */
    @Test
    @DisplayName("Should set airframe fleet relationship")
    public void testSetAirframeFleet() throws SQLException {
        int airframeId = 1001;
        int fleetId = 3;

        // This should not throw an exception
        Airframes.setAirframeFleet(connection, airframeId, fleetId);

        // Verify the relationship was created
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT COUNT(*) FROM fleet_airframes WHERE fleet_id = ? AND airframe_id = ?")) {
            stmt.setInt(1, fleetId);
            stmt.setInt(2, airframeId);

            try (ResultSet rs = stmt.executeQuery()) {
                assertTrue(rs.next());
                assertEquals(1, rs.getInt(1));
            }
        }
    }

    /**
     * Verifies {@code setAirframeFleet} is idempotent: calling it twice for the same pair leaves exactly one row.
     *
     * @throws SQLException if the inserts or verification query fail
     */
    @Test
    @DisplayName("Should handle setAirframeFleet with existing relationship")
    public void testSetAirframeFleetWithExistingRelationship() throws SQLException {
        int airframeId = 1001;
        int fleetId = 1;

        // First call should succeed
        Airframes.setAirframeFleet(connection, airframeId, fleetId);

        // Second call should also succeed (no exception thrown)
        Airframes.setAirframeFleet(connection, airframeId, fleetId);

        // Verify only one relationship exists
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT COUNT(*) FROM fleet_airframes WHERE fleet_id = ? AND airframe_id = ?")) {
            stmt.setInt(1, fleetId);
            stmt.setInt(2, airframeId);

            try (ResultSet rs = stmt.executeQuery()) {
                assertTrue(rs.next());
                assertEquals(1, rs.getInt(1));
            }
        }
    }

    /**
     * Verifies {@code getAll(connection, fleetId)} returns an empty list for a fleet with no airframes.
     *
     * @throws SQLException if the query fails
     */
    @Test
    @DisplayName("Should get all airframes for fleet with no airframes")
    public void testGetAllForFleetWithNoAirframes() throws SQLException {
        int fleetId = 999; // Non-existent fleet

        ArrayList<String> airframes = Airframes.getAll(connection, fleetId);

        assertNotNull(airframes);
        assertTrue(airframes.isEmpty());
    }

    /**
     * Verifies {@code getAll(connection, fleetId)} returns an empty list for a negative fleet id.
     *
     * @throws SQLException if the query fails
     */
    @Test
    @DisplayName("Should get all airframes for fleet with negative ID")
    public void testGetAllForFleetWithNegativeId() throws SQLException {
        int fleetId = -1;

        ArrayList<String> airframes = Airframes.getAll(connection, fleetId);

        assertNotNull(airframes);
        assertTrue(airframes.isEmpty());
    }

    /**
     * Verifies {@code getAll(connection, fleetId)} returns an empty list for {@code Integer.MAX_VALUE}.
     *
     * @throws SQLException if the query fails
     */
    @Test
    @DisplayName("Should get all airframes for fleet with very large ID")
    public void testGetAllForFleetWithVeryLargeId() throws SQLException {
        int fleetId = Integer.MAX_VALUE;

        ArrayList<String> airframes = Airframes.getAll(connection, fleetId);

        assertNotNull(airframes);
        assertTrue(airframes.isEmpty());
    }

    /**
     * Verifies {@code getAll(connection, fleetId)} returns an empty list for {@code Integer.MIN_VALUE}.
     *
     * @throws SQLException if the query fails
     */
    @Test
    @DisplayName("Should get all airframes for fleet with very small ID")
    public void testGetAllForFleetWithVerySmallId() throws SQLException {
        int fleetId = Integer.MIN_VALUE;

        ArrayList<String> airframes = Airframes.getAll(connection, fleetId);

        assertNotNull(airframes);
        assertTrue(airframes.isEmpty());
    }

    /**
     * Verifies {@code getAll(connection, fleetId)} throws {@link NullPointerException} for a null connection.
     */
    @Test
    @DisplayName("Should handle getAll with null connection")
    public void testGetAllWithNullConnection() {
        assertThrows(NullPointerException.class, () -> {
            Airframes.getAll(null, 1);
        });
    }

    /**
     * Verifies {@code getAll(connection)} returns a non-empty list of non-blank airframe names across all fleets.
     *
     * @throws SQLException if the query fails
     */
    @Test
    @DisplayName("Should get all airframes regardless of fleet")
    public void testGetAllAirframes() throws SQLException {
        ArrayList<String> airframes = Airframes.getAll(connection);

        assertNotNull(airframes);
        assertTrue(airframes.size() >= 1); // Should have at least 1 airframe
        // Just verify we get some airframes back - don't check for specific names
        // as they may vary depending on test data setup

        // Additional verification to ensure the while loop executes
        assertTrue(airframes.size() > 0, "Should have retrieved at least one airframe");
        for (String airframe : airframes) {
            assertNotNull(airframe, "Airframe name should not be null");
            assertFalse(airframe.trim().isEmpty(), "Airframe name should not be empty");
        }
    }

    /**
     * Verifies {@code getAll(connection)} throws {@link NullPointerException} for a null connection.
     */
    @Test
    @DisplayName("Should handle getAll with null connection")
    public void testGetAllWithNullConnectionNoFleet() {
        assertThrows(NullPointerException.class, () -> {
            Airframes.getAll(null);
        });
    }

    /**
     * Verifies {@code getAllWithIds(connection)} returns all airframes as name/id records with positive ids.
     *
     * @throws SQLException if the query fails
     */
    @Test
    @DisplayName("Should get all airframes with IDs regardless of fleet")
    public void testGetAllWithIds() throws SQLException {
        Airframes.AirframeNameID[] airframes = Airframes.getAllWithIds(connection);

        assertNotNull(airframes);
        assertTrue(airframes.length >= 3); // Should have at least 3 airframes

        // Verify the structure
        for (Airframes.AirframeNameID airframe : airframes) {
            assertNotNull(airframe);
            assertNotNull(airframe.name());
            assertTrue(airframe.id() > 0);
        }
    }

    /**
     * Verifies {@code getAllWithIds(connection, fleetId)} returns a specific fleet's airframes as name/id records.
     *
     * @throws SQLException if the query fails
     */
    @Test
    @DisplayName("Should get all airframes with IDs for specific fleet")
    public void testGetAllWithIdsForFleet() throws SQLException {
        int fleetId = 1;

        Airframes.AirframeNameID[] airframes = Airframes.getAllWithIds(connection, fleetId);

        assertNotNull(airframes);
        assertTrue(airframes.length >= 2); // Should have at least 2 airframes for fleet 1

        // Verify the structure
        for (Airframes.AirframeNameID airframe : airframes) {
            assertNotNull(airframe);
            assertNotNull(airframe.name());
            assertTrue(airframe.id() > 0);
        }
    }

    /**
     * Verifies {@code getAllWithIds} with a negative fleet id (the all-fleets sentinel) returns every airframe.
     *
     * @throws SQLException if the query fails
     */
    @Test
    @DisplayName("Should get all airframes with IDs for fleet with negative ID")
    public void testGetAllWithIdsForFleetWithNegativeId() throws SQLException {
        int fleetId = -1;

        Airframes.AirframeNameID[] airframes = Airframes.getAllWithIds(connection, fleetId);

        assertNotNull(airframes);
        assertTrue(airframes.length >= 3); // Should return all airframes
    }

    /**
     * Verifies {@code getAllWithIds} returns an empty array for fleet id 0 (no such fleet).
     *
     * @throws SQLException if the query fails
     */
    @Test
    @DisplayName("Should get all airframes with IDs for fleet with zero ID")
    public void testGetAllWithIdsForFleetWithZeroId() throws SQLException {
        int fleetId = 0;

        Airframes.AirframeNameID[] airframes = Airframes.getAllWithIds(connection, fleetId);

        assertNotNull(airframes);
        assertTrue(airframes.length == 0);
    }

    /**
     * Verifies {@code getAllWithIds} returns an empty array for {@code Integer.MIN_VALUE} (no such fleet).
     *
     * @throws SQLException if the query fails
     */
    @Test
    @DisplayName("Should get all airframes with IDs for fleet with very small ID")
    public void testGetAllWithIdsForFleetWithVerySmallId() throws SQLException {
        int fleetId = Integer.MIN_VALUE;

        Airframes.AirframeNameID[] airframes = Airframes.getAllWithIds(connection, fleetId);

        assertNotNull(airframes);
        assertTrue(airframes.length == 0);
    }

    /**
     * Verifies {@code getAllWithIds(connection)} throws {@link NullPointerException} for a null connection.
     */
    @Test
    @DisplayName("Should handle getAllWithIds with null connection")
    public void testGetAllWithIdsWithNullConnection() {
        assertThrows(NullPointerException.class, () -> {
            Airframes.getAllWithIds(null);
        });
    }

    /**
     * Verifies {@code getAllWithIds(connection, fleetId)} throws {@link NullPointerException} for a null connection.
     */
    @Test
    @DisplayName("Should handle getAllWithIds with null connection and fleet ID")
    public void testGetAllWithIdsWithNullConnectionAndFleetId() {
        assertThrows(NullPointerException.class, () -> {
            Airframes.getAllWithIds(null, 1);
        });
    }

    /**
     * Verifies {@code getIdToNameMap(connection)} returns a map of positive ids to non-empty airframe names across all
     * fleets.
     *
     * @throws SQLException if the query fails
     */
    @Test
    @DisplayName("Should get ID to name map")
    public void testGetIdToNameMap() throws SQLException {
        HashMap<Integer, String> idToNameMap = Airframes.getIdToNameMap(connection);

        assertNotNull(idToNameMap);
        assertTrue(idToNameMap.size() >= 3); // Should have at least 3 airframes

        // Verify the structure
        for (Map.Entry<Integer, String> entry : idToNameMap.entrySet()) {
            assertNotNull(entry.getKey());
            assertNotNull(entry.getValue());
            assertTrue(entry.getKey() > 0);
            assertFalse(entry.getValue().isEmpty());
        }
    }

    /**
     * Verifies {@code getIdToNameMap(connection, fleetId)} returns the id-to-name map scoped to a specific fleet.
     *
     * @throws SQLException if the query fails
     */
    @Test
    @DisplayName("Should get ID to name map for specific fleet")
    public void testGetIdToNameMapForFleet() throws SQLException {
        int fleetId = 1;

        HashMap<Integer, String> idToNameMap = Airframes.getIdToNameMap(connection, fleetId);

        assertNotNull(idToNameMap);
        assertTrue(idToNameMap.size() >= 2); // Should have at least 2 airframes for fleet 1

        // Verify the structure
        for (Map.Entry<Integer, String> entry : idToNameMap.entrySet()) {
            assertNotNull(entry.getKey());
            assertNotNull(entry.getValue());
            assertTrue(entry.getKey() > 0);
            assertFalse(entry.getValue().isEmpty());
        }
    }

    /**
     * Verifies {@code getIdToNameMap(connection, fleetId)} returns an empty map for a fleet with no airframes.
     *
     * @throws SQLException if the query fails
     */
    @Test
    @DisplayName("Should get ID to name map for fleet with no airframes")
    public void testGetIdToNameMapForFleetWithNoAirframes() throws SQLException {
        int fleetId = 999; // Non-existent fleet

        HashMap<Integer, String> idToNameMap = Airframes.getIdToNameMap(connection, fleetId);

        assertNotNull(idToNameMap);
        assertTrue(idToNameMap.isEmpty());
    }

    /**
     * Verifies {@code getIdToNameMap(connection, fleetId)} returns an empty map for {@code Integer.MIN_VALUE}.
     *
     * @throws SQLException if the query fails
     */
    @Test
    @DisplayName("Should get ID to name map for fleet with very small ID")
    public void testGetIdToNameMapForFleetWithVerySmallId() throws SQLException {
        int fleetId = Integer.MIN_VALUE;

        HashMap<Integer, String> idToNameMap = Airframes.getIdToNameMap(connection, fleetId);

        assertNotNull(idToNameMap);
        assertTrue(idToNameMap.isEmpty());
    }

    /**
     * Verifies {@code getIdToNameMap(connection)} throws {@link NullPointerException} for a null connection.
     */
    @Test
    @DisplayName("Should handle getIdToNameMap with null connection")
    public void testGetIdToNameMapWithNullConnection() {
        assertThrows(NullPointerException.class, () -> {
            Airframes.getIdToNameMap(null);
        });
    }

    /**
     * Verifies {@code getIdToNameMap(connection, fleetId)} throws {@link NullPointerException} for a null connection.
     */
    @Test
    @DisplayName("Should handle getIdToNameMap with null connection and fleet ID")
    public void testGetIdToNameMapWithNullConnectionAndFleetId() {
        assertThrows(NullPointerException.class, () -> {
            Airframes.getIdToNameMap(null, 1);
        });
    }
}
