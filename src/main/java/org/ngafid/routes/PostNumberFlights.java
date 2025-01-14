package org.ngafid.routes;

import com.google.gson.Gson;
import java.util.logging.Logger;
import spark.Request;
import spark.Response;
import spark.Route;


public class PostNumberFlights implements Route {
    private static final Logger LOG = Logger.getLogger(PostNumberFlights.class.getName());
    private Gson gson;

    public PostNumberFlights(Gson gson) {
        this.gson = gson;

        LOG.info("initialized " + this.getClass().getName() + " route!");
    }

    @Override
    public Object handle(Request request, Response response) {
        LOG.info("handling " + this.getClass().getName() + " route!");
        return "";
        // final Session session = request.session();
        // User user = session.attribute("user");

        // int airframeId = Integer.parseInt(request.queryParams("airframe_id"));
        // int tagId = Integer.parseInt(request.queryParams("tag_id"));

        // try {
        //     Connection connection = Database.getConnection();

        //     if (!user.hasFlightAccess(connection, flightId)) {
        //         LOG.severe("INVALID ACCESS: user did not have access to this flight.");
        //         Spark.halt(401, "User did not have access to this flight.");
        //     }

        //     Flight.associateTag(flightId, tagId, connection);

        //     FlightTag ft = Flight.getTag(connection, tagId);

        //     return gson.toJson(ft);
        // } catch (SQLException e) {
        //     System.err.println("Error in SQL ");
        //     e.printStackTrace();
        //     return gson.toJson(new ErrorResponse(e));
        // }
    }
}
