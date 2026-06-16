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
 * All endpoints are scoped to the authenticated user's currently-selected fleet —
 * callers cannot specify a fleet ID, eliminating any cross-fleet attack surface.
 */
public final class ApiExternalUploadRoutes {
    private static final Logger LOG = Logger.getLogger(ApiExternalUploadRoutes.class.getName());
    private static final String FILE_PART = "file";

    private ApiExternalUploadRoutes() {}

    public static void bindRoutes(Javalin app) {
        app.before("/api/external/*", ApiTokenAuth::requireApiToken);

        app.post("/api/external/uploads", ApiExternalUploadRoutes::postUpload);
        app.get("/api/external/uploads", ApiExternalUploadRoutes::listUploads);
        app.get("/api/external/uploads/{uploadId}", ApiExternalUploadRoutes::getUpload);
    }

    private static void postUpload(Context ctx) {
        final User user = Objects.requireNonNull(ctx.attribute("user"));
        final int uploaderId = user.getId();

        // Get fleet by fleet_name ( unique in the table)
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

        // Check if user has upload access
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

    private static int parseIntOrDefault(String s, int dflt) {
        if (s == null) return dflt;
        try {
            return Math.max(0, Integer.parseInt(s));
        } catch (NumberFormatException e) {
            return dflt;
        }
    }

    // DTOs — public fields serialized by Gson

    public static final class UploadResponse {
        public final int uploadId;
        public final String status;
        public final String filename;
        public final String md5Hash;
        public final long sizeBytes;
        public final int fleetId;

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

    public static final class DuplicateUploadResponse {
        public final int existingUploadId;
        public final String existingStatus;
        public final String existingFilename;
        public final String error;

        public DuplicateUploadResponse(int id, String status, String filename, String error) {
            this.existingUploadId = id;
            this.existingStatus = status;
            this.existingFilename = filename;
            this.error = error;
        }
    }

    public static final class UploadSummary {
        public final int id;
        public final String filename;
        public final String status;
        public final String startTime;
        public final String endTime;

        public UploadSummary(int id, String filename, String status, String startTime, String endTime) {
            this.id = id;
            this.filename = filename;
            this.status = status;
            this.startTime = startTime;
            this.endTime = endTime;
        }
    }

    public static final class UploadDetail {
        public final int id;
        public final String filename;
        public final String status;
        public final String md5Hash;
        public final String startTime;
        public final String endTime;
        public final int fleetId;

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

    public static final class PagedResponse<T> {
        public final List<T> items;
        public final int page;
        public final int pageSize;
        public final int total;

        public PagedResponse(List<T> items, int page, int pageSize, int total) {
            this.items = items;
            this.page = page;
            this.pageSize = pageSize;
            this.total = total;
        }
    }
}
