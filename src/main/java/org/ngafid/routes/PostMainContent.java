package org.ngafid.routes;

import java.util.ArrayList;
import java.util.logging.Logger;

import java.sql.SQLException;

import com.google.gson.Gson;

import spark.Route;
import spark.Request;
import spark.Response;

import org.ngafid.Database;
import org.ngafid.flights.Flight;
import org.ngafid.flights.Upload;

public class PostMainContent implements Route {
    private static final Logger LOG = Logger.getLogger(PostMainContent.class.getName());
    private Gson gson;

    public PostMainContent(Gson gson) {
        this.gson = gson;

        System.out.println("post main content route initalized");
        LOG.info("post main content route initialized.");
    }

    private class MainContent {
        ArrayList<Flight> flights;
        ArrayList<Upload> uploads;
        ArrayList<Upload> imports;

        public MainContent(int fleetId) throws SQLException {
            flights = Flight.getFlights(Database.getConnection(), fleetId);
            uploads = Upload.getUploads(Database.getConnection(), fleetId);
            imports = Upload.getUploads(Database.getConnection(), fleetId, new String[]{"IMPORTED", "ERROR"});
        }
    }

    @Override
    public Object handle(Request request, Response response) {
        LOG.info("handling main content route!");

        try {
            MainContent mainContent = new MainContent(1);

            //LOG.info(gson.toJson(mainContent));

            return gson.toJson(mainContent);
        } catch (SQLException e) {
            return gson.toJson(new ErrorResponse(e));
        }
    }
}
