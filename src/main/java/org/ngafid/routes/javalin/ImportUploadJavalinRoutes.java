package org.ngafid.routes.javalin;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.UploadedFile;
import jakarta.servlet.MultipartConfigElement;
import org.ngafid.Database;
import org.ngafid.WebServer;
import org.ngafid.accounts.User;
import org.ngafid.flights.*;
import org.ngafid.routes.ErrorResponse;
import org.ngafid.routes.Navbar;

import javax.xml.bind.DatatypeConverter;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
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

public class ImportUploadJavalinRoutes {
    private static final Logger LOG = Logger.getLogger(ImportUploadJavalinRoutes.class.getName());

    private static class UploadsResponse {
        @JsonProperty
        public List<Upload> uploads;
        @JsonProperty
        public int numberPages;

        public UploadsResponse(List<Upload> uploads, int numberPages) {
            this.uploads = uploads;
            this.numberPages = numberPages;
        }
    }

    private static class UploadDetails {
        @JsonProperty
        List<UploadError> uploadErrors;
        @JsonProperty
        List<FlightError> flightErrors;
        @JsonProperty
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
        @JsonProperty
        public List<Upload> imports;
        @JsonProperty
        public int numberPages;

        public ImportsResponse(List<Upload> imports, int numberPages) {
            this.imports = imports;
            this.numberPages = numberPages;
        }
    }

    private static void getUpload(Context ctx) {
        LOG.info("Retrieving upload: " + ctx.formParams("uploadId") + " " + ctx.formParams("md5Hash"));

        final User user = Objects.requireNonNull(ctx.sessionAttribute("user"));
        Upload upload;

        try (Connection connection = Database.getConnection()) {
            upload = Upload.getUploadById(connection, Integer.parseInt(Objects.requireNonNull(ctx.formParam("uploadId"))), ctx.formParam("md5Hash"));
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
        String identifier = ctx.formParam("identifier");
        if (identifier == null) {
            LOG.severe("ERROR! Missing upload identifier");
            ctx.result(gson.toJson(new ErrorResponse("File Chunk Upload Failure", "File identifier was missing.")));
            return;
        }

        String md5Hash = ctx.formParam("md5Hash");
        if (md5Hash == null) {
            LOG.severe("ERROR! Missing upload md5Hash");
            ctx.result(gson.toJson(new ErrorResponse("File Chunk Upload Failure", "File md5Hash was missing.")));
            return;
        }

        String sChunkNumber = ctx.formParam("chunkNumber");
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
                ctx.result(gson.toJson(new ErrorResponse("File Upload Failure", "A system error occurred where this upload was not in the database. Please try again.")));
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
                        ctx.result(gson.toJson(new ErrorResponse("File Upload Failure", "An error occurred while merging the chunks. The final file size was incorrect. Please try again.")));
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
                        ctx.result(gson.toJson(new ErrorResponse("File Upload Failure", "MD5 hash mismatch. File corruption might have occurred during upload.")));
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

            ctx.header("Content-Type", "text/html; charset=UTF-8");
            ctx.render(templateFile, scopes);
        } catch (SQLException e) {
            ctx.json(new ErrorResponse(e)).status(500);
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
            final int currentPage = Integer.parseInt(Objects.requireNonNull(ctx.formParam("currentPage")));
            final int pageSize = Integer.parseInt(Objects.requireNonNull(ctx.formParam("pageSize")));
            final int totalUploads = Upload.getNumUploads(connection, fleetId, null);
            final int numberPages = totalUploads / pageSize;

            final List<Upload> uploads = Upload.getUploads(connection, fleetId, " LIMIT " + (currentPage * pageSize) + "," + pageSize);

            ctx.json(new UploadsResponse(uploads, numberPages));
        } catch (Exception e) {
            ctx.json(new ErrorResponse(e)).status(500);
        }
    }

    private static void postUploadDetails(Context ctx) {
        final int uploadId = Integer.parseInt(Objects.requireNonNull(ctx.formParam("uploadId")));
        try {
            ctx.json(new UploadDetails(uploadId));
        } catch (SQLException e) {
            ctx.json(new ErrorResponse(e)).status(500);
        }
    }

    private static void postRemoveUpload(Context ctx) {
        try (Connection connection = Database.getConnection()) {
            final User user = Objects.requireNonNull(ctx.sessionAttribute("user"));
            final int uploadId = Integer.parseInt(ctx.formParam("uploadId"));
            String md5Hash = ctx.formParam("md5Hash");
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
            ctx.json(new ErrorResponse(e)).status(500);
        }

    }

    private static void getImports(Context ctx) {
        final String templateFile = "imports.html";

        try (Connection connection = Database.getConnection()) {
            final User user = Objects.requireNonNull(ctx.sessionAttribute("user"));
            final int fleetId = user.getFleetId();
            Map<String, Object> scopes = new HashMap<String, Object>();

            // default page values
            final int totalImports = Upload.getNumUploads(connection, fleetId, null);
            final int startPage = 0;
            final int pageSize = 10;
            final int numberPages = totalImports / pageSize;
            final List<Upload> imports = Upload.getUploads(connection, fleetId, new String[]{"IMPORTED", "ERROR"}, " LIMIT " + startPage + "," + pageSize);

            scopes.put("numPages_js", "var numberPages = " + numberPages + ";");
            scopes.put("index_js", "var currentPage = 0;");
            scopes.put("navbar_js", Navbar.getJavascript(ctx));
            scopes.put("imports_js", "var imports = JSON.parse('" + gson.toJson(imports) + "');");

            for (String key : scopes.keySet()) {
                if (scopes.get(key) == null) {
                    LOG.severe("ERROR! key '" + key + "' was null.");
                }
            }

            ctx.header("Content-Type", "text/html; charset=UTF-8");
            ctx.render(templateFile, scopes);
        } catch (SQLException e) {
            ctx.json(new ErrorResponse(e)).status(500);
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

        LOG.info("Getting imports for fleet: " + fleetId);
        try (Connection connection = Database.getConnection()) {
            int currentPage = Integer.parseInt(Objects.requireNonNull(ctx.formParam("currentPage")));
            int pageSize = Integer.parseInt(Objects.requireNonNull(ctx.formParam("pageSize")));

            int totalImports = Upload.getNumUploads(connection, fleetId, null);
            int numberPages = totalImports / pageSize;
            List<Upload> imports = Upload.getUploads(connection, fleetId, new String[]{"IMPORTED", "ERROR"}, " LIMIT "+ (currentPage * pageSize) + "," + pageSize);

            ctx.json(new ImportsResponse(imports, numberPages));
        } catch (SQLException e) {
            ctx.json(new ErrorResponse(e)).status(500);
        }
    }

    private static void postNewUpload(Context ctx) {
        final User user = Objects.requireNonNull(ctx.sessionAttribute("user"));
        final int uploaderId = user.getId();
        final int fleetId = user.getFleetId();
        final String filename = Objects.requireNonNull(ctx.formParam("filename")).replaceAll(" ", "_");
        final String identifier = Objects.requireNonNull(ctx.formParam("identifier"));
        final int numberChunks = Integer.parseInt(Objects.requireNonNull(ctx.formParam("numberChunks")));
        final long sizeBytes = Long.parseLong(Objects.requireNonNull(ctx.formParam("sizeBytes")));
        final String md5Hash = Objects.requireNonNull(ctx.formParam("md5Hash"));

        ctx.attribute("org.eclipse.jetty.multipartConfig", new MultipartConfigElement("/mnt/ngafid/temp"));

        if (!filename.matches("^[a-zA-Z0-9_.-]*$")) {
            LOG.info("ERROR! malformed filename");

            ErrorResponse errorResponse = new ErrorResponse("File Upload Failure", "The filename was malformed. Filenames must only contain letters, numbers, dashes ('-'), underscores ('_') and periods.");
            ctx.json(errorResponse);
            return;
        }

        // options:
        // 1. file does not exist, insert into database -- start upload
        // 2. file does exist and has not finished uploading -- restart upload
        // 3. file does exist and has finished uploading -- report finished
        // 4. file does exist but with different hash -- error message

        try (Connection connection = Database.getConnection()) {
            try (PreparedStatement query = connection.prepareStatement("SELECT md5_hash, number_chunks, uploaded_chunks, chunk_status, status, filename FROM uploads WHERE md5_hash = ? AND uploader_id = ?")) {
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
            ctx.json(new ErrorResponse(e)).status(500);
        }
    }

    public static void bindRoutes(Javalin app) {
        app.get("/protected/download_upload", ImportUploadJavalinRoutes::getUpload);
        app.post("/protected/new_upload", ImportUploadJavalinRoutes::postNewUpload);
        app.post("/protected/upload", ImportUploadJavalinRoutes::postUpload); // Might be weird. Spark has a "multipart/form-data" in args
        app.post("/protected/remove_upload", ImportUploadJavalinRoutes::postRemoveUpload);

        app.get("/protected/uploads", ImportUploadJavalinRoutes::getUploads);
        app.post("/protected/uploads", ImportUploadJavalinRoutes::postUploads);

        app.get("/protected/imports", ImportUploadJavalinRoutes::getImports);
        app.post("/protected/get_imports", ImportUploadJavalinRoutes::postImports);

        app.post("/protected/upload_details", ImportUploadJavalinRoutes::postUploadDetails);
    }
}
