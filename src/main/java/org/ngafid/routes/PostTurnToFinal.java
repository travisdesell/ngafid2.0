package org.ngafid.routes;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import org.ngafid.Database;
import org.ngafid.WebServer;
import org.ngafid.accounts.User;
import org.ngafid.airports.Airport;
import org.ngafid.airports.Airports;
import org.ngafid.flights.Flight;
import org.ngafid.flights.TurnToFinal;
import spark.*;

import javax.xml.crypto.Data;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class PostTurnToFinal implements Route {
    private static final Logger LOG = Logger.getLogger(PostCreateAccount.class.getName());

    private final Gson gson;

    public PostTurnToFinal(Gson gson) {
        this.gson = new GsonBuilder().setPrettyPrinting().create();
    }

    @Override
    public Object handle(Request request, Response response) throws Exception {
        String startDate = request.queryParams("startDate");
        String endDate = request.queryParams("endDate");
        String airportIataCode = request.queryParams("airport");
        System.out.println(startDate);
        System.out.println(endDate);

        final Session session = request.session();
        User user = session.attribute("user");

        List<JsonElement> _ttfs = new ArrayList<>();

        List<Flight> flights =
                Flight.getFlightsWithinDateRangeFromAirport(Database.getConnection(), startDate, endDate, airportIataCode, 0);
        Set<String> iataCodes = new HashSet<>();

        for (Flight flight : flights) {
            int flightId = flight.getId();

            try {
                for (TurnToFinal ttf : TurnToFinal.getTurnToFinal(Database.getConnection(), flightId, airportIataCode)) {
                    JsonElement jsonElement = ttf.jsonify();
                    if (jsonElement != null) {
                        _ttfs.add(jsonElement);
                        iataCodes.add(ttf.airportIataCode);
                    }
                }
            } catch (SQLException e) {
                LOG.severe(e.toString());
                return gson.toJson(new ErrorResponse(e));
            }
            // break;
        }

        System.out.println("hiii");

        List<String> iataCodesList = new ArrayList<>(iataCodes.size());
        iataCodesList.addAll(iataCodes);

        for (Airport ap : Airports.getAirports(iataCodesList).values()) {
            System.out.println("long = " + ap.longitude + ", lat = " + ap.latitude);
        }


        Map<String, JsonElement> airports = Airports.getAirports(iataCodesList)
                .entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().jsonify(gson)));

        String json = gson.toJson(Map.of(
            "airports", airports,
            "ttfs", _ttfs
        ));
        System.out.println("What the fu");
        return json;
    }
}
