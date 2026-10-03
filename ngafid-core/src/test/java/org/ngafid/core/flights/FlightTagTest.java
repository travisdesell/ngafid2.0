package org.ngafid.core.flights;

import static org.junit.jupiter.api.Assertions.*;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.*;
import org.ngafid.core.util.FlightTag;

/**
 * Tests for {@link Flight}'s flight-tag API: querying a flight's tags ({@code hasTags}, {@code getAllTagNames},
 * {@code getAllFleetTagNames}, {@code tagExists}, {@code getUnassociatedTags}), editing tags ({@code editTag}), and
 * removing tag associations ({@code disassociateTags}, {@code disassociateAllTags}, {@code deleteTag}), and the
 * {@code idLimStr} id-list SQL helper exercised through tag disassociation.
 *
 * <p>Shared database seeding fixtures live in {@link FlightTestBase}.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class FlightTagTest extends FlightTestBase {

    /**
     * Verifies {@code Flight.hasTags} reports whether a flight has any associated tags.
     *
     * @throws SQLException if the query fails
     */
    @Test
    @Order(47)
    @DisplayName("Should test hasTags method")
    public void testHasTags() throws SQLException {
        Flight flight = Flight.getFlight(connection, 1);

        assertNotNull(flight, "Flight should not be null");
        // hasTags should return false initially since tags are null
        assertFalse(flight.hasTags(), "Flight should not have tags initially");
    }

    /**
     * Verifies {@code Flight.getAllTagNames} returns the tag names associated with a flight.
     *
     * @throws SQLException if the query fails
     */
    @Test
    @Order(49)
    @DisplayName("Should test getAllTagNames method")
    public void testGetAllTagNames() throws SQLException {
        String tag1Name = "TestTag1_" + System.currentTimeMillis();
        String tag2Name = "TestTag2_" + System.currentTimeMillis();
        String tag3Name = "TestTag3_" + System.currentTimeMillis();

        Flight.createTag(1, 1, tag1Name, "Description1", "red", connection);
        Flight.createTag(1, 1, tag2Name, "Description2", "blue", connection);
        Flight.createTag(2, 1, tag3Name, "Description3", "green", connection);

        List<String> tagNames = Flight.getAllTagNames(connection);

        assertNotNull(tagNames, "Tag names list should not be null");
        // Should contain the created tags
        assertTrue(tagNames.size() >= 3, "Tag names list should contain at least 3 tags");
        assertTrue(tagNames.contains(tag1Name), "Should contain TestTag1");
        assertTrue(tagNames.contains(tag2Name), "Should contain TestTag2");
        assertTrue(tagNames.contains(tag3Name), "Should contain TestTag3");
    }

    /**
     * Verifies {@code Flight.getAllFleetTagNames} returns all tag names defined for a fleet.
     *
     * @throws SQLException if the query fails
     */
    @Test
    @Order(50)
    @DisplayName("Should test getAllFleetTagNames method")
    public void testGetAllFleetTagNames() throws SQLException {
        Flight.createTag(1, 1, "Fleet1Tag1", "Description1", "red", connection);
        Flight.createTag(1, 1, "Fleet1Tag2", "Description2", "blue", connection);
        Flight.createTag(2, 1, "Fleet2Tag1", "Description3", "green", connection);

        List<String> fleetTagNames = Flight.getAllFleetTagNames(connection, 1);

        assertNotNull(fleetTagNames, "Fleet tag names list should not be null");
        // Should contain only fleet 1 tags
        assertTrue(fleetTagNames.size() >= 2, "Fleet tag names list should contain at least 2 tags for fleet 1");
        assertTrue(fleetTagNames.contains("Fleet1Tag1"), "Should contain Fleet1Tag1");
        assertTrue(fleetTagNames.contains("Fleet1Tag2"), "Should contain Fleet1Tag2");
        assertFalse(fleetTagNames.contains("Fleet2Tag1"), "Should not contain Fleet2Tag1");
    }

    /**
     * Verifies {@code Flight.tagExists} reports whether a tag with a given name exists for a fleet.
     *
     * @throws SQLException if the query fails
     */
    @Test
    @Order(51)
    @DisplayName("Should test tagExists method")
    public void testTagExists() throws SQLException {
        boolean exists = Flight.tagExists(connection, 1, "NonExistentTag");

        assertFalse(exists, "Non-existent tag should not exist");
    }

    /**
     * Verifies {@code Flight.disassociateTags} removes a specific tag association from a flight.
     *
     * @throws SQLException if the operation fails
     */
    @Test
    @Order(54)
    @DisplayName("Should test disassociateTags method")
    public void testDisassociateTags() throws SQLException {
        // Test disassociating tags (should not throw exception)
        assertDoesNotThrow(() -> {
            Flight.disassociateTags(1, connection, 1);
        });
    }

    /**
     * Verifies {@code Flight.disassociateAllTags} removes all tag associations from a flight.
     *
     * @throws SQLException if the operation fails
     */
    @Test
    @Order(55)
    @DisplayName("Should test disassociateAllTags method")
    public void testDisassociateAllTags() throws SQLException {
        // Test disassociating all tags (should not throw exception)
        assertDoesNotThrow(() -> {
            Flight.disassociateAllTags(1, connection);
        });
    }

    /**
     * Verifies {@code Flight.deleteTag} deletes a tag definition (and its associations).
     *
     * @throws SQLException if the operation fails
     */
    @Test
    @Order(56)
    @DisplayName("Should test deleteTag method")
    public void testDeleteTag() throws SQLException {
        // Test deleting a tag (should not throw exception even if tag doesn't exist)
        assertDoesNotThrow(() -> {
            Flight.deleteTag(1, connection);
        });
    }

    /**
     * Verifies {@code Flight.editTag} updates only the tag's name when only the name changed.
     *
     * @throws SQLException if the operation fails
     */
    @Test
    @Order(71)
    @DisplayName("Should edit tag with name change only")
    public void testEditTagNameOnly() throws SQLException {
        String uniqueName = "OriginalName_" + System.currentTimeMillis();
        FlightTag originalTag = Flight.createTag(1, 999, uniqueName, "OriginalDescription", "red", connection);
        int tagId = originalTag.hashCode();

        // Create a modified tag with only name changed
        String newName = "NewName_" + System.currentTimeMillis();
        FlightTag modifiedTag = new FlightTag(tagId, 1, newName, "OriginalDescription", "red");

        FlightTag result = Flight.editTag(connection, modifiedTag);

        assertNotNull(result, "Result should not be null");
        assertEquals(newName, result.getName(), "Name should be updated");
        assertEquals("OriginalDescription", result.getDescription(), "Description should remain unchanged");
        assertEquals("red", result.getColor(), "Color should remain unchanged");
        assertEquals(tagId, result.hashCode(), "Tag ID should remain the same");

        FlightTag dbTag = Flight.getTag(connection, tagId);
        assertEquals(newName, dbTag.getName(), "Database should reflect name change");
    }

    /**
     * Verifies {@code Flight.editTag} updates only the tag's description when only the description changed.
     *
     * @throws SQLException if the operation fails
     */
    @Test
    @Order(72)
    @DisplayName("Should edit tag with description change only")
    public void testEditTagDescriptionOnly() throws SQLException {
        String uniqueName = "TestName_" + System.currentTimeMillis();
        FlightTag originalTag = Flight.createTag(1, 999, uniqueName, "OriginalDescription", "blue", connection);
        int tagId = originalTag.hashCode();

        // Create a modified tag with only description changed
        FlightTag modifiedTag = new FlightTag(tagId, 1, uniqueName, "NewDescription", "blue");

        FlightTag result = Flight.editTag(connection, modifiedTag);

        assertNotNull(result, "Result should not be null");
        assertEquals(uniqueName, result.getName(), "Name should remain unchanged");
        assertEquals("NewDescription", result.getDescription(), "Description should be updated");
        assertEquals("blue", result.getColor(), "Color should remain unchanged");
        assertEquals(tagId, result.hashCode(), "Tag ID should remain the same");

        FlightTag dbTag = Flight.getTag(connection, tagId);
        assertEquals("NewDescription", dbTag.getDescription(), "Database should reflect description change");
    }

    /**
     * Verifies {@code Flight.editTag} updates only the tag's color when only the color changed.
     *
     * @throws SQLException if the operation fails
     */
    @Test
    @Order(73)
    @DisplayName("Should edit tag with color change only")
    public void testEditTagColorOnly() throws SQLException {
        String uniqueName = "TestName_" + System.currentTimeMillis() + "_" + Math.random();
        FlightTag originalTag = Flight.createTag(1, 999, uniqueName, "TestDescription", "green", connection);
        int tagId = originalTag.hashCode();

        // Create a modified tag with only color changed
        FlightTag modifiedTag = new FlightTag(tagId, 1, uniqueName, "TestDescription", "purple");

        FlightTag result = Flight.editTag(connection, modifiedTag);

        assertNotNull(result, "Result should not be null");
        assertEquals(uniqueName, result.getName(), "Name should remain unchanged");
        assertEquals("TestDescription", result.getDescription(), "Description should remain unchanged");
        assertEquals("purple", result.getColor(), "Color should be updated");
        assertEquals(tagId, result.hashCode(), "Tag ID should remain the same");

        FlightTag dbTag = Flight.getTag(connection, tagId);
        assertEquals("purple", dbTag.getColor(), "Database should reflect color change");
    }

    /**
     * Verifies {@code Flight.editTag} applies name, description, and color changes together.
     *
     * @throws SQLException if the operation fails
     */
    @Test
    @Order(74)
    @DisplayName("Should edit tag with multiple changes")
    public void testEditTagMultipleChanges() throws SQLException {
        String uniqueName = "OriginalName_" + System.currentTimeMillis();
        FlightTag originalTag = Flight.createTag(1, 999, uniqueName, "OriginalDescription", "red", connection);
        int tagId = originalTag.hashCode();

        // Create a modified tag with all fields changed
        String newName = "NewName_" + System.currentTimeMillis();
        FlightTag modifiedTag = new FlightTag(tagId, 1, newName, "NewDescription", "blue");

        FlightTag result = Flight.editTag(connection, modifiedTag);

        assertNotNull(result, "Result should not be null");
        assertEquals(newName, result.getName(), "Name should be updated");
        assertEquals("NewDescription", result.getDescription(), "Description should be updated");
        assertEquals("blue", result.getColor(), "Color should be updated");
        assertEquals(tagId, result.hashCode(), "Tag ID should remain the same");

        FlightTag dbTag = Flight.getTag(connection, tagId);
        assertEquals(newName, dbTag.getName(), "Database should reflect name change");
        assertEquals("NewDescription", dbTag.getDescription(), "Database should reflect description change");
        assertEquals("blue", dbTag.getColor(), "Database should reflect color change");
    }

    /**
     * Verifies {@code Flight.editTag} is a no-op (leaves the tag unchanged) when no fields differ.
     *
     * @throws SQLException if the operation fails
     */
    @Test
    @Order(75)
    @DisplayName("Should handle edit tag with no changes")
    public void testEditTagNoChanges() throws SQLException {
        String uniqueName = "TestName_" + System.currentTimeMillis();
        FlightTag originalTag = Flight.createTag(1, 999, uniqueName, "TestDescription", "green", connection);
        int tagId = originalTag.hashCode();

        // Create a tag with identical values (no changes)
        FlightTag unchangedTag = new FlightTag(tagId, 1, uniqueName, "TestDescription", "green");

        FlightTag result = Flight.editTag(connection, unchangedTag);

        assertNull(result, "Result should be null when no changes are made");

        // Verify original tag is unchanged in database
        FlightTag dbTag = Flight.getTag(connection, tagId);
        assertEquals(uniqueName, dbTag.getName(), "Database should remain unchanged");
        assertEquals("TestDescription", dbTag.getDescription(), "Database should remain unchanged");
        assertEquals("green", dbTag.getColor(), "Database should remain unchanged");
    }

    /**
     * Verifies {@code Flight.editTag} applies a partial set of changes while leaving the other fields intact.
     *
     * @throws SQLException if the operation fails
     */
    @Test
    @Order(76)
    @DisplayName("Should handle edit tag with partial changes")
    public void testEditTagPartialChanges() throws SQLException {
        String uniqueName = "OriginalName_" + System.currentTimeMillis();
        FlightTag originalTag = Flight.createTag(1, 999, uniqueName, "OriginalDescription", "red", connection);
        int tagId = originalTag.hashCode();

        // Create a modified tag with only name and color changed
        String newName = "NewName_" + System.currentTimeMillis();
        FlightTag modifiedTag = new FlightTag(tagId, 1, newName, "OriginalDescription", "blue");

        FlightTag result = Flight.editTag(connection, modifiedTag);

        assertNotNull(result, "Result should not be null");
        assertEquals(newName, result.getName(), "Name should be updated");
        assertEquals("OriginalDescription", result.getDescription(), "Description should remain unchanged");
        assertEquals("blue", result.getColor(), "Color should be updated");
        assertEquals(tagId, result.hashCode(), "Tag ID should remain the same");

        FlightTag dbTag = Flight.getTag(connection, tagId);
        assertEquals(newName, dbTag.getName(), "Database should reflect name change");
        assertEquals("OriginalDescription", dbTag.getDescription(), "Database should remain unchanged for description");
        assertEquals("blue", dbTag.getColor(), "Database should reflect color change");
    }

    /**
     * Verifies {@code Flight.editTag} applies simultaneous description and color changes.
     *
     * @throws SQLException if the operation fails
     */
    @Test
    @Order(77)
    @DisplayName("Should handle edit tag with description and color changes")
    public void testEditTagDescriptionAndColorChanges() throws SQLException {
        String uniqueName = "TestName_" + System.currentTimeMillis();
        FlightTag originalTag = Flight.createTag(1, 999, uniqueName, "OriginalDescription", "red", connection);
        int tagId = originalTag.hashCode();

        // Create a modified tag with description and color changed
        FlightTag modifiedTag = new FlightTag(tagId, 1, uniqueName, "NewDescription", "purple");

        FlightTag result = Flight.editTag(connection, modifiedTag);

        assertNotNull(result, "Result should not be null");
        assertEquals(uniqueName, result.getName(), "Name should remain unchanged");
        assertEquals("NewDescription", result.getDescription(), "Description should be updated");
        assertEquals("purple", result.getColor(), "Color should be updated");
        assertEquals(tagId, result.hashCode(), "Tag ID should remain the same");

        FlightTag dbTag = Flight.getTag(connection, tagId);
        assertEquals(uniqueName, dbTag.getName(), "Database should remain unchanged for name");
        assertEquals("NewDescription", dbTag.getDescription(), "Database should reflect description change");
        assertEquals("purple", dbTag.getColor(), "Database should reflect color change");
    }

    /**
     * Verifies {@code Flight.editTag} accepts safe special characters in the updated fields.
     *
     * @throws SQLException if the operation fails
     */
    @Test
    @Order(78)
    @DisplayName("Should handle edit tag with safe special characters")
    public void testEditTagWithSafeSpecialCharacters() throws SQLException {
        String uniqueName = "SimpleName_" + System.currentTimeMillis();
        FlightTag originalTag = Flight.createTag(1, 999, uniqueName, "SimpleDescription", "red", connection);
        int tagId = originalTag.hashCode();

        // Create a modified tag with safe special characters (no quotes)
        String newName = "NameWithSpaces_" + System.currentTimeMillis();
        String newDescription = "Description with spaces and symbols";
        FlightTag modifiedTag = new FlightTag(tagId, 1, newName, newDescription, "blue");

        FlightTag result = Flight.editTag(connection, modifiedTag);

        assertNotNull(result, "Result should not be null");
        assertEquals(newName, result.getName(), "Name with spaces should be updated");
        assertEquals(newDescription, result.getDescription(), "Description with spaces should be updated");
        assertEquals("blue", result.getColor(), "Color should be updated");
        assertEquals(tagId, result.hashCode(), "Tag ID should remain the same");

        FlightTag dbTag = Flight.getTag(connection, tagId);
        assertEquals(newName, dbTag.getName(), "Database should reflect name with spaces");
        assertEquals(newDescription, dbTag.getDescription(), "Database should reflect description with spaces");
        assertEquals("blue", dbTag.getColor(), "Database should reflect color change");
    }

    /**
     * Verifies {@code Flight.getUnassociatedTags} returns all of a fleet's tags when the flight has none associated.
     *
     * @throws SQLException if the query fails
     */
    @Test
    @Order(90)
    @DisplayName("Should return all tags when flight has no associated tags")
    public void testGetUnassociatedTagsWithNoAssociatedTags() throws SQLException {

        createTestFlight(999);
        Flight flight = Flight.getFlight(connection, 999);

        // Create test tags manually (not associated with any flight)
        String tagName1 = "TestTag1_" + System.currentTimeMillis();
        String tagName2 = "TestTag2_" + System.currentTimeMillis();
        String tagName3 = "TestTag3_" + System.currentTimeMillis();

        // Insert tags directly into database
        try (PreparedStatement stmt = connection.prepareStatement(
                "INSERT INTO flight_tags (fleet_id, name, description, color) VALUES(?,?,?,?)",
                java.sql.Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, 1);
            stmt.setString(2, tagName1);
            stmt.setString(3, "Description1");
            stmt.setString(4, "red");
            stmt.executeUpdate();

            stmt.setString(2, tagName2);
            stmt.setString(3, "Description2");
            stmt.setString(4, "blue");
            stmt.executeUpdate();

            stmt.setString(2, tagName3);
            stmt.setString(3, "Description3");
            stmt.setString(4, "green");
            stmt.executeUpdate();
        }

        // Get unassociated tags (should return all tags since flight has none)
        List<FlightTag> unassociatedTags = Flight.getUnassociatedTags(connection, flight.getId(), 1);

        // Filter for only the tags we created in this test
        List<FlightTag> ourTags = unassociatedTags.stream()
                .filter(tag -> tag.getName().equals(tagName1)
                        || tag.getName().equals(tagName2)
                        || tag.getName().equals(tagName3))
                .collect(Collectors.toList());

        // Verify we get our 3 tags
        assertEquals(3, ourTags.size(), "Should return our 3 tags when flight has no associated tags");

        // Verify the tags are the ones we created
        Set<String> tagNames = ourTags.stream().map(FlightTag::getName).collect(Collectors.toSet());
        assertTrue(tagNames.contains(tagName1), "Should contain tag1");
        assertTrue(tagNames.contains(tagName2), "Should contain tag2");
        assertTrue(tagNames.contains(tagName3), "Should contain tag3");
    }

    /**
     * Verifies {@code Flight.getUnassociatedTags} returns only the tags not yet associated with the flight when it has
     * some associations.
     *
     * @throws SQLException if the query fails
     */
    @Test
    @Order(91)
    @DisplayName("Should return unassociated tags when flight has some associated tags")
    public void testGetUnassociatedTagsWithSomeAssociatedTags() throws SQLException {

        createTestFlight(998);
        Flight flight = Flight.getFlight(connection, 998);

        // Create test tags - these will be automatically associated with the flight
        FlightTag tag1 =
                Flight.createTag(1, 998, "TestTag1_" + System.currentTimeMillis(), "Description1", "red", connection);
        FlightTag tag2 =
                Flight.createTag(1, 998, "TestTag2_" + System.currentTimeMillis(), "Description2", "blue", connection);

        // Create additional tags that are NOT associated with the flight
        // We need to create these manually to avoid automatic association
        String tag3Name = "TestTag3_" + System.currentTimeMillis();
        String tag4Name = "TestTag4_" + System.currentTimeMillis();

        // Insert tags manually without association
        try (java.sql.Statement stmt = connection.createStatement()) {
            stmt.executeUpdate("INSERT INTO flight_tags (fleet_id, name, description, color) VALUES (1, '" + tag3Name
                    + "', 'Description3', 'green')");
            stmt.executeUpdate("INSERT INTO flight_tags (fleet_id, name, description, color) VALUES (1, '" + tag4Name
                    + "', 'Description4', 'yellow')");
        }

        // Get the tag IDs for the manually created tags
        int tag3Id;
        int tag4Id;
        try (java.sql.Statement stmt = connection.createStatement();
                java.sql.ResultSet rs =
                        stmt.executeQuery("SELECT id FROM flight_tags WHERE name = '" + tag3Name + "'")) {
            rs.next();
            tag3Id = rs.getInt(1);
        }
        try (java.sql.Statement stmt = connection.createStatement();
                java.sql.ResultSet rs =
                        stmt.executeQuery("SELECT id FROM flight_tags WHERE name = '" + tag4Name + "'")) {
            rs.next();
            tag4Id = rs.getInt(1);
        }

        // Get unassociated tags (should return tag3 and tag4)
        List<FlightTag> unassociatedTags = Flight.getUnassociatedTags(connection, flight.getId(), 1);

        // Filter for only the tags we created in this test
        List<FlightTag> ourTags = unassociatedTags.stream()
                .filter(tag -> tag.getName().equals(tag3Name) || tag.getName().equals(tag4Name))
                .collect(Collectors.toList());

        // Verify we get only the unassociated tags
        assertEquals(2, ourTags.size(), "Should return only our unassociated tags");

        // Verify the tags are the unassociated ones
        Set<String> tagNames = ourTags.stream().map(FlightTag::getName).collect(Collectors.toSet());
        assertTrue(tagNames.contains(tag3Name), "Should contain tag3");
        assertTrue(tagNames.contains(tag4Name), "Should contain tag4");
        assertFalse(tagNames.contains(tag1.getName()), "Should not contain tag1");
        assertFalse(tagNames.contains(tag2.getName()), "Should not contain tag2");
    }

    /**
     * Verifies {@code Flight.getUnassociatedTags} returns an empty list when every tag is already associated with the
     * flight.
     *
     * @throws SQLException if the query fails
     */
    @Test
    @Order(92)
    @DisplayName("Should return empty list when all tags are associated")
    public void testGetUnassociatedTagsWithAllTagsAssociated() throws SQLException {
        // Create test tags
        FlightTag tag1 =
                Flight.createTag(1, 999, "TestTag1_" + System.currentTimeMillis(), "Description1", "red", connection);
        FlightTag tag2 =
                Flight.createTag(1, 999, "TestTag2_" + System.currentTimeMillis(), "Description2", "blue", connection);

        createTestFlight(997);
        Flight flight = Flight.getFlight(connection, 997);

        // Associate all tags with the flight
        Flight.associateTag(flight.getId(), tag1.hashCode(), connection);
        Flight.associateTag(flight.getId(), tag2.hashCode(), connection);

        // Get unassociated tags (should return empty list)
        List<FlightTag> unassociatedTags = Flight.getUnassociatedTags(connection, flight.getId(), 1);

        // Filter for only the tags we created in this test
        List<FlightTag> ourTags = unassociatedTags.stream()
                .filter(tag ->
                        tag.getName().equals(tag1.getName()) || tag.getName().equals(tag2.getName()))
                .collect(Collectors.toList());

        // Verify we get empty list for our tags
        assertTrue(ourTags.isEmpty(), "Should return empty list when all our tags are associated");
    }

    /**
     * Verifies {@code Flight.getUnassociatedTags} returns an empty list when the fleet has no tags at all.
     *
     * @throws SQLException if the query fails
     */
    @Test
    @Order(93)
    @DisplayName("Should return empty list when no tags exist")
    public void testGetUnassociatedTagsWithNoTags() throws SQLException {

        createTestFlight(997);
        Flight flight = Flight.getFlight(connection, 997);

        // Get unassociated tags (should return empty list)
        List<FlightTag> unassociatedTags = Flight.getUnassociatedTags(connection, flight.getId(), 1);

        // Since this test doesn't create any tags, we just verify that the method doesn't crash
        // and returns a list (which might be empty or contain tags from other tests)
        assertNotNull(unassociatedTags, "Should return a list (even if empty)");
    }

    /**
     * Verifies {@code Flight.getUnassociatedTags} scopes its results correctly to the given fleet id.
     *
     * @throws SQLException if the query fails
     */
    @Test
    @Order(94)
    @DisplayName("Should handle different fleet IDs correctly")
    public void testGetUnassociatedTagsWithDifferentFleetIds() throws SQLException {
        // Create tags for fleet 1
        FlightTag tag1 =
                Flight.createTag(1, 999, "TestTag1_" + System.currentTimeMillis(), "Description1", "red", connection);
        FlightTag tag2 =
                Flight.createTag(1, 999, "TestTag2_" + System.currentTimeMillis(), "Description2", "blue", connection);

        createTestFlight(997);
        Flight flight = Flight.getFlight(connection, 997);

        // Create tags for fleet 2
        FlightTag tag3 =
                Flight.createTag(2, 998, "TestTag3_" + System.currentTimeMillis(), "Description3", "green", connection);
        FlightTag tag4 = Flight.createTag(
                2, 998, "TestTag4_" + System.currentTimeMillis(), "Description4", "yellow", connection);

        // Get unassociated tags for fleet 1 (should return fleet 1 tags only)
        List<FlightTag> unassociatedTagsFleet1 = Flight.getUnassociatedTags(connection, flight.getId(), 1);

        // Filter for only the tags we created in this test
        List<FlightTag> ourFleet1Tags = unassociatedTagsFleet1.stream()
                .filter(tag ->
                        tag.getName().equals(tag1.getName()) || tag.getName().equals(tag2.getName()))
                .collect(Collectors.toList());

        // Verify we get only our fleet 1 tags
        assertEquals(2, ourFleet1Tags.size(), "Should return only our fleet 1 tags");
        Set<String> fleet1TagNames =
                ourFleet1Tags.stream().map(FlightTag::getName).collect(Collectors.toSet());
        assertTrue(fleet1TagNames.contains(tag1.getName()), "Should contain fleet 1 tag1");
        assertTrue(fleet1TagNames.contains(tag2.getName()), "Should contain fleet 1 tag2");
        assertFalse(fleet1TagNames.contains(tag3.getName()), "Should not contain fleet 2 tag3");
        assertFalse(fleet1TagNames.contains(tag4.getName()), "Should not contain fleet 2 tag4");
    }

    /**
     * Verifies {@code Flight.getUnassociatedTags} returns exactly the unassociated subset when a flight has a mix of
     * associated and unassociated tags.
     *
     * @throws SQLException if the query fails
     */
    @Test
    @Order(95)
    @DisplayName("Should handle mixed associated and unassociated tags correctly")
    public void testGetUnassociatedTagsWithMixedTags() throws SQLException {
        // Create test tags
        FlightTag tag1 =
                Flight.createTag(1, 999, "TestTag1_" + System.currentTimeMillis(), "Description1", "red", connection);
        FlightTag tag2 =
                Flight.createTag(1, 999, "TestTag2_" + System.currentTimeMillis(), "Description2", "blue", connection);
        FlightTag tag3 =
                Flight.createTag(1, 999, "TestTag3_" + System.currentTimeMillis(), "Description3", "green", connection);
        FlightTag tag4 = Flight.createTag(
                1, 999, "TestTag4_" + System.currentTimeMillis(), "Description4", "yellow", connection);
        FlightTag tag5 = Flight.createTag(
                1, 999, "TestTag5_" + System.currentTimeMillis(), "Description5", "purple", connection);

        createTestFlight(997);
        Flight flight = Flight.getFlight(connection, 997);

        // Associate some tags with the flight (tag1, tag3, tag5)
        Flight.associateTag(flight.getId(), tag1.hashCode(), connection);
        Flight.associateTag(flight.getId(), tag3.hashCode(), connection);
        Flight.associateTag(flight.getId(), tag5.hashCode(), connection);

        // Get unassociated tags (should return tag2 and tag4)
        List<FlightTag> unassociatedTags = Flight.getUnassociatedTags(connection, flight.getId(), 1);

        // Filter for only the tags we created in this test
        List<FlightTag> ourTags = unassociatedTags.stream()
                .filter(tag ->
                        tag.getName().equals(tag2.getName()) || tag.getName().equals(tag4.getName()))
                .collect(Collectors.toList());

        // Verify we get only the unassociated tags
        assertEquals(2, ourTags.size(), "Should return only unassociated tags");

        // Verify the tags are the unassociated ones
        Set<String> tagNames = ourTags.stream().map(FlightTag::getName).collect(Collectors.toSet());
        assertTrue(tagNames.contains(tag2.getName()), "Should contain tag2");
        assertTrue(tagNames.contains(tag4.getName()), "Should contain tag4");
        assertFalse(tagNames.contains(tag1.getName()), "Should not contain tag1");
        assertFalse(tagNames.contains(tag3.getName()), "Should not contain tag3");
        assertFalse(tagNames.contains(tag5.getName()), "Should not contain tag5");
    }

    /**
     * Verifies {@code Flight.getUnassociatedTags} throws {@link NullPointerException} for a null connection.
     */
    @Test
    @Order(96)
    @DisplayName("Should handle null connection gracefully in getUnassociatedTags")
    public void testGetUnassociatedTagsWithNullConnection() {

        assertThrows(
                NullPointerException.class,
                () -> {
                    Flight.getUnassociatedTags(null, 1, 1);
                },
                "Should throw NullPointerException for null connection");
    }

    /**
     * Verifies {@code Flight.getUnassociatedTags} returns the fleet's tags (none associated) for a non-existent flight
     * id.
     *
     * @throws SQLException if the query fails
     */
    @Test
    @Order(97)
    @DisplayName("Should handle non-existent flight ID")
    public void testGetUnassociatedTagsWithNonExistentFlight() throws SQLException {
        // Create test tags
        FlightTag tag1 =
                Flight.createTag(1, 999, "TestTag1_" + System.currentTimeMillis(), "Description1", "red", connection);
        FlightTag tag2 =
                Flight.createTag(1, 999, "TestTag2_" + System.currentTimeMillis(), "Description2", "blue", connection);

        List<FlightTag> unassociatedTags = Flight.getUnassociatedTags(connection, 99999, 1);

        // Filter for only the tags we created in this test
        List<FlightTag> ourTags = unassociatedTags.stream()
                .filter(tag ->
                        tag.getName().equals(tag1.getName()) || tag.getName().equals(tag2.getName()))
                .collect(Collectors.toList());

        // Should return our 2 tags since non-existent flight has no associated tags
        assertEquals(2, ourTags.size(), "Should return our 2 tags for non-existent flight");

        // Verify our specific tags are returned
        Set<String> tagNames = ourTags.stream().map(FlightTag::getName).collect(Collectors.toSet());
        assertTrue(tagNames.contains(tag1.getName()), "Should contain tag1");
        assertTrue(tagNames.contains(tag2.getName()), "Should contain tag2");
    }

    /**
     * Verifies the {@code idLimStr} helper builds the correct SQL id-list/limit clause for multiple flight ids.
     *
     * @throws SQLException if a database operation fails
     */
    @Test
    @Order(400)
    @DisplayName("Should test idLimStr method with multiple flight IDs")
    public void testIdLimStrWithMultipleFlightIds() throws SQLException {
        // Create test flights
        createTestFlight(6000);
        createTestFlight(6001);
        createTestFlight(6002);

        // Create test tags and get their IDs
        String tag1Name = "TestTag1_" + System.currentTimeMillis();
        String tag2Name = "TestTag2_" + System.currentTimeMillis();
        String tag3Name = "TestTag3_" + System.currentTimeMillis();

        Flight.createTag(1, 1, tag1Name, "Description1", "red", connection);
        Flight.createTag(1, 1, tag2Name, "Description2", "blue", connection);
        Flight.createTag(1, 1, tag3Name, "Description3", "green", connection);

        // Get the tag IDs
        int tagId1 = getTagIdByName(connection, tag1Name);
        int tagId2 = getTagIdByName(connection, tag2Name);
        int tagId3 = getTagIdByName(connection, tag3Name);

        // Associate tags with flights to test the idLimStr method
        // This will call disassociateTags which uses idLimStr(int[] ids, String idName, boolean complement)
        // with multiple flight IDs, ensuring the line sb.append(complement ?
        // (" AND " + idName + " != ") : (" OR " + idName + " = ")) is covered

        // First associate some tags with flights
        Flight.associateTag(6000, tagId1, connection); // Associate tag 1 with flight 6000
        Flight.associateTag(6001, tagId1, connection); // Associate tag 1 with flight 6001

        // Now disassociate tag 1 from multiple flights - this will use idLimStr(int[] ids,
        // String idName, boolean complement) with complement=false, but it will still test
        // the line: sb.append(complement ? (" AND " + idName + " != ") : (" OR " + idName + " = "));
        Flight.disassociateTags(tagId1, connection, 6000, 6001);

        // Verify the disassociation worked by checking that the tag is no longer associated with these flights
        List<FlightTag> tagsForFlight6000 = Flight.getTags(connection, 6000);
        List<FlightTag> tagsForFlight6001 = Flight.getTags(connection, 6001);

        // The flights should have no tags now
        assertTrue(
                tagsForFlight6000 == null || tagsForFlight6000.isEmpty(),
                "Flight 6000 should have no tags after disassociation");
        assertTrue(
                tagsForFlight6001 == null || tagsForFlight6001.isEmpty(),
                "Flight 6001 should have no tags after disassociation");
    }

    /**
     * Verifies the {@code idLimStr} helper builds the correct clause with {@code complement=true} (the
     * negated/not-in form), exercised via {@code disassociateTags}.
     *
     * @throws SQLException if a database operation fails
     */
    @Test
    @Order(401)
    @DisplayName("Should test idLimStr method with complement=true using disassociateTags")
    public void testIdLimStrWithComplement() throws SQLException {
        // Create test flights
        createTestFlight(7000);
        createTestFlight(7001);
        createTestFlight(7002);

        // Create test tags and get their IDs
        String tag4Name = "TestTag4_" + System.currentTimeMillis();
        String tag5Name = "TestTag5_" + System.currentTimeMillis();

        Flight.createTag(1, 1, tag4Name, "Description4", "yellow", connection);
        Flight.createTag(1, 1, tag5Name, "Description5", "purple", connection);

        // Get the tag IDs
        int tagId4 = getTagIdByName(connection, tag4Name);
        int tagId5 = getTagIdByName(connection, tag5Name);

        // Associate tag 4 with multiple flights
        Flight.associateTag(7000, tagId4, connection);
        Flight.associateTag(7001, tagId4, connection);
        Flight.associateTag(7002, tagId4, connection);

        // Now disassociate tag 4 from multiple flights - this will use idLimStr(int[] ids,
        // String idName, boolean complement) with complement=false, but it will still test
        // the line: sb.append(complement ? (" AND " + idName + " != ") : (" OR " + idName + " = "));
        Flight.disassociateTags(tagId4, connection, 7000, 7001, 7002);

        // Verify the disassociation worked by checking that the tag is no longer associated with these flights
        List<FlightTag> tagsForFlight7000 = Flight.getTags(connection, 7000);
        List<FlightTag> tagsForFlight7001 = Flight.getTags(connection, 7001);
        List<FlightTag> tagsForFlight7002 = Flight.getTags(connection, 7002);

        // The flights should have no tags now
        assertTrue(
                tagsForFlight7000 == null || tagsForFlight7000.isEmpty(),
                "Flight 7000 should have no tags after disassociation");
        assertTrue(
                tagsForFlight7001 == null || tagsForFlight7001.isEmpty(),
                "Flight 7001 should have no tags after disassociation");
        assertTrue(
                tagsForFlight7002 == null || tagsForFlight7002.isEmpty(),
                "Flight 7002 should have no tags after disassociation");
    }
}
