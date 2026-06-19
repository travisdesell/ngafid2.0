package org.ngafid.core.accounts;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.logging.Logger;

/**
 * An API token used to authenticate against the external API.
 * Only the SHA-256 hash is stored — the plaintext is shown to the user once
 * at creation time via {@link CreatedApiToken} and never persisted.
 *
 * Tokens are bound to a user, not a fleet. Fleet-level access is checked
 * at the route handler using the user's currently-selected fleet.
 */
public final class ApiToken implements Serializable {
    private static final Logger LOG = Logger.getLogger(ApiToken.class.getName());

    public static final String TOKEN_PREFIX = "ngafid_";
    private static final int TOKEN_BYTE_LENGTH = 32;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    public static final String DEFAULT_COLUMNS =
            "id, user_id, token_hash, token_name, created_at, expires_at, revoked_at, last_used_at";

    private int id;
    private int userId;
    private String tokenHash;
    private String tokenName;
    private Timestamp createdAt;
    private Timestamp expiresAt;
    private Timestamp revokedAt;
    private Timestamp lastUsedAt;

    /**
     * Holds the persisted token record alongside its plaintext value.
     * The plaintext is only available right after creation — it can't be recovered later.
     */
    public static final class CreatedApiToken {
        private final ApiToken token;
        private final String plaintext;

        CreatedApiToken(ApiToken token, String plaintext) {
            this.token = token;
            this.plaintext = plaintext;
        }

        public ApiToken getToken() {
            return token;
        }

        public String getPlaintext() {
            return plaintext;
        }
    }

    private ApiToken() {}

    private ApiToken(ResultSet rs) throws SQLException {
        this.id = rs.getInt(1);
        this.userId = rs.getInt(2);
        this.tokenHash = rs.getString(3);
        this.tokenName = rs.getString(4);
        this.createdAt = rs.getTimestamp(5);
        this.expiresAt = rs.getTimestamp(6);
        this.revokedAt = rs.getTimestamp(7);
        this.lastUsedAt = rs.getTimestamp(8);
    }

    public int getId() {
        return id;
    }

    public int getUserId() {
        return userId;
    }

    public String getTokenName() {
        return tokenName;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public Timestamp getExpiresAt() {
        return expiresAt;
    }

    public Timestamp getRevokedAt() {
        return revokedAt;
    }

    public Timestamp getLastUsedAt() {
        return lastUsedAt;
    }

    /**
     * Returns true if the token hasn't been revoked and hasn't expired.
     *
     * @return {@code true} when the token is neither revoked nor past its expiration,
     *         {@code false} otherwise
     */
    public boolean isActive() {
        if (revokedAt != null) return false;
        if (expiresAt != null && expiresAt.before(Timestamp.from(Instant.now()))) return false;
        return true;
    }

    /**
     * Creates a new token, stores its hash, and returns both the record and the plaintext.
     *
     * @param connection an open database connection
     * @param userId the id of the user the token belongs to
     * @param tokenName a human-readable label for the token
     * @param expiresAt the token's expiration timestamp, or {@code null} for a token that
     *                  never expires
     * @return a {@link CreatedApiToken} carrying both the persisted record and the
     *         one-time plaintext value
     * @throws SQLException if the insert or follow-up lookup fails
     */
    public static CreatedApiToken create(Connection connection, int userId, String tokenName, Timestamp expiresAt)
            throws SQLException {
        String plaintext = generatePlaintextToken();
        String hash = sha256Hex(plaintext);

        try (PreparedStatement query = connection.prepareStatement(
                "INSERT INTO api_token (user_id, token_hash, token_name, expires_at) VALUES (?, ?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS)) {
            query.setInt(1, userId);
            query.setString(2, hash);
            query.setString(3, tokenName);
            if (expiresAt != null) query.setTimestamp(4, expiresAt);
            else query.setNull(4, Types.TIMESTAMP);

            LOG.info(query.toString());
            query.executeUpdate();

            int newId;
            try (ResultSet keys = query.getGeneratedKeys()) {
                keys.next();
                newId = keys.getInt(1);
            }

            return new CreatedApiToken(getById(connection, newId), plaintext);
        }
    }

    /**
     * Looks up a token by its plaintext value. The input is hashed before querying —
     * the plaintext never touches the SQL string. Callers must check {@link #isActive()}.
     *
     * @param connection an open database connection
     * @param plaintext the plaintext token value supplied by the client
     * @return the matching {@link ApiToken}, or {@code null} when no row matches the hash
     *         or {@code plaintext} is null/blank
     * @throws SQLException if the database query fails
     */
    public static ApiToken findByPlaintextToken(Connection connection, String plaintext) throws SQLException {
        if (plaintext == null || plaintext.isBlank()) return null;
        String hash = sha256Hex(plaintext);

        try (PreparedStatement query =
                connection.prepareStatement("SELECT " + DEFAULT_COLUMNS + " FROM api_token WHERE token_hash = ?")) {
            query.setString(1, hash);
            try (ResultSet rs = query.executeQuery()) {
                if (rs.next()) return new ApiToken(rs);
                return null;
            }
        }
    }

    public static ApiToken getById(Connection connection, int id) throws SQLException {
        try (PreparedStatement query =
                connection.prepareStatement("SELECT " + DEFAULT_COLUMNS + " FROM api_token WHERE id = ?")) {
            query.setInt(1, id);
            try (ResultSet rs = query.executeQuery()) {
                if (rs.next()) return new ApiToken(rs);
                return null;
            }
        }
    }

    /**
     * Returns all tokens for a user (active, revoked, and expired), newest first.
     *
     * @param connection an open database connection
     * @param userId the id of the user whose tokens to list
     * @return every {@link ApiToken} owned by the user, ordered by {@code created_at}
     *         descending; never null
     * @throws SQLException if the database query fails
     */
    public static List<ApiToken> listForUser(Connection connection, int userId) throws SQLException {
        ArrayList<ApiToken> tokens = new ArrayList<>();
        try (PreparedStatement query = connection.prepareStatement(
                "SELECT " + DEFAULT_COLUMNS + " FROM api_token WHERE user_id = ? ORDER BY created_at DESC")) {
            query.setInt(1, userId);
            try (ResultSet rs = query.executeQuery()) {
                while (rs.next()) tokens.add(new ApiToken(rs));
            }
        }
        return tokens;
    }

    /**
     * Revokes the token. Safe to call multiple times — does nothing if already revoked.
     *
     * @param connection an open database connection
     * @throws SQLException if the update fails
     */
    public void revoke(Connection connection) throws SQLException {
        if (revokedAt != null) return;
        try (PreparedStatement query = connection.prepareStatement(
                "UPDATE api_token SET revoked_at = CURRENT_TIMESTAMP WHERE id = ? AND revoked_at IS NULL")) {
            query.setInt(1, id);
            query.executeUpdate();
        }
        this.revokedAt = Timestamp.from(Instant.now());
    }

    /**
     * Updates last_used_at to now. Failure here is non-fatal.
     *
     * @param connection an open database connection
     * @throws SQLException if the update fails
     */
    public void touchLastUsed(Connection connection) throws SQLException {
        try (PreparedStatement query =
                connection.prepareStatement("UPDATE api_token SET last_used_at = CURRENT_TIMESTAMP WHERE id = ?")) {
            query.setInt(1, id);
            query.executeUpdate();
        }
    }

    private static String generatePlaintextToken() {
        byte[] bytes = new byte[TOKEN_BYTE_LENGTH];
        SECURE_RANDOM.nextBytes(bytes);
        return TOKEN_PREFIX + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String sha256Hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(hashBytes.length * 2);
            for (byte b : hashBytes) {
                String h = Integer.toHexString(b & 0xFF);
                if (h.length() == 1) hex.append('0');
                hex.append(h);
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
