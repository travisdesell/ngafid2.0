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

        Connection connection = Database.getConnection();

        int uploaderId = user.getId();
        int fleetId = user.getFleetId();

        /*
           String location = "image";          // the directory location where files will be stored
           long maxFileSize = 100000000;       // the maximum size allowed for uploaded files
           long maxRequestSize = 100000000;    // the maximum size allowed for multipart/form-data requests
           int fileSizeThreshold = 1024;       // the size threshold after which files will be written to disk

           MultipartConfigElement multipartConfigElement = new MultipartConfigElement(
           location, maxFileSize, maxRequestSize, fileSizeThreshold);

           request.raw().setAttribute("org.eclipse.jetty.multipartConfig", multipartConfigElement);
           */

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

        int sizeBytes = Integer.parseInt(request.queryParams("sizeBytes"));
        LOG.info("sizeBytes: " + sizeBytes);

        String md5Hash = request.queryParams("md5Hash");
        LOG.info("md5Hash: " + md5Hash);

        filename = filename.replaceAll(" ", "_");

        if (!filename.matches("^[a-zA-Z0-9_.-]*$")) {
            LOG.info("ERROR! malformed filename");

            ErrorResponse errorResponse = new ErrorResponse("File Upload Failure", "The filename was malformed. Filenames must only contain letters, numbers, dashes ('-'), underscores ('_') and periods.");
            return gson.toJson(errorResponse);
        }   


        //options:
        //  1. file does not exist, insert into database -- start upload
        //  2. file does exist and has not finished uploading -- restart upload
        //  3. file does exist and has finished uploading -- report finished
        //  4. file does exist but with different hash -- error message

        try {
            PreparedStatement query = connection.prepareStatement("SELECT md5_hash, number_chunks, uploaded_chunks, chunk_status, status, filename FROM uploads WHERE md5_hash = ? AND uploader_id = ?");
            query.setString(1, md5Hash);
            query.setInt(2, uploaderId);

            ResultSet resultSet = query.executeQuery();

            if (!resultSet.next()) {
                //  1. file does not exist, insert into database -- start upload
                String chunkStatus = ""; 
                for (int i = 0; i < numberChunks; i++) {
                    chunkStatus += '0';
                }   

                query = connection.prepareStatement("INSERT INTO uploads SET uploader_id = ?, fleet_id = ?, filename = ?, identifier = ?, size_bytes = ?, number_chunks = ?, md5_hash=?, uploaded_chunks = 0, chunk_status = ?, status = 'UPLOADING', start_time = now()");
                query.setInt(1, uploaderId);
                query.setInt(2, fleetId);
                query.setString(3, filename);
                query.setString(4, identifier);
                query.setInt(5, sizeBytes);
                query.setInt(6, numberChunks);
                query.setString(7, md5Hash);
                query.setString(8, chunkStatus);

                query.executeUpdate();

                return gson.toJson(Upload.getUpload(connection, uploaderId, md5Hash));

            } else {
                //a file with this md5 hash exists
                String dbMd5Hash = resultSet.getString(1);
                int dbNumberChunks = resultSet.getInt(2);
                int dbUploadedChunks = resultSet.getInt(3);
                String dbChunkStatuts = resultSet.getString(4);
                String dbStatus = resultSet.getString(5);
                String dbFilename = resultSet.getString(6);

                if (dbStatus.equals("UPLOADED") || dbStatus.equals("IMPORTED")) {
                    //  3. file does exist and has finished uploading -- report finished
                    //do the same thing, client will handle completion
                    LOG.severe("ERROR! Final file has already been uploaded.");

                    return gson.toJson(new ErrorResponse("File Already Exists", "This file has already been uploaded to the server as '" + dbFilename + "' and does not need to be uploaded again."));

                } else {
                    //  2. file does exist and has not finished uploading -- restart upload

                    return gson.toJson(Upload.getUpload(connection, uploaderId, md5Hash));
                }
            }
        } catch (SQLException e) {
            return gson.toJson(new ErrorResponse(e));
        }
    }
}



