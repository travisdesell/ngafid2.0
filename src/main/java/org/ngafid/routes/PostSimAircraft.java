package org.ngafid.routes;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.Optional;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.google.gson.Gson;

import spark.Route;
import spark.Request;
import spark.Response;
import spark.Session;
import spark.Spark;

import org.ngafid.Database;
import org.ngafid.WebServer;
import org.ngafid.common.FlightTag;
import org.ngafid.common.FlightPaginator;
import org.ngafid.common.Page;
import org.ngafid.accounts.User;
import org.ngafid.events.Event;
import org.ngafid.events.EventDefinition;
import org.ngafid.flights.DoubleTimeSeries;
import org.ngafid.flights.Flight;

public class PostSimAircraft implements Route {
    private static final Logger LOG = Logger.getLogger(PostSimAircraft.class.getName());
    private Gson gson;

    public PostSimAircraft(Gson gson) {
        this.gson = gson;
        LOG.info("initialized " + this.getClass().getName() + " route!");
    }

    @Override
    public Object handle(Request request, Response response) {
        LOG.info("handling " + this.getClass().getName() + " route!");

        final Session session = request.session();
        User user = session.attribute("user");

		String type = request.queryParams("TYPE");
		int fleetId = user.getFleetId();

        try {
            Connection connection = Database.getConnection();

			switch (type) {
				case "INIT":
					List<String> acft = Flight.getSimAircraft(connection, fleetId);
					return gson.toJson(acft);
				default:
					return gson.toJson(type);
			}

        } catch (SQLException e) {
            System.err.println("Error in SQL ");
            e.printStackTrace();
            return gson.toJson(new ErrorResponse(e));
        }
    }
}
