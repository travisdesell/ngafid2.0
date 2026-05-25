package org.ngafid.core.flights;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

/**
 * Fleet-independent tail → airframe lookup ({@code tail_airframe_registry}).
 * Only rows whose airframe {@code type_id} resolves to {@code Rotorcraft} are returned.
 */
public final class RotorcraftTailAirframeRegistry {

    public static final String TABLE = "tail_airframe_registry";

    private RotorcraftTailAirframeRegistry() {}

    public record Entry(String tail, String airframe, String airframeType) {}

    /**
     * USCG IGS exports sometimes use a longer bureau number (e.g. {@code 660407}) while the registry stores the
     * four-digit tail ({@code 6604}).
     */
    public static String normalizeTailForLookup(String tail) {
        if (tail == null) {
            return "";
        }
        String trimmed = tail.trim();
        if (trimmed.matches("\\d{5,}")) {
            return trimmed.substring(0, 4);
        }
        return trimmed;
    }

    /**
     * @param tail operator tail from filename or USCG {@code Aircraft Serial Number} metadata
     * @return registry row when tail exists and {@code airframe_types.name} is Rotorcraft
     */
    public static Optional<Entry> findRotorcraft(Connection connection, String tail) throws SQLException {
        Optional<Entry> direct = findRotorcraftExact(connection, tail);
        if (direct.isPresent()) {
            return direct;
        }
        String normalized = normalizeTailForLookup(tail);
        if (!normalized.isEmpty() && !normalized.equals(tail)) {
            return findRotorcraftExact(connection, normalized);
        }
        return Optional.empty();
    }

    private static Optional<Entry> findRotorcraftExact(Connection connection, String tail) throws SQLException {
        String sql = """
                SELECT r.tail, a.airframe, t.name AS airframe_type
                FROM tail_airframe_registry r
                INNER JOIN airframes a ON a.airframe = r.airframe
                INNER JOIN airframe_types t ON t.id = a.type_id
                WHERE r.tail = ? AND t.name = 'Rotorcraft'
                """;
        try (PreparedStatement query = connection.prepareStatement(sql)) {
            query.setString(1, tail);
            try (ResultSet rs = query.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new Entry(
                            rs.getString("tail"), rs.getString("airframe"), rs.getString("airframe_type")));
                }
            }
        }
        return Optional.empty();
    }
}
