package org.ngafid.routes;

import com.google.gson.Gson;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Logger;
import org.ngafid.Database;
import org.ngafid.accounts.User;
import org.ngafid.flights.DoubleTimeSeries;
import spark.Request;
import spark.Response;
import spark.Route;
import spark.Session;
import spark.Spark;

public class PostDoubleSeries implements Route {
    private static final Logger LOG = Logger.getLogger(PostDoubleSeries.class.getName());
    private Gson gson;

    public PostDoubleSeries(Gson gson) {
        this.gson = gson;

        System.out.println("post main content route initalized");
        LOG.info("post main content route initialized.");
    }

    private class DoubleSeries {
        String[] x;
        double[] y;

        public DoubleSeries(int flightId, String name) throws SQLException, IOException {
            Connection connection = Database.getConnection();
            DoubleTimeSeries doubleTimeSeries = DoubleTimeSeries.getDoubleTimeSeries(connection, flightId, name);
            LOG.info("POST double series getting double time series for flight id: " + flightId + " and name: '" + name + "'");

            /*
            StringTimeSeries dateSeries = StringTimeSeries.getStringTimeSeries(connection, flightId, "Lcl Date");
            StringTimeSeries timeSeries = StringTimeSeries.getStringTimeSeries(connection, flightId, "Lcl Time");
            StringTimeSeries utcOffsetSeries = StringTimeSeries.getStringTimeSeries(connection, flightId, "UTCOfst");

            ArrayList<String> times = new ArrayList<String>();
            ArrayList<Double> values = new ArrayList<Double>();
            */

            //if (dateSeries == null || timeSeries == null || utcOffsetSeries == null) {
            int size = 0;
            if (doubleTimeSeries != null) {
                size = doubleTimeSeries.size();
            }

            x = new String[size];
            y = new double[size];

            for (int i = 0; i < size; i++) {
                x[i] = String.valueOf(i);
                y[i] = doubleTimeSeries.get(i);
            }

                /*
            } else {
                for (int i = 0; i < doubleTimeSeries.size(); i++) {
                    if (dateSeries.get(i).equals("") || dateSeries.get(i) == null) continue;
                    if (timeSeries.get(i).equals("") || timeSeries.get(i) == null) continue;
                    if (utcOffsetSeries.get(i).equals("") || utcOffsetSeries.get(i) == null) continue;

                    times.add( TimeUtils.toUTC(dateSeries.get(i), timeSeries.get(i), utcOffsetSeries.get(i)) );
                    values.add( doubleTimeSeries.get(i) );
                }

                x = new String[times.size()];
                y = new double[times.size()];

                for (int i = 0; i < times.size(); i++) {
                    x[i] = times.get(i);
                    y[i] = values.get(i);
                }
            }
            */
       }
    }

    @Override
    public Object handle(Request request, Response response) {
        LOG.info("handling double series route!");

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

            DoubleSeries doubleSeries = new DoubleSeries(flightId, name);

            //System.out.println(gson.toJson(uploadDetails));
            String output = gson.toJson(doubleSeries);
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
