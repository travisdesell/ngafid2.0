package org.ngafid.routes;

import com.google.gson.Gson;
import org.ngafid.Database;
import org.ngafid.events.EventDefinition;
import spark.Request;
import spark.Response;
import spark.Route;

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
        String expectedName = request.params("name");

        String query = "SELECT id, fleet_id, name, start_buffer, stop_buffer, airframe_id, condition_json, column_names, severity_column_names, severity_type FROM event_definitions WHERE name = " + expectedName;
        Logger.log("query: " + query);

        PreparedStatement preparedStatement = Database.getConnection().prepareStatement(query);
        ResultSet resultSet = preparedStatement.executeQuery();
        EventDefinition eventDefinition = new EventDefinition(resultSet);

        String text = eventDefinition.toHumanReadable();


        return gson.toJson(text);
    }
}
