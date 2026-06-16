package org.ngafid.www.routes;

import io.javalin.http.Context;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.ngafid.core.Database;
import org.ngafid.core.accounts.AccountException;
import org.ngafid.core.accounts.ApiToken;
import org.ngafid.core.accounts.FleetAccess;
import org.ngafid.core.accounts.User;

/**
 * Bearer-token middleware for {@code /api/external/*} routes.
 *
 * Expects: {@code Authorization: Bearer <plaintext-token>}
 *
 * On success, attaches {@code "user"} and {@code "apiToken"} to the context.
 * On failure, responds 401 and halts the request chain.
 *
 * Wire up with: {@code app.before("/api/external/*", ApiTokenAuth::requireApiToken)}
 */
public final class ApiTokenAuth {
    private static final Logger LOG = Logger.getLogger(ApiTokenAuth.class.getName());
    private static final String AUTH_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private ApiTokenAuth() {}

    public static void requireApiToken(Context ctx) {
        String header = ctx.header(AUTH_HEADER);
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            reject(ctx, 401, "Missing or malformed Authorization header (expected: Bearer <token>)");
            return;
        }

        String plaintext = header.substring(BEARER_PREFIX.length()).trim();
        if (plaintext.isEmpty()) {
            reject(ctx, 401, "Empty bearer token");
            return;
        }

        try (Connection connection = Database.getConnection()) {
            ApiToken token = ApiToken.findByPlaintextToken(connection, plaintext);

            // Use the same message for "not found" and "expired/revoked" — don't leak which.
            if (token == null || !token.isActive()) {
                reject(ctx, 401, "Invalid or expired token");
                return;
            }

            User user = resolveUserWithSelectedFleet(connection, token.getUserId());
            if (user == null) {
                reject(ctx, 401, "Token user has no usable fleet");
                return;
            }

            try {
                token.touchLastUsed(connection);
            } catch (SQLException ignored) {
            }

            ctx.attribute("user", user);
            ctx.attribute("apiToken", token);

        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "DB error during token auth", e);
            ctx.status(500).json(new ApiError("Internal error during authentication"));
            ctx.skipRemainingHandlers();
        } catch (AccountException e) {
            LOG.log(Level.WARNING, "Failed to resolve user for token", e);
            reject(ctx, 401, "Token user is invalid");
        }
    }

    /**
     * Loads the user with their currently-selected fleet, verifying the fleet
     * exists and the user still has a fleet_access row for it.
     */
    private static User resolveUserWithSelectedFleet(Connection connection, int userId)
            throws SQLException, AccountException {
        int selectedFleetId;
        try (PreparedStatement q = connection.prepareStatement("SELECT fleet_selected FROM user WHERE id = ?")) {
            q.setInt(1, userId);
            try (ResultSet rs = q.executeQuery()) {
                if (!rs.next()) return null;
                selectedFleetId = rs.getInt(1);
                if (rs.wasNull() || selectedFleetId <= 0) return null;
            }
        }

        if (FleetAccess.get(connection, userId, selectedFleetId) == null) return null;
        return User.get(connection, userId, selectedFleetId);
    }

    private static void reject(Context ctx, int status, String message) {
        ctx.status(status).json(new ApiError(message));
        ctx.skipRemainingHandlers();
    }

    public static final class ApiError {
        public final String error;

        public ApiError(String error) {
            this.error = error;
        }
    }
}
