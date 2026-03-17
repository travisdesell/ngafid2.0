package org.ngafid.core.labels;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/** Predefined label options per fleet for the labeling tool (stored in label_definitions). */
public class FleetLabel {
    public int id;
    public int fleetId;
    public String labelText;
    public int displayOrder;

    public FleetLabel() {
    }

    public static List<FleetLabel> getByFleet(Connection connection, int fleetId) throws SQLException {
        String sql = "SELECT id, fleet_id, label_text, display_order FROM label_definitions WHERE fleet_id = ? ORDER BY display_order, label_text";
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

    /** Returns true if the given label text is allowed for this fleet (or blank). */
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

    public static FleetLabel insert(Connection connection, int fleetId, String labelText) throws SQLException {
        if (isAllowedForFleet(connection, fleetId, labelText)) return null; // already exists
        int nextOrder = 0;
        try (PreparedStatement sel = connection.prepareStatement("SELECT COALESCE(MAX(display_order), -1) + 1 FROM label_definitions WHERE fleet_id = ?")) {
            sel.setInt(1, fleetId);
            try (ResultSet rs = sel.executeQuery()) {
                if (rs.next()) nextOrder = rs.getInt(1);
            }
        }
        try (PreparedStatement stmt = connection.prepareStatement("INSERT INTO label_definitions (fleet_id, label_text, display_order) VALUES (?, ?, ?)", Statement.RETURN_GENERATED_KEYS)) {
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

    /** Remove a label definition. Caller must ensure the definition belongs to the user's fleet. */
    public static void delete(Connection connection, int id) throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement("DELETE FROM label_definitions WHERE id = ?")) {
            stmt.setInt(1, id);
            stmt.executeUpdate();
        }
    }

    /** Returns the fleet_id for a label definition, or null if not found. */
    public static Integer getFleetIdForDefinition(Connection connection, int id) throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement("SELECT fleet_id FROM label_definitions WHERE id = ?")) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getInt("fleet_id") : null;
            }
        }
    }
}
