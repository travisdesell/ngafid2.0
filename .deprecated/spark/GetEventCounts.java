package org.ngafid.routes.spark;

import java.util.logging.Logger;

import com.google.gson.Gson;

import spark.Route;
import spark.Request;
import spark.Response;

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
        return null;
        // try {
        // Connection connection = Database.getConnection();

        // final Session session = request.session();
        // User user = session.attribute("user");
        // int fleetId = user.getFleetId();

        // String airframe = request.formParams("airframe_id");
        // int airframeId = airframe == null ? -1 : Integer.parseInt(airframe);

        // boolean aggregate = Boolean.parseBoolean(request.formParams("aggregate"));

        // String startDate = request.formParams("start_date");
        // String endDate = request.formParams("end_date");

        // // Map<Integer, EventStatistics.EventCount> eventCounts =
        // EventStatistics.getEventCountsFast(connection, aggregate ? -1 : fleetId,
        // airframeId, startDate, endDate);
        // // Map<String, Integer> aggregatedCounts = new HashMap<>();

        // // for (EventStatistics.EventCount ec : eventCounts.values()) {
        // // if (airframeId != -1 && airframeId ==
        // ec.eventDefinition.getAirframeNameId())
        // // aggregatedCounts.compute(ec.eventDefinition.getName(), (k, v) -> v == null
        // ? ec.count : v + ec.count);
        // // }

        // // return gson.toJson(aggregatedCounts);
        // return null;

        // } catch (SQLException e) {
        // LOG.severe(e.toString());
        // return gson.toJson(new ErrorResponse(e));
        // }
    }
}
