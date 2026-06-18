package org.ngafid.www.routes;

import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.UploadedFile;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.ngafid.core.Database;
import org.ngafid.core.accounts.User;
import org.ngafid.core.uploads.Upload;
import org.ngafid.core.util.MD5;

/**
 * External API endpoints for uploading flight data via bearer-token auth.
 *
 * Authentication is enforced by {@link ApiTokenAuth} on every {@code /api/external/*}
 * path. The token identifies a single user; all routes operate on that user's behalf.
 *
 * Uploads may target a specific fleet by supplying the {@code fleetName} form parameter,
 * which is resolved against the unique {@code fleet.fleet_name} column. If omitted, the
 * request falls back to the user's currently-selected fleet. The user must hold
 * {@code MANAGER} or {@code UPLOAD} access on the resolved fleet, otherwise the request
 * is rejected with {@code 403}.
 *
 * All error responses share a single shape: {@link ApiTokenAuth.ApiError} with a generic
 * client-facing message. The underlying exception (when any) is logged server-side only.
 *
 * Wire up with: {@code ApiExternalUploadRoutes.bindRoutes(app)}
 */
public final class ApiExternalUploadRoutes {
    private static final Logger LOG = Logger.getLogger(ApiExternalUploadRoutes.class.getName());
    private static final String FILE_PART = "file";

    private ApiExternalUploadRoutes() {}

    /**
     * Registers the external upload routes and installs the bearer-token filter on every
     * {@code /api/external/*} path so the token is validated before any handler runs.
     *
     * @param app the Javalin application to register routes on
     */
    public static void bindRoutes(Javalin app) {
        app.before("/api/external/*", ApiTokenAuth::requireApiToken);

        app.post("/api/external/uploads", ApiExternalUploadRoutes::postUpload);
        app.get("/api/external/uploads", ApiExternalUploadRoutes::listUploads);
        app.get("/api/external/uploads/{uploadId}", ApiExternalUploadRoutes::getUpload);
    }

    /**
     * Handles {@code POST /api/external/uploads}: accepts a multipart file upload and
     * enqueues it for processing.
     *
     * Expected multipart fields: {@code file} (required, non-empty) and {@code fleetName}
     * (optional; defaults to the user's selected fleet). The handler sanitizes the
     * filename, writes the body to a temp file, hashes it with {@link MD5}, looks up any
     * prior upload with the same hash for the same user, then either rejects (if the
     * existing upload is already finalized) or returns the existing record (if it is in
     * a retryable state). Otherwise it creates a new {@code uploads} row, moves the temp
     * file into the archive, and publishes to Kafka via
     * {@link Upload.LockedUpload#complete()}.
     *
     * If any step after the {@code uploads} row is created fails, the row and any
     * partially-written archive file are removed so the upload can be retried.
     *
     * Returns 201 for a newly-accepted upload, 200 when an existing retryable upload
     * with the same MD5 is returned, 400 for a missing/empty file or unsafe filename,
     * 403 for insufficient access, 404 for an unknown {@code fleetName}, 409 when an
     * already-finalized duplicate exists, and 500 on internal errors.
     *
     * @param ctx the Javalin request context
     */
    private static void postUpload(Context ctx) {
        final User user = Objects.requireNonNull(ctx.attribute("user"));
        final int uploaderId = user.getId();

        // Resolve target fleet: prefer the supplied fleetName, otherwise fall back to
        // the user's selected fleet. fleet_name has a UNIQUE constraint.
        final int fleetId;
        String fleetNameParam = ctx.formParam("fleetName");
        if (fleetNameParam != null && !fleetNameParam.isBlank()) {
            String fleetName = fleetNameParam.trim();
            try (Connection connection = Database.getConnection();
                    PreparedStatement ps = connection.prepareStatement("SELECT id FROM fleet WHERE fleet_name = ?")) {
                ps.setString(1, fleetName);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        ctx.status(404).json(new ApiTokenAuth.ApiError("No fleet named '" + fleetName + "'"));
                        return;
                    }
                    fleetId = rs.getInt("id");
                }
            } catch (SQLException e) {
                LOG.log(Level.SEVERE, "fleet name lookup failed", e);
                ctx.status(500).json(new ApiTokenAuth.ApiError("Internal error"));
                return;
            }
        } else {
            fleetId = user.getFleetId();
        }

        // Reject if the user lacks upload access on the resolved fleet.
        if (!user.hasUploadAccess(fleetId)) {
            ctx.status(403)
                    .json(new ApiTokenAuth.ApiError(
                            "No upload access to fleet '" + fleetNameParam + "'. " + "Need MANAGER or UPLOAD."));
            return;
        }

        UploadedFile file = ctx.uploadedFile(FILE_PART);
        if (file == null) {
            ctx.status(400).json(new ApiTokenAuth.ApiError("Missing '" + FILE_PART + "' part in multipart form-data"));
            return;
        }
        if (file.size() <= 0) {
            ctx.status(400).json(new ApiTokenAuth.ApiError("Empty file"));
            return;
        }

        // Sanitize filename: collapse whitespace / parens / brackets to underscores,
        // strip anything that isn't [a-zA-Z0-9_.-], then reject if nothing usable remains.
        String rawName = file.filename();
        String sanitizedName = (rawName == null ? "" : rawName)
                .replaceAll("[\\s()\\[\\]]+", "_")
                .replaceAll("[^a-zA-Z0-9_.-]", "");
        if (!sanitizedName.matches("^[a-zA-Z0-9_.-]+$")) {
            ctx.status(400)
                    .json(new ApiTokenAuth.ApiError("Invalid filename '" + rawName
                            + "'. Allowed characters: letters, digits, underscore, dot, hyphen."));
            return;
        }

        Path tempFile = null;
        Integer createdUploadId = null; // set once the uploads row is inserted
        Path archivePath = null; // set once the temp file is moved into place
        boolean committed = false;
        try {
            // Stream the upload body to a temp file, then hash with the shared MD5 util.
            tempFile = Files.createTempFile("ngafid-api-" + sanitizedName + "-", ".tmp");
            try (InputStream in = file.content()) {
                Files.copy(in, tempFile, StandardCopyOption.REPLACE_EXISTING);
            }
            String md5Hash;
            try (InputStream in = Files.newInputStream(tempFile)) {
                md5Hash = MD5.computeHexHash(in);
            }

            try (Connection connection = Database.getConnection()) {
                Upload existing = Upload.getUploadByUser(connection, uploaderId, md5Hash);
                if (existing != null) {
                    Upload.Status s = existing.getStatus();
                    // Treat the upload as a duplicate only when the prior row is already
                    // finalized. For transient / failed states we return 200 with the
                    // existing record so the client can decide to retry or discard it.
                    boolean isFinalized = s == Upload.Status.UPLOADED
                            || s == Upload.Status.PROCESSED_OK
                            || s == Upload.Status.PROCESSED_WARNING;
                    if (isFinalized) {
                        ctx.status(409)
                                .json(new DuplicateUploadResponse(
                                        existing.getId(),
                                        s.name(),
                                        existing.getFilename(),
                                        "You have already uploaded a file with identical contents."));
                        return;
                    }
                    ctx.status(200)
                            .json(new UploadResponse(
                                    existing.getId(),
                                    s.name(),
                                    existing.getFilename(),
                                    existing.getMd5Hash(),
                                    existing.sizeBytes,
                                    existing.getFleetId()));
                    return;
                }

                Upload upload = Upload.createNewUpload(
                        connection,
                        uploaderId,
                        fleetId,
                        sanitizedName,
                        "api-" + UUID.randomUUID(),
                        Upload.Kind.FILE,
                        file.size(),
                        1,
                        md5Hash);
                createdUploadId = upload.getId();

                archivePath = upload.getArchivePath();
                Files.createDirectories(archivePath.getParent());
                Files.move(tempFile, archivePath, StandardCopyOption.REPLACE_EXISTING);
                tempFile = null;

                // On lock close, Upload auto-publishes to Topic.UPLOAD and kicks off processing.
                try (Upload.LockedUpload locked = upload.getLockedUpload(connection)) {
                    locked.chunkUploaded(0, file.size());
                    locked.complete();
                }
                committed = true;

                ctx.status(201)
                        .json(new UploadResponse(
                                upload.getId(),
                                upload.getStatus().name(),
                                upload.getFilename(),
                                upload.getMd5Hash(),
                                file.size(),
                                fleetId));
            }
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "DB error during external upload", e);
            ctx.status(500).json(new ApiTokenAuth.ApiError("Internal error"));
        } catch (IOException e) {
            LOG.log(Level.SEVERE, "IO error during external upload", e);
            ctx.status(500).json(new ApiTokenAuth.ApiError("Failed to store uploaded file"));
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Unexpected error during external upload", e);
            ctx.status(500).json(new ApiTokenAuth.ApiError("Internal error"));
        } finally {
            // If the request failed after we inserted the uploads row, roll it back so
            // a retry isn't blocked by the duplicate-MD5 check on the abandoned row.
            if (!committed && createdUploadId != null) {
                try (Connection cleanupConn = Database.getConnection();
                        PreparedStatement del = cleanupConn.prepareStatement("DELETE FROM uploads WHERE id = ?")) {
                    del.setInt(1, createdUploadId);
                    del.executeUpdate();
                } catch (SQLException cleanupEx) {
                    LOG.log(Level.WARNING, "failed to roll back orphaned upload row " + createdUploadId, cleanupEx);
                }
                if (archivePath != null) {
                    try {
                        Files.deleteIfExists(archivePath);
                    } catch (IOException ignored) {
                    }
                }
            }
            if (tempFile != null) {
                try {
                    Files.deleteIfExists(tempFile);
                } catch (IOException ignored) {
                }
            }
        }
    }

    /**
     * Handles {@code GET /api/external/uploads}: returns a paginated list of uploads for
     * the user's selected fleet.
     *
     * Supports {@code page} (zero-based, default 0) and {@code pageSize} (default 25,
     * max 100) query parameters. Returns 403 if the user lacks view access, 500 on
     * database errors.
     *
     * @param ctx the Javalin request context
     */
    private static void listUploads(Context ctx) {
        final User user = Objects.requireNonNull(ctx.attribute("user"));
        final int fleetId = user.getFleetId();

        if (!user.hasViewAccess(fleetId)) {
            ctx.status(403).json(new ApiTokenAuth.ApiError("Insufficient access level for fleet " + fleetId));
            return;
        }

        int page = parseIntOrDefault(ctx.queryParam("page"), 0);
        int pageSize = Math.min(100, parseIntOrDefault(ctx.queryParam("pageSize"), 25));
        String limit = " LIMIT " + (page * pageSize) + "," + pageSize;

        try (Connection connection = Database.getConnection()) {
            List<Upload> uploads = Upload.getUploads(connection, fleetId, limit);
            int total = Upload.getNumUploads(connection, fleetId, null);

            List<UploadSummary> summaries = new ArrayList<>(uploads.size());
            for (Upload u : uploads) {
                summaries.add(new UploadSummary(
                        u.getId(), u.getFilename(), u.getStatus().name(), u.getStartTime(), u.getEndTime()));
            }

            ctx.json(new PagedResponse<>(summaries, page, pageSize, total));
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "DB error listing uploads for fleet " + fleetId, e);
            ctx.status(500).json(new ApiTokenAuth.ApiError("Internal error"));
        }
    }

    /**
     * Handles {@code GET /api/external/uploads/{uploadId}}: returns details for a single
     * upload owned by the user's selected fleet.
     *
     * Returns 404 (not 403) when the upload exists but belongs to a different fleet, to
     * avoid leaking its existence.
     *
     * @param ctx the Javalin request context
     */
    private static void getUpload(Context ctx) {
        final User user = Objects.requireNonNull(ctx.attribute("user"));
        final int fleetId = user.getFleetId();

        if (!user.hasViewAccess(fleetId)) {
            ctx.status(403).json(new ApiTokenAuth.ApiError("Insufficient access level"));
            return;
        }

        int uploadId;
        try {
            uploadId = Integer.parseInt(ctx.pathParam("uploadId"));
        } catch (NumberFormatException e) {
            ctx.status(400).json(new ApiTokenAuth.ApiError("uploadId must be an integer"));
            return;
        }

        try (Connection connection = Database.getConnection()) {
            Upload upload = Upload.getUploadById(connection, uploadId);
            // Return 404 (not 403) for wrong-fleet uploads — don't leak their existence.
            if (upload == null || upload.getFleetId() != fleetId) {
                ctx.status(404).json(new ApiTokenAuth.ApiError("Upload not found"));
                return;
            }
            ctx.json(new UploadDetail(
                    upload.getId(),
                    upload.getFilename(),
                    upload.getStatus().name(),
                    upload.getMd5Hash(),
                    upload.getStartTime(),
                    upload.getEndTime(),
                    upload.getFleetId()));
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "DB error fetching upload " + uploadId, e);
            ctx.status(500).json(new ApiTokenAuth.ApiError("Internal error"));
        }
    }

    /**
     * Parses {@code s} as a non-negative integer, returning {@code dflt} if {@code s} is
     * null or not parseable. Negative parsed values are clamped to 0.
     *
     * @param s the string to parse (may be null)
     * @param dflt the default value to return when {@code s} is null or not parseable
     * @return the parsed non-negative integer, or {@code dflt} on failure
     */
    private static int parseIntOrDefault(String s, int dflt) {
        if (s == null) return dflt;
        try {
            return Math.max(0, Integer.parseInt(s));
        } catch (NumberFormatException e) {
            return dflt;
        }
    }

    // DTOs — private fields serialized by Gson via reflection on field names

    /** Response body for a successful upload (201 Created) or a retryable existing upload (200 OK). */
    public static final class UploadResponse {
        private final int uploadId;
        private final String status;
        private final String filename;
        private final String md5Hash;
        private final long sizeBytes;
        private final int fleetId;

        public UploadResponse(
                int uploadId, String status, String filename, String md5Hash, long sizeBytes, int fleetId) {
            this.uploadId = uploadId;
            this.status = status;
            this.filename = filename;
            this.md5Hash = md5Hash;
            this.sizeBytes = sizeBytes;
            this.fleetId = fleetId;
        }

        public int getUploadId() {
            return uploadId;
        }

        public String getStatus() {
            return status;
        }

        public String getFilename() {
            return filename;
        }

        public String getMd5Hash() {
            return md5Hash;
        }

        public long getSizeBytes() {
            return sizeBytes;
        }

        public int getFleetId() {
            return fleetId;
        }
    }

    /** Response body for 409 Conflict when the user already uploaded a finalized file with the same MD5. */
    public static final class DuplicateUploadResponse {
        private final int existingUploadId;
        private final String existingStatus;
        private final String existingFilename;
        private final String error;

        public DuplicateUploadResponse(int id, String status, String filename, String error) {
            this.existingUploadId = id;
            this.existingStatus = status;
            this.existingFilename = filename;
            this.error = error;
        }

        public int getExistingUploadId() {
            return existingUploadId;
        }

        public String getExistingStatus() {
            return existingStatus;
        }

        public String getExistingFilename() {
            return existingFilename;
        }

        public String getError() {
            return error;
        }
    }

    /** Summary view of an upload, returned in list responses. */
    public static final class UploadSummary {
        private final int id;
        private final String filename;
        private final String status;
        private final String startTime;
        private final String endTime;

        public UploadSummary(int id, String filename, String status, String startTime, String endTime) {
            this.id = id;
            this.filename = filename;
            this.status = status;
            this.startTime = startTime;
            this.endTime = endTime;
        }

        public int getId() {
            return id;
        }

        public String getFilename() {
            return filename;
        }

        public String getStatus() {
            return status;
        }

        public String getStartTime() {
            return startTime;
        }

        public String getEndTime() {
            return endTime;
        }
    }

    /** Detailed view of a single upload, returned by the single-upload endpoint. */
    public static final class UploadDetail {
        private final int id;
        private final String filename;
        private final String status;
        private final String md5Hash;
        private final String startTime;
        private final String endTime;
        private final int fleetId;

        public UploadDetail(
                int id, String filename, String status, String md5Hash, String startTime, String endTime, int fleetId) {
            this.id = id;
            this.filename = filename;
            this.status = status;
            this.md5Hash = md5Hash;
            this.startTime = startTime;
            this.endTime = endTime;
            this.fleetId = fleetId;
        }

        public int getId() {
            return id;
        }

        public String getFilename() {
            return filename;
        }

        public String getStatus() {
            return status;
        }

        public String getMd5Hash() {
            return md5Hash;
        }

        public String getStartTime() {
            return startTime;
        }

        public String getEndTime() {
            return endTime;
        }

        public int getFleetId() {
            return fleetId;
        }
    }

    /** Generic page wrapper for list responses. */
    public static final class PagedResponse<T> {
        private final List<T> items;
        private final int page;
        private final int pageSize;
        private final int total;

        public PagedResponse(List<T> items, int page, int pageSize, int total) {
            this.items = items;
            this.page = page;
            this.pageSize = pageSize;
            this.total = total;
        }

        public List<T> getItems() {
            return items;
        }

        public int getPage() {
            return page;
        }

        public int getPageSize() {
            return pageSize;
        }

        public int getTotal() {
            return total;
        }
    }
}
