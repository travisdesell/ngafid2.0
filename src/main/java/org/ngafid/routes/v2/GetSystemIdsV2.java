package org.ngafid.routes.v2;

import java.io.IOException;
import org.ngafid.routes.ErrorResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.HashMap;
import java.sql.Connection;
import java.sql.SQLException;
import com.google.gson.Gson;
import spark.Route;
import spark.Request;
import spark.Response;
import spark.Session;
import org.ngafid.Database;
import org.ngafid.accounts.User;
import org.ngafid.flights.Tail;
import org.ngafid.flights.Tails;

public class GetSystemIdsV2 implements Route {
    private static final Logger LOG = Logger.getLogger(GetSystemIdsV2.class.getName());
    private Gson gson;


    public GetSystemIdsV2(Gson gson) {
        this.gson = gson;

        LOG.info("post " + this.getClass().getName() + " initalized");
    }

    

    @Override
    public Object handle(Request request, Response response) {
        
        LOG.info("handling " + this.getClass().getName() + " route");

        final Session session = request.session();
        User user = session.attribute("user");
        int fleetId = user.getFleetId();

        try  {
            Connection connection = Database.getConnection();

            ArrayList<Tail> tailInfo = Tails.getAll(connection, fleetId);

            return gson.toJson(tailInfo);

        } catch (SQLException e) {
            LOG.severe(e.toString());
            return gson.toJson(new ErrorResponse(e));

        }
    }
}