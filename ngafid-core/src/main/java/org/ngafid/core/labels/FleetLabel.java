package org.ngafid.core.labels;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/** Predefined label options per fleet for the labeling tool (stored in label_definitions). */
public class FleetLabel {
    private int id;
    private int fleetId;
    private String labelText;
    private int displayOrder;

    /** Constructs an empty fleet label; fields are populated when loaded from a result row or after insertion. */
    public FleetLabel() {}

    public int getId() {
        return id;
    }

    public int getFleetId() {
        return fleetId;
    }

    public String getLabelText() {
        return labelText;
    }

    public int getDisplayOrder() {
        return displayOrder;
    }

    /**
     * Loads all predefined label options for a fleet from {@code label_definitions}, ordered by display order and then
     * label text.
     *
     * @param connection the database connection
     * @param fleetId the fleet whose label definitions to load
     * @return the fleet's label definitions (empty if none)
     * @throws SQLException if the query fails
     */
    public static List<FleetLabel> getByFleet(Connection connection, int fleetId) throws SQLException {
        String sql = "SELECT id, fleet_id, label_text, display_order FROM label_definitions "
                + "WHERE fleet_id = ? ORDER BY display_order, label_text";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, fleetId);
            try (ResultSet rs = stmt.executeQuery()) {
                List<FleetLabel> result = new ArrayList<>();
                while (rs.next()) {
                    FleetLabel row = new FleetLabel();
                    row.id = rs.getInt("id");
                    row.fleetId = rs.getInt("fleet_id");
                    row.labelText = rs.getString("label_text");
                    row.displayOrder = rs.getInt("display_order");
                    result.add(row);
                }
                return result;
            }
        }
    }

    /**
     * Returns true if the given label text is allowed for this fleet (or blank).
     *
     * @param connection the database connection
     * @param fleetId the fleet to check
     * @param labelText the label text to validate
     * @return true when the label is blank or already defined for the fleet
     * @throws SQLException if the lookup fails
     */
    public static boolean isAllowedForFleet(Connection connection, int fleetId, String labelText) throws SQLException {
        if (labelText == null || labelText.isBlank()) return true;
        String sql = "SELECT 1 FROM label_definitions WHERE fleet_id = ? AND label_text = ? LIMIT 1";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, fleetId);
            stmt.setString(2, labelText.trim());
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    /**
     * Adds a new label definition for a fleet, assigning it the next display-order slot (one past the fleet's current
     * maximum). Does nothing and returns null when the label is blank or already defined for the fleet (as reported by
     * {@link #isAllowedForFleet}). The label text is trimmed before insertion.
     *
     * @param connection the database connection
     * @param fleetId the fleet to add the label to
     * @param labelText the label text to add
     * @return the newly created label (with its generated id and display order), or null if it was blank or already
     *     existed
     * @throws SQLException if any of the queries or the insert fail
     */
    public static FleetLabel insert(Connection connection, int fleetId, String labelText) throws SQLException {
        if (isAllowedForFleet(connection, fleetId, labelText)) return null; // already exists
        int nextOrder = 0;
        try (PreparedStatement sel = connection.prepareStatement(
                "SELECT COALESCE(MAX(display_order), -1) + 1 FROM label_definitions WHERE fleet_id = ?")) {
            sel.setInt(1, fleetId);
            try (ResultSet rs = sel.executeQuery()) {
                if (rs.next()) nextOrder = rs.getInt(1);
            }
        }
        try (PreparedStatement stmt = connection.prepareStatement(
                "INSERT INTO label_definitions (fleet_id, label_text, display_order) VALUES (?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, fleetId);
            stmt.setString(2, labelText.trim());
            stmt.setInt(3, nextOrder);
            stmt.executeUpdate();
            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) {
                    FleetLabel created = new FleetLabel();
                    created.id = keys.getInt(1);
                    created.fleetId = fleetId;
                    created.labelText = labelText.trim();
                    created.displayOrder = nextOrder;
                    return created;
                }
            }
        }
        return null;
    }

    /**
     * Remove a label definition. Caller must ensure the definition belongs to the user's fleet.
     *
     * @param connection the database connection
     * @param id the label definition ID to delete
     * @throws SQLException if the delete fails
     */
    public static void delete(Connection connection, int id) throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement("DELETE FROM label_definitions WHERE id = ?")) {
            stmt.setInt(1, id);
            stmt.executeUpdate();
        }
    }

    /**
     * Returns the fleet_id for a label definition, or null if not found.
     *
     * @param connection the database connection
     * @param id the label definition ID
     * @return the owning fleet ID, or null when the definition does not exist
     * @throws SQLException if the lookup fails
     */
    public static Integer getFleetIdForDefinition(Connection connection, int id) throws SQLException {
        try (PreparedStatement stmt =
                connection.prepareStatement("SELECT fleet_id FROM label_definitions WHERE id = ?")) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getInt("fleet_id") : null;
            }
        }
    }
}
