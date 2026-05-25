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
     * @param tail operator tail from filename (e.g. {@code N351LL}, {@code 0131})
     * @return registry row when tail exists and {@code airframe_types.name} is Rotorcraft
     */
    public static Optional<Entry> findRotorcraft(Connection connection, String tail) throws SQLException {
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
