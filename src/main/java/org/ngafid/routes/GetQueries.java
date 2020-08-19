package org.ngafid.routes;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.logging.Logger;

import java.sql.Connection;
import java.sql.SQLException;

import com.google.gson.Gson;

import org.ngafid.accounts.Fleet;
import spark.Route;
import spark.Request;
import spark.Response;
import spark.Session;


import org.ngafid.Database;
import org.ngafid.accounts.User;

// A route to fetch all saved queries for a given group (site, user, or fleet specific)
public class GetQueries implements Route {
    private static final Logger LOG = Logger.getLogger(GetFlights.class.getName());
    private Gson gson;

    public GetQueries(Gson gson) {
        this.gson = gson;

        LOG.info("post " + this.getClass().getName() + " initalized");
    }

    @Override
    public Object handle(Request request, Response response) {
        LOG.info("handling " + this.getClass().getName() + " route");

        try {
            final Session session = request.session();
            User user = session.attribute("user");
            int fleetID = request.attribute("fleetID");                    // get fleet ID from submissionData (request?)
            Connection connection = Database.getConnection();
            // prepare query
            //String queryString = "SELECT id, fleet_id, uploader_id, upload_id, system_id, airframe_id, start_time, end_time, filename, md5_hash, number_rows, status, has_coords, has_agl, insert_completed FROM flights WHERE fleet_id = ? AND (" + filter.toQueryString(fleetId, parameters) + ")";


            //PreparedStatement query = connection.prepareStatement(queryString);
            //query.setInt(1, fleetID);
            //ResultSet resultSet = query.executeQuery();

            // check if querying by user
            if (fleetID == -1) {

            } else {

            }

            Fleet fleet = Fleet.get(connection, fleetID);

            String resultString = user.getId() + "," + fleetID + "," + fleet.getName() + "," + user.getFleetAccessType();

            return resultString;

        } catch (SQLException e) {
            LOG.severe(e.toString());
            return gson.toJson(new ErrorResponse(e));
        }
    }
}
