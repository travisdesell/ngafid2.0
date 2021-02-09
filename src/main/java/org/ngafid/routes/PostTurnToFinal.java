package org.ngafid.routes;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mysql.jdbc.exceptions.jdbc4.MySQLIntegrityConstraintViolationException;
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
    private static final Logger LOG = Logger.getLogger(PostTurnToFinal.class.getName());

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

        // TODO: Update this limit when caching of TTF objects is implemented.
        List<Flight> flights =
                Flight.getFlightsWithinDateRangeFromAirport(Database.getConnection(), startDate, endDate, airportIataCode, 10000);
        Set<String> iataCodes = new HashSet<>();

        for (Flight flight : flights) {
            try {
                for (TurnToFinal ttf : TurnToFinal.getTurnToFinal(Database.getConnection(), flight, airportIataCode)) {
                    JsonElement jsonElement = ttf.jsonify();
                    if (jsonElement != null) {
                        _ttfs.add(jsonElement);
                        iataCodes.add(ttf.airportIataCode);
                    }
                }
            } catch (MySQLIntegrityConstraintViolationException e) {
                e.printStackTrace();
                return gson.toJson(new ErrorResponse(e));
            } catch (SQLException e) {
                LOG.severe(e.toString());
                return gson.toJson(new ErrorResponse(e));
            }
        }


        List<String> iataCodesList = new ArrayList<>(iataCodes.size());
        iataCodesList.addAll(iataCodes);

        for (Airport ap : Airports.getAirports(iataCodesList).values()) {
            System.out.println("long = " + ap.longitude + ", lat = " + ap.latitude);
        }


        Map<String, JsonElement> airports = Airports.getAirports(iataCodesList)
                .entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().jsonify(gson)));

        return gson.toJson(Map.of(
            "airports", airports,
            "ttfs", _ttfs
        ));
    }
}
