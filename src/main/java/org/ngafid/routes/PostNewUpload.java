package org.ngafid.routes;

import java.util.ArrayList;
import java.util.Collection;
import java.util.logging.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.google.gson.Gson;

import javax.servlet.MultipartConfigElement;
import javax.servlet.http.Part;

import spark.Route;
import spark.Request;
import spark.Response;
import spark.Session;

import org.ngafid.Database;
import org.ngafid.WebServer;

import org.ngafid.accounts.User;

import org.ngafid.flights.Upload;

public class PostNewUpload implements Route {
    private static final Logger LOG = Logger.getLogger(PostNewUpload.class.getName());
    private Gson gson;

    public PostNewUpload(Gson gson) {
        this.gson = gson;

        LOG.info("post " + this.getClass().getName() + " initalized");
    }

    @Override
    public Object handle(Request request, Response response) {
        LOG.info("handling " + this.getClass().getName() + " route!");

        final Session session = request.session();
        User user = session.attribute("user");

        int uploaderId = user.getId();
        int fleetId = user.getFleetId();

        request.attribute("org.eclipse.jetty.multipartConfig", new MultipartConfigElement("/mnt/ngafid/temp"));

        String idToken = request.queryParams("idToken");
        LOG.info("idToken: " + idToken);

        String paramRequest = request.queryParams("request");
        LOG.info("request: " + paramRequest);

        String filename = request.queryParams("filename");
        LOG.info("filename: " + filename);

        String identifier = request.queryParams("identifier");
        LOG.info("identifier: " + identifier);

        int numberChunks = Integer.parseInt(request.queryParams("numberChunks"));
        LOG.info("numberChunks: " + numberChunks);

        long sizeBytes = Long.parseLong(request.queryParams("sizeBytes"));
        LOG.info("sizeBytes: " + sizeBytes);

        String md5Hash = request.queryParams("md5Hash");
        LOG.info("md5Hash: " + md5Hash);

        filename = filename.replaceAll(" ", "_");

        if (!filename.matches("^[a-zA-Z0-9_.-]*$")) {
            LOG.info("ERROR! malformed filename");

            ErrorResponse errorResponse = new ErrorResponse("File Upload Failure",
                    "The filename was malformed. Filenames must only contain letters, numbers, dashes ('-'), underscores ('_') and periods.");
            return gson.toJson(errorResponse);
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

                        return gson.toJson(upload);
                    } else {
                        // a file with this md5 hash exists
                        String dbStatus = resultSet.getString(5);
                        String dbFilename = resultSet.getString(6);

                        if (dbStatus.equals("UPLOADED") || dbStatus.equals("IMPORTED")) {
                            // 3. file does exist and has finished uploading -- report finished
                            // do the same thing, client will handle completion
                            LOG.severe("ERROR! Final file has already been uploaded.");

                            return gson.toJson(new ErrorResponse("File Already Exists",
                                    "This file has already been uploaded to the server as '" + dbFilename
                                            + "' and does not need to be uploaded again."));

                        } else {
                            // 2. file does exist and has not finished uploading -- restart upload

                            return gson.toJson(Upload.getUploadByUser(connection, uploaderId, md5Hash));
                        }
                    }
                }
            }
        } catch (SQLException e) {
            LOG.severe(gson.toJson(e));
            e.printStackTrace();
            return gson.toJson(new ErrorResponse(e));
        }
    }
}
