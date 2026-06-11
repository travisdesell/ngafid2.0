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
| POST | `/api/external/uploads` | multipart, field `file` | `201` `{uploadId, status, filename, md5Hash, sizeBytes, fleetId}` |
| GET | `/api/external/uploads` | `?page=0&pageSize=25` | `200` `{items[], page, pageSize, total}` |
| GET | `/api/external/uploads/{uploadId}` | — | `200` upload detail, or `404` if not in caller's fleet |

Errors: `401` invalid/expired token, `403` insufficient access (`VIEW`/`WAITING`/`DENIED`), `409` duplicate by `(uploader_id, md5_hash)`, `400` missing/empty file, `500` server error.

### `/protected/api_tokens` — session-cookie auth

| Method | Path | Body | Returns |
|---|---|---|---|
| POST | `/protected/api_tokens` | `{"name":"...","expiresInDays":N}` | `201` includes plaintext token (once) |
| GET | `/protected/api_tokens` | — | `200` array of `{id, name, createdAt, expiresAt, revokedAt, lastUsedAt, active}` |
| DELETE | `/protected/api_tokens/{tokenId}` | — | `204`, or `404` if not owned by caller |

---

## Request flow (POST upload)

1. Javalin `before("/api/external/*")` → `ApiTokenAuth.requireApiToken`.
2. Extract `Bearer` token → SHA-256 → `SELECT FROM api_token WHERE token_hash = ?`.
3. Check `revoked_at IS NULL AND (expires_at IS NULL OR expires_at > NOW())`.
4. Load user: `SELECT fleet_selected FROM user`, verify `fleet_access` row exists, then `User.get(conn, userId, fleetId)`.
5. Update `last_used_at`.
6. Attach `User` + `ApiToken` to `ctx.attribute(...)`.
7. Route handler:
   - `user.hasUploadAccess(user.getFleetId())` → 403 if `VIEW`/`WAITING`/`DENIED`.
   - `ctx.uploadedFile("file")` → stream to temp file + compute MD5 .
   - `Upload.getUploadByUser(conn, uploaderId, md5)` → 409 if duplicate.
   - `Upload.createNewUpload(conn, uploaderId, fleetId, filename, "api-" + UUID, Kind.FILE, size, 1, md5)`.
   - `Files.move(tempFile, upload.getArchivePath())`.
   - `try (LockedUpload l = upload.getLockedUpload(conn)) { l.chunkUploaded(0, size); l.complete(); }`.
8. `LockedUpload.close()` publishes upload id to Kafka `Topic.UPLOAD` .
9. `upload-consumer` polls, picks up the id, runs standard processing pipeline.

Status progression: `UPLOADING → UPLOADED → ENQUEUED → PROCESSING → PROCESSED_OK | PROCESSED_WARNING | FAILED_*`.

---

## Token format

```
ngafid_<43 chars base64url, no padding>
```

32 random bytes from `SecureRandom`, base64url encoded, prefixed for visual identification. SHA-256 hex stored in `token_hash`. Plaintext returned exactly once at creation; not recoverable afterward.

---

## Usage

### Get a session cookie

Log in at `http://localhost:8181`. Dev tools → Cookies → copy `JSESSIONID` value.

### Create a token

```bash
COOKIE='JSESSIONID=node0xxx.node0'

curl -X POST http://localhost:8181/protected/api_tokens \
    -H "Cookie: $COOKIE" \
    -H "Content-Type: application/json" \
    -d '{"name":"my-token","expiresInDays":90}'
```

Save the returned `token` value.

### Upload

```bash
TOKEN='ngafid_xxx'

curl -X POST http://localhost:8181/api/external/uploads \
    -H "Authorization: Bearer $TOKEN" \
    -F "file=@/path/to/flights.zip"
```

WSL path for Windows files: `/mnt/c/Users/<name>/Downloads/...`.

### Check status

```bash
curl -H "Authorization: Bearer $TOKEN" \
    http://localhost:8181/api/external/uploads/{id}
```

### Revoke

```bash
curl -X DELETE -H "Cookie: $COOKIE" \
    http://localhost:8181/protected/api_tokens/{id}
```

---

## Security model

- **At rest:** only SHA-256 hash stored; plaintext shown once.
- **In SQL:** plaintext never appears in a query string — always hashed first.
- **Fleet scope:** route ignores any client-supplied `fleet_id`. Fleet is `user.getFleetId()` (selected fleet).
- **Access check:** `user.hasUploadAccess(fleetId)` — only `MANAGER` + `UPLOAD` pass.
- **Error ambiguity:** invalid/expired/revoked all return same 401 message. Cross-fleet GET returns 404, not 403.
- **Token mgmt requires session:** stolen token cannot create or revoke other tokens.

---



## Rebuild

```bash
cd ~/Coop/ngafid2.0
mvn clean install -DskipTests
dcl build ngafid-www
dcl up -d ngafid-www
```




---

