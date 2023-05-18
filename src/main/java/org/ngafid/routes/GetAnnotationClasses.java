/**
 * Gets event annotation classes
 * @author <a href=mailto:apl1341@cs.rit.edu>Aidan LaBella</a>
 */
package org.ngafid.routes;

import java.util.logging.Logger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.google.gson.Gson;

import spark.Route;
import spark.Request;
import spark.Response;
import spark.Session;
import spark.Spark;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.ngafid.Database;
import org.ngafid.accounts.User;
import org.ngafid.flights.Flight;

public class GetAnnotationClasses implements Route {
    private static final Logger LOG = Logger.getLogger(GetAnnotationClasses.class.getName());
    private static Connection connection = Database.getConnection();
    private Gson gson;

    /**
     * Constructor
     * @param gson the gson object for JSON conversions
     */
    public GetAnnotationClasses(Gson gson) {
        this.gson = gson;

        LOG.info("GET " + this.getClass().getName() + " initalized");
    }

    public static Map<Integer, String> getAnnotationClasses(int fleetId) throws SQLException {
        String queryString = "SELECT id, name FROM loci_event_classes WHERE fleet_id = ?";
        PreparedStatement query = connection.prepareStatement(queryString);

        query.setInt(1, fleetId);

        ResultSet resultSet = query.executeQuery();

        Map<Integer, String> classNames = new HashMap<>();

        while (resultSet.next()) {
            classNames.put(resultSet.getInt(1), resultSet.getString(2));
        }

        return classNames;
    }

    /**
     * {inheritDoc}
     */
    @Override
    public Object handle(Request request, Response response) {
        LOG.info("handling " + this.getClass().getName() + " route");

        final Session session = request.session();
        User user = session.attribute("user");
        int fleetId = user.getFleetId();

        if (!user.hasViewAccess(fleetId)) {
            LOG.severe("INVALID ACCESS: user did not have view access this fleet.");
            Spark.halt(401, "User did not have access to view acces for this fleet.");
            return null;
        }

        try {
            Map<Integer, String> classes = getAnnotationClasses(fleetId);
            return gson.toJson(classes);
        } catch(SQLException e) {
            return gson.toJson(new ErrorResponse(e));
        }
    }
}
