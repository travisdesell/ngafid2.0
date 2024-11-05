package org.ngafid.routes.javalin;

import io.javalin.http.Context;
import org.ngafid.Database;
import org.ngafid.WebServer;
import org.ngafid.accounts.User;
import org.ngafid.flights.Upload;
import org.ngafid.routes.ErrorResponse;

import java.io.*;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ImportUploadRoutes {
    private static Logger LOG = Logger.getLogger(ImportUploadRoutes.class.getName());

    public static class UploadsResponse {
        public List<Upload> uploads;
        public int numberPages;

        public UploadsResponse(List<Upload> uploads, int numberPages) {
            this.uploads = uploads;
            this.numberPages = numberPages;
        }
    }

    public static class ImportsResponse {
        public List<Upload> imports;
        public int numberPages;

        public ImportsResponse(List<Upload> imports, int numberPages) {
            this.imports = imports;
            this.numberPages = numberPages;
        }
    }

    public static void getUpload(Context ctx) {
        LOG.info("Retrieving upload: " + ctx.queryParams("uploadId") + " " + ctx.queryParams("md5Hash"));

        final User user = Objects.requireNonNull(ctx.sessionAttribute("user"));
        Upload upload;

        try (Connection connection = Database.getConnection()) {
            upload = Upload.getUploadById(connection, Integer.parseInt(Objects.requireNonNull(ctx.queryParam("uploadId"))), ctx.queryParam("md5Hash"));
        } catch (SQLException e) {
            LOG.severe(e.toString());
            ctx.status(500);
            ctx.result("Failed to retrieve upload.");
            return;
        }

        if (upload == null) {
            ctx.status(404);
            ctx.result("Upload not found");
            return;
        }

        if (!user.hasUploadAccess(upload.getFleetId())) {
            LOG.severe("INVALID ACCESS: user did not have upload or manager access this fleet.");
            ctx.status(401);
            ctx.result("User did not have access to delete this upload.");
            return;
        }

        File file = new File(String.format("%s/%d/%d/%d__%s", WebServer.NGAFID_ARCHIVE_DIR, upload.getFleetId(),
                upload.getUploaderId(), upload.getId(), upload.getFilename()));
        LOG.info("File: " + file.getAbsolutePath());
        if (file.exists()) {
            ctx.contentType("application/zip");
            ctx.header("Content-Disposition", "attachment; filename=" + upload.getFilename());

            try (InputStream buffInputStream = new BufferedInputStream(new FileInputStream(file)); OutputStream outputStream = ctx.outputStream()) {
                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = buffInputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }

                LOG.log(Level.INFO, "%s file sent", file.getName());
            } catch (IOException e) {
                LOG.severe(e.toString());
            }
        } else {
            LOG.severe(String.format("File not found: %s", file.getName()));
            ctx.status(404);
            ctx.result("File was not found on server");
        }
    }

    public static void postUpload(Context ctx) {
    }

    public static void getUploads(Context ctx) {
    }

    public static void postUploads(Context ctx) {
        final User user = Objects.requireNonNull(ctx.sessionAttribute("user"));
        final int fleetId = user.getFleetId();

        // check to see if the user has view access for this fleet.
        if (!user.hasViewAccess(fleetId)) {
            LOG.severe("INVALID ACCESS: user did not have access to view flights for this fleet.");
            ctx.status(401);
            ctx.result("User did not have access to view flights for this fleet.");
        }

        try (Connection connection = Database.getConnection()) {
            final int currentPage = Integer.parseInt(Objects.requireNonNull(ctx.queryParam("currentPage")));
            final int pageSize = Integer.parseInt(Objects.requireNonNull(ctx.queryParam("pageSize")));
            final int totalUploads = Upload.getNumUploads(connection, fleetId, null);
            final int numberPages = totalUploads / pageSize;

            final List<Upload> uploads = Upload.getUploads(connection, fleetId,
                    " LIMIT " + (currentPage * pageSize) + "," + pageSize);

            ctx.json(new UploadsResponse(uploads, numberPages));
        } catch (Exception e) {
            ctx.json(new ErrorResponse(e));
        }
    }

    public static void postUploadDetails(Context ctx) {
    }

    public static void postRemoveUpload(Context ctx) {
    }

    public static void getImports(Context ctx) {
    }

    public static void postImports(Context ctx) {
        final User user = Objects.requireNonNull(ctx.sessionAttribute("user"));
        final int fleetId = user.getFleetId();

        // check to see if the user has upload access for this fleet.
        if (!user.hasUploadAccess(fleetId)) {
            LOG.severe("INVALID ACCESS: user did not have access view imports for this fleet.");
            ctx.status(401);
            ctx.result("User did not have access to view imports for this fleet.");
            return;
        }

        try (Connection connection = Database.getConnection()) {
            int currentPage = Integer.parseInt(Objects.requireNonNull(ctx.queryParam("currentPage")));
            int pageSize = Integer.parseInt(Objects.requireNonNull(ctx.queryParam("pageSize")));

            int totalImports = Upload.getNumUploads(connection, fleetId, null);
            int numberPages = totalImports / pageSize;
            List<Upload> imports = Upload.getUploads(connection, fleetId, new String[] { "IMPORTED", "ERROR" },
                    " LIMIT " + (currentPage * pageSize) + "," + pageSize);

            ctx.json(new ImportsResponse(imports, numberPages));
        } catch (SQLException e) {
            ctx.json(new ErrorResponse(e));
        }
    }
}
