package org.ngafid.routes;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Map;
import java.util.logging.Logger;

import org.ngafid.Database;
import org.ngafid.accounts.User;
import org.ngafid.events.EventStatistics;
import org.ngafid.events.EventStatisticsFetch.CacheObject;

import com.google.gson.Gson;

import spark.Request;
import spark.Response;
import spark.Route;
import spark.Session;

/**
 * POST route for retrieving event statistics (preferrably from the JSON cache).
 */

@SuppressWarnings("LoggerStringConcat")
public class PostEventStatistics implements Route {

    private static final Logger LOG = Logger.getLogger(PostEventStatistics.class.getName());
    private final Gson gson;

    public PostEventStatistics(Gson gson) {
        this.gson = gson;
        LOG.info("Post " + this.getClass().getName() + " initialized");
    }

    @Override
    public Object handle(Request request, Response response) {

        LOG.info("Handling " + this.getClass().getName());

        final Session session = request.session();
        User user = session.attribute("user");

        //No user found...
        if (user == null) {
            LOG.severe("No user found in session!");
            return null;
        }

        int fleetId = user.getFleetId();
        int airframeNameId = Integer.parseInt(request.queryParams("airframeNameId"));
        String airframeName = request.queryParams("airframeName");

        try {

            //Load the entire map from the JSON cache
            Map<Integer, CacheObject> fleetDataMap = EventStatistics.importAllFleetsCache();
            if (fleetDataMap == null || fleetDataMap.isEmpty()) {
                LOG.warning("Cache is null or empty; fallback to DB fetch for fleetId=" + fleetId);
                return fetchFromDatabase(fleetId, airframeNameId, airframeName);
            }

            //Grab the CacheObject for the user’s fleetId
            CacheObject thisFleetCache = fleetDataMap.get(fleetId);
            if (thisFleetCache == null) {
                LOG.warning("No cache entry found for fleetId=" + fleetId + "; fallback to DB fetch.");
                return fetchFromDatabase(fleetId, airframeNameId, airframeName);
            }

            //Attempt to find the matching event stats in the array
            ArrayList<EventStatistics> statsList = thisFleetCache.getStatsList();
            if (statsList == null || statsList.isEmpty()) {
                LOG.warning("Fleet " + fleetId + " has an empty statsList in cache; fallback to DB.");
                return fetchFromDatabase(fleetId, airframeNameId, airframeName);
            }

            EventStatistics matched = null;
            for (EventStatistics eventStat : statsList) {

                if (eventStat.getAirframeNameId() == airframeNameId && airframeName.equals(eventStat.getAirframeName())) {
                    matched = eventStat;
                    break;
                }

            }

            if (matched == null) {

                LOG.warning(
                    "Fleet " + fleetId + ": no matching EventStatistics for "
                    + "airframeNameId=" + airframeNameId + ", "
                    + "airframeName=" + airframeName + ". "
                    + "Fallback to DB."
                );

                return fetchFromDatabase(fleetId, airframeNameId, airframeName);

            }

            // Found the stats in the cache, return them
            LOG.info(
                "Returning cached data for "
                + "fleetId=" + fleetId
                + ", airframeNameId=" + airframeNameId
                + ", airframeName=" + airframeName
            );

            return gson.toJson(matched);

        } catch (Exception e) {

            //Revert to DB fetch
            LOG.severe("Error handling event statistics from cache: " + e.getMessage());
            e.printStackTrace();
            return fetchFromDatabase(fleetId, airframeNameId, airframeName);

        }

    }

    private Object fetchFromDatabase(int fleetId, int airframeNameId, String airframeName) {

        LOG.info(
            "Fetching data from DB as a fallback for "
            + "fleetId=" + fleetId + ", "
            + "airframeNameId=" + airframeNameId + ", "
            + "airframeName=" + airframeName
        );

        try {

            Connection connection = Database.getConnection();
            EventStatistics freshStats = new EventStatistics(connection, airframeNameId, airframeName, fleetId);
            return gson.toJson(freshStats);

        } catch (SQLException e) {

            LOG.severe("SQL error: " + e);
            return gson.toJson(new ErrorResponse(e));

        }
        
    }

}