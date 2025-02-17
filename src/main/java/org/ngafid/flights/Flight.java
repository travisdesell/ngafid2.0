package org.ngafid.flights;

import org.ngafid.Database;
import org.ngafid.airports.Airport;
import org.ngafid.airports.Airports;
import org.ngafid.airports.Runway;
import org.ngafid.common.FlightTag;
import org.ngafid.common.MutableDouble;
import org.ngafid.filters.Filter;
import org.ngafid.flights.calculations.CalculatedDoubleTimeSeries;
import org.ngafid.flights.calculations.VSPDRegression;
import org.ngafid.flights.process.FlightMeta;
import org.ngafid.terrain.TerrainCache;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.NoSuchFileException;
import java.sql.*;
import java.util.*;
import java.util.logging.Logger;

import static org.ngafid.flights.Parameters.*;

/**
 * This class represents a Flight in the NGAFID. It also contains static methods
 * for database interaction
 *
 * @author <a href = tjdvse@rit.edu>Travis Desell @ RIT SE</a>
 * @author <a href = josh@mail.rit.edu>Josh Karns @ RIT SE</a>
 * @author <a href = fa3019@rit.edu>Farhad Akhbardeh @ RIT SE</a>
 * @author <a href = apl1341@cs.rit.edu>Aidan LaBella @ RIT CS</a>
 */

public class Flight {
    // can set various bitfields to track status of different flight update
    // events in the database (so we can recalculate or calculate new things
    // as needed):
    public static final long CHT_DIVERGENCE_CALCULATED = 0b1;
    // This flag will only ever be set for flights with BE-GPS-2200 airframe ID.
    public static final long NIFA_EVENTS_CALCULATED = 0b10;
    // private static final long NEXT_CALCULATION = 0b10;
    // private static final long NEXT_NEXT_CALCULATION = 0b100;
    // etc
    //
    public static final String WARNING = "WARNING";
    public static final String SUCCESS = "SUCCESS";
    public static final String ERROR = "ERROR";
    private static final Logger LOG = Logger.getLogger(Flight.class.getName());
    private static final double MAX_AIRPORT_DISTANCE_FT = 10000;
    private static final double MAX_RUNWAY_DISTANCE_FT = 100;
    private static final String FLIGHT_COLUMNS = "id, fleet_id, uploader_id, upload_id, system_id, " + "airframe_id, " +
            "airframe_type_id, start_time, end_time, filename, md5_hash, number_rows, " + "status, has_coords, " +
            "has_agl, insert_completed, processing_status";
    private static final String FLIGHT_COLUMNS_TAILS = "id, fleet_id, uploader_id, upload_id, f.system_id, " +
            "airframe_id, airframe_type_id, start_time, end_time, filename, md5_hash, number_rows, status," + " " +
            "has_coords, has_agl, insert_completed, processing_status";
    private final String filename;
    private final String systemId;
    private final String md5Hash;
    private final String startDateTime;
    private final String endDateTime;
    private final List<Itinerary> itinerary;
    // TODO: Roll a lot of this stuff up into some sort of meta-data object?
    private int id = -1;
    private int fleetId;
    private int uploaderId;
    private int uploadId;
    // Make / model of the aircraft
    private Airframes.Airframe airframe;
    // The "type" meaning a fixed wing, rotorcraft, etc.
    private Airframes.AirframeType airframeType;
    private String tailNumber;
    private String suggestedTailNumber;
    // these will be set to true if the flight has
    // latitude/longitude coordinates and can therefore
    // calculate AGL, airport and runway proximity
    // hasAGL also requires an altitudeMSL column
    private boolean hasCoords;
    private boolean hasAGL;
    private boolean insertCompleted = false;
    private long processingStatus = 0;
    private String status;
    private transient List<MalformedFlightFileException> exceptions = new ArrayList<>();
    private int numberRows;
    private List<String> dataTypes;
    private List<String> headers;
    // the tags associated with this flight
    private List<FlightTag> tags = null;
    private Map<String, DoubleTimeSeries> doubleTimeSeries = new HashMap<>();
    private Map<String, StringTimeSeries> stringTimeSeries = new HashMap<>();

    public Flight(FlightMeta meta, Map<String, DoubleTimeSeries> doubleTimeSeries,
                  Map<String, StringTimeSeries> stringTimeSeries, List<Itinerary> itinerary,
                  List<MalformedFlightFileException> exceptions) throws SQLException {
        fleetId = meta.fleetId;
        uploaderId = meta.uploaderId;
        uploadId = meta.uploadId;

        filename = meta.filename;

        airframe = meta.airframe;
        airframeType = meta.airframeType;

        systemId = meta.systemId;
        suggestedTailNumber = meta.suggestedTailNumber;
        md5Hash = meta.md5Hash;
        startDateTime = meta.startDateTime;
        endDateTime = meta.endDateTime;

        this.itinerary = itinerary;

        hasCoords = doubleTimeSeries.containsKey(LATITUDE) && doubleTimeSeries.containsKey(LONGITUDE);
        hasAGL = doubleTimeSeries.containsKey(ALT_AGL);

        this.exceptions = exceptions;
        checkExceptions();

        this.stringTimeSeries = Map.copyOf(stringTimeSeries);
        this.doubleTimeSeries = Map.copyOf(doubleTimeSeries);
    }

    public Flight(Connection connection, ResultSet resultSet) throws SQLException {
        LOG.info("Got flight w/ id = " + resultSet.getInt(1));
        id = resultSet.getInt(1);
        fleetId = resultSet.getInt(2);
        uploaderId = resultSet.getInt(3);
        uploadId = resultSet.getInt(4);

        systemId = resultSet.getString(5);

        airframe = new Airframes.Airframe(connection, resultSet.getInt(6));
        airframeType = new Airframes.AirframeType(connection, resultSet.getInt(7));

        // this will set tailNumber and tailConfirmed
        tailNumber = Tails.getTail(connection, fleetId, systemId);

        startDateTime = resultSet.getString(8);
        endDateTime = resultSet.getString(9);
        filename = resultSet.getString(10);
        md5Hash = resultSet.getString(11);
        numberRows = resultSet.getInt(12);
        status = resultSet.getString(13);
        hasCoords = resultSet.getBoolean(14);
        hasAGL = resultSet.getBoolean(15);
        insertCompleted = resultSet.getBoolean(16);
        processingStatus = resultSet.getLong(17);

        itinerary = Itinerary.getItinerary(connection, id);

        // Populate the tags
        this.tags = getTags(connection, id);
    }

    public static ArrayList<Flight> getFlightsFromUpload(Connection connection, int uploadId) throws SQLException {
        String queryString = "SELECT " + FLIGHT_COLUMNS + " FROM flights WHERE upload_id = " + uploadId;

        return getFlightsFromDb(connection, queryString);
    }

    public static ArrayList<Flight> getFlights(Connection connection, int fleetId) throws SQLException {
        return getFlights(connection, fleetId, 0);
    }

    /**
     * Worth noting - if any portion of the flight occurs between startDate and
     * endDate it will be grabbed - it doesn't
     * have to lie entirely within startDate and endDate. endDate is inclusive, as
     * is startDate.
     *
     * @param connection is the database connection
     * @param startDate  is the start date
     * @param endDate    is the end date
     * @return a list of flights where at least part of the flight occurs between
     */
    public static List<Flight> getFlightsWithinDateRange(Connection connection, String startDate, String endDate)
            throws SQLException {
        String extraCondition = "((start_time BETWEEN '" + startDate + "' AND '" + endDate + "') OR (end_time BETWEEN"
                + " '" + startDate + "' AND '" + endDate + "'))";
        return getFlights(connection, extraCondition);
    }

    /**
     * Like Flights.getFlightsWithinDateRange, but also only grabs flights that
     * visit a certain airport.
     *
     * @param connection      connection to the database
     * @param startDate       start date which must be formatted like this:
     *                        "yyyy-mm-dd"
     * @param endDate         formatted the same as the start date.
     * @param airportIataCode airport iata code
     * @param limit           limit the number of flights returned
     * @return a list of flights where at least part of the flight occurs between
     * the startDate and the endDate.
     * This list could be potentially huge if the date range is large so it
     * may be smart to not give the users
     * full control over this parameter on the frontend? We'll see.
     * @throws SQLException if there is an issue with the query
     *                      the startDate and the endDate. This list could be potentially huge if
     *                      the date range is large so it may be smart to not give the users full
     *                      control over this parameter on the frontend? We'll see.
     */
    public static List<Flight> getFlightsWithinDateRangeFromAirport(Connection connection, String startDate,
                                                                    String endDate, String airportIataCode, int limit) throws SQLException {
        //CHECKSTYLE:OFF
        String extraCondition = "    (                " +
                "    EXISTS(          " +
                "        SELECT       " +
                "          id         " +
                "        FROM         " +
                "          itinerary  " +
                "        WHERE        " +
                "          itinerary.flight_id = flights.id AND " +
                "          airport = '" + airportIataCode + "' " +
                "    ) " +
                "AND   " +
                "    ( " +
                "           (start_time BETWEEN '" + startDate + "' AND '" + endDate + "') " +
                "        OR (end_time   BETWEEN '" + startDate + "' AND '" + endDate + "')  " +
                "    )" +
                " ) ";
        //CHECKSTYLE:ON
        return getFlights(connection, extraCondition, limit);
    }

    public static ArrayList<Flight> getFlights(Connection connection, int fleetId, int limit) throws SQLException {
        String queryString = "SELECT " + FLIGHT_COLUMNS + " FROM flights WHERE fleet_id = " + fleetId;
        if (limit > 0) queryString += " LIMIT " + limit;

        return getFlightsFromDb(connection, queryString);
    }

    private static ArrayList<Flight> getFlightsFromDb(Connection connection, String queryString) throws SQLException {
        try (PreparedStatement query = connection.prepareStatement(queryString); ResultSet resultSet =
                query.executeQuery()) {

            ArrayList<Flight> flights = new ArrayList<>();
            while (resultSet.next()) {
                flights.add(new Flight(connection, resultSet));
            }

            return flights;
        }
    }

    public static ArrayList<Flight> getFlights(Connection connection, int fleetId, Filter filter) throws SQLException {
        return getFlights(connection, fleetId, filter, 0);
    }

    public static int getNumFlights(Connection connection, int flightId) throws SQLException {
        return getNumFlights(connection, flightId, null);
    }

    /**
     * Gets the total number of flights for a given fleet and filter. If the filter
     * is null it returns the number of flights for the fleet.
     *
     * @param connection is the database connection
     * @param fleetId    is the id of the fleet, <= 0 will select for all fleets
     * @param filter     the filter to select the flights, can be null.
     * @return the number of flights for the fleet, given the specified filter (or
     * no filter if the filter is null).
     */
    public static int getNumFlights(Connection connection, int fleetId, Filter filter) throws SQLException {
        ArrayList<Object> parameters = new ArrayList<Object>();

        String queryStr;
        if (fleetId <= 0) {
            if (filter != null) {
                queryStr = "SELECT count(id) FROM flights WHERE (" + filter.toQueryString(fleetId, parameters) + ")";
            } else {
                queryStr = "SELECT count(id) FROM flights";
            }
        } else {
            if (filter != null) {
                queryStr = "SELECT count(id) FROM flights WHERE fleet_id = ? AND (" + filter.toQueryString(fleetId,
                        parameters) + ")";
            } else {
                queryStr = "SELECT count(id) FROM flights WHERE fleet_id = ?";
            }
        }

        try (PreparedStatement query = connection.prepareStatement(queryStr)) {
            prepareFilterQuery(fleetId, filter, parameters, query);

            LOG.info(query.toString());

            try (ResultSet resultSet = query.executeQuery()) {
                resultSet.next();
                return resultSet.getInt(1);
            }
        }
    }

    private static void prepareFilterQuery(int fleetId, Filter filter, ArrayList<Object> parameters,
                                           PreparedStatement query) throws SQLException {
        if (fleetId > 0) query.setInt(1, fleetId);

        if (filter != null) {
            setQueryParameters(parameters, query);
        }
    }

    public static int getNumFlights(Connection connection, String queryString, int fleetId) throws SQLException {
        if (fleetId > 0) {
            queryString += " AND fleet_id = " + fleetId;
        }

        return getNumFlights(connection, queryString);
    }

    /**
     * Gets the total number of flights for a given fleet and queryString. This
     * method is private to ensure public access doesn't get used in a way that
     * accidentally exposes a SQL injection surface.
     *
     * @param connection  is the database connection
     * @param queryString is what gets put into the WHERE clause of the query
     * @return the number of flights for the fleet, given the specified queryString
     */
    private static int getNumFlights(Connection connection, String queryString) throws SQLException {
        String fullQueryString = "SELECT count(id) FROM flights WHERE (" + queryString + ")";
        LOG.info("getting number of flights with query string: '" + fullQueryString + "'");

        try (PreparedStatement query = connection.prepareStatement(fullQueryString); ResultSet resultSet =
                query.executeQuery()) {
            LOG.info(query.toString());

            resultSet.next();
            return resultSet.getInt(1);
        }
    }

    /**
     * Gets the total number of flight hours for a given fleet and filter. If the
     * filter is null it returns the number of flight hours for the fleet.
     *
     * @param connection is the database connection
     * @param fleetId    is the id of the fleet, if <= 0 it will be for the entire
     *                   NGAFID
     * @param filter     the filter to select the flights, can be null.
     * @return the number of flight hours for the fleet, given the specified filter
     * (or no filter if the filter is null).
     */
    public static long getTotalFlightTime(Connection connection, int fleetId, Filter filter) throws SQLException {
        ArrayList<Object> parameters = new ArrayList<Object>();

        String queryString;
        if (fleetId <= 0) {
            if (filter != null) {
                queryString =
                        "SELECT sum(TIMESTAMPDIFF(SECOND, start_time, end_time)) FROM flights WHERE (" +
                                filter.toQueryString(fleetId, parameters) + ")";
            } else {
                queryString = "SELECT sum(TIMESTAMPDIFF(SECOND, start_time, end_time)) FROM flights";
            }
        } else {
            if (filter != null) {
                queryString = "SELECT sum(TIMESTAMPDIFF(SECOND, start_time, end_time)) " + "FROM flights WHERE " +
                        "fleet_id = ? AND (" + filter.toQueryString(fleetId, parameters) + ")";
            } else {
                queryString = "SELECT sum(TIMESTAMPDIFF(SECOND, start_time, end_time)) FROM flights WHERE fleet_id = ?";
            }
        }

        try (PreparedStatement query = connection.prepareStatement(queryString)) {
            prepareFilterQuery(fleetId, filter, parameters, query);

            try (ResultSet resultSet = query.executeQuery()) {
                LOG.info(query.toString());

                resultSet.next();
                long diffSeconds = resultSet.getLong(1);
                System.out.println("total time is: " + diffSeconds);

                return diffSeconds;
            }
        }
    }

    public static long getTotalFlightTime(Connection connection, String queryString) throws SQLException {
        return getTotalFlightTime(connection, queryString, 0);
    }

    /**
     * Gets the total number of flight hours for a given fleet and WHERE clause
     * query string for the fleet.
     *
     * @param connection  is the database connection
     * @param queryString is the string to put into the query's WHERE clause
     * @param fleetId     is the id of the fleet, if <= 0 it will be for the entire NGAFID
     * @return the number of flight hours for the fleet, given the specified
     * queryString
     */
    public static long getTotalFlightTime(Connection connection, String queryString, int fleetId) throws SQLException {
        String fullQueryString =
                "SELECT sum(TIMESTAMPDIFF(SECOND, start_time, end_time)) FROM flights WHERE (" + queryString + ")";
        LOG.info("getting total flight hours with query string: '" + fullQueryString + "'");

        if (fleetId > 0) fullQueryString += " AND fleet_id = " + fleetId;

        try (PreparedStatement query = connection.prepareStatement(fullQueryString); ResultSet resultSet =
                query.executeQuery()) {
            LOG.info(query.toString());

            resultSet.next();
            return resultSet.getLong(1);
        }
    }

    public static ArrayList<Flight> getFlightsSorted(Connection connection, int fleetId, Filter filter,
                                                     int currentPage, int pageSize, String orderingParameter,
                                                     boolean isAscending) throws SQLException {
        return switch (orderingParameter) {
            case "tail_number" ->
                    getFlightsSortedByTails(connection, fleetId, filter, currentPage, pageSize, isAscending);
            case "itinerary" -> getFlightsSortedByOccurrencesInTable(connection, fleetId, filter, currentPage, pageSize,
                    "itinerary", isAscending);
            case "flight_tags" ->
                    getFlightsSortedByOccurrencesInTable(connection, fleetId, filter, currentPage, pageSize,
                            "flight_tag_map", isAscending);
            case "events" -> getFlightsSortedByOccurrencesInTable(connection, fleetId, filter, currentPage, pageSize,
                    "events", isAscending);
            case "airports_visited" ->
                    getFlightsSortedByAirportsVisited(connection, fleetId, filter, currentPage, pageSize, isAscending);
            default -> Flight.getFlights(connection, fleetId, filter,
                    " ORDER BY " + orderingParameter + " " + (isAscending ? "ASC" : "DESC") + " LIMIT " +
                            (currentPage * pageSize) + "," + pageSize);
        };
    }

    public static ArrayList<Flight> getFlights(Connection connection, int fleetId, Filter filter, int currentPage,
                                               int pageSize) throws SQLException {
        return Flight.getFlights(connection, fleetId, filter, " LIMIT " + (currentPage * pageSize) + "," + pageSize);
    }

    /**
     * Retrieves flights ordered by the number of occurrences in another table within
     * the database
     *
     * @param connection  the db connection
     * @param fleetId     the fleet ID
     * @param filter      the filter being used to retrieve flights
     * @param currentPage the current page the UI is on
     * @param pageSize    how large each page is
     * @param tableName   the table to search for occurrences in and order by
     * @param isAscending whether to order in an ascending or descending
     *                    manner
     * @return an {@link ArrayList} of flights
     * @throws SQLException if there is an issue with the query
     * @pre target table MUST have a flight_id foreign key to the flights table
     */
    private static ArrayList<Flight> getFlightsSortedByOccurrencesInTable(Connection connection, int fleetId,
                                                                          Filter filter, int currentPage,
                                                                          int pageSize, String tableName,
                                                                          boolean isAscending) throws SQLException {
        ArrayList<Object> parameters = new ArrayList<>();

        String queryString = "SELECT " + FLIGHT_COLUMNS + " FROM(SELECT " + FLIGHT_COLUMNS + " FROM flights WHERE " +
                "fleet_id = ? AND " + filter.toQueryString(fleetId, parameters) + ") f LEFT OUTER JOIN(SELECT " +
                "flight_id FROM " + tableName + ") AS i ON f.id = i.flight_id" + " GROUP BY f.id ORDER BY COUNT(i" +
                ".flight_id) " + (isAscending ? "" : "DESC") + " LIMIT " + (currentPage * pageSize) + "," + pageSize;

        try (PreparedStatement query = connection.prepareStatement(queryString)) {
            query.setInt(1, fleetId);
            for (int i = 0; i < parameters.size(); i++) {

                if (parameters.get(i) instanceof String) {
                    query.setString(i + 2, (String) parameters.get(i));
                } else if (parameters.get(i) instanceof Double) {
                    query.setDouble(i + 2, (Double) parameters.get(i));
                } else if (parameters.get(i) instanceof Integer) {
                    query.setInt(i + 2, (Integer) parameters.get(i));
                }
            }

            LOG.info(query.toString());
            try (ResultSet resultSet = query.executeQuery()) {
                ArrayList<Flight> flights = new ArrayList<>();
                while (resultSet.next()) {
                    flights.add(new Flight(connection, resultSet));
                }

                return flights;
            }
        }
    }

    private static ArrayList<Flight> getFlightsSortedByTails(Connection connection, int fleetId, Filter filter,
                                                             int currentPage, int pageSize, boolean isAscending)
            throws SQLException {
        ArrayList<Object> parameters = new ArrayList<>();

        String queryString = " SELECT " + FLIGHT_COLUMNS_TAILS + " FROM(SELECT " + FLIGHT_COLUMNS + " FROM flights " +
                "WHERE fleet_id = ? AND " + filter.toQueryString(fleetId, parameters) + ")f LEFT OUTER JOIN(SELECT " +
                "system_id, tail FROM tails) AS t ON f.system_id = t.system_id ORDER BY t.tail " + (isAscending ?
                "ASC" : "DESC") + " LIMIT " + (currentPage * pageSize) + "," + pageSize;

        return getFlightsFromQueryString(connection, fleetId, parameters, queryString);
    }

    private static ArrayList<Flight> getFlightsSortedByAirportsVisited(Connection connection, int fleetId,
                                                                       Filter filter, int currentPage, int pageSize,
                                                                       boolean isAscending) throws SQLException {
        ArrayList<Object> parameters = new ArrayList<>();

        String queryString = "SELECT " + FLIGHT_COLUMNS + " FROM (SELECT " + FLIGHT_COLUMNS + " FROM flights WHERE " +
                "fleet_id = ? AND " + filter.toQueryString(fleetId, parameters) + ")f LEFT OUTER JOIN(SELECT DISTINCT" +
                " airport, flight_id FROM itinerary)a ON id = a.flight_id GROUP BY f.id ORDER BY COUNT(a.flight_id) " +
                (isAscending ? "ASC" : "DESC") + " LIMIT " + (currentPage * pageSize) + "," + pageSize;

        return getFlightsFromQueryString(connection, fleetId, parameters, queryString);
    }

    /**
     * This method allows for the query using a given filter to be modified by
     * appending a SQL constraint such as LIMIT or ORDER BY or combinations thereof
     *
     * @param connection  the database connection
     * @param fleetId     the fleet id
     * @param filter      the filter used to query flights
     * @param constraints the additional query constraints appended to the query
     * @return an {@link ArrayList} of flights
     */
    private static ArrayList<Flight> getFlights(Connection connection, int fleetId, Filter filter,
                                                String constraints) throws SQLException {
        ArrayList<Object> parameters = new ArrayList<>();

        String queryString =
                "SELECT " + FLIGHT_COLUMNS + " FROM flights WHERE fleet_id = ? AND (" + filter.toQueryString(fleetId,
                        parameters) + ")";

        queryString = (constraints != null && !constraints.isEmpty()) ? (queryString + constraints) : queryString;

        LOG.info(queryString);

        return getFlightsFromQueryString(connection, fleetId, parameters, queryString);
    }

    private static ArrayList<Flight> getFlightsFromQueryString(Connection connection, int fleetId,
                                                               ArrayList<Object> parameters, String queryString) throws SQLException {
        try (PreparedStatement query = connection.prepareStatement(queryString)) {
            query.setInt(1, fleetId);
            setQueryParameters(parameters, query);

            LOG.info(query.toString());

            try (ResultSet resultSet = query.executeQuery()) {
                ArrayList<Flight> flights = new ArrayList<>();
                while (resultSet.next()) {
                    flights.add(new Flight(connection, resultSet));
                }

                return flights;
            }
        }
    }

    private static void setQueryParameters(ArrayList<Object> parameters, PreparedStatement query) throws SQLException {
        for (int i = 0; i < parameters.size(); i++) {
            LOG.info("setting query parameter " + i + ": " + parameters.get(i));

            if (parameters.get(i) instanceof String) {
                query.setString(i + 2, (String) parameters.get(i));
            } else if (parameters.get(i) instanceof Double) {
                query.setDouble(i + 2, (Double) parameters.get(i));
            } else if (parameters.get(i) instanceof Integer) {
                query.setInt(i + 2, (Integer) parameters.get(i));
            }
        }
    }

    public static ArrayList<Flight> getFlights(Connection connection, int fleetId, Filter filter, int limit)
            throws SQLException {
        String lim = "";
        if (limit > 0) {
            lim = " LIMIT 100";
        }
        return getFlights(connection, fleetId, filter, lim);
    }

    public static List<Flight> getFlightsByRange(Connection connection, Filter filter, int fleetId, int lowerId,
                                                 int upperId) throws SQLException {
        ArrayList<Object> parameters = new ArrayList<>();

        String queryString =
                "SELECT " + FLIGHT_COLUMNS + " FROM flights WHERE fleet_id = ?" + " AND (" +
                        filter.toQueryString(fleetId, parameters) + ") LIMIT " + lowerId + ", " + (upperId - lowerId);

        return getFlightsFromQueryString(connection, fleetId, parameters, queryString);
    }

    public static List<Flight> getFlightsByRange(Connection connection, int fleetId, int lowerId, int upperId)
            throws SQLException {
        String queryString =
                "SELECT " + FLIGHT_COLUMNS + " FROM flights WHERE fleet_id = " +
                        fleetId + " LIMIT " + lowerId + ", " + (upperId - lowerId);

        LOG.info(queryString);

        return getFlightsFromDb(connection, queryString);
    }

    public static int[] getFlightNumbers(Connection connection, int fleetId, Filter filter) throws SQLException {
        String queryString = "SELECT id FROM flights WHERE fleet_id = " + fleetId + " AND airframe_id=1";

        int[] nums = new int[getNumFlights(connection, fleetId, filter)];

        try (PreparedStatement query = connection.prepareStatement(queryString); ResultSet resultSet =
                query.executeQuery()) {
            int i = 0;

            while (resultSet.next()) {
                nums[i] = resultSet.getInt(1);
                i++;
            }

            return nums;
        }
    }

    public static ArrayList<Flight> getFlights(Connection connection, String extraCondition) throws SQLException {
        return getFlights(connection, extraCondition, 0);
    }

    public static ArrayList<Flight> getFlights(Connection connection, String extraCondition, int limit)
            throws SQLException {
        String queryString = "SELECT " + FLIGHT_COLUMNS + " FROM flights WHERE (" + extraCondition + ")";

        if (limit > 0) queryString += " LIMIT 100";

        LOG.info(queryString);

        try (PreparedStatement query = connection.prepareStatement(queryString); ResultSet resultSet =
                query.executeQuery()) {
            LOG.info(query.toString());

            ArrayList<Flight> flights = new ArrayList<>();
            while (resultSet.next()) {
                flights.add(new Flight(connection, resultSet));
            }

            return flights;
        }
    }

    // Added to use in pitch_db
    public static Flight getFlight(Connection connection, int flightId) throws SQLException {
        String queryString = "SELECT " + FLIGHT_COLUMNS + " FROM flights WHERE id = " + flightId;
        try (PreparedStatement query = connection.prepareStatement(queryString); ResultSet resultSet =
                query.executeQuery()) {
            if (resultSet.next()) {
                return new Flight(connection, resultSet);
            } else {
                return null;
            }
        }
    }

    /**
     * Gets the CSV filepath from the database for a flight
     *
     * @param connection the database connection
     * @param flightId   the id of the flight which we want the CSV file for
     * @return a String with the filepath in unix-format
     * @throws SQLException if there is an error with the database query
     */
    public static String getFilename(Connection connection, int flightId) throws SQLException {
        String queryString = "SELECT filename FROM flights WHERE id = " + flightId;
        try (PreparedStatement query = connection.prepareStatement(queryString); ResultSet resultSet =
                query.executeQuery()) {
            String filename = "";
            if (resultSet.next()) {
                filename = resultSet.getString(1);
            }

            return filename;
        }
    }

    /**
     * Generates a unique set of tagIds whose cardinality is not greater than the
     * total number of tags in the database
     *
     * @param connection the database connection
     * @param flightId   the flightId to get tag ids for
     * @return a Set of Integers with the tag ids
     * @throws SQLException if there is an error with the database query
     */
    private static Set<Integer> getTagIds(Connection connection, int flightId) throws SQLException {
        String queryString = "SELECT tag_id FROM flight_tag_map WHERE flight_id = " + flightId;
        try (PreparedStatement query = connection.prepareStatement(queryString); ResultSet resultSet =
                query.executeQuery()) {
            Set<Integer> ids = new HashSet<>();

            while (resultSet.next()) {
                ids.add(resultSet.getInt(1));
            }

            return ids;
        }
    }

    /**
     * Creates part of a SQL query to produce only the tags associated with a given
     * flight
     *
     * @param ids        the SET of tag ids this flight has
     * @param complement a flag to indicate if the string is used to query for tags
     *                   that are not associated with this flight
     * @return a String that is usable in a SQL query
     */
    private static String idLimStr(Set<Integer> ids, boolean complement) {
        StringBuilder sb = new StringBuilder("WHERE ID " + (complement ? "!" : "") + "= ");
        Iterator<Integer> it = ids.iterator();

        while (it.hasNext()) {
            sb.append(it.next());
            if (!it.hasNext()) {
                break;
            }
            sb.append(complement ? " AND ID != " : " OR ID = ");
        }

        return sb.toString();
    }

    /**
     * Creates part of a SQL query to produce only the tags associated with a given
     * flight
     *
     * @param ids        the array of int ids
     * @param idName     the name of the id column in the database
     * @param complement a flag to indicate if the string is used to query for tags
     *                   that are not associated with this flight
     * @return a String that is usable in a SQL query
     */
    private static String idLimStr(int[] ids, String idName, boolean complement) {
        StringBuilder sb = new StringBuilder("WHERE " + idName + (complement ? "!" : "") + "= ");

        int size = ids.length;
        for (int i = 0; i < size; i++) {
            sb.append(ids[i]);
            if (i == size - 1) {
                break;
            }
            sb.append(complement ? (" AND " + idName + " != ") : (" OR " + idName + " = "));
        }
        return sb.toString();
    }

    /**
     * Gets the tags associated with a given flight
     *
     * @param connection the database connection
     * @param flightId   the id of the flight that the tags are retrieved for
     * @return a List of tags
     * @throws SQLException if there is an error with the database query
     */
    public static List<FlightTag> getTags(Connection connection, int flightId) throws SQLException {
        Set<Integer> tagIds = getTagIds(connection, flightId);
        if (tagIds.isEmpty()) {
            return null;
        }
        String queryString = "SELECT id, fleet_id, name, description, color FROM flight_tags " + idLimStr(tagIds,
                false);
        try (PreparedStatement query = connection.prepareStatement(queryString); ResultSet resultSet =
                query.executeQuery()) {
            List<FlightTag> tags = new ArrayList<>();

            while (resultSet.next()) {
                tags.add(new FlightTag(resultSet));
            }
            return tags;
        }
    }

    /**
     * Gets all the tags for a given fleet
     *
     * @param connection the database connection
     * @param fleetId    the fleet to query
     * @return a List with all the tags
     * @throws SQLException if there is an error with the database query
     */
    public static List<FlightTag> getAllTags(Connection connection, int fleetId) throws SQLException {
        String queryString =
                "SELECT id, fleet_id, name, description, color FROM flight_tags WHERE fleet_id = " + fleetId;
        try (PreparedStatement query = connection.prepareStatement(queryString); ResultSet resultSet =
                query.executeQuery()) {
            List<FlightTag> tags = new ArrayList<>();
            while (resultSet.next()) {
                tags.add(new FlightTag(resultSet));
            }
            return tags;
        }
    }

    /**
     * Returns a list of all the tag names in the database
     *
     * @param connection the connection to the database
     * @return a List with strings containing the tag names
     * @throws SQLException if there is an error with the database query
     */
    public static List<String> getAllTagNames(Connection connection) throws SQLException {
        String queryString = "SELECT name FROM flight_tags ";
        try (PreparedStatement query = connection.prepareStatement(queryString); ResultSet resultSet =
                query.executeQuery()) {
            List<String> tagNames = new ArrayList<>();

            while (resultSet.next()) {
                tagNames.add(resultSet.getString(1));
            }

            return tagNames;
        }
    }

    /**
     * Returns a list of all the tag names in the database for a fleet
     *
     * @param connection the connection to the database
     * @param fleetId    the fleet ID
     * @return a List with strings containing the tag names
     * @throws SQLException if there is an error with the database query
     */
    public static List<String> getAllFleetTagNames(Connection connection, int fleetId) throws SQLException {
        String queryString = "SELECT name FROM flight_tags WHERE fleet_id =" + fleetId;
        try (PreparedStatement query = connection.prepareStatement(queryString); ResultSet resultSet =
                query.executeQuery()) {
            List<String> tagNames = new ArrayList<>();
            while (resultSet.next()) {
                tagNames.add(resultSet.getString(1));
            }

            return tagNames;
        }
    }

    /**
     * Gets a specific tag from the database
     *
     * @param connection the database connection
     * @param tagId      the tag id to query
     * @return the FlightTag instance associated with the id
     * @throws SQLException if there is an error with the database query
     */
    public static FlightTag getTag(Connection connection, int tagId) throws SQLException {
        String queryString = "SELECT id, fleet_id, name, description, color FROM flight_tags WHERE id = " + tagId;
        try (PreparedStatement query = connection.prepareStatement(queryString); ResultSet resultSet =
                query.executeQuery()) {
            return resultSet.next() ? new FlightTag(resultSet) : null;
        }
    }

    /**
     * Provides a collection of all the tags not yet associated with a given flight
     *
     * @param connection the db connection
     * @param flightId   the flightId used to find th unassociated tags
     * @param fleetId    the id of the fleet
     * @return a List of FlightTags
     * @throws SQLException if there is an error with the database query
     */
    public static List<FlightTag> getUnassociatedTags(Connection connection, int flightId,
                                                      int fleetId) throws SQLException {
        Set<Integer> tagIds = getTagIds(connection, flightId);
        if (tagIds.isEmpty()) {
            return getAllTags(connection, fleetId);
        }

        String queryString = "SELECT id, fleet_id, name, description, color FROM flight_tags "
                + idLimStr(tagIds, true);
        try (PreparedStatement query = connection.prepareStatement(queryString); ResultSet resultSet =
                query.executeQuery()) {
            List<FlightTag> tags = new ArrayList<>();

            while (resultSet.next()) {
                tags.add(new FlightTag(resultSet));
            }
            return tags;
        }
    }

    /**
     * Checks to see if a tag already exists in the database Tags are considered
     * unique if they have different names
     *
     * @param connection the connection to the database
     * @param fleetId    the fleetId for the fleet
     * @param name       the name to check for
     * @return true if the tag already exists, false otherwise
     * @throws SQLException if there is an error with the database query
     */
    public static boolean tagExists(Connection connection, int fleetId, String name) throws SQLException {
        String queryString =
                "SELECT EXISTS (SELECT * FROM flight_tags WHERE name = '" + name + "' AND fleet_id = " + fleetId + ")";
        try (PreparedStatement query = connection.prepareStatement(queryString); ResultSet resultSet =
                query.executeQuery()) {
            return resultSet.next() && resultSet.getBoolean(1);
        }
    }

    /**
     * Associates a tag with a given flight ID
     *
     * @param flightId   the flightId that the tag will be associated with
     * @param tagId      the tagId being associated
     * @param connection the database connection
     * @throws SQLException if there is an error with the database query
     */
    public static void associateTag(int flightId, int tagId, Connection connection) throws SQLException {
        String queryString = "INSERT INTO flight_tag_map (flight_id, tag_id) VALUES(?,?)";

        try (PreparedStatement query = connection.prepareStatement(queryString)) {
            query.setInt(1, flightId);
            query.setInt(2, tagId);

            query.executeUpdate();
        }
    }

    /**
     * dissociates tag(s) from a flight
     *
     * @param tagId      the tag to dissociate
     * @param connection the database connection
     * @param flightId   (vararg) the flightId to dissociate from
     * @throws SQLException if there is an error with the database query
     */
    public static void disassociateTags(int tagId, Connection connection, int... flightId) throws SQLException {
        String queryString = "DELETE FROM flight_tag_map " +
                idLimStr(flightId, "flight_id", false) + " AND tag_id = " + tagId;
        try (PreparedStatement query = connection.prepareStatement(queryString)) {
            query.executeUpdate();
        }
    }

    /**
     * dissociates all tags from a given flight
     *
     * @param flightId   the flight to remove tags from
     * @param connection the connection to the database
     * @throws SQLException if there is an error with the database query
     */
    public static void disassociateAllTags(int flightId, Connection connection) throws SQLException {
        String queryString = "DELETE FROM flight_tag_map WHERE flight_id = " + flightId;
        try (PreparedStatement query = connection.prepareStatement(queryString)) {
            query.executeUpdate();
        }
    }

    /**
     * permanently deletes a tag from the database
     *
     * @param tagId      the tag to dissociate
     * @param connection the database connection
     * @throws SQLException if there is an error with the database query
     */
    public static void deleteTag(int tagId, Connection connection) throws SQLException {
        String queryString = "DELETE FROM flight_tag_map WHERE tag_id = " + tagId;
        try (PreparedStatement query = connection.prepareStatement(queryString)) {
            query.executeUpdate();
        }

        queryString = "DELETE FROM flight_tags WHERE id = " + tagId;
        try (PreparedStatement query = connection.prepareStatement(queryString)) {
            query.executeUpdate();
        }
    }

    /**
     * Edits a tag that is already in the database
     *
     * @param connection the database connection
     * @param flightTag  the edited flightTag
     * @return the new instance of the flightTag in the database
     * @throws SQLException if there is an error with the database query
     * @pre @param flightTag is not equal to the tag currently in the db
     */
    public static FlightTag editTag(Connection connection, FlightTag flightTag) throws SQLException {
        FlightTag current = getTag(connection, flightTag.hashCode());
        String newName = flightTag.getName();
        String newDescription = flightTag.getDescription();
        String newColor = flightTag.getColor();

        if (!current.equals(flightTag)) {
            StringBuilder queryString = new StringBuilder("UPDATE flight_tags SET");
            boolean first = true;
            if (!current.getName().equals(newName)) {
                queryString.append(" name = '");
                queryString.append(newName);
                queryString.append("' ");
                first = false;
            }
            if (!current.getDescription().equals(newDescription)) {
                queryString.append((first ? " " : ", "));
                queryString.append("description = '");
                queryString.append(newDescription);
                queryString.append("' ");
                first = false;
            }
            if (!current.getColor().equals(newColor)) {
                queryString.append((first ? " " : ", "));
                queryString.append("color = '");
                queryString.append(newColor);
                queryString.append("' ");
            }

            queryString.append("WHERE id = ").append(flightTag.hashCode());
            LOG.info("Query String Update: " + queryString);
            try (PreparedStatement query = connection.prepareStatement(queryString.toString())) {
                query.executeUpdate();
            }

            return getTag(connection, flightTag.hashCode());
        }
        return null; // this should never happen, it violates the precondition!
    }

    /**
     * Creates a tag in the database tables
     *
     * @param fleetId     the fleetId to use
     * @param flightId    the flightId to use
     * @param name        the name of the new tag (has to be unique!)
     * @param description the description of the new tag
     * @param color       the color of the new tag
     * @param connection  the database connection
     * @return the new FlightTag instance
     * @throws SQLException if there is an error with the database query
     */
    public static FlightTag createTag(int fleetId, int flightId, String name, String description, String color,
                                      Connection connection) throws SQLException {
        String queryString = "INSERT INTO flight_tags (fleet_id, name, description, color) VALUES(?,?,?,?)";

        try (PreparedStatement stmt = connection.prepareStatement(queryString, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setInt(1, fleetId);
            stmt.setString(2, name);
            stmt.setString(3, description);
            stmt.setString(4, color);

            stmt.executeUpdate();

            try (ResultSet resultSet = stmt.getGeneratedKeys()) {
                int index = -1;

                if (resultSet.next()) {
                    index = resultSet.getInt(1);
                }

                associateTag(flightId, index, connection);

                return new FlightTag(index, fleetId, name, description, color);
            }
        }
    }

    public static void addSimAircraft(Connection connection, int fleetId, String path) throws SQLException {
        String queryString = "INSERT INTO sim_aircraft (fleet_id, path) VALUES(?,?)";

        try (PreparedStatement query = connection.prepareStatement(queryString)) {
            query.setInt(1, fleetId);
            query.setString(2, path);

            query.executeUpdate();
        }
    }

    public static void removeSimAircraft(Connection connection, int fleetId, String path) throws SQLException {
        String queryString = "DELETE FROM sim_aircraft WHERE fleet_id = ? AND path = ?";

        try (PreparedStatement query = connection.prepareStatement(queryString)) {
            query.setInt(1, fleetId);
            query.setString(2, path);

            query.executeUpdate();
        }
    }

    public static List<String> getSimAircraft(Connection connection, int fleetId) throws SQLException {
        String queryString = "SELECT path FROM sim_aircraft WHERE fleet_id = " + fleetId;

        try (PreparedStatement query = connection.prepareStatement(queryString); ResultSet resultSet =
                query.executeQuery()) {
            List<String> paths = new ArrayList<>();
            while (resultSet.next()) {
                paths.add(resultSet.getString(1));
            }

            return paths;
        }
    }

    public static double calculateLOCI(DoubleTimeSeries hdg, int index, DoubleTimeSeries roll, DoubleTimeSeries tas,
                                       double laggedHdg) {
        double yawRate = Double.isNaN(laggedHdg) ? 0 :
                180 - Math.abs(180 - Math.abs(hdg.get(index) - laggedHdg) % 360);

        double yawComp = yawRate * COMP_CONV;
        double vrComp = ((tas.get(index) / 60) * yawComp);
        double rollComp = roll.get(index) * COMP_CONV;
        double ctComp = Math.sin(rollComp) * 32.2;
        return Math.min(((Math.abs(ctComp - vrComp) * 100) / PROSPIN_LIM), 100);
    }

    public static void getNearbyLandingAreas(DoubleTimeSeries latitudeTS, DoubleTimeSeries longitudeTS,
                                             DoubleTimeSeries altitudeAGLTS, StringTimeSeries nearestAirportTS,
                                             DoubleTimeSeries airportDistanceTS, StringTimeSeries nearestRunwayTS,
                                             DoubleTimeSeries runwayDistanceTS, double maxAirportDistanceFt,
                                             double maxRunwayDistanceFt) {
        for (int i = 0; i < latitudeTS.size(); i++) {
            double latitude = latitudeTS.get(i);
            double longitude = longitudeTS.get(i);
            double altitudeAGL = altitudeAGLTS.get(i);

            MutableDouble airportDistance = new MutableDouble();
            Airport airport = null;
            if (altitudeAGL <= 2000) {
                airport = Airports.getNearestAirportWithin(latitude, longitude, maxAirportDistanceFt, airportDistance);
            }

            if (airport == null) {
                nearestAirportTS.add("");
                airportDistanceTS.add(Double.NaN);
                nearestRunwayTS.add("");
                runwayDistanceTS.add(Double.NaN);

                // System.out.println(latitude + ", " + longitude + ", null, null, null, null");
            } else {
                nearestAirportTS.add(airport.getIataCode());
                airportDistanceTS.add(airportDistance.getValue());

                MutableDouble runwayDistance = new MutableDouble();
                Runway runway = airport.getNearestRunwayWithin(latitude, longitude, maxRunwayDistanceFt,
                        runwayDistance);
                if (runway == null) {
                    nearestRunwayTS.add("");
                    runwayDistanceTS.add(Double.NaN);
                } else {
                    nearestRunwayTS.add(runway.getName());
                    runwayDistanceTS.add(runwayDistance.getValue());
                }
            }
        }
    }

    public static void batchUpdateDatabase(Connection connection, Upload upload, Iterable<Flight> flights)
            throws IOException, SQLException {
        int fleetId = upload.getFleetId();
        int uplderId = upload.getUploaderId();
        int upldId = upload.getId();

        try (PreparedStatement preparedStatement = createPreparedStatement(connection)) {
            for (Flight flight : flights) {
                // Ensure that the `id` values are set. This will grab them from the database if not.
                flight.airframe = new Airframes.Airframe(connection, flight.airframe.getName());
                flight.airframeType = new Airframes.AirframeType(connection, flight.airframeType.getName());

                Airframes.setAirframeFleet(connection, flight.airframe.getId(), fleetId);

                Tails.setSuggestedTail(connection, fleetId, flight.systemId, flight.suggestedTailNumber);
                flight.tailNumber = Tails.getTail(connection, fleetId, flight.systemId);
                flight.fleetId = fleetId;
                flight.uploaderId = uplderId;
                flight.uploadId = upldId;
                flight.addBatch(preparedStatement);
            }

            preparedStatement.executeBatch();
            ResultSet results = preparedStatement.getGeneratedKeys();
            for (Flight flight : flights) {
                results.next();
                flight.id = results.getInt(1);
            }
        }

        try (PreparedStatement doubleTSPreparedStatement = DoubleTimeSeries.createPreparedStatement(connection)) {
            for (Flight flight : flights)
                for (var doubleTS : flight.doubleTimeSeries.values())
                    doubleTS.addBatch(connection, doubleTSPreparedStatement, flight.id);
            doubleTSPreparedStatement.executeBatch();
        }

        try (PreparedStatement stringTSPreparedStatement = StringTimeSeries.createPreparedStatement(connection)) {
            for (Flight flight : flights)
                for (var stringTS : flight.stringTimeSeries.values())
                    stringTS.addBatch(connection, stringTSPreparedStatement, flight.id);

            stringTSPreparedStatement.executeBatch();
        }

        try (PreparedStatement itineraryPreparedStatement = Itinerary.createPreparedStatement(connection);
             PreparedStatement airportPreparedStatement = Itinerary.createAirportPreparedStatement(connection);
             PreparedStatement runwayPreparedStatement = Itinerary.createRunwayPreparedStatement(connection)) {
            for (Flight flight : flights) {
                if (flight.itinerary != null) {
                    for (int i = 0; i < flight.itinerary.size(); i++)
                        flight.itinerary.get(i).addBatch(itineraryPreparedStatement, airportPreparedStatement,
                                runwayPreparedStatement, fleetId, flight.id, i);
                }
            }
            itineraryPreparedStatement.executeBatch();
            airportPreparedStatement.executeBatch();
            runwayPreparedStatement.executeBatch();
        }

        try (PreparedStatement warningPreparedStatement = FlightWarning.createPreparedStatement(connection)) {

            for (Flight flight : flights)
                for (var e : flight.exceptions)
                    new FlightWarning(e.getMessage()).addBatch(connection, warningPreparedStatement, flight.id);

            warningPreparedStatement.executeBatch();
        }
        try (PreparedStatement processingStatusStatement = connection.prepareStatement("UPDATE flights SET " +
                "insert_completed = 1 WHERE id = ?")) {

            for (Flight flight : flights) {
                processingStatusStatement.setInt(1, flight.id);
                processingStatusStatement.addBatch();
            }

            processingStatusStatement.executeBatch();
        }
    }

    private static PreparedStatement createPreparedStatement(Connection connection) throws SQLException {
        return connection.prepareStatement("INSERT INTO flights (fleet_id, uploader_id, upload_id, airframe_id, " +
                        "airframe_type_id, system_id, start_time, end_time, filename, md5_hash, number_rows, status, " +
                        "has_coords, has_agl, insert_completed, processing_status, start_timestamp, end_timestamp) " +
                        "VALUES (?," +
                        " ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, UNIX_TIMESTAMP(?), UNIX_TIMESTAMP(?))",
                Statement.RETURN_GENERATED_KEYS);
    }

    public List<MalformedFlightFileException> getExceptions() {
        return exceptions;
    }

    public void remove(Connection connection) throws SQLException {
        // String query = "SELECT id FROM events WHERE flight_id = " + this.id + " AND event_definition_id = -1";

        String clearRateOfClosure = """
                    DELETE rate_of_closure FROM rate_of_closure
                        INNER JOIN events ON events.event_definition_id = -1 AND (flight_id = ? OR other_flight_id = ?)
                """;
        String clearEventMetaData = """
                    DELETE event_metadata FROM event_metadata
                    INNER JOIN events ON events.event_definition_id = -1 AND (flight_id = ? OR other_flight_id = ?)
                """;

        try (PreparedStatement preparedStatement = connection.prepareStatement(clearRateOfClosure)) {
            preparedStatement.setInt(1, id);
            preparedStatement.setInt(2, id);
            int deleted = preparedStatement.executeUpdate();
            LOG.info("Deleted " + deleted + " rows from rate of closure");
        }
        try (PreparedStatement preparedStatement = connection.prepareStatement(clearEventMetaData)) {
            preparedStatement.setInt(1, id);
            preparedStatement.setInt(2, id);
            int deleted = preparedStatement.executeUpdate();
            LOG.info("Deleted " + deleted + " rows from event meta data");
        }

        String query = "DELETE FROM events WHERE flight_id = ?";
        try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            preparedStatement.setInt(1, this.id);
            LOG.info(preparedStatement.toString());
            preparedStatement.executeUpdate();
        }

        query = "DELETE FROM events WHERE other_flight_id = ?";
        try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            preparedStatement.setInt(1, this.id);
            LOG.info(preparedStatement.toString());
            preparedStatement.executeUpdate();
        }

        query = "DELETE FROM flight_warnings WHERE flight_id = ?";
        try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            preparedStatement.setInt(1, this.id);
            LOG.info(preparedStatement.toString());
            preparedStatement.executeUpdate();
        }

        query = "DELETE FROM flight_processed WHERE flight_id = ?";
        try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            preparedStatement.setInt(1, this.id);
            LOG.info(preparedStatement.toString());
            preparedStatement.executeUpdate();
        }

        query = "DELETE FROM itinerary WHERE flight_id = ?";
        try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            preparedStatement.setInt(1, this.id);
            LOG.info(preparedStatement.toString());
            preparedStatement.executeUpdate();
        }

        query = "DELETE FROM double_series WHERE flight_id = ?";
        try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            preparedStatement.setInt(1, this.id);
            LOG.info(preparedStatement.toString());
            preparedStatement.executeUpdate();
        }

        query = "DELETE FROM string_series WHERE flight_id = ?";
        try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            preparedStatement.setInt(1, this.id);
            LOG.info(preparedStatement.toString());
            preparedStatement.executeUpdate();
        }

        query = "DELETE FROM flight_tag_map WHERE flight_id = ?";
        try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            preparedStatement.setInt(1, this.id);
            LOG.info(preparedStatement.toString());
            preparedStatement.executeUpdate();
        }

        query = "DELETE FROM turn_to_final WHERE flight_id = ?";
        try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            preparedStatement.setInt(1, this.id);
            LOG.info(preparedStatement.toString());
            preparedStatement.executeUpdate();
        }

        query = "DELETE FROM flights WHERE id = ?";
        try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            preparedStatement.setInt(1, this.id);
            LOG.info(preparedStatement.toString());
            preparedStatement.executeUpdate();
        }
    }

    /**
     * Checks references to {@link DoubleTimeSeries} for this flight and if there is
     * not a required reference present, throws an exception
     * detailing which parameter is missing and for what calculation
     *
     * @param calculationName is the name of the calculation for which the method is
     *                        checking for parameters
     * @param seriesNames     is the names of the series to check for
     * @throws IOException                  if decompression fails for the DoubleTimeSeries
     * @throws SQLException                 if there is an issue with the database
     * @throws MalformedFlightFileException if a required column is missing
     */
    public void checkCalculationParameters(String calculationName, String... seriesNames) throws IOException,
            MalformedFlightFileException, SQLException {
        for (String param : seriesNames) {
            if (!this.doubleTimeSeries.containsKey(param) && this.getDoubleTimeSeries(param) == null) {
                String errMsg = "Cannot calculate '" + calculationName + "' as parameter '" + param + "' was missing.";
                LOG.severe("WARNING: " + errMsg);
                throw new MalformedFlightFileException(errMsg);
            }
        }
    }

    public List<String> checkCalculationParameters(String[] seriesNames) throws IOException, SQLException {
        List<String> missingParams = new ArrayList<>();
        for (String param : seriesNames) {
            if (!this.doubleTimeSeries.containsKey(param) && this.getDoubleTimeSeries(param) == null) {
                missingParams.add(param);
            }
        }
        return missingParams;
    }

    public int getId() {
        return id;
    }

    public boolean hasTags() {
        return this.tags != null;
    }

    public int getFleetId() {
        return fleetId;
    }

    public String getTailNumber() {
        return this.tailNumber;
    }

    public int getNumberRows() {
        return this.numberRows;
    }

    /**
     * @return the airframe id for this flight
     */
    public int getAirframeNameId() {
        return airframe.getId();
    }

    /**
     * @return the airframe name for this aircraft
     */
    public String getAirframeName() {
        return airframe.getName();
    }

    /**
     * @return the airframe type id for this flight
     */
    public int getAirframeTypeId() {
        return airframeType.getId();
    }

    /**
     * @return the airframe type for this aircraft
     */
    public String getAirframeType() {
        return airframeType.getName();
    }

    /**
     * Used for LOCI calculations to determine if this Aircraft is applicable for a
     * LOC-I index
     *
     * @return true if the aircraft is a Cessna 172SP
     */
    public boolean isC172() {
        return this.airframe.getName().equals("Cessna 172S");
    }

    public String getFilename() {
        return filename;
    }

    /**
     * Gets the upload id for this flight
     *
     * @return the upload id as an int
     */
    public int getUploadId() {
        return uploadId;
    }

    /**
     * Gets the uploader id for this flight
     *
     * @return the uploader id as an int
     */
    public int getUploaderId() {
        return uploaderId;
    }

    public String getStatus() {
        return status;
    }

    public boolean insertCompleted() {
        return insertCompleted;
    }

    public String getStartDateTime() {
        return startDateTime;
    }

    public String getEndDateTime() {
        return endDateTime;
    }

    public void addHeader(String column, String dataType) {
        headers.add(column);
        dataTypes.add(dataType);
    }

    public void addDoubleTimeSeries(String name, DoubleTimeSeries dts) {
        this.doubleTimeSeries.put(name, dts);
    }

    public Map<String, DoubleTimeSeries> getDoubleTimeSeriesMap() {
        return doubleTimeSeries;
    }

    public Map<String, StringTimeSeries> getStringTimeSeriesMap() {
        return stringTimeSeries;
    }

    public DoubleTimeSeries getDoubleTimeSeries(String name) throws IOException, SQLException {
        if (this.doubleTimeSeries.containsKey(name)) {
            return this.doubleTimeSeries.get(name);
        } else {
            try (Connection connection = Database.getConnection()) {
                DoubleTimeSeries dts = DoubleTimeSeries.getDoubleTimeSeries(connection, this.id, name);
                if (dts != null) {
                    this.doubleTimeSeries.put(name, dts);
                }
                return dts;
            }
        }
    }

    public StringTimeSeries getStringTimeSeries(String name) {
        return stringTimeSeries.get(name);
    }

    public DoubleTimeSeries getDoubleTimeSeries(Connection connection, String name) throws IOException, SQLException {
        DoubleTimeSeries series = DoubleTimeSeries.getDoubleTimeSeries(connection, id, name);
        this.doubleTimeSeries.put(name, series);
        return series;
    }

    public StringTimeSeries getStringTimeSeries(Connection connection, String name) throws IOException, SQLException {
        StringTimeSeries series = StringTimeSeries.getStringTimeSeries(connection, id, name);
        this.stringTimeSeries.put(name, series);
        return series;
    }

    private void checkExceptions() {
        if (!exceptions.isEmpty()) {
            status = "WARNING";
            /*
             * for (MalformedFlightFileException e : exceptions) { e.printStackTrace(); }
             */
        } else {
            status = "SUCCESS";
        }
    }

    /**
     * Runs the Loss of Control/Stall Index calculations
     *
     * @param connection the database connection
     */
    public void runLOCICalculations(Connection connection) throws MalformedFlightFileException, SQLException,
            IOException {
        checkCalculationParameters(STALL_PROB, STALL_DEPENDENCIES);

        if (this.isC172()) {
            CalculatedDoubleTimeSeries cas = new CalculatedDoubleTimeSeries(connection, CAS, "knots", true, this);
            cas.create(index -> {
                DoubleTimeSeries ias = getDoubleTimeSeries(IAS);
                double iasValue = ias.get(index);

                if (iasValue < 70.d) {
                    iasValue = (0.7d * iasValue) + 20.667;
                }

                return iasValue;
            });
        }

        CalculatedDoubleTimeSeries vspdCalculated = new CalculatedDoubleTimeSeries(connection, VSPD_CALCULATED, "ft" +
                "/min", true, this);
        vspdCalculated.create(new VSPDRegression(getDoubleTimeSeries(ALT_B)));

        CalculatedDoubleTimeSeries stallIndex = getCalculatedDoubleTimeSeries(connection, vspdCalculated);

        if (this.isC172()) {
            // We still can only perform a LOC-I calculation on the Skyhawks. This can be changed down the road
            checkCalculationParameters(LOCI, LOCI_DEPENDENCIES);
            DoubleTimeSeries hdg = getDoubleTimeSeries(HDG);
            DoubleTimeSeries hdgLagged = hdg.lag(connection, YAW_RATE_LAG);
            CalculatedDoubleTimeSeries coordIndex = new CalculatedDoubleTimeSeries(connection, PRO_SPIN_FORCE, "index",
                    true, this);
            coordIndex.create(index -> {
                DoubleTimeSeries roll = getDoubleTimeSeries(ROLL);
                DoubleTimeSeries tas = getDoubleTimeSeries(TAS_FTMIN);

                double laggedHdg = hdgLagged.get(index);
                return calculateLOCI(hdg, index, roll, tas, laggedHdg);
            });

            CalculatedDoubleTimeSeries loci = new CalculatedDoubleTimeSeries(connection, LOCI, "index", true, this);
            loci.create(index -> {
                double prob = (stallIndex.get(index) * getDoubleTimeSeries(PRO_SPIN_FORCE).get(index));
                return prob / 100;
            });
        }
    }

    private CalculatedDoubleTimeSeries getCalculatedDoubleTimeSeries(Connection connection,
                                                                     CalculatedDoubleTimeSeries vspdCalculated)
            throws SQLException, IOException {
        CalculatedDoubleTimeSeries densityRatio = new CalculatedDoubleTimeSeries(connection, DENSITY_RATIO, "ratio",
                false, this);
        densityRatio.create(index -> {
            DoubleTimeSeries baroA = getDoubleTimeSeries(BARO_A);
            DoubleTimeSeries oat = getDoubleTimeSeries(OAT);

            double pressRatio = baroA.get(index) / STD_PRESS_INHG;
            double tempRatio = (273 + oat.get(index)) / 288;

            return pressRatio / tempRatio;
        });

        CalculatedDoubleTimeSeries aoaSimple = getCalculatedDoubleTimeSeries(connection, vspdCalculated, densityRatio);

        CalculatedDoubleTimeSeries stallIndex = new CalculatedDoubleTimeSeries(connection, STALL_PROB, "index", true,
                this);
        stallIndex.create(index -> {
            return (Math.min(((Math.abs(aoaSimple.get(index) / AOA_CRIT)) * 100), 100)) / 100;
        });
        return stallIndex;
    }

    private CalculatedDoubleTimeSeries getCalculatedDoubleTimeSeries(Connection connection,
                                                                     CalculatedDoubleTimeSeries vspdCalculated,
                                                                     CalculatedDoubleTimeSeries densityRatio)
            throws SQLException, IOException {
        CalculatedDoubleTimeSeries tasFtMin = new CalculatedDoubleTimeSeries(connection, TAS_FTMIN, "ft/min", false,
                this);
        tasFtMin.create(index -> {
            DoubleTimeSeries airspeed = this.isC172() ? getDoubleTimeSeries(CAS) : getDoubleTimeSeries(IAS);

            return (airspeed.get(index) * Math.pow(densityRatio.get(index), -0.5)) * ((double) 6076 / 60);
        });

        CalculatedDoubleTimeSeries aoaSimple = new CalculatedDoubleTimeSeries(connection, AOA_SIMPLE,
                "degrees", true, this);
        aoaSimple.create(index -> {
            DoubleTimeSeries pitch = getDoubleTimeSeries(PITCH);

            double vspdGeo = vspdCalculated.get(index) * Math.pow(densityRatio.get(index), -0.5);
            double fltPthAngle = Math.asin(vspdGeo / tasFtMin.get(index));
            fltPthAngle = fltPthAngle * (180 / Math.PI);

            return pitch.get(index) - fltPthAngle;
        });
        return aoaSimple;
    }

    public void calculateLaggedAltMSL(Connection connection, String altMSLColumnName, int lag,
                                      String laggedColumnName) throws MalformedFlightFileException, SQLException {
        headers.add(laggedColumnName);
        dataTypes.add("ft msl");

        DoubleTimeSeries altMSL = doubleTimeSeries.get(altMSLColumnName);
        if (altMSL == null) {
            throw new MalformedFlightFileException("Cannot calculate '" + laggedColumnName + "' as parameter '" +
                    altMSLColumnName + "' was missing.");
        }

        DoubleTimeSeries laggedAltMSL = new DoubleTimeSeries(connection, laggedColumnName, "ft msl");

        for (int i = 0; i < altMSL.size(); i++) {
            if (i < lag) laggedAltMSL.add(0.0);
            else {
                laggedAltMSL.add(altMSL.get(i) - altMSL.get(i - lag));
            }
        }

        doubleTimeSeries.put(laggedColumnName, laggedAltMSL);
    }

    public void calculateDivergence(Connection connection, String[] columnNames, String varianceColumnName,
                                    String varianceDataType) throws MalformedFlightFileException, SQLException {
        // need to initialize these if we're fixing the divergence calculation error
        // (they aren't initialized in the constructor)
        if (headers == null) headers = new ArrayList<String>();
        if (dataTypes == null) dataTypes = new ArrayList<String>();
        headers.add(varianceColumnName);
        dataTypes.add(varianceDataType);

        DoubleTimeSeries[] columns = new DoubleTimeSeries[columnNames.length];
        for (int i = 0; i < columns.length; i++) {
            columns[i] = doubleTimeSeries.get(columnNames[i]);

            if (columns[i] == null) {
                throw new MalformedFlightFileException("Cannot calculate '" + varianceColumnName + "' as parameter '"
                        + columnNames[i] + "' was missing.");
            }
        }

        DoubleTimeSeries variance = new DoubleTimeSeries(connection, varianceColumnName, varianceDataType);
        for (int i = 0; i < columns[0].size(); i++) {
            double max = -Double.MAX_VALUE;
            double min = Double.MAX_VALUE;

            for (DoubleTimeSeries column : columns) {
                double current = column.get(i);
                if (!Double.isNaN(current) && current > max) max = column.get(i);
                if (!Double.isNaN(current) && current < min) min = column.get(i);
            }

            double v = 0;
            if (max != -Double.MAX_VALUE && min != Double.MAX_VALUE) {
                v = max - min;
            }
            variance.add(v);
        }
        doubleTimeSeries.put(varianceColumnName, variance);
    }

    public void calculateTotalFuel(Connection connection, String[] fuelColumnNames, String totalFuelColumnName)
            throws MalformedFlightFileException, SQLException {
        headers.add(totalFuelColumnName);
        dataTypes.add("gals");

        DoubleTimeSeries[] fuelQuantities = new DoubleTimeSeries[fuelColumnNames.length];
        for (int i = 0; i < fuelQuantities.length; i++) {
            fuelQuantities[i] = doubleTimeSeries.get(fuelColumnNames[i]);

            if (fuelQuantities[i] == null) {
                throw new MalformedFlightFileException("Cannot calculate 'Total Fuel' as fuel parameter '"
                        + fuelColumnNames[i] + "' was missing.");
            }
        }

        DoubleTimeSeries totalFuel = new DoubleTimeSeries(connection, totalFuelColumnName, "gals");
        for (int i = 0; i < fuelQuantities[0].size(); i++) {
            double totalFuelValue = 0.0;
            for (DoubleTimeSeries fuelQuantity : fuelQuantities) {
                totalFuelValue += fuelQuantity.get(i);
            }
            totalFuel.add(totalFuelValue);
        }
        doubleTimeSeries.put(totalFuelColumnName, totalFuel);
    }

    public void calculateAGL(Connection connection, String altitudeAGLColumnName, String altitudeMSLColumnName,
                             String latitudeColumnName, String longitudeColumnName)
            throws MalformedFlightFileException, SQLException {
        // calculates altitudeAGL (above ground level) from altitudeMSL (mean sea levl)
        headers.add(altitudeAGLColumnName);
        dataTypes.add("ft agl");

        DoubleTimeSeries altitudeMSLTS = doubleTimeSeries.get(altitudeMSLColumnName);
        DoubleTimeSeries latitudeTS = doubleTimeSeries.get(latitudeColumnName);
        DoubleTimeSeries longitudeTS = doubleTimeSeries.get(longitudeColumnName);

        if (altitudeMSLTS == null || latitudeTS == null || longitudeTS == null) {
            String message = "Cannot calculate AGL, flight file had empty or missing ";

            int count = 0;
            if (altitudeMSLTS == null) {
                message += "'" + altitudeMSLColumnName + "'";
                count++;
            }

            if (latitudeTS == null) {
                if (count > 0) message += ", ";
                message += "'" + latitudeColumnName + "'";
                count++;
            }

            if (longitudeTS == null) {
                if (count > 0) message += " and ";
                message += "'" + longitudeColumnName + "'";
                count++;
            }

            message += " column";
            if (count >= 2) message += "s";
            message += ".";

            // should be initialized to false, but let's make sure
            hasCoords = false;
            hasAGL = false;
            throw new MalformedFlightFileException(message);
        }
        hasCoords = true;
        hasAGL = true;

        DoubleTimeSeries altitudeAGLTS = new DoubleTimeSeries(connection, altitudeAGLColumnName, "ft agl");

        for (int i = 0; i < altitudeMSLTS.size(); i++) {
            double altitudeMSL = altitudeMSLTS.get(i);
            double latitude = latitudeTS.get(i);
            double longitude = longitudeTS.get(i);

            if (Double.isNaN(altitudeMSL) || Double.isNaN(latitude) || Double.isNaN(longitude)) {
                altitudeAGLTS.add(Double.NaN);
                // System.err.println("result is: " + Double.NaN);
                continue;
            }

            try {
                int altitudeAGL = TerrainCache.getAltitudeFt(altitudeMSL, latitude, longitude);

                // System.out.println("msl: " + altitudeMSL + ", agl: " + altitudeAGL);

                altitudeAGLTS.add(altitudeAGL);

                // the terrain cache will not be able to find the file if the lat/long is
                // outside the USA
            } catch (NoSuchFileException e) {
                System.err.println("ERROR: could not read terrain file: " + e);

                hasAGL = false;
                throw new MalformedFlightFileException("Could not calculate AGL for this flight as it had " +
                        "latitudes/longitudes " + "outside of the United States.");
            }
        }
        doubleTimeSeries.put(altitudeAGLColumnName, altitudeAGLTS);
    }

    public void calculateAirportProximity(Connection connection, String latitudeColumnName,
                                          String longitudeColumnName, String altitudeAGLColumnName)
            throws MalformedFlightFileException, SQLException {
        // calculates if the aircraft is within maxAirportDistance from an airport

        DoubleTimeSeries latitudeTS = doubleTimeSeries.get(latitudeColumnName);
        DoubleTimeSeries longitudeTS = doubleTimeSeries.get(longitudeColumnName);
        DoubleTimeSeries altitudeAGLTS = doubleTimeSeries.get(altitudeAGLColumnName);

        if (latitudeTS == null || longitudeTS == null || altitudeAGLTS == null) {
            String message = "Cannot calculate airport and runway distances, flight file had empty or missing ";

            int count = 0;
            if (latitudeTS == null) {
                message += "'" + latitudeColumnName + "'";
                count++;
            }

            if (longitudeTS == null) {
                if (count > 0) message += " and ";
                message += "'" + longitudeColumnName + "'";
                count++;
            }

            if (altitudeAGLTS == null) {
                if (count > 0) message += " and ";
                message += "'" + altitudeAGLColumnName + "'";
                count++;
            }

            message += " column";
            if (count >= 2) message += "s";
            message += ".";

            // should be initialized to false, but let's make sure
            hasCoords = false;
            throw new MalformedFlightFileException(message);
        }
        hasCoords = true;

        headers.add("NearestAirport");
        dataTypes.add("IATA Code");

        headers.add("AirportDistance");
        dataTypes.add("ft");

        headers.add("NearestRunway");
        dataTypes.add("IATA Code");

        headers.add("RunwayDistance");
        dataTypes.add("ft");

        StringTimeSeries nearestAirportTS = new StringTimeSeries(connection, "NearestAirport", "txt");
        stringTimeSeries.put("NearestAirport", nearestAirportTS);
        DoubleTimeSeries airportDistanceTS = new DoubleTimeSeries(connection, "AirportDistance", "ft");
        doubleTimeSeries.put("AirportDistance", airportDistanceTS);

        StringTimeSeries nearestRunwayTS = new StringTimeSeries(connection, "NearestRunway", "txt");
        stringTimeSeries.put("NearestRunway", nearestRunwayTS);
        DoubleTimeSeries runwayDistanceTS = new DoubleTimeSeries(connection, "RunwayDistance", "ft");
        doubleTimeSeries.put("RunwayDistance", runwayDistanceTS);

        getNearbyLandingAreas(latitudeTS, longitudeTS, altitudeAGLTS, nearestAirportTS, airportDistanceTS,
                nearestRunwayTS, runwayDistanceTS, MAX_AIRPORT_DISTANCE_FT, MAX_RUNWAY_DISTANCE_FT);
    }

    public void updateTail(Connection connection, String tailNum) throws SQLException {
        if (this.systemId != null && !this.systemId.isBlank()) {
            String sql = "INSERT INTO tails(system_id, fleet_id, tail, confirmed) VALUES(?,?,?,?) " + "ON DUPLICATE " +
                    "KEY UPDATE tail = ?";
            try (PreparedStatement query = connection.prepareStatement(sql)) {
                query.setString(1, this.systemId);
                query.setInt(2, this.fleetId);
                query.setString(3, tailNum);
                query.setBoolean(4, true);
                query.setString(5, tailNum);

                query.executeUpdate();
            }
        }
    }

    private void addBatch(PreparedStatement preparedStatement) throws SQLException {
        preparedStatement.setInt(1, fleetId);
        preparedStatement.setInt(2, uploaderId);
        preparedStatement.setInt(3, uploadId);
        LOG.info("AIRFRAME = " + airframe.getId());
        LOG.info("AIRFRAME TYPE = " + airframeType.getId());
        preparedStatement.setInt(4, airframe.getId());
        preparedStatement.setInt(5, airframeType.getId());
        preparedStatement.setString(6, systemId);
        preparedStatement.setString(7, startDateTime);
        preparedStatement.setString(8, endDateTime);
        preparedStatement.setString(9, filename);
        preparedStatement.setString(10, md5Hash);
        preparedStatement.setInt(11, numberRows);
        preparedStatement.setString(12, status);
        preparedStatement.setBoolean(13, hasCoords);
        preparedStatement.setBoolean(14, hasAGL);
        preparedStatement.setBoolean(15, false); // insert not yet completed
        preparedStatement.setLong(16, processingStatus);

        preparedStatement.setString(17, startDateTime);
        preparedStatement.setString(18, endDateTime);
        preparedStatement.addBatch();
    }

    public void updateDatabase(Connection connection, int newUploadId, int newUploaderId, int fltId)
            throws IOException, SQLException {
        this.fleetId = fltId;
        this.uploaderId = newUploaderId;
        this.uploadId = newUploadId;

        try (PreparedStatement preparedStatement = createPreparedStatement(connection)) {
            // first check and see if the airframe and tail number already exist in the
            // database for this flight
            airframe = new Airframes.Airframe(connection, airframe.getName());
            airframeType = new Airframes.AirframeType(connection, airframeType.getName());
            Airframes.setAirframeFleet(connection, airframe.getId(), fltId);

            Tails.setSuggestedTail(connection, fltId, systemId, suggestedTailNumber);
            tailNumber = Tails.getTail(connection, fltId, systemId);

            this.addBatch(preparedStatement);

            LOG.info(preparedStatement.toString());
            preparedStatement.executeBatch();

            try (ResultSet resultSet = preparedStatement.getGeneratedKeys()) {
                int flightId = resultSet.getInt(1);
                this.id = flightId;

                for (DoubleTimeSeries series : doubleTimeSeries.values())
                    series.updateDatabase(connection, flightId);

                for (StringTimeSeries series : stringTimeSeries.values())
                    series.updateDatabase(connection, flightId);

                for (Exception exception : exceptions)
                    FlightWarning.insertWarning(connection, flightId, exception.getMessage());

                for (int i = 0; i < itinerary.size(); i++)
                    itinerary.get(i).updateDatabase(connection, fltId, flightId, i);
            }

            try (PreparedStatement ps = connection.prepareStatement("UPDATE flights SET insert_completed = 1 WHERE id" +
                    " = ?")) {
                ps.setInt(1, this.id);
                ps.executeUpdate();
            }
        }
    }

    /**
     * Writes the DoubleTimeSeries of this flight to the specified filename
     *
     * @param connection is a connection to the database.
     * @param fname      is the output filename.
     */
    public void writeToFile(Connection connection, String fname) throws IOException, SQLException {
        ArrayList<DoubleTimeSeries> series = DoubleTimeSeries.getAllDoubleTimeSeries(connection, id);

        PrintWriter printWriter = new PrintWriter(new FileWriter(fname));

        boolean afterFirst = false;
        printWriter.print("#");
        for (DoubleTimeSeries item : series) {
            String name = item.getName();
            if (name.equals("AirportDistance") || name.equals("RunwayDistance") || item.getMin() == item.getMax()) {
                LOG.warning("Skipping column: '" + name + "'");
                continue;
            }

            if (afterFirst) printWriter.print(",");
            printWriter.print(item.getName());
            afterFirst = true;
        }
        printWriter.println();
        printWriter.flush();

        afterFirst = false;
        printWriter.print("#");
        for (DoubleTimeSeries value : series) {
            String name = value.getName();
            if (name.equals("AirportDistance") || name.equals("RunwayDistance") || value.getMin() == value.getMax())
                continue;
            if (afterFirst) printWriter.print(",");
            printWriter.print(value.getDataType());
            afterFirst = true;
        }
        printWriter.println();
        printWriter.flush();

        // Skip the first 2 minutes to get rid of initial weird values
        for (int i = 119; i < numberRows; i++) {
            afterFirst = false;
            for (DoubleTimeSeries timeSeries : series) {
                String name = timeSeries.getName();
                if (name.equals("AirportDistance") || name.equals("RunwayDistance") ||
                        timeSeries.getMin() == timeSeries.getMax())
                    continue;
                if (afterFirst) printWriter.print(",");
                printWriter.print(timeSeries.get(i));
                afterFirst = true;
            }
            printWriter.println();
        }
        printWriter.close();
    }
}
