package org.ngafid.common;

import java.net.UnknownHostException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.Instant;
import java.net.InetAddress;

public class APILogger {
    public static void logRequest(String method, String path, int statusCode, String ipString, String referer) {
        String sql = "INSERT INTO api_logs (method, path, status_code, ip, referer) VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, method);
            ps.setString(2, path);
            ps.setInt(3, statusCode);

            byte[] ipBytes = InetAddress.getByName(ipString).getAddress();
            ps.setBytes(4, ipBytes);
            ps.setString(5, referer);

            ps.executeUpdate();
        } catch (SQLException | UnknownHostException e) {
            System.err.println("Failed to log API request: " + e.getMessage());
        }
    }
}