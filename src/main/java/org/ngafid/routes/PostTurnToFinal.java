package org.ngafid.routes;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.ngafid.Database;
import org.ngafid.accounts.User;
import org.ngafid.airports.Airport;
import org.ngafid.airports.Airports;
import org.ngafid.flights.Flight;
import org.ngafid.flights.calculations.TurnToFinal;
import spark.*;

public class PostTurnToFinal implements Route {
    private static final Logger LOG = Logger.getLogger(PostTurnToFinal.class.getName());

    private final Gson gson;

    public PostTurnToFinal(Gson gson) {
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        LOG.info("post " + PostTurnToFinal.class.getName() + " initialized");
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
            } catch (SQLIntegrityConstraintViolationException e) {
                e.printStackTrace();
                return gson.toJson(new ErrorResponse(e));
            } catch (SQLException e) {
                e.printStackTrace();
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

        response.type("application/json");
        response.status(200);
        return gson.toJson(Map.of(
            "airports", airports,
            "ttfs", _ttfs
        ));
    }
}
