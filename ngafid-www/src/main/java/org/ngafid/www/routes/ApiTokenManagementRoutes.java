package org.ngafid.www.routes;

import io.javalin.Javalin;
import io.javalin.http.Context;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.ngafid.core.Database;
import org.ngafid.core.accounts.ApiToken;
import org.ngafid.core.accounts.User;
import org.ngafid.www.ErrorResponse;

/**
 * Session-authenticated routes for managing the current user's API tokens.
 * <p>
 * NOTE: These routes are session-authed (cookie), NOT token-authed -- a user
 * must be logged in via the normal web UI to create or revoke tokens.
 * That prevents a stolen token from minting more tokens.
 */
public final class ApiTokenManagementRoutes {

    private ApiTokenManagementRoutes() {
        /* utility */
    }

    public static void bindRoutes(Javalin app) {
        app.post("/protected/api_tokens", ApiTokenManagementRoutes::createToken);
        app.get("/protected/api_tokens", ApiTokenManagementRoutes::listTokens);
        app.delete("/protected/api_tokens/{tokenId}", ApiTokenManagementRoutes::revokeToken);
    }

    /** Body: {@code { "name": "...", "expiresInDays": 90 (optional) }} */
    private static void createToken(Context ctx) {
        User user = Objects.requireNonNull(ctx.sessionAttribute("user"));

        CreateTokenRequest req;
        try {
            req = ctx.bodyAsClass(CreateTokenRequest.class);
        } catch (Exception e) {
            ctx.status(400).json(new ApiTokenAuth.ApiError("Invalid JSON body"));
            return;
        }
        if (req == null || req.name == null || req.name.isBlank()) {
            ctx.status(400).json(new ApiTokenAuth.ApiError("Token name is required"));
            return;
        }
        if (req.name.length() > 128) {
            ctx.status(400).json(new ApiTokenAuth.ApiError("Token name too long (max 128 chars)"));
            return;
        }

        Timestamp expiresAt = null;
        if (req.expiresInDays != null && req.expiresInDays > 0) {
            expiresAt = Timestamp.from(Instant.now().plus(req.expiresInDays, ChronoUnit.DAYS));
        }

        try (Connection connection = Database.getConnection()) {
            ApiToken.CreatedApiToken created = ApiToken.create(connection, user.getId(), req.name, expiresAt);

            ctx.status(201)
                    .json(new CreatedTokenResponse(
                            created.getToken().getId(),
                            created.getPlaintext(), // shown ONCE
                            created.getToken().getTokenName(),
                            created.getToken().getCreatedAt(),
                            created.getToken().getExpiresAt(),
                            "Store this token somewhere safe -- it will NOT be shown again."));
        } catch (SQLException e) {
            ctx.status(500).json(new ErrorResponse(e));
        }
    }

    private static void listTokens(Context ctx) {
        User user = Objects.requireNonNull(ctx.sessionAttribute("user"));

        try (Connection connection = Database.getConnection()) {
            List<ApiToken> tokens = ApiToken.listForUser(connection, user.getId());
            List<TokenSummary> out = new ArrayList<>(tokens.size());
            for (ApiToken t : tokens) {
                out.add(new TokenSummary(
                        t.getId(),
                        t.getTokenName(),
                        t.getCreatedAt(),
                        t.getExpiresAt(),
                        t.getRevokedAt(),
                        t.getLastUsedAt(),
                        t.isActive()));
            }
            ctx.json(out);
        } catch (SQLException e) {
            ctx.status(500).json(new ErrorResponse(e));
        }
    }

    private static void revokeToken(Context ctx) {
        User user = Objects.requireNonNull(ctx.sessionAttribute("user"));

        int tokenId;
        try {
            tokenId = Integer.parseInt(ctx.pathParam("tokenId"));
        } catch (NumberFormatException e) {
            ctx.status(400).json(new ApiTokenAuth.ApiError("tokenId must be an integer"));
            return;
        }

        try (Connection connection = Database.getConnection()) {
            ApiToken token = ApiToken.getById(connection, tokenId);
            // 404 (not 403) when token belongs to someone else -- avoids leaking existence.
            if (token == null || token.getUserId() != user.getId()) {
                ctx.status(404).json(new ApiTokenAuth.ApiError("Token not found"));
                return;
            }
            token.revoke(connection);
            ctx.status(204);
        } catch (SQLException e) {
            ctx.status(500).json(new ErrorResponse(e));
        }
    }

    // -- DTOs ------------------------------------------------------------
    public static final class CreateTokenRequest {
        public String name;
        public Integer expiresInDays; // optional; null = never expires
    }

    public static final class CreatedTokenResponse {
        public final int id;
        public final String token; // PLAINTEXT -- only returned here, once
        public final String name;
        public final Timestamp createdAt;
        public final Timestamp expiresAt;
        public final String warning;

        public CreatedTokenResponse(
                int id, String token, String name, Timestamp createdAt, Timestamp expiresAt, String warning) {
            this.id = id;
            this.token = token;
            this.name = name;
            this.createdAt = createdAt;
            this.expiresAt = expiresAt;
            this.warning = warning;
        }
    }

    public static final class TokenSummary {
        public final int id;
        public final String name;
        public final Timestamp createdAt;
        public final Timestamp expiresAt;
        public final Timestamp revokedAt;
        public final Timestamp lastUsedAt;
        public final boolean active;

        public TokenSummary(
                int id,
                String name,
                Timestamp createdAt,
                Timestamp expiresAt,
                Timestamp revokedAt,
                Timestamp lastUsedAt,
                boolean active) {
            this.id = id;
            this.name = name;
            this.createdAt = createdAt;
            this.expiresAt = expiresAt;
            this.revokedAt = revokedAt;
            this.lastUsedAt = lastUsedAt;
            this.active = active;
        }
    }
}
