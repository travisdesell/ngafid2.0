package org.ngafid.www;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import org.ngafid.core.Database;

/**
 * Persists a record of each served API request to the {@code api_logs} table for auditing and usage analysis.
 *
 * <p>Logging is best-effort: database and host-resolution failures are caught and swallowed so that recording a
 * request never interferes with serving it.
 */
public class APILogger {
    private APILogger() {
        // Utility class
    }

    /**
     * Records an API request by inserting a row into the {@code api_logs} table (the client IP is stored as its raw
     * address bytes). Database or host-resolution failures are logged and swallowed so logging never breaks the
     * request being served.
     *
     * @param method the HTTP method of the request
     * @param path the request path
     * @param statusCode the HTTP status code the request produced
     * @param ipString the client's IP address as a string, resolved to bytes for storage
     * @param referer the HTTP referer header value, or null
     */
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
            // hello
        }
    }
}
