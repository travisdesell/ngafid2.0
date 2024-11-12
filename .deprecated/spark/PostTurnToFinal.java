package org.ngafid.routes.spark;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import java.sql.SQLIntegrityConstraintViolationException;
import org.ngafid.Database;
import org.ngafid.airports.Airport;
import org.ngafid.airports.Airports;
import org.ngafid.flights.Flight;
import org.ngafid.flights.calculations.TurnToFinal;
import org.ngafid.routes.ErrorResponse;
import spark.*;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class PostTurnToFinal implements Route {
    private static final Logger LOG = Logger.getLogger(PostTurnToFinal.class.getName());

    private final Gson gson;

    public PostTurnToFinal(Gson gson) {
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        LOG.info("post " + PostTurnToFinal.class.getName() + " initialized");
    }

    @Override
    public Object handle(Request request, Response response) throws Exception {
        String startDate = request.formParams("startDate");
        String endDate = request.formParams("endDate");
        String airportIataCode = request.formParams("airport");
        System.out.println(startDate);
        System.out.println(endDate);

        final Session session = request.session();

        List<JsonElement> _ttfs = new ArrayList<>();
        Set<String> iataCodes = new HashSet<>();
        try (Connection connection = Database.getConnection()) {

            // TODO: Update this limit when caching of TTF objects is implemented.
            List<Flight> flights = Flight.getFlightsWithinDateRangeFromAirport(connection, startDate,
                    endDate, airportIataCode, 10000);

            for (Flight flight : flights) {
                try {
                    for (TurnToFinal ttf : TurnToFinal.getTurnToFinal(connection, flight,
                            airportIataCode)) {
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
                "ttfs", _ttfs));
    }
}
