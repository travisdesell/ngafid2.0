package org.ngafid.routes;

import com.google.gson.Gson;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
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

public class PostTags implements Route {
    private static final Logger LOG = Logger.getLogger(PostTags.class.getName());
    private Gson gson;

    public PostTags(Gson gson) {
        this.gson = gson;

        LOG.info("initialized " + this.getClass().getName() + " route!");
    }

    @Override
    public Object handle(Request request, Response response) {
        LOG.info("handling " + this.getClass().getName() + " route!");

        final Session session = request.session();
        User user = session.attribute("user");

        int flightId = Integer.parseInt(request.queryParams("flightId"));
        System.out.println("TAGGED FLTID: "+flightId);


        try {
            Connection connection = Database.getConnection();

            List<FlightTag> fltTags = null;

            List<FlightTag> tags = Flight.getTags(connection, flightId);
            if(tags != null){
                fltTags = tags;
            }

            //check to see if the user has access to this data
            if (!user.hasFlightAccess(Database.getConnection(), flightId)) {
                LOG.severe("INVALID ACCESS: user did not have access to this flight.");
                Spark.halt(401, "User did not have access to this flight.");
            }




            return gson.toJson(fltTags);
        } catch (SQLException e) {
            System.err.println("Error in SQL ");
            e.printStackTrace();
            return gson.toJson(new ErrorResponse(e));
        }
    }
}
