package org.ngafid.routes;

import com.google.gson.Gson;

import org.ngafid.Database;
import org.ngafid.WebServer;
import org.ngafid.accounts.User;
import org.ngafid.flights.Upload;
import spark.*;

import java.io.*;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;


public class GetUpload implements Route {
    private static final Logger LOG = Logger.getLogger(GetUpload.class.getName());
    private Gson gson;


    public GetUpload(Gson gson) {
        this.gson = gson;

        LOG.info("get " + this.getClass().getName() + " initialized");
    }

    @Override
    public Object handle(Request request, Response response) throws SQLException {
        final Session session = request.session();
        User user = session.attribute("user");
        Connection connection = Database.getConnection();

        LOG.info("Retrieving upload: " + request.queryParams("uploadId") + " " + request.queryParams("md5Hash"));
        Upload upload = Upload.getUploadById(connection, Integer.parseInt(request.queryParams("uploadId")), request.queryParams("md5Hash"));

        if (upload == null) {
            response.status(404);
            return "Upload not found";
        }

        if (!user.hasUploadAccess(upload.getFleetId())) {
            LOG.severe("INVALID ACCESS: user did not have upload or manager access this fleet.");
            Spark.halt(401, "User did not have access to delete this upload.");
            return null;
        }

        File file = new File(String.format("%s/%d/%d/%d__%s", WebServer.NGAFID_ARCHIVE_DIR, upload.getFleetId(), upload.getUploaderId(), upload.getId(), upload.getFilename()));
        LOG.info("File: " + file.getAbsolutePath());
        if (file.exists()) {
            System.out.println(file.getName());

            response.raw().setContentType("application/zip");
            response.raw().setHeader("Content-Disposition", "attachment; filename=" + upload.getFilename());
            response.raw().setHeader("Content-Length", String.valueOf(file.length()));


            try (FileInputStream fileInputStream = new FileInputStream(file); OutputStream outputStream = response.raw().getOutputStream()) {
                byte[] buffer = new byte[8192];
                int bytesRead;

                while ((bytesRead = fileInputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }

                outputStream.flush();

                LOG.info("File sent successfully");
            } catch (IOException e) {
                LOG.severe(e.toString());
            }

            return response.raw();
        }

        LOG.severe(String.format("File not found: %s", file.getName()));
        return "File was not found";
    }
}
