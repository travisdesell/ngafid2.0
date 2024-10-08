package org.ngafid.routes.spark;

import com.google.gson.Gson;
import org.ngafid.Database;
import org.ngafid.events.RateOfClosure;
import spark.Request;
import spark.Response;
import spark.Route;
import java.sql.Connection;
import java.util.logging.Logger;

public class PostRateOfClosure implements Route {

    private static final Logger LOG = Logger.getLogger(PostRateOfClosure.class.getName());
    private Gson gson;

    public PostRateOfClosure(Gson gson) {
        this.gson = gson;
    }

    private class RateOfClosurePlotData {

        int[] x;
        double[] y;

        public RateOfClosurePlotData(RateOfClosure rateOfClosure) {
            this.x = new int[rateOfClosure.getSize()];
            this.y = rateOfClosure.getRateOfClosureArray();
            for (int i = -5; i < rateOfClosure.getSize() - 5; i++) {
                x[i + 5] = i;
            }
        }
    }

    @Override
    public Object handle(Request request, Response response) {

        LOG.info("handling rate of closure route");
        int eventId = Integer.parseInt(request.queryParams("eventId"));
        try (Connection connection = Database.getConnection()) {
            RateOfClosure rateOfClosure = RateOfClosure.getRateOfClosureOfEvent(connection, eventId);
            if (rateOfClosure != null) {
                RateOfClosurePlotData rocData = new RateOfClosurePlotData(rateOfClosure);
                String output = gson.toJson(rocData);
                return output;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return gson.toJson(new ErrorResponse(e));
        }
        return gson.toJson(null);
    }
}
