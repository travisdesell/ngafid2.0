package org.ngafid.routes;

import com.google.gson.Gson;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Logger;
import org.ngafid.Database;
import org.ngafid.accounts.User;
import org.ngafid.common.FlightTag;
import org.ngafid.flights.Flight;
import spark.Request;
import spark.Response;
import spark.Route;
import spark.Session;
import spark.Spark;

public class PostAssociateTag implements Route {
    private static final Logger LOG = Logger.getLogger(PostAssociateTag.class.getName());
    private Gson gson;

    public PostAssociateTag(Gson gson) {
        this.gson = gson;

        LOG.info("initialized " + this.getClass().getName() + " route!");
    }

    @Override
    public Object handle(Request request, Response response) {
        LOG.info("handling " + this.getClass().getName() + " route!");

        final Session session = request.session();
        User user = session.attribute("user");

        int flightId = Integer.parseInt(request.queryParams("id"));
        int tagId = Integer.parseInt(request.queryParams("tag_id"));

        try {
            Connection connection = Database.getConnection();

            if (!user.hasFlightAccess(Database.getConnection(), flightId)) {
                LOG.severe("INVALID ACCESS: user did not have access to this flight.");
                Spark.halt(401, "User did not have access to this flight.");
            }

            Flight.associateTag(flightId, tagId, connection);

            FlightTag ft = Flight.getTag(connection, tagId);

            return gson.toJson(ft);
        } catch (SQLException e) {
            System.err.println("Error in SQL ");
            e.printStackTrace();
            return gson.toJson(new ErrorResponse(e));
        }
    }
}
