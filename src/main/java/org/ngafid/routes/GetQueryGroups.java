package org.ngafid.routes;

import java.util.logging.Logger;

import java.sql.Connection;
import java.sql.SQLException;

import com.google.gson.Gson;

import org.ngafid.accounts.Fleet;
import spark.Route;
import spark.Request;
import spark.Response;
import spark.Session;


import org.ngafid.Database;
import org.ngafid.accounts.User;


public class GetQueryGroups implements Route {
    private static final Logger LOG = Logger.getLogger(GetFlights.class.getName());
    private Gson gson;

    public GetQueryGroups(Gson gson) {
        this.gson = gson;

        LOG.info("post " + this.getClass().getName() + " initalized");
    }

    @Override
    public Object handle(Request request, Response response) {
        LOG.info("handling " + this.getClass().getName() + " route");

        try {
            final Session session = request.session();
            User user = session.attribute("user");
            int fleetID = user.getFleetId();                    // currently every user has only one fleet

            Connection connection = Database.getConnection();
            Fleet fleet = Fleet.get(connection, fleetID);

            String resultString = user.getId() + "," + fleetID + "," + fleet.getName() + "," + user.getFleetAccessType();

            return resultString;

        } catch (SQLException e) {
            LOG.severe(e.toString());
            return gson.toJson(new ErrorResponse(e));
        }
    }
}