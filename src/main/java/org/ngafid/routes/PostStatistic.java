package org.ngafid.routes;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;

import java.util.Map;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.HashMap;
import java.util.function.Function;

import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

import java.sql.Connection;
import java.sql.SQLException;

import com.google.gson.Gson;

import spark.Route;
import spark.Request;
import spark.Response;
import spark.Session;
import spark.Spark;

import org.ngafid.accounts.User;
import org.ngafid.events.EventStatistics;
import org.ngafid.Database;
import org.ngafid.flights.*;
import org.ngafid.accounts.*;

public class PostStatistic implements Route {
    private static final Logger LOG = Logger.getLogger(PostStatistic.class.getName());

    private Gson gson;
    private final boolean aggregate;

    public static class StatFetcher {
        public interface StatFunction<T> {
            T execute(StatFetcher f) throws SQLException;
        }

        public static Map<String, StatFunction<Object>> function_map = Map.ofEntries(
            Map.entry("flightTime", StatFetcher::flightTime),
            Map.entry("yearFlightTime", StatFetcher::yearFlightTime),
            Map.entry("monthFlightTime", StatFetcher::monthFlightTime),
            
            Map.entry("numberFlights", StatFetcher::numberFlights),
            Map.entry("numberAircraft", StatFetcher::numberAircraft),
            Map.entry("yearNumberFlights", StatFetcher::yearNumberFlights),
            Map.entry("monthNumberFlights", StatFetcher::monthNumberFlights),
            Map.entry("totalEvents", StatFetcher::totalEvents),
            Map.entry("yearEvents", StatFetcher::yearEvents),
            Map.entry("monthEvents", StatFetcher::monthEvents),
            Map.entry("numberFleets", StatFetcher::numberFleets),
            Map.entry("numberUsers", StatFetcher::numberUsers),
            Map.entry("uploads", StatFetcher::uploads),
            Map.entry("uploadsNotImported", StatFetcher::uploadsNotImported),
            Map.entry("uploadsWithError", StatFetcher::uploadsWithError),
            Map.entry("flightsWithWarning", StatFetcher::flightsWithWarning),
            Map.entry("flightsWithError", StatFetcher::flightsWithError)
        );

        final Connection connection;
        final User user;
        final int fleetId;
        final boolean aggregate;

        public StatFetcher(Connection connection, User user, boolean aggregate) {
            this.connection = connection;
            this.user = user;

            if (aggregate) { 
                this.fleetId = -1;
            } else {
                this.fleetId = user.getFleetId();
            }

            this.aggregate = aggregate;
        }

        boolean aggregate() { return this.fleetId > 0; }

        LocalDate thirtyDaysAgo() { return LocalDate.now().minusDays(30); }
        LocalDate firstOfMonth() { return LocalDate.now().with(TemporalAdjusters.firstDayOfMonth()); }
        LocalDate firstOfYear() { return LocalDate.now().with(TemporalAdjusters.firstDayOfYear()); }
        
        String lastThirtyDaysQuery() { return "start_time >= '" + thirtyDaysAgo().toString() + "'"; }
        String yearQuery() { return "start_time >= '" + firstOfYear().toString() + "'"; }
        
        Long flightTime() throws SQLException { return Flight.getTotalFlightTime(connection, fleetId, null); }
        Long yearFlightTime() throws SQLException { return Flight.getTotalFlightTime(connection, yearQuery(), fleetId); }
        Long monthFlightTime() throws SQLException { return Flight.getTotalFlightTime(connection, lastThirtyDaysQuery(), fleetId); }

        Integer numberFlights() throws SQLException { return Flight.getNumFlights(connection, fleetId); }
        Integer numberAircraft() throws SQLException { return Tails.getNumberTails(connection, fleetId); }
        Integer yearNumberFlights() throws SQLException { return Flight.getNumFlights(connection, yearQuery(), fleetId); }
        Integer monthNumberFlights() throws SQLException { return Flight.getNumFlights(connection, lastThirtyDaysQuery(), fleetId); }
        Integer totalEvents() throws SQLException { return EventStatistics.getEventCount(connection, fleetId, null, null); }
        Integer yearEvents() throws SQLException { return EventStatistics.getEventCount(connection, fleetId, firstOfYear(), null); }
        Integer monthEvents() throws SQLException { return EventStatistics.getEventCount(connection, fleetId, firstOfMonth(), null); }
        Integer numberFleets() throws SQLException { return aggregate ? Fleet.getNumberFleets(connection) : null; }
        Integer numberUsers() throws SQLException { return User.getNumberUsers(connection, fleetId); }
        Integer uploads() throws SQLException { return Upload.getNumUploads(connection, fleetId, ""); }
        Integer uploadsNotImported() throws SQLException { return Upload.getNumUploads(connection, fleetId, " AND status = 'UPLOADED'"); }
        Integer uploadsWithError() throws SQLException { return Upload.getNumUploads(connection, fleetId, " AND status = 'ERROR'"); }
        Integer flightsWithWarning() throws SQLException { return FlightWarning.getCount(connection, fleetId); }
        Integer flightsWithError() throws SQLException { return FlightError.getCount(connection, fleetId); }
    }


    public PostStatistic(Gson gson, boolean aggregate) {
        this.gson = gson;
        this.aggregate = aggregate;

        LOG.info("post " + this.getClass().getName() + " initalized");
    }

    private static class StatRequest {
        String[] statistics;
    }

    @Override
    public Object handle(Request request, Response response) {
        LOG.info("handling " + this.getClass().getName() + " route");

        final Session session = request.session();
        User user = session.attribute("user");
        int fleetId = user.getFleetId();


        try {
            Connection connection = Database.getConnection();
            StatFetcher fetcher = new StatFetcher(connection, user, aggregate);
            
            String[] stats;
            
            if (request.splat().length > 0) {
                stats = request.splat();
            } else {
                stats = gson.fromJson(request.body(), String[].class);
            }

            HashMap<String, Object> statistics = new HashMap<>();

            for (int i = 0; i < stats.length; i++) {
                LOG.info("Computing " + stats[i]);
                statistics.put(stats[i], StatFetcher.function_map.get(stats[i]).execute(fetcher));
            }
            
            return gson.toJson(statistics);

        } catch (SQLException e) {
            e.printStackTrace();
            return gson.toJson(new ErrorResponse(e));
        }

        
    }
}
