package org.ngafid.routes;

import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.google.gson.Gson;

import spark.Route;
import spark.Request;
import spark.Response;
import spark.Session;
import spark.Spark;

import org.ngafid.common.TimeUtils;

import org.ngafid.Database;
import org.ngafid.accounts.User;
import org.ngafid.flights.DoubleTimeSeries;
import org.ngafid.flights.StringTimeSeries;

public class PostStringSeries implements Route {
    private static final Logger LOG = Logger.getLogger(PostStringSeries.class.getName());
    private Gson gson;

    public PostStringSeries(Gson gson) {
        this.gson = gson;

        System.out.println("post main content route initalized");
        LOG.info("post main content route initialized.");
    }

    private class StringSeries {
        String[] x;
        String[] y;

        public StringSeries(int flightId, String name) throws SQLException, IOException {
            Connection connection = Database.getConnection();
            StringTimeSeries stringTimeSeries = StringTimeSeries.getStringTimeSeries(connection, flightId, name);
            LOG.info("POST string series getting string time series for flight id: " + flightId + " and name: '" + name + "'");

           
            

                x = new String[stringTimeSeries.size()];
                y = new String[stringTimeSeries.size()];

                for (int i = 0; i < stringTimeSeries.size(); i++) {
                    x[i] = String.valueOf(i);
                    y[i] = stringTimeSeries.get(i);
                }
                
                
       }
    }

    @Override
    public Object handle(Request request, Response response) {
        LOG.info("handling string series route!");

        final Session session = request.session();
        User user = session.attribute("user");

        int flightId = Integer.parseInt(request.queryParams("flightId"));
        String name = request.queryParams("seriesName");

        try {
            //check to see if the user has access to this data
            if (!user.hasFlightAccess(Database.getConnection(), flightId)) {
                LOG.severe("INVALID ACCESS: user did not have access to this flight.");
                Spark.halt(401, "User did not have access to this flight.");
            }

            StringSeries stringSeries = new StringSeries(flightId, name);

            //System.out.println(gson.toJson(uploadDetails));
            String output = gson.toJson(stringSeries);
            //need to convert NaNs to null so they can be parsed by JSON
            output = output.replaceAll("NaN","null");

            //LOG.info(output);

            return output;
        } catch (SQLException | IOException e) {
            e.printStackTrace();
            return gson.toJson(new ErrorResponse(e));
        }
    }
}
