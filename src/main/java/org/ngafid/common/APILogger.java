package org.ngafid.common;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.Instant;

public class APILogger {
    public static void logRequest(String method, String url, String path, String statusCode, String ip, String referer) {
        String sql = "INSERT INTO api_logs (method, url, path, status_code, ip, referer, timestamp) VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, method);
            ps.setString(2, url);
            ps.setString(3, path);
            ps.setString(4, statusCode);
            ps.setString(5, ip);
            ps.setString(6, referer);
            ps.setTimestamp(7, java.sql.Timestamp.from(Instant.now()));

            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Failed to log API request: " + e.getMessage());
        }
    }
}