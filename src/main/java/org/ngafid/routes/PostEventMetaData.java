package org.ngafid.routes;

import java.sql.Connection;
import java.util.List;
import java.util.logging.Logger;

import org.ngafid.Database;
import org.ngafid.events.EventMetaData;

import com.google.gson.Gson;
import spark.Route;
import spark.Request;
import spark.Response;

/**
 * PostEventMetaData
 */
public class PostEventMetaData implements Route {

    private static final Logger LOG = Logger.getLogger(PostEventMetaData.class.getName());

    private Gson gson;

    public PostEventMetaData(Gson gson) {
        this.gson = gson;
    }

    @Override
    public Object handle(Request request, Response response) {

        LOG.info("handling rate of closure route");
        int eventId = Integer.parseInt(request.queryParams("eventId"));
        try {
            Connection connection = Database.getConnection();
            List<EventMetaData> metaDataList = EventMetaData.getEventMetaData(connection, eventId);
            if (metaDataList.size() > 0) {
                return gson.toJson(metaDataList);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return gson.toJson(new ErrorResponse(e));
        }
        return gson.toJson(null);
    }
}
