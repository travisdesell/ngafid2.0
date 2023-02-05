package org.ngafid.routes;

import com.google.gson.Gson;

import org.ngafid.WebServer;
import spark.Request;
import spark.Response;
import spark.Route;

import java.io.File;
import java.util.logging.Logger;

public class GetUpload implements Route {
    private static final Logger LOG = Logger.getLogger(GetUpload.class.getName());
    private Gson gson;


    public GetUpload(Gson gson) {
        this.gson = gson;

        LOG.info("get " + this.getClass().getName() + " initialized");
    }

    @Override
    public Object handle(Request request, Response response) {
        String directory = WebServer.NGAFID_UPLOAD_DIR + "/" + request.params("fleetId") + "/" + request.params("uploaderId") + "/" + request.params("identifier");
        LOG.info("Retrieving upload: " + directory);

        File file = new File(directory + ".zip");
        if (file.exists()) {
            response.header("Content-Disposition", "attachment; filename=" + file.getName());
            response.type("application/zip");

            return file;
        }

        return null;
    }
}
