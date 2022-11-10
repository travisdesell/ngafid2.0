package org.ngafid.routes;

import com.google.gson.Gson;
import org.ngafid.Database;
import org.ngafid.events.EventDefinition;
import spark.Request;
import spark.Response;
import spark.Route;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public class GetAllEventDescriptions implements Route {
    private static final Logger LOG = Logger.getLogger(GetAllEventDescriptions.class.getName());
    private Gson gson;

    public GetAllEventDescriptions(Gson gson) {
        this.gson = gson;

        LOG.info("post " + this.getClass().getName() + " initialized");
    }

    @Override
    public Object handle(Request request, Response response) throws Exception {
        String expectedName = request.queryParams("eventName");
        LOG.info("expectedName: " + expectedName);

        String query = "SELECT event_definitions.id, fleet_id, name, start_buffer, stop_buffer, airframe_id, condition_json, column_names, severity_column_names, severity_type, airframe " +
                "FROM event_definitions INNER JOIN airframes ON event_definitions.airframe_id=airframes.id";
        LOG.info("query: " + query);

        PreparedStatement preparedStatement = Database.getConnection().prepareStatement(query);
        LOG.info("preparedStatement: " + preparedStatement);

        ResultSet resultSet = preparedStatement.executeQuery();
        LOG.info("resultSet: " + resultSet);
        Map<String, Map<Integer, String>> definitions = new HashMap<>();

        while (resultSet.next()) {
            EventDefinition eventDefinition = new EventDefinition(resultSet);
            LOG.info("eventDefinition: " + eventDefinition);

            String text = eventDefinition.toHumanReadable();
            LOG.info("text: " + text);

            if (!definitions.containsKey(eventDefinition.getName())) {
                definitions.put(eventDefinition.getName(), new HashMap<>());
            }

            definitions.get(eventDefinition.getName()).put(eventDefinition.getAirframeNameId(), eventDefinition.toHumanReadable());
        }

        return gson.toJson(definitions);
    }
}
