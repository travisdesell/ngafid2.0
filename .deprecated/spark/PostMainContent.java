package org.ngafid.routes.spark;

import java.util.List;
import java.util.logging.Logger;
import java.sql.Connection;
import java.sql.SQLException;

import com.google.gson.Gson;

import org.ngafid.routes.ErrorResponse;
import spark.Route;
import spark.Request;
import spark.Response;
import spark.Session;

import org.ngafid.Database;
import org.ngafid.accounts.User;
import org.ngafid.flights.Flight;
import org.ngafid.flights.Upload;

public class PostMainContent implements Route {
    private static final Logger LOG = Logger.getLogger(PostMainContent.class.getName());
    private Gson gson;

    public PostMainContent(Gson gson) {
        this.gson = gson;

        LOG.info("post " + this.getClass().getName() + " initalized");
    }

    private class MainContent {
        List<Flight> flights;
        List<Upload> uploads;
        List<Upload> imports;

        public MainContent(Connection connection, int fleetId) throws SQLException {
            flights = Flight.getFlights(connection, fleetId);
            uploads = Upload.getUploads(connection, fleetId);
            imports = Upload.getUploads(connection, fleetId, new String[] { "IMPORTED", "ERROR" });
        }
    }

    @Override
    public Object handle(Request request, Response response) {
        LOG.info("handling " + this.getClass().getName() + " route");

        final Session session = request.session();
        User user = session.attribute("user");

        try (Connection connection = Database.getConnection()) {
            MainContent mainContent = new MainContent(connection, user.getFleetId());

            // LOG.info(gson.toJson(mainContent));

            return gson.toJson(mainContent);
        } catch (SQLException e) {
            return gson.toJson(new ErrorResponse(e));
        }
    }
}
