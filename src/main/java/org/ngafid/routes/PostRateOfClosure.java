package org.ngafid.routes;

import com.google.gson.Gson;
import org.ngafid.Database;
import org.ngafid.accounts.User;
import org.ngafid.events.RateOfClosure;
import spark.Request;
import spark.Response;
import spark.Route;
import spark.Session;

import java.sql.Connection;
import java.util.logging.Logger;

public class PostRateOfClosure implements Route {

    private static final Logger LOG = Logger.getLogger(PostRateOfClosure.class.getName());
    private Gson gson;

    public PostRateOfClosure(Gson gson) {
        this.gson = gson;
    }

    private class RateOfClosureData {

        int[] x;
        double[] y;

        public RateOfClosureData(int eventId) {
            Connection connection = Database.getConnection();
            RateOfClosure rateOfClosure = RateOfClosure.getRateOfClosureOfEvent(connection, eventId);
            this.x = new int[rateOfClosure.getSize()];
            this.y = rateOfClosure.getRateOfClosureArray();
            for(int i = 0; i < rateOfClosure.getSize();i++){
                x[i] = i;
            }
        }
    }
    @Override
    public Object handle(Request request, Response response) {

        LOG.info("handling rate of closure route");

        int eventId = Integer.parseInt(request.queryParams("eventId"));

        try {
            RateOfClosureData rocData = new RateOfClosureData(eventId);
            String output = gson.toJson(rocData);
            return output;
        } catch (Exception e){
            e.printStackTrace();
            return gson.toJson(new ErrorResponse(e));
        }


    }
}
