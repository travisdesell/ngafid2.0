package org.ngafid.routes.spark;

import java.util.ArrayList;
import java.util.logging.Logger;

import java.sql.Connection;
import java.sql.SQLException;

import com.google.gson.Gson;

import spark.Route;
import spark.Request;
import spark.Response;

import org.ngafid.Database;
import org.ngafid.flights.UploadError;
import org.ngafid.flights.FlightError;
import org.ngafid.flights.FlightWarning;

public class PostUploadDetails implements Route {
    private static final Logger LOG = Logger.getLogger(PostUploadDetails.class.getName());
    private Gson gson;

    public PostUploadDetails(Gson gson) {
        this.gson = gson;

        System.out.println("post main content route initalized");
        LOG.info("post main content route initialized.");
    }

    private class UploadDetails {
        ArrayList<UploadError> uploadErrors;
        ArrayList<FlightError> flightErrors;
        ArrayList<FlightWarning> flightWarnings;

        public UploadDetails(int uploadId) throws SQLException {
            try (Connection connection = Database.getConnection()) {
                uploadErrors = UploadError.getUploadErrors(connection, uploadId);
                flightErrors = FlightError.getFlightErrors(connection, uploadId);
                flightWarnings = FlightWarning.getFlightWarnings(connection, uploadId);
            }
        }
    }

    @Override
    public Object handle(Request request, Response response) {
        LOG.info("handling upload details route!");
        int uploadId = Integer.parseInt(request.queryParams("uploadId"));

        try {
            UploadDetails uploadDetails = new UploadDetails(uploadId);

            return gson.toJson(uploadDetails);
        } catch (SQLException e) {
            return gson.toJson(new ErrorResponse(e));
        }
    }
}
