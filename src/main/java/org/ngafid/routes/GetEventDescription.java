package org.ngafid.routes;

import com.google.gson.Gson;
import org.ngafid.Database;
import org.ngafid.events.EventDefinition;
import org.ngafid.flights.Airframes;
import spark.Request;
import spark.Response;
import spark.Route;
import spark.Session;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.logging.Logger;

public class GetEventDescription implements Route {
    private static final Logger LOG = Logger.getLogger(GetEventDescription.class.getName());
    private Gson gson;

    public GetEventDescription(Gson gson) {
        this.gson = gson;

        LOG.info("post " + this.getClass().getName() + " initialized");
    }

    @Override
    public Object handle(Request request, Response response) throws Exception {
        String expectedName = request.queryParams("eventName");
        LOG.info("expectedName: " + expectedName);

        String query = "SELECT id, fleet_id, name, start_buffer, stop_buffer, airframe_id, condition_json, column_names, severity_column_names, severity_type FROM event_definitions WHERE event_definitions.name = " + "\"" + expectedName + "\"";
        if (request.queryParams("airframe_id") != null) { // If airframe ID is not null, filter for specific airframe + shared descriptions
            query += " AND airframe_id = " + request.queryParams("airframe") + " OR airframe_id = 0";
        }

        LOG.info("query: " + query);

        PreparedStatement preparedStatement = Database.getConnection().prepareStatement(query);
        LOG.info("preparedStatement: " + preparedStatement);

        ResultSet resultSet = preparedStatement.executeQuery();
        LOG.info("resultSet: " + resultSet);

        // Get airframe_id
        if (resultSet.getInt("airframe_id") == 0) {
            EventDefinition eventDefinition = new EventDefinition(resultSet);
            String text = eventDefinition.toHumanReadable();

            LOG.info("text: " + text);
            return gson.toJson(text);
        } else {
            StringBuilder textBuilder = new StringBuilder();
            EventDefinition eventDefinition = new EventDefinition(resultSet);
            String airframeName = Airframes.getAirframeName(preparedStatement.getConnection(), resultSet.getInt("airframe_id"));

            while (!resultSet.next()) {

                textBuilder.append("\n");
                textBuilder.append(eventDefinition.toHumanReadable());
            }

        }





    }
}
