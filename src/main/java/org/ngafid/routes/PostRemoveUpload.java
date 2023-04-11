package org.ngafid.routes;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.IOException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

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
import spark.Spark;

import org.ngafid.Database;
import org.ngafid.WebServer;
import org.ngafid.accounts.User;
import org.ngafid.flights.Flight;
import org.ngafid.flights.Tails;
import org.ngafid.flights.Upload;

public class PostRemoveUpload implements Route {
    private static final Logger LOG = Logger.getLogger(PostRemoveUpload.class.getName());
    private Gson gson;

    public PostRemoveUpload(Gson gson) {
        this.gson = gson;

        LOG.info("post " + this.getClass().getName() + " initalized");
    }
    @Override
    public Object handle(Request request, Response response) {
        try {
            LOG.info("handling " + this.getClass().getName() + " route!");
            final Session session = request.session();
            User user = session.attribute("user");

            Connection connection = Database.getConnection();

            int uploaderId = user.getId();

            int uploadId = Integer.parseInt(request.queryParams("uploadId"));
            String md5Hash = request.queryParams("md5Hash");

            Upload upload = Upload.getUploadById(connection, uploadId, md5Hash);
            if (upload == null) {
                throw new Exception("Retrieved upload was not found");
            }

            //check to see if the user has upload access for this fleet.
            if (!user.hasUploadAccess(upload.getFleetId())) {
                LOG.severe("INVALID ACCESS: user did not have upload or manager access this fleet.");
                Spark.halt(401, "User did not have access to delete this upload.");
                return null;
            }

            if (upload.getFleetId() != user.getFleetId()) {
                LOG.severe("INVALID ACCESS: user did not have access to this fleet.");
                Spark.halt(401, "User did not have access to delete this upload.");
                return null;
            }

            ArrayList<Flight> flights = Flight.getFlightsFromUpload(connection, uploadId);

            //get all flights, delete:
            //flight warning
            //flgiht error
            //
            for (Flight flight : flights) {
                flight.remove(connection);
            }

            upload.remove(connection);

            Tails.removeUnused(connection);

            return "{}";
        } catch (Exception e) {
            LOG.info(e.getMessage());
            e.printStackTrace();

            return gson.toJson(new ErrorResponse(e));
        }

    }

}



