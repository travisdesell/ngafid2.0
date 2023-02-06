package org.ngafid.routes;

import com.google.gson.Gson;

import org.ngafid.Database;
import org.ngafid.WebServer;
import org.ngafid.accounts.User;
import org.ngafid.flights.Upload;
import spark.*;

import java.io.File;
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
            response.header("Content-Disposition", "attachment; filename=" + file.getName());
            response.type("application/zip");

            return file;
        }

        LOG.log(Level.SEVERE, "File not found: ", file.getAbsolutePath());


        return null;
    }
}
