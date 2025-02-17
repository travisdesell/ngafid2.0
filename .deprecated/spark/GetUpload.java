package org.ngafid.routes.spark;

import com.google.gson.Gson;

import org.ngafid.Database;
import org.ngafid.WebServer;
import org.ngafid.accounts.User;
import org.ngafid.flights.Upload;
import spark.*;
import spark.utils.IOUtils;

import java.io.*;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

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

        LOG.info("Retrieving upload: " + request.formParams("uploadId") + " " + request.formParams("md5Hash"));
        Upload upload;

        try (Connection connection = Database.getConnection()) {
            upload = Upload.getUploadById(connection, Integer.parseInt(request.formParams("uploadId")),
                    request.formParams("md5Hash"));
        }

        if (upload == null) {
            response.status(404);
            return "Upload not found";
        }

        //Verify that a user has access to the download the uploaded file (requires View Access)
        if (!user.hasViewAccess(upload.getFleetId())) {
            LOG.severe("INVALID ACCESS: user did not have view, upload, or manager access this fleet.");
            Spark.halt(401, "User did not have access to download the uploaded file.");
            return null;
        }

        File file = new File(String.format("%s/%d/%d/%d__%s", WebServer.NGAFID_ARCHIVE_DIR, upload.getFleetId(),
                upload.getUploaderId(), upload.getId(), upload.getFilename()));
        LOG.info("File: " + file.getAbsolutePath());
        if (file.exists()) {
            response.raw().setContentType("application/zip");
            response.raw().setHeader("Content-Disposition", "attachment; filename=" + upload.getFilename());

            try (InputStream buffInputStream = new BufferedInputStream(new FileInputStream(file));
                    OutputStream outputStream = response.raw().getOutputStream()) {

                IOUtils.copy(buffInputStream, outputStream);

                LOG.log(Level.INFO, "%s file sent", file.getName());
                return null;
            } catch (IOException e) {
                LOG.severe(e.toString());
            }
        }

        LOG.severe(String.format("File not found: %s", file.getName()));
        return "File was not found on server";
    }
}
