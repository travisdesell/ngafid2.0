package org.ngafid.routes;

import com.google.gson.Gson;
import java.sql.SQLException;
import java.util.List;
import java.util.logging.Logger;
import org.ngafid.Database;
import org.ngafid.accounts.User;
import org.ngafid.flights.Flight;
import org.ngafid.flights.Upload;
import spark.Request;
import spark.Response;
import spark.Route;
import spark.Session;

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

        public MainContent(int fleetId) throws SQLException {
            flights = Flight.getFlights(Database.getConnection(), fleetId);
            uploads = Upload.getUploads(Database.getConnection(), fleetId);
            imports = Upload.getUploads(Database.getConnection(), fleetId, new String[]{"IMPORTED", "ERROR"});
        }
    }

    @Override
    public Object handle(Request request, Response response) {
        LOG.info("handling " + this.getClass().getName() + " route");

        final Session session = request.session();
        User user = session.attribute("user");

        try {
            MainContent mainContent = new MainContent(user.getFleetId());

            //LOG.info(gson.toJson(mainContent));

            return gson.toJson(mainContent);
        } catch (SQLException e) {
            return gson.toJson(new ErrorResponse(e));
        }
    }
}
