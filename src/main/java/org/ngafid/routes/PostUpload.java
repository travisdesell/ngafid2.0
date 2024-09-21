package org.ngafid.routes;

import java.io.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import java.security.DigestInputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.logging.Logger;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.xml.bind.DatatypeConverter;

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

public class PostUpload implements Route {
    private static final Logger LOG = Logger.getLogger(PostUpload.class.getName());
    private Gson gson;

    public PostUpload(Gson gson) {
        this.gson = gson;

        LOG.info("post " + this.getClass().getName() + " initalized");
    }

    private static void deleteDirectory(File folder) {
        File[] files = folder.listFiles();
        if (files != null) { // some JVMs return null for empty dirs
            for (File f : files) {
                if (f.isDirectory()) {
                    deleteDirectory(f);
                } else {
                    f.delete();
                }
            }
        }
        folder.delete();
    }

    @Override
    public Object handle(Request request, Response response) {
        LOG.info("handling " + this.getClass().getName() + " route!");
        final Session session = request.session();
        User user = session.attribute("user");

        int uploaderId = user.getId();

        /*
         * String location = "image"; // the directory location where files will be
         * stored
         * long maxFileSize = 100000000; // the maximum size allowed for uploaded files
         * long maxRequestSize = 100000000; // the maximum size allowed for
         * multipart/form-data requests
         * int fileSizeThreshold = 1024; // the size threshold after which files will be
         * written to disk
         * 
         * MultipartConfigElement multipartConfigElement = new MultipartConfigElement(
         * location, maxFileSize, maxRequestSize, fileSizeThreshold);
         * 
         * request.raw().setAttribute("org.eclipse.jetty.multipartConfig",
         * multipartConfigElement);
         */

        request.attribute("org.eclipse.jetty.multipartConfig", new MultipartConfigElement("/mnt/ngafid/temp"));

        String identifier = request.queryParams("identifier");
        if (identifier == null) {
            LOG.severe("ERROR! Missing upload identifier");
            return gson.toJson(new ErrorResponse("File Chunk Upload Failure", "File identifier was missing."));
        }

        String md5Hash = request.queryParams("md5Hash");
        if (md5Hash == null) {
            LOG.severe("ERROR! Missing upload md5Hash");
            return gson.toJson(new ErrorResponse("File Chunk Upload Failure", "File md5Hash was missing."));
        }

        String sChunkNumber = request.queryParams("chunkNumber");
        if (sChunkNumber == null) {
            LOG.severe("ERROR! Missing upload chunk number");
            return gson.toJson(new ErrorResponse("File Chunk Upload Failure", "File chunk was missing."));
        }
        int chunkNumber = Integer.parseInt(sChunkNumber);

        Upload upload = null;
        try (Connection connection = Database.getConnection()) {
            upload = Upload.getUploadByUser(connection, uploaderId, md5Hash);

            if (upload == null) {
                LOG.severe("ERROR! Upload was not in the database!");
                return gson.toJson(new ErrorResponse("File Upload Failure",
                        "A system error occured where this upload was not in the database. Please try again."));
            }

            int fleetId = upload.getFleetId();

            Part uploadPart = request.raw().getPart("chunk");
            InputStream chunkInputStream = uploadPart.getInputStream();
            long chunkSize = chunkInputStream.available();

            String chunkDirectory = WebServer.NGAFID_UPLOAD_DIR + "/" + fleetId + "/" + uploaderId + "/" + identifier;
            // create the directory structure in case it doesn't exit
            new File(chunkDirectory).mkdirs();

            String chunkFilename = chunkDirectory + "/" + chunkNumber + ".part";

            // overwrite chunk if it already exists due to some issue
            LOG.info("copying uploaded chunk to: '" + chunkFilename + "'");
            Files.copy(chunkInputStream, Paths.get(chunkFilename), StandardCopyOption.REPLACE_EXISTING);

            LOG.info("chunk file size (from stream): " + chunkSize);
            chunkSize = new File(chunkFilename).length();
            LOG.info("chunk file size (from file): " + chunkSize);

            // update database setting chunk as uploaded
            // if all chunks uploaded, combine file and report progress
            // if not all chunks uploaded, report progress
            upload.chunkUploaded(connection, chunkNumber, chunkSize);

            if (upload.completed()) {
                // create the final file
                String targetDirectory = upload.getArchiveDirectory();
                // create the directory structure in case it doesn't exit
                new File(targetDirectory).mkdirs();
                String targetFilename = targetDirectory + "/" + upload.getArchiveFilename();

                LOG.info("attempting to write file to '" + targetFilename + "'");

                FileOutputStream out = new FileOutputStream(targetFilename);
                for (int i = 0; i < upload.getNumberChunks(); i++) {
                    byte[] bytes = Files.readAllBytes(Paths.get(chunkDirectory + "/" + i + ".part"));
                    out.write(bytes);
                }
                out.close();

                // check to see if the size matches
                if (!upload.checkSize()) {
                    LOG.severe("ERROR! Final file had incorrect number of bytes.");
                    return gson.toJson(new ErrorResponse("File Upload Failure",
                            "An error occurred while putting the chunk files together to make the full uploaded file. The new full file had a different number of bytes than the one that was originally uploaded, so some corruption may have occurred on transfer. Please delete this file, reload the webpage and retry."));
                }

                // check to see if the MD5 hash matches
                String newMd5Hash = null;
                InputStream is = null;
                try {
                    MessageDigest md = MessageDigest.getInstance("MD5");
                    is = new BufferedInputStream(new FileInputStream(Paths.get(targetFilename).toFile()));
                    DigestInputStream dis = new DigestInputStream(is, md);

                    // This walks through the input streams bytes
                    while (dis.read() != -1) {
                    }

                    byte[] hash = md.digest();
                    newMd5Hash = DatatypeConverter.printHexBinary(hash).toLowerCase();
                } catch (NoSuchAlgorithmException | IOException e) {
                    e.printStackTrace();
                    System.exit(1);
                } finally {
                    if (is != null)
                        is.close();
                }

                LOG.info("new md5 hash:      '" + newMd5Hash + "'");
                LOG.info("expected md5 hash: '" + upload.getMd5Hash() + "'");

                if (!newMd5Hash.equals(upload.getMd5Hash())) {
                    LOG.severe(
                            "ERROR! Final file had incorrect bytes, original MD5 hash and uploaded MD5 hashes do not match, some data may have been corrupted.");
                    return gson.toJson(new ErrorResponse("File Upload Failure",
                            "An error occurred while putting the chunk files together to make the full uploaded file. The new full file had different bytes than the one that was originally uploaded, so some corruption may have occurred on transfer. Please delete this file, reload the webpage and retry."));
                }

                upload.complete(connection);

                // delete the directory and parts
                deleteDirectory(new File(chunkDirectory));
            }
        } catch (Exception e) {
            LOG.info(e.getMessage());
            e.printStackTrace();

            return gson.toJson(new ErrorResponse(e));
        }

        return gson.toJson(upload);
    }

}
