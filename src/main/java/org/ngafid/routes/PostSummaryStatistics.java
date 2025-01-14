package org.ngafid.routes;


import com.google.gson.Gson;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.logging.Logger;
import org.ngafid.Database;
import org.ngafid.accounts.*;
import org.ngafid.accounts.User;
import org.ngafid.events.EventStatistics;
import org.ngafid.flights.*;
import spark.Request;
import spark.Response;
import spark.Route;
import spark.Session;
import spark.Spark;

public class PostSummaryStatistics implements Route {
    private static final Logger LOG = Logger.getLogger(PostSummaryStatistics.class.getName());

    private Gson gson;
    private final boolean aggregate;

    static record SummaryStatistics(
        boolean aggregate,

        int numberFlights,
        int numberAircraft,
        int yearNumberFlights,
        int monthNumberFlights,
        int totalEvents,
        int yearEvents,
        int monthEvents,
        int numberUsers,

        // These should be null if `aggregate` is false.
        Integer numberFleets,

        // Null if aggregate is true
        Integer uploads,
        Integer uploadsNotImported,
        Integer uploadsWithError,
        Integer flightsWithWarning,
        Integer flightsWithError,

        long flightTime,
        long yearFlightTime,
        long monthFlightTime
    ) {}


    public PostSummaryStatistics(Gson gson, boolean aggregate) {
        this.gson = gson;
        this.aggregate = aggregate;

        LOG.info("post " + this.getClass().getName() + " initalized");
    }

    @Override
    public Object handle(Request request, Response response) {
        LOG.info("handling " + this.getClass().getName() + " route");

        final Session session = request.session();
        User user = session.attribute("user");
        int fleetId = user.getFleetId();

        if (!user.hasViewAccess(fleetId)) {
            LOG.severe("INVALID ACCESS: user did not have access to view events for this fleet.");
            Spark.halt(401, "User did not have access to view events for this fleet.");
            return null;
        }

        //check to see if the user has access to view aggregate information
        if (aggregate && !user.hasAggregateView()) {
            LOG.severe("INVALID ACCESS: user did not have aggregate access to view aggregate dashboard.");
            Spark.halt(401, "User did not have aggregate access to view aggregate dashboard.");
            return null;
        }

        LocalDate firstOfMonth = LocalDate.now().with( TemporalAdjusters.firstDayOfMonth() );
        LocalDate firstOfYear = LocalDate.now().with( TemporalAdjusters.firstDayOfYear() );
        LocalDate lastThirtyDays = LocalDate.now().minusDays(30);

        String lastThirtyDaysQuery = "start_time >= '" + lastThirtyDays.toString() + "'";
        String yearQuery = "start_time >= '" + firstOfYear.toString() + "'";

        try {
            if (aggregate)
                fleetId = -1;

            Connection connection = Database.getConnection();

            var numberFlights =         Flight.getNumFlights(connection, fleetId);
            var numberAircraft =        Tails.getNumberTails(connection, fleetId);
            var flightTime =            Flight.getTotalFlightTime(connection, fleetId, null);
            var yearNumberFlights =     Flight.getNumFlights(connection, yearQuery, fleetId);
            var yearFlightTime =        Flight.getTotalFlightTime(connection, yearQuery, fleetId);
            var monthNumberFlights =    Flight.getNumFlights(connection, lastThirtyDaysQuery, fleetId);
            var monthFlightTime =       Flight.getTotalFlightTime(connection, lastThirtyDaysQuery, fleetId);

            var totalEvents =           EventStatistics.getEventCount(connection, fleetId, null, null);
            var yearEvents =            EventStatistics.getEventCount(connection, fleetId, firstOfYear, null);
            var monthEvents =           EventStatistics.getEventCount(connection, fleetId, firstOfMonth, null);

            var numberFleets =          aggregate ? Fleet.getNumberFleets(connection) : null;

            var numberUsers =           User.getNumberUsers(connection, fleetId);
            var uploads =               Upload.getNumUploads(connection, fleetId, "");
            var uploadsNotImported =    Upload.getNumUploads(connection, fleetId, " AND status = 'UPLOADED'");
            var uploadsWithError =      Upload.getNumUploads(connection, fleetId, " AND status = 'ERROR'");
            var flightsWithWarning =    FlightWarning.getCount(connection, fleetId);
            var flightsWithError =      FlightError.getCount(connection, fleetId);

            return gson.toJson(new SummaryStatistics(
                this.aggregate,
                numberFlights,
                numberAircraft,
                yearNumberFlights,
                monthNumberFlights,
                totalEvents,
                yearEvents,
                monthEvents,
                numberUsers,
                numberFleets,
                uploads,
                uploadsNotImported,
                uploadsWithError,
                flightsWithWarning,
                flightsWithError,
                flightTime,
                yearFlightTime,
                monthFlightTime
            ));
        } catch (SQLException e) {
            e.printStackTrace();
            return gson.toJson(new ErrorResponse(e));
        }
    }
}
