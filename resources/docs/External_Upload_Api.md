# NGAFID External Upload API

Bearer-token REST API for uploading flight data without a session. Same processing pipeline as the web UI.

---

## Files created/updated

| Path | Purpose |
|---|---|
| `ngafid-db/src/changelogs/00-accounts/09-api-tokens.sql` | `api_token` table |
| `ngafid-core/src/main/java/org/ngafid/core/accounts/ApiToken.java` | Token model: generation, hashing, lookup, revocation |
| `ngafid-www/src/main/java/org/ngafid/www/routes/ApiTokenAuth.java` | Bearer-token `before` filter for `/api/external/*` |
| `ngafid-www/src/main/java/org/ngafid/www/routes/ApiExternalUploadRoutes.java` | POST/GET upload endpoints + filter registration |
| `ngafid-www/src/main/java/org/ngafid/www/routes/ApiTokenManagementRoutes.java` | Session-authed CRUD for tokens |
| `ngafid-www/src/main/java/org/ngafid/www/JavalinWebServer.java` | +2 `bindRoutes` calls, +2 imports |

---

## Schema

```sql
api_token (
    id            INT PK AUTO_INCREMENT,
    user_id       INT NOT NULL FK -> user(id) ON DELETE CASCADE,
    token_hash    CHAR(64) UNIQUE,         -- SHA-256 hex of plaintext
    token_name    VARCHAR(128),
    created_at    DATETIME DEFAULT NOW(),
    expires_at    DATETIME NULL,
    revoked_at    DATETIME NULL,
    last_used_at  DATETIME NULL
)
```

Index on `(user_id)` for the management list query. `token_hash` UNIQUE is the auth lookup index.

---

## Endpoints

### `/api/external/*` — bearer-token auth

`Authorization: Bearer ngafid_<base64url>` required on every request.

| Method | Path | Body / Params | Returns |
|---|---|---|---|
| POST | `/api/external/uploads` | multipart: `file` (required), `fleetName` (optional) | `201` new upload, or `200` existing retryable upload |
| GET | `/api/external/uploads` | `?page=0&pageSize=25` | `200` `{items[], page, pageSize, total}` |
| GET | `/api/external/uploads/{uploadId}` | — | `200` upload detail, or `404` if not in caller's fleet |

Response codes for POST:

| Code | Meaning |
|---|---|
| `201` | New upload accepted; body is `UploadResponse` |
| `200` | Existing upload with same MD5 is in a retryable state (`UPLOADING_FAILED`, `FAILED_*`, `ENQUEUED`, `PROCESSING`); body is the existing `UploadResponse` |
| `400` | Missing/empty `file`, or unsafe filename after sanitization |
| `403` | User lacks `MANAGER`/`UPLOAD` access on the resolved fleet |
| `404` | Supplied `fleetName` does not exist |
| `409` | Existing upload with same MD5 is already finalized (`UPLOADED`, `PROCESSED_OK`, `PROCESSED_WARNING`) |
| `500` | Internal error (always `{"error":"..."}` shape) |

All error bodies share the same shape: `{"error": "..."}`. Internal exceptions are logged server-side but not exposed.

### `/protected/api_tokens` — session-cookie auth

| Method | Path | Body | Returns |
|---|---|---|---|
| POST | `/protected/api_tokens` | `{"name":"...","expiresInDays":N}` | `201` includes plaintext token (once) |
| GET | `/protected/api_tokens` | — | `200` array of `{id, name, createdAt, expiresAt, revokedAt, lastUsedAt, active}` |
| DELETE | `/protected/api_tokens/{tokenId}` | — | `204`, or `404` if not owned by caller |

---

## Fleet targeting

The optional `fleetName` form field selects the destination fleet:

- Provided → looked up against the unique `fleet.fleet_name` column. `404` if no match.
- Omitted → falls back to the user's currently-selected fleet (`user.getFleetId()`).

Either way, the resolved fleet is checked against `fleet_access` — only `MANAGER` or `UPLOAD` proceed; everything else returns `403`.

---

## Filename sanitization

Before any file IO or DB write:

1. Whitespace, parentheses, and square brackets collapse to underscores.
2. Anything outside `[a-zA-Z0-9_.-]` is stripped.
3. If nothing usable remains, the request is rejected with `400`.

Examples:

| Raw | Sanitized |
|---|---|
| `C172.zip` | `C172.zip` |
| `C172 (1).zip` | `C172_1.zip` |
| `flight log [2024].zip` | `flight_log_2024.zip` |
| `🛫.zip` | `.zip` → rejected (no name remaining besides extension prefix) |

The sanitized name is what's stored in `uploads.filename` and used for the archive path.

---

## Request flow (POST upload)

1. Javalin `before("/api/external/*")` → `ApiTokenAuth.requireApiToken`.
2. Extract `Bearer` token → SHA-256 → `SELECT FROM api_token WHERE token_hash = ?`.
3. Check `revoked_at IS NULL AND (expires_at IS NULL OR expires_at > NOW())`.
4. Load user: `SELECT fleet_selected FROM user`, verify `fleet_access` row exists, then `User.get(conn, userId, fleetId)`.
5. Update `last_used_at`.
6. Attach `User` + `ApiToken` to `ctx.attribute(...)`.
7. Route handler:
   - Resolve fleet: `fleetName` form param → `SELECT id FROM fleet WHERE fleet_name = ?`, else `user.getFleetId()`.
   - `user.hasUploadAccess(fleetId)` → 403 if not `MANAGER`/`UPLOAD`.
   - Validate `file` part present + non-empty.
   - Sanitize filename → 400 if nothing usable remains.
   - Stream body to temp file (`Files.copy`), then hash with `org.ngafid.core.util.MD5.computeHexHash` (single shared util, not a re-implementation).
   - `Upload.getUploadByUser(conn, uploaderId, md5)` → branch on status:
     - Finalized → 409 with `DuplicateUploadResponse`.
     - Retryable → 200 with existing `UploadResponse`.
     - None → proceed.
   - `Upload.createNewUpload(conn, uploaderId, fleetId, sanitizedName, "api-" + UUID, Kind.FILE, size, 1, md5)`.
   - `Files.move(tempFile, upload.getArchivePath())`.
   - `try (LockedUpload l = upload.getLockedUpload(conn)) { l.chunkUploaded(0, size); l.complete(); }`.
8. `LockedUpload.close()` publishes upload id to Kafka `Topic.UPLOAD`.
9. `upload-consumer` polls, picks up the id, runs standard processing pipeline.

Status progression: `UPLOADING → UPLOADED → ENQUEUED → PROCESSING → PROCESSED_OK | PROCESSED_WARNING | FAILED_*`.

### Rollback on partial failure

If anything throws after `createNewUpload` (insert, file move, lock, complete), the `finally` block:

1. Deletes the `uploads` row by id.
2. Deletes the archive file if it was moved into place.
3. Deletes the temp file if still present.

This avoids leaving an orphaned row that would block retries via the duplicate-MD5 check.

---

## Token format