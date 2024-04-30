package org.ngafid.routes;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;


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


import org.ngafid.accounts.User;
import org.ngafid.events.EventStatistics;
import org.ngafid.Database;

public class GetEventCounts implements Route {
    private static final Logger LOG = Logger.getLogger(GetEventStatistics.class.getName());
    private Gson gson;

    public GetEventCounts(Gson gson) {
        this.gson = gson;
        LOG.info("post " + this.getClass().getName() + " initalized");
    }

    @Override
    public Object handle(Request request, Response response) {
        LOG.info("handling " + this.getClass().getName() + " route");

        //try  {
            Connection connection = Database.getConnection();

            final Session session = request.session();
            User user = session.attribute("user");
            int fleetId = user.getFleetId();

            String airframe = request.queryParams("airframe_id");
            int airframeId = airframe == null ? -1 : Integer.parseInt(airframe);

            boolean aggregate = Boolean.parseBoolean(request.queryParams("aggregate"));

            String startDate = request.queryParams("start_date");
            String endDate = request.queryParams("end_date");

            return null;

        /*} catch (SQLException e) {
            LOG.severe(e.toString());
            return gson.toJson(new ErrorResponse(e));
        }*/
    }
}
