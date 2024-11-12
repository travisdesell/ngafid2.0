package org.ngafid.routes.javalin;

import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.UploadedFile;
import jakarta.servlet.MultipartConfigElement;
import org.ngafid.Database;
import org.ngafid.WebServer;
import org.ngafid.accounts.User;
import org.ngafid.flights.*;
import org.ngafid.routes.ErrorResponse;
import org.ngafid.routes.MustacheHandler;
import org.ngafid.routes.Navbar;

import javax.xml.bind.DatatypeConverter;
import java.io.*;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.apache.commons.io.FileUtils.deleteDirectory;
import static org.ngafid.WebServer.gson;

public class ImportUploadRoutes {
    private static final Logger LOG = Logger.getLogger(ImportUploadRoutes.class.getName());

    private static class UploadsResponse {
        public List<Upload> uploads;
        public int numberPages;

        public UploadsResponse(List<Upload> uploads, int numberPages) {
            this.uploads = uploads;
            this.numberPages = numberPages;
        }
    }

    private static class UploadDetails {
        List<UploadError> uploadErrors;
        List<FlightError> flightErrors;
        List<FlightWarning> flightWarnings;

        public UploadDetails(int uploadId) throws SQLException {
            try (Connection connection = Database.getConnection()) {
                uploadErrors = UploadError.getUploadErrors(connection, uploadId);
                flightErrors = FlightError.getFlightErrors(connection, uploadId);
                flightWarnings = FlightWarning.getFlightWarnings(connection, uploadId);
            }
        }
    }

    private static class ImportsResponse {
        public List<Upload> imports;
        public int numberPages;

        public ImportsResponse(List<Upload> imports, int numberPages) {
            this.imports = imports;
            this.numberPages = numberPages;
        }
    }

    private static void getUpload(Context ctx) {
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

        File file = new File(String.format("%s/%d/%d/%d__%s", WebServer.NGAFID_ARCHIVE_DIR, upload.getFleetId(), upload.getUploaderId(), upload.getId(), upload.getFilename()));
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

    private static void postUpload(Context ctx) {
        User user = Objects.requireNonNull(ctx.sessionAttribute("user"));
        int uploaderId = user.getId();

        // Extract parameters from query string
        String identifier = ctx.queryParam("identifier");
        if (identifier == null) {
            LOG.severe("ERROR! Missing upload identifier");
            ctx.result(gson.toJson(new ErrorResponse("File Chunk Upload Failure", "File identifier was missing.")));
            return;
        }

        String md5Hash = ctx.queryParam("md5Hash");
        if (md5Hash == null) {
            LOG.severe("ERROR! Missing upload md5Hash");
            ctx.result(gson.toJson(new ErrorResponse("File Chunk Upload Failure", "File md5Hash was missing.")));
            return;
        }

        String sChunkNumber = ctx.queryParam("chunkNumber");
        if (sChunkNumber == null) {
            LOG.severe("ERROR! Missing upload chunk number");
            ctx.result(gson.toJson(new ErrorResponse("File Chunk Upload Failure", "File chunk was missing.")));
            return;
        }
        int chunkNumber = Integer.parseInt(sChunkNumber);

        try (Connection connection = Database.getConnection()) {
            Upload upload = Upload.getUploadByUser(connection, uploaderId, md5Hash);
            if (upload == null) {
                LOG.severe("ERROR! Upload was not in the database!");
                ctx.result(gson.toJson(new ErrorResponse("File Upload Failure",
                        "A system error occurred where this upload was not in the database. Please try again.")));
                return;
            }

            int fleetId = upload.getFleetId();

            UploadedFile uploadPart = ctx.uploadedFile("chunk");
            if (uploadPart == null) {
                ctx.status(400).result("No file part uploaded");
                return;
            }

            InputStream chunkInputStream = uploadPart.content();
            long chunkSize = chunkInputStream.available();

            String chunkDirectory = WebServer.NGAFID_UPLOAD_DIR + "/" + fleetId + "/" + uploaderId + "/" + identifier;
            new File(chunkDirectory).mkdirs();  // Create directories if they don't exist

            String chunkFilename = chunkDirectory + "/" + chunkNumber + ".part";
            LOG.info("Copying uploaded chunk to: '" + chunkFilename + "'");
            Files.copy(chunkInputStream, Paths.get(chunkFilename), StandardCopyOption.REPLACE_EXISTING);

            chunkSize = new File(chunkFilename).length();
            LOG.info("Chunk file size: " + chunkSize);

            upload.chunkUploaded(connection, chunkNumber, chunkSize);

            if (upload.completed()) {
                String targetDirectory = upload.getArchiveDirectory();
                new File(targetDirectory).mkdirs();
                String targetFilename = targetDirectory + "/" + upload.getArchiveFilename();

                LOG.info("Attempting to write final file to '" + targetFilename + "'");
                try (FileOutputStream out = new FileOutputStream(targetFilename)) {
                    for (int i = 0; i < upload.getNumberChunks(); i++) {
                        byte[] bytes = Files.readAllBytes(Paths.get(chunkDirectory + "/" + i + ".part"));
                        out.write(bytes);
                    }

                    if (!upload.checkSize()) {
                        LOG.severe("ERROR! Final file had incorrect number of bytes.");
                        ctx.result(gson.toJson(new ErrorResponse("File Upload Failure",
                                "An error occurred while merging the chunks. The final file size was incorrect. Please try again.")));
                        return;
                    }

                    String newMd5Hash = null;
                    try (InputStream is = new FileInputStream(Paths.get(targetFilename).toFile())) {
                        MessageDigest md = MessageDigest.getInstance("MD5");
                        byte[] hash = md.digest(is.readAllBytes());
                        newMd5Hash = DatatypeConverter.printHexBinary(hash).toLowerCase();
                    } catch (NoSuchAlgorithmException | IOException e) {
                        LOG.severe("Error calculating MD5 hash: " + e.getMessage());
                        ctx.status(500);
                        ctx.result(gson.toJson(new ErrorResponse("File Upload Failure", "Error calculating MD5 hash.")));
                        return;
                    }

                    if (!newMd5Hash.equals(upload.getMd5Hash())) {
                        LOG.severe("ERROR! MD5 hashes do not match.");
                        ctx.result(gson.toJson(new ErrorResponse("File Upload Failure",
                                "MD5 hash mismatch. File corruption might have occurred during upload.")));
                        return;
                    }

                    upload.complete(connection);
                    deleteDirectory(new File(chunkDirectory));
                } catch (IOException e) {
                    LOG.severe("Error writing final file: " + e.getMessage());
                    ctx.result(gson.toJson(new ErrorResponse("File Upload Failure", "Error writing final file.")));
                    return;
                }
            }

            ctx.result(gson.toJson(upload));
        } catch (Exception e) {
            LOG.severe("Error during file upload: " + e.getMessage());
            ctx.result(gson.toJson(new ErrorResponse(e)));
        }
    }

    private static void getUploads(Context ctx) {
        final String templateFile = "uploads.html";

        try (Connection connection = Database.getConnection()) {
            Map<String, Object> scopes = new HashMap<>();

            scopes.put("navbar_js", Navbar.getJavascript(ctx));

            final User user = Objects.requireNonNull(ctx.sessionAttribute("user"));
            final int fleetId = user.getFleetId();

            // default page values
            final int pageSize = 10;
            int currentPage = 0;

            final int totalUploads = Upload.getNumUploads(connection, fleetId, null);
            final int numberPages = totalUploads / pageSize;

            List<Upload> pending_uploads = Upload.getUploads(connection, fleetId, new String[]{"UPLOADING"});

            // update the status of all the uploads currently uploading to incomplete so the
            // webpage knows they
            // need to be restarted and aren't currently being uploaded.
            for (Upload upload : pending_uploads) {
                if (upload.getStatus().equals("UPLOADING")) {
                    upload.setStatus("UPLOAD INCOMPLETE");
                }
            }

            List<Upload> other_uploads = Upload.getUploads(connection, fleetId, new String[]{"UPLOADED", "IMPORTED", "ERROR"}, " LIMIT " + (currentPage * pageSize) + "," + pageSize);

            scopes.put("numPages_js", "var numberPages = " + numberPages + ";");
            scopes.put("index_js", "var currentPage = 0;");

            scopes.put("uploads_js", "var uploads = JSON.parse('" + gson.toJson(other_uploads) + "'); var pending_uploads = JSON.parse('" + gson.toJson(pending_uploads) + "');");

            ctx.contentType("text/html");
            ctx.result(MustacheHandler.handle(templateFile, scopes));
        } catch (SQLException e) {
            ctx.json(new ErrorResponse(e));
        } catch (Exception e) {
            LOG.severe(e.toString());
        }
    }

    private static void postUploads(Context ctx) {
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

            final List<Upload> uploads = Upload.getUploads(connection, fleetId, " LIMIT " + (currentPage * pageSize) + "," + pageSize);

            ctx.json(new UploadsResponse(uploads, numberPages));
        } catch (Exception e) {
            ctx.json(new ErrorResponse(e));
        }
    }

    private static void postUploadDetails(Context ctx) {
        final int uploadId = Integer.parseInt(Objects.requireNonNull(ctx.queryParam("uploadId")));
        try {
            ctx.json(new UploadDetails(uploadId));
        } catch (SQLException e) {
            ctx.json(new ErrorResponse(e));
        }
    }

    private static void postRemoveUpload(Context ctx) {
        try (Connection connection = Database.getConnection()) {
            final User user = Objects.requireNonNull(ctx.sessionAttribute("user"));
            final int uploadId = Integer.parseInt(ctx.queryParam("uploadId"));
            String md5Hash = ctx.queryParam("md5Hash");
            Upload upload = Objects.requireNonNull(Upload.getUploadById(connection, uploadId, md5Hash));

            // check to see if the user has upload access for this fleet.
            if (!user.hasUploadAccess(upload.getFleetId())) {
                LOG.severe("INVALID ACCESS: user did not have upload or manager access this fleet.");
                ctx.status(401);
                ctx.result("User did not have access to delete this upload.");
                return;
            }

            if (upload.getFleetId() != user.getFleetId()) {
                LOG.severe("INVALID ACCESS: user did not have access to this fleet.");
                ctx.status(401);
                ctx.result("User did not have access to delete this upload.");
                return;
            }

            final List<Flight> flights = Flight.getFlightsFromUpload(connection, uploadId);

            // get all flights, delete:
            // flight warning
            // flight error
            for (Flight flight : flights) {
                flight.remove(connection);
            }

            upload.remove(connection);
            Tails.removeUnused(connection);
        } catch (Exception e) {
            LOG.info(e.getMessage());
            ctx.json(new ErrorResponse(e));
        }

    }

    private static void getImports(Context ctx) {
        final String templateFile = WebServer.MUSTACHE_TEMPLATE_DIR + "imports.html";

        try (Connection connection = Database.getConnection()) {
            Map<String, Object> scopes = new HashMap<String, Object>();

            scopes.put("navbar_js", Navbar.getJavascript(ctx));

            final User user = Objects.requireNonNull(ctx.attribute("user"));
            final int fleetId = user.getFleetId();

            // default page values
            final int pageSize = 10;
            int currentPage = 0;

            final int totalImports = Upload.getNumUploads(connection, fleetId, null);
            final int numberPages = totalImports / pageSize;
            List<Upload> imports = Upload.getUploads(connection, fleetId, new String[]{"IMPORTED", "ERROR"}, " LIMIT " + (currentPage * pageSize) + "," + pageSize);

            scopes.put("numPages_js", "var numberPages = " + numberPages + ";");
            scopes.put("index_js", "var currentPage = 0;");

            scopes.put("imports_js", "var imports = JSON.parse('" + gson.toJson(imports) + "');");

            StringWriter stringOut = new StringWriter();
            ctx.contentType("text/html");
            ctx.result(MustacheHandler.handle(templateFile, scopes));
        } catch (SQLException e) {
            ctx.json(new ErrorResponse(e));
        } catch (Exception e) {
            LOG.severe(e.toString());
        }
    }

    private static void postImports(Context ctx) {
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
            List<Upload> imports = Upload.getUploads(connection, fleetId, new String[]{"IMPORTED", "ERROR"}, " LIMIT " + (currentPage * pageSize) + "," + pageSize);

            ctx.json(new ImportsResponse(imports, numberPages));
        } catch (SQLException e) {
            ctx.json(new ErrorResponse(e));
        }
    }

    private static void postNewUpload(Context ctx) {
        final User user = Objects.requireNonNull(ctx.sessionAttribute("user"));
        final int uploaderId = user.getId();
        final int fleetId = user.getFleetId();
        final String filename = Objects.requireNonNull(ctx.queryParam("filename")).replaceAll(" ", "_");
        final String identifier = Objects.requireNonNull(ctx.queryParam("identifier"));
        final int numberChunks = Integer.parseInt(Objects.requireNonNull(ctx.queryParam("numberChunks")));
        final long sizeBytes = Long.parseLong(Objects.requireNonNull(ctx.queryParam("sizeBytes")));
        final String md5Hash = Objects.requireNonNull(ctx.queryParam("md5Hash"));

        ctx.attribute("org.eclipse.jetty.multipartConfig", new MultipartConfigElement("/mnt/ngafid/temp"));

        if (!filename.matches("^[a-zA-Z0-9_.-]*$")) {
            LOG.info("ERROR! malformed filename");

            ErrorResponse errorResponse = new ErrorResponse("File Upload Failure",
                    "The filename was malformed. Filenames must only contain letters, numbers, dashes ('-'), underscores ('_') and periods.");
            ctx.json(errorResponse);
            return;
        }

        // options:
        // 1. file does not exist, insert into database -- start upload
        // 2. file does exist and has not finished uploading -- restart upload
        // 3. file does exist and has finished uploading -- report finished
        // 4. file does exist but with different hash -- error message

        try (Connection connection = Database.getConnection()) {
            try (PreparedStatement query = connection.prepareStatement(
                    "SELECT md5_hash, number_chunks, uploaded_chunks, chunk_status, status, filename FROM uploads WHERE md5_hash = ? AND uploader_id = ?")) {
                query.setString(1, md5Hash);
                query.setInt(2, uploaderId);

                try (ResultSet resultSet = query.executeQuery()) {
                    if (!resultSet.next()) {
                        Upload upload = Upload.createNewUpload(connection, uploaderId, fleetId, filename, identifier,
                                Upload.Kind.FILE, sizeBytes, numberChunks, md5Hash);

                        ctx.json(upload);
                    } else {
                        // a file with this md5 hash exists
                        String dbStatus = resultSet.getString(5);
                        String dbFilename = resultSet.getString(6);

                        if (dbStatus.equals("UPLOADED") || dbStatus.equals("IMPORTED")) {
                            // 3. file does exist and has finished uploading -- report finished
                            // do the same thing, client will handle completion
                            LOG.severe("ERROR! Final file has already been uploaded.");

                            ctx.json(new ErrorResponse("File Already Exists",
                                    "This file has already been uploaded to the server as '" + dbFilename
                                            + "' and does not need to be uploaded again."));

                        } else {
                            // 2. file does exist and has not finished uploading -- restart upload
                            ctx.json(Objects.requireNonNull(Upload.getUploadByUser(connection, uploaderId, md5Hash)));
                        }
                    }
                }
            }
        } catch (SQLException e) {
            LOG.severe(gson.toJson(e));
            ctx.json(new ErrorResponse(e));
        }
    }

    public static void bindRoutes(Javalin app) {
        app.get("/protected/download_upload", ImportUploadRoutes::getUpload);
        app.post("/uploads/new", ImportUploadRoutes::postNewUpload);
        app.post("/protected/upload", ImportUploadRoutes::postUpload); // Might be weird. Spark has a "multipart/form-data" in args
        app.post("/protected/remove_upload", ImportUploadRoutes::postRemoveUpload);

        app.get("/protected/uploads", ImportUploadRoutes::getUploads);
        app.post("/protected/uploads", ImportUploadRoutes::postUploads);

        app.get("/protected/imports", ImportUploadRoutes::getImports);
        app.post("/protected/get_imports", ImportUploadRoutes::postImports);

        app.post("/protected/upload_details", ImportUploadRoutes::postUploadDetails);
    }
}
