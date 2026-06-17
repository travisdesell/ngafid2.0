package org.ngafid.www.routes;

import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.UploadedFile;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
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
import org.ngafid.www.ErrorResponse;

/**
 * External API endpoints for uploading flight data via bearer-token auth.
 *
 * <p>Callers authenticate by passing a bearer token in the {@code Authorization} header
 * (validated by {@link ApiTokenAuth}). The token is associated with a single user; the
 * routes below operate on behalf of that user.
 *
 * <p>An upload may target a specific fleet by supplying the {@code fleetName} form
 * parameter. If {@code fleetName} is omitted, the request falls back to the user's
 * currently-selected fleet. In either case, the user must hold {@code MANAGER} or
 * {@code UPLOAD} access on the resolved fleet, otherwise the request is rejected with
 * {@code 403}.
 */
public final class ApiExternalUploadRoutes {
    private static final Logger LOG = Logger.getLogger(ApiExternalUploadRoutes.class.getName());
    private static final String FILE_PART = "file";

    private ApiExternalUploadRoutes() {}

    /**
     * Registers the external upload routes on the supplied Javalin app.
     *
     * <p>A {@code before} filter is installed on every {@code /api/external/*} path so that
     * the bearer token is validated before any handler runs.
     *
     * @param app the Javalin application to register the routes on
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
     * <p>Expected multipart form fields:
     * <ul>
     *   <li>{@code file} — the file to upload (required, non-empty)</li>
     *   <li>{@code fleetName} — name of the target fleet (optional; defaults to the
     *       user's currently-selected fleet)</li>
     * </ul>
     *
     * <p>The handler streams the request body to a temp file, computes its MD5 to detect
     * duplicates, creates an {@code uploads} row, moves the temp file into the archive
     * location, and publishes to the upload Kafka topic via
     * {@link Upload.LockedUpload#complete()}.
     *
     * <p>Response codes:
     * <ul>
     *   <li>{@code 201} — upload accepted; body is an {@link UploadResponse}</li>
     *   <li>{@code 400} — missing or empty {@code file} part</li>
     *   <li>{@code 403} — user lacks upload access on the resolved fleet</li>
     *   <li>{@code 404} — supplied {@code fleetName} does not exist</li>
     *   <li>{@code 409} — the user already uploaded a file with the same MD5; body is a
     *       {@link DuplicateUploadResponse}</li>
     *   <li>{@code 500} — internal database or I/O error</li>
     * </ul>
     *
     * @param ctx the Javalin request context, populated by {@link ApiTokenAuth} with the
     *            authenticated {@link User} attribute
     */
    private static void postUpload(Context ctx) {
        final User user = Objects.requireNonNull(ctx.attribute("user"));
        final int uploaderId = user.getId();

        // Resolve the target fleet: prefer the supplied fleetName, fall back to the
        // user's currently-selected fleet. fleet_name has a UNIQUE constraint so the
        // lookup is deterministic.
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

        // Reject the request if the user does not have upload access on the resolved fleet.
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

        Path tempFile = null;
        try {
            tempFile = Files.createTempFile("ngafid-api-upload-", ".tmp");
            String md5Hash = streamToTempAndHash(file, tempFile);

            try (Connection connection = Database.getConnection()) {
                Upload existing = Upload.getUploadByUser(connection, uploaderId, md5Hash);
                if (existing != null) {
                    ctx.status(409)
                            .json(new DuplicateUploadResponse(
                                    existing.getId(),
                                    existing.getStatus().name(),
                                    existing.getFilename(),
                                    "You have already uploaded a file with identical contents."));
                    return;
                }

                Upload upload = Upload.createNewUpload(
                        connection,
                        uploaderId,
                        fleetId,
                        file.filename(),
                        "api-" + UUID.randomUUID(),
                        Upload.Kind.FILE,
                        file.size(),
                        1,
                        md5Hash);

                Path archivePath = upload.getArchivePath();
                Files.createDirectories(archivePath.getParent());
                Files.move(tempFile, archivePath, StandardCopyOption.REPLACE_EXISTING);
                tempFile = null;

                // On lock close, Upload auto-publishes to Topic.UPLOAD and kicks off processing.
                try (Upload.LockedUpload locked = upload.getLockedUpload(connection)) {
                    locked.chunkUploaded(0, file.size());
                    locked.complete();
                }

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
            ctx.status(500).json(new ErrorResponse(e));
        } catch (IOException e) {
            LOG.log(Level.SEVERE, "IO error during external upload", e);
            ctx.status(500).json(new ApiTokenAuth.ApiError("Failed to store uploaded file"));
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Unexpected error during external upload", e);
            ctx.status(500).json(new ApiTokenAuth.ApiError("Internal server error"));
        } finally {
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
     * the authenticated user's currently-selected fleet.
     *
     * <p>Query parameters:
     * <ul>
     *   <li>{@code page} — zero-based page index (default {@code 0})</li>
     *   <li>{@code pageSize} — number of items per page (default {@code 25}, max {@code 100})</li>
     * </ul>
     *
     * <p>Response codes:
     * <ul>
     *   <li>{@code 200} — body is a {@link PagedResponse} of {@link UploadSummary}</li>
     *   <li>{@code 403} — user lacks view access on the fleet</li>
     *   <li>{@code 500} — internal database error</li>
     * </ul>
     *
     * @param ctx the Javalin request context, populated by {@link ApiTokenAuth} with the
     *            authenticated {@link User} attribute
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
            ctx.status(500).json(new ErrorResponse(e));
        }
    }

    /**
     * Handles {@code GET /api/external/uploads/{uploadId}}: returns details for a single
     * upload owned by the authenticated user's fleet.
     *
     * <p>To avoid leaking the existence of uploads belonging to other fleets, this method
     * returns {@code 404} (not {@code 403}) when the requested upload exists but belongs
     * to a different fleet.
     *
     * <p>Response codes:
     * <ul>
     *   <li>{@code 200} — body is an {@link UploadDetail}</li>
     *   <li>{@code 400} — {@code uploadId} path param is not an integer</li>
     *   <li>{@code 403} — user lacks view access on their selected fleet</li>
     *   <li>{@code 404} — upload does not exist, or belongs to another fleet</li>
     *   <li>{@code 500} — internal database error</li>
     * </ul>
     *
     * @param ctx the Javalin request context, populated by {@link ApiTokenAuth} with the
     *            authenticated {@link User} attribute and the {@code uploadId} path param
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
            ctx.status(500).json(new ErrorResponse(e));
        }
    }

    /**
     * Streams the contents of an uploaded file to a destination path on disk while
     * simultaneously computing an MD5 digest over the bytes.
     *
     * <p>Reading the request body and writing to disk is done in a single pass so the
     * full file is never buffered in memory; the same byte buffer feeds both the
     * {@link MessageDigest} update and the destination {@link OutputStream}.
     *
     * @param file the multipart file part from the request
     * @param dest the destination path to write the file's contents to
     * @return the lowercase hex-encoded MD5 hash of the file's contents
     * @throws IOException if reading from the request or writing to {@code dest} fails
     * @throws IllegalStateException if the MD5 algorithm is unavailable in the JVM
     */
    private static String streamToTempAndHash(UploadedFile file, Path dest) throws IOException {
        MessageDigest md;
        try {
            md = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("MD5 unavailable", e);
        }

        try (InputStream in = file.content();
                OutputStream out = Files.newOutputStream(dest)) {
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) != -1) {
                md.update(buf, 0, n);
                out.write(buf, 0, n);
            }
        }

        byte[] digest = md.digest();
        StringBuilder hex = new StringBuilder(digest.length * 2);
        for (byte b : digest) {
            String h = Integer.toHexString(b & 0xFF);
            if (h.length() == 1) hex.append('0');
            hex.append(h);
        }
        return hex.toString();
    }

    /**
     * Parses a string as a non-negative integer, returning a default if the string is
     * {@code null} or not a valid integer.
     *
     * <p>Negative parsed values are clamped to {@code 0}.
     *
     * @param s    the string to parse; may be {@code null}
     * @param dflt the default value to return when {@code s} is {@code null} or unparseable
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

    // DTOs — public fields serialized by Gson

    /**
     * Response body for a successful upload ({@code 201 Created}).
     */
    public static final class UploadResponse {
        public final int uploadId;
        public final String status;
        public final String filename;
        public final String md5Hash;
        public final long sizeBytes;
        public final int fleetId;

        /**
         * Constructs an upload response.
         *
         * @param uploadId  the newly-created upload's primary key
         * @param status    the upload's status enum name (e.g. {@code UPLOADED})
         * @param filename  the original filename supplied by the client
         * @param md5Hash   the MD5 hash of the uploaded bytes (lowercase hex)
         * @param sizeBytes the size of the uploaded file in bytes
         * @param fleetId   the fleet the upload was attributed to
         */
        public UploadResponse(
                int uploadId, String status, String filename, String md5Hash, long sizeBytes, int fleetId) {
            this.uploadId = uploadId;
            this.status = status;
            this.filename = filename;
            this.md5Hash = md5Hash;
            this.sizeBytes = sizeBytes;
            this.fleetId = fleetId;
        }
    }

    /**
     * Response body returned with {@code 409 Conflict} when the user has already uploaded
     * a file with identical contents (matching MD5).
     */
    public static final class DuplicateUploadResponse {
        public final int existingUploadId;
        public final String existingStatus;
        public final String existingFilename;
        public final String error;

        /**
         * Constructs a duplicate-upload response.
         *
         * @param id       primary key of the existing upload with the matching hash
         * @param status   the existing upload's status enum name
         * @param filename the existing upload's filename
         * @param error    human-readable explanation
         */
        public DuplicateUploadResponse(int id, String status, String filename, String error) {
            this.existingUploadId = id;
            this.existingStatus = status;
            this.existingFilename = filename;
            this.error = error;
        }
    }

    /**
     * Summary view of an upload, returned in list responses.
     */
    public static final class UploadSummary {
        public final int id;
        public final String filename;
        public final String status;
        public final String startTime;
        public final String endTime;

        /**
         * Constructs an upload summary.
         *
         * @param id        the upload's primary key
         * @param filename  the original filename
         * @param status    the upload's status enum name
         * @param startTime ISO-8601 timestamp at which the upload was created
         * @param endTime   ISO-8601 timestamp at which processing finished, or {@code null}
         *                  if still in progress
         */
        public UploadSummary(int id, String filename, String status, String startTime, String endTime) {
            this.id = id;
            this.filename = filename;
            this.status = status;
            this.startTime = startTime;
            this.endTime = endTime;
        }
    }

    /**
     * Detailed view of a single upload, returned by the single-upload endpoint.
     */
    public static final class UploadDetail {
        public final int id;
        public final String filename;
        public final String status;
        public final String md5Hash;
        public final String startTime;
        public final String endTime;
        public final int fleetId;

        /**
         * Constructs an upload detail.
         *
         * @param id        the upload's primary key
         * @param filename  the original filename
         * @param status    the upload's status enum name
         * @param md5Hash   MD5 hash of the uploaded bytes (lowercase hex)
         * @param startTime ISO-8601 timestamp at which the upload was created
         * @param endTime   ISO-8601 timestamp at which processing finished, or {@code null}
         *                  if still in progress
         * @param fleetId   the fleet the upload was attributed to
         */
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
    }

    /**
     * Generic page wrapper for list responses.
     *
     * @param <T> the type of items contained in the page
     */
    public static final class PagedResponse<T> {
        public final List<T> items;
        public final int page;
        public final int pageSize;
        public final int total;

        /**
         * Constructs a paged response.
         *
         * @param items    the items on this page
         * @param page     the zero-based page index
         * @param pageSize the maximum number of items per page
         * @param total    the total number of items across all pages
         */
        public PagedResponse(List<T> items, int page, int pageSize, int total) {
            this.items = items;
            this.page = page;
            this.pageSize = pageSize;
            this.total = total;
        }
    }
}
