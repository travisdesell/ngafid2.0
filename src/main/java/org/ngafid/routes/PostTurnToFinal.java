package org.ngafid.routes;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import org.ngafid.Database;
import org.ngafid.WebServer;
import org.ngafid.accounts.User;
import org.ngafid.flights.TurnToFinal;
import spark.*;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class PostTurnToFinal implements Route {
    private static final Logger LOG = Logger.getLogger(PostCreateAccount.class.getName());

    private final Gson gson;

    public PostTurnToFinal(Gson gson) {
        this.gson = gson;
    }

    @Override
    public Object handle(Request request, Response response) throws Exception {
        String flightIdS = request.queryParams("flightId");
        int flightId;
        try {
            flightId = Integer.parseInt(flightIdS);
        } catch (NumberFormatException nfe) {
            // Invalid flight Id
            LOG.warning("GetTurnToFinal request supplied an invalid flight id: '" + flightIdS + "'");
            return  gson.toJson(new ErrorResponse("Invalid Flight Id",
                        "A turn to final request was made for an invalid flight Id"));
        }

        String resultString = "";

        final Session session = request.session();
        User user = session.attribute("user");

        if (!user.hasFlightAccess(Database.getConnection(), flightId)) {
            LOG.severe("INVALID ACCESS: user did not have access to this flight, id = " + flightIdS);
            Spark.halt(401, "User did not have access to flight with id = " + flightIdS);
            return null;
        }

        try {
            List<JsonElement> ttfs = TurnToFinal.getTurnToFinal(Database.getConnection(), flightId)
                    .stream()
                    .map(TurnToFinal::jsonify)
                    .collect(Collectors.toList());
            // This should be a list of js objects. each object represents 1 turn to final, containing the
            // indices associated with exceedences
            return gson.toJson(ttfs);
        } catch (SQLException e) {
            LOG.severe(e.toString());
            return gson.toJson(new ErrorResponse(e));
        }
    }
}
