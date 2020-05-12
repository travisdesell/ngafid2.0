package org.ngafid.routes;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import org.ngafid.Database;
import org.ngafid.WebServer;
import org.ngafid.accounts.User;
import org.ngafid.flights.Flight;
import org.ngafid.flights.TurnToFinal;
import spark.*;

import javax.xml.crypto.Data;
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
        String startDate = request.queryParams("startDate");
        String endDate = request.queryParams("endDate");


        final Session session = request.session();
        User user = session.attribute("user");

        List<JsonElement> ttfs = new ArrayList<>();

        List<Flight> flights = Flight.getFlightsWithinDateRange(Database.getConnection(), startDate, endDate);

        for (Flight flight : flights) {
            int flightId = flight.getId();

            try {
                TurnToFinal.getTurnToFinal(Database.getConnection(), flightId)
                        .stream()
                        .map(TurnToFinal::jsonify)
                        .forEach(ttfs::add);
            } catch (SQLException e) {
                LOG.severe(e.toString());
                return gson.toJson(new ErrorResponse(e));
            }
        }
        return gson.toJson(ttfs);
    }
}
