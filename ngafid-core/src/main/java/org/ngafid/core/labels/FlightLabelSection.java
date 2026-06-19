package org.ngafid.core.labels;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

public class FlightLabelSection {
    private int id;
    private int flightId;
    private String tailNumber;
    private String airframe;
    private int startIndex;
    private int endIndex;
    private Timestamp startTime;
    private Timestamp endTime;
    /** Raw datetime string from DB (preserves value, no timezone conversion). */
    private String startTimeRaw;
    private String endTimeRaw;
    private Double startValue;
    private Double endValue;
    private String labelText;
    private List<String> parameterNames = new ArrayList<>();
    /** When set, insert uses this literal string instead of Timestamp (preserves value). */
    private String startTimeStr;
    private String endTimeStr;

    public FlightLabelSection() {
    }

    public int getId() {
        return id;
    }

    public int getFlightId() {
        return flightId;
    }

    public String getTailNumber() {
        return tailNumber;
    }

    public String getAirframe() {
        return airframe;
    }

    public int getStartIndex() {
        return startIndex;
    }

    public int getEndIndex() {
        return endIndex;
    }

    public Timestamp getStartTime() {
        return startTime;
    }

    public Timestamp getEndTime() {
        return endTime;
    }

    public String getStartTimeRaw() {
        return startTimeRaw;
    }

    public String getEndTimeRaw() {
        return endTimeRaw;
    }

    public Double getStartValue() {
        return startValue;
    }

    public Double getEndValue() {
        return endValue;
    }

    public String getLabelText() {
        return labelText;
    }

    public List<String> getParameterNames() {
        return parameterNames;
    }

    public String getStartTimeStr() {
        return startTimeStr;
    }

    public String getEndTimeStr() {
        return endTimeStr;
    }

    private static void fillFromRow(FlightLabelSection section, ResultSet rs) throws SQLException {
        section.id = rs.getInt("id");
        section.flightId = rs.getInt("flight_id");
        section.tailNumber = rs.getString("tail_number");
        section.airframe = rs.getString("airframe");
        section.startIndex = rs.getInt("start_index");
        section.endIndex = rs.getInt("end_index");
        section.startTime = rs.getTimestamp("start_time");
        section.endTime = rs.getTimestamp("end_time");
        section.startTimeRaw = rs.getString("start_time_str");
        section.endTimeRaw = rs.getString("end_time_str");
        section.startValue = (Double) rs.getObject("start_value");
        section.endValue = (Double) rs.getObject("end_value");
        section.labelText = rs.getString("label_text");
    }

    private static List<FlightLabelSection> fetchSections(
            Connection connection, String sql, int param) throws SQLException {
        List<FlightLabelSection> result = new ArrayList<>();
        FlightLabelSection current = null;
        int currentId = -1;
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, param);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    int id = rs.getInt("id");
                    if (current == null || id != currentId) {
                        current = new FlightLabelSection();
                        fillFromRow(current, rs);
                        current.parameterNames = new ArrayList<>();
                        result.add(current);
                        currentId = id;
                    }
                    String parameterName = rs.getString("parameter_name");
                    if (parameterName != null) current.parameterNames.add(parameterName);
                }
            }
        }
        return result;
    }

    private static final String SECTION_SELECT = """
        SELECT s.id, s.flight_id, s.tail_number, s.airframe, s.start_index, s.end_index,
               s.start_time, s.end_time,
               DATE_FORMAT(s.start_time, '%Y-%m-%d %H:%i:%s') AS start_time_str,
               DATE_FORMAT(s.end_time, '%Y-%m-%d %H:%i:%s') AS end_time_str,
               s.start_value, s.end_value, s.label_text, p.parameter_name
        FROM flight_label_section s
        LEFT JOIN flight_label_section_param p ON p.label_section_id = s.id
        """;

    public static List<FlightLabelSection> getByFlight(Connection connection, int flightId) throws SQLException {
        return fetchSections(connection, SECTION_SELECT + " WHERE s.flight_id = ? ORDER BY s.id", flightId);
    }

    /**
     * Returns all label sections for flights in the given fleet (for CSV export).
     *
     * @param connection the database connection
     * @param fleetId the fleet whose sections should be loaded
     * @return the label sections for all flights in the fleet
     * @throws SQLException if the query fails
     */
    public static List<FlightLabelSection> getByFleet(Connection connection, int fleetId) throws SQLException {
        String sql = SECTION_SELECT + """
            JOIN flights f ON s.flight_id = f.id AND f.fleet_id = ?
            ORDER BY s.flight_id, s.id
            """;
        return fetchSections(connection, sql, fleetId);
    }

    public static FlightLabelSection insert(Connection connection, FlightLabelSection in) throws SQLException {
        // Fetch tail_number and airframe from flight
        String tail = null;
        String airframe = null;
        String fetchSql = """
            SELECT t.tail, a.airframe FROM flights f
            LEFT JOIN tails t ON f.fleet_id = t.fleet_id AND f.system_id = t.system_id
            JOIN airframes a ON f.airframe_id = a.id
            WHERE f.id = ?
            """;
        try (PreparedStatement fetchStmt = connection.prepareStatement(fetchSql)) {
            fetchStmt.setInt(1, in.flightId);
            try (ResultSet rs = fetchStmt.executeQuery()) {
                if (rs.next()) {
                    tail = rs.getString(1);
                    airframe = rs.getString(2);
                }
            }
        }

        String sql = """
            INSERT INTO flight_label_section
                (flight_id, tail_number, airframe, start_index, end_index, start_time, end_time,
                 start_value, end_value, label_text)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

        try (PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, in.flightId);
            stmt.setString(2, tail);
            stmt.setString(3, airframe);
            stmt.setInt(4, in.startIndex);
            stmt.setInt(5, in.endIndex);
            if (in.startTimeStr != null) {
                stmt.setString(6, in.startTimeStr);
            } else {
                stmt.setTimestamp(6, in.startTime);
            }
            if (in.endTimeStr != null) {
                stmt.setString(7, in.endTimeStr);
            } else {
                stmt.setTimestamp(7, in.endTime);
            }
            if (in.startValue != null) {
                stmt.setDouble(8, in.startValue);
            } else {
                stmt.setNull(8, Types.DOUBLE);
            }
            if (in.endValue != null) {
                stmt.setDouble(9, in.endValue);
            } else {
                stmt.setNull(9, Types.DOUBLE);
            }
            stmt.setString(10, in.labelText);
            stmt.executeUpdate();
            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) in.id = keys.getInt(1);
            }
        }
        in.tailNumber = tail;
        in.airframe = airframe;

        if (in.parameterNames != null && !in.parameterNames.isEmpty()) {
            try (PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO flight_label_section_param (label_section_id, parameter_name) VALUES (?, ?)")) {
                for (String parameterName : in.parameterNames) {
                    ps.setInt(1, in.id);
                    ps.setString(2, parameterName);
                    ps.addBatch();
                }
                ps.executeBatch();
            }
        }

        return in;
    }

    /**
     * Returns the flight_id for a label section, or null if not found.
     *
     * @param connection the database connection
     * @param labelId the label section ID
     * @return the owning flight ID, or null when the label section does not exist
     * @throws SQLException if the lookup fails
     */
    public static Integer getFlightIdForLabel(Connection connection, int labelId) throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT flight_id FROM flight_label_section WHERE id = ?")) {
            stmt.setInt(1, labelId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getInt("flight_id") : null;
            }
        }
    }

    public static void updateLabelText(Connection connection, int id, String labelText) throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement(
                "UPDATE flight_label_section SET label_text = ? WHERE id = ?")) {
            stmt.setString(1, labelText);
            stmt.setInt(2, id);
            stmt.executeUpdate();
        }
    }

    public static void delete(Connection connection, int id) throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement(
                "DELETE FROM flight_label_section WHERE id = ?")) {
            stmt.setInt(1, id);
            stmt.executeUpdate();
        }
    }

    public static void deleteByFlight(Connection connection, int flightId) throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement(
                "DELETE FROM flight_label_section WHERE flight_id = ?")) {
            stmt.setInt(1, flightId);
            stmt.executeUpdate();
        }
    }
}
