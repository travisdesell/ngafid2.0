package org.ngafid.processor.steps;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import org.ngafid.core.flights.LossOfTailRotorEffectiveness.HelicopterSpec;

/**
 * Loads LTE helicopter specifications from the rotorcraft_airframe_specs database table.
 *
 * <p>The LTE model only needs the main-rotor and weight fields selected here; the rest of the table is retained for
 * auditability and future rotorcraft analyses.
 */
final class RotorcraftAirframeSpecRepository {
    private RotorcraftAirframeSpecRepository() {}

    static Optional<HelicopterSpec> findByAirframeId(Connection connection, int airframeId) throws SQLException {
        String sql = """
                SELECT
                    model,
                    series,
                    max_gross_weight_lbs,
                    min_flying_weight_lbs,
                    empty_weight_lbs,
                    mr_number_blades,
                    mr_diameter_in,
                    mr_inboard_blade_chord_in,
                    mr_power_on_max_continuous_rpm
                FROM rotorcraft_airframe_specs
                WHERE airframe_id = ?
                ORDER BY year DESC, id DESC
                LIMIT 1
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, airframeId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                return Optional.of(toHelicopterSpec(resultSet));
            }
        }
    }

    private static HelicopterSpec toHelicopterSpec(ResultSet resultSet) throws SQLException {
        String model = resultSet.getString("model");
        String series = resultSet.getString("series");
        String airframe = series == null || series.isBlank() ? model : model + " " + series;

        return new HelicopterSpec(
                airframe,
                nullableDouble(resultSet, "max_gross_weight_lbs"),
                nullableDouble(resultSet, "min_flying_weight_lbs"),
                nullableDouble(resultSet, "empty_weight_lbs"),
                nullableInt(resultSet, "mr_number_blades"),
                nullableDouble(resultSet, "mr_diameter_in"),
                nullableDouble(resultSet, "mr_inboard_blade_chord_in"),
                nullableDouble(resultSet, "mr_power_on_max_continuous_rpm"));
    }

    private static double nullableDouble(ResultSet resultSet, String column) throws SQLException {
        double value = resultSet.getDouble(column);
        return resultSet.wasNull() ? Double.NaN : value;
    }

    private static int nullableInt(ResultSet resultSet, String column) throws SQLException {
        int value = resultSet.getInt(column);
        return resultSet.wasNull() ? 0 : value;
    }
}