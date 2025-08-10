package org.ngafid.core.flights;

import org.ngafid.core.Database;
import org.ngafid.core.event.Event;
import org.ngafid.core.event.EventDefinition;
import org.ngafid.core.heatmap.HeatmapPointsProcessor;
import org.ngafid.core.util.FlightTag;
import org.ngafid.core.util.TimeUtils;
import org.ngafid.core.util.filters.Filter;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.*;
import java.util.*;
import java.util.logging.Logger;

import static org.ngafid.core.flights.Parameters.COMP_CONV;
import static org.ngafid.core.flights.Parameters.PROSPIN_LIM;

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

    public enum FlightStatus {
        PROCESSING,
        SUCCESS,
        WARNING,
        ERROR;
    }

    private static final Logger LOG = Logger.getLogger(Flight.class.getName());
    private static final String FLIGHT_COLUMNS = "id, fleet_id, uploader_id, upload_id, system_id, airframe_id, " +
            " start_time, end_time, filename, md5_hash, number_rows, status ";
    private static final String FLIGHT_COLUMNS_TAILS = "id, fleet_id, uploader_id, upload_id, f.system_id, " +
            "airframe_id, start_time, end_time, filename, md5_hash, number_rows, status";

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

    // Model and type of aircraft.
    private Airframes.Airframe airframe;
    private String tailNumber;
    private String suggestedTailNumber;
    private FlightStatus status;
    private int numberRows;

    // the tags associated with this flight
    private List<FlightTag> tags = null;
    private Map<String, DoubleTimeSeries> doubleTimeSeries = new HashMap<>();
    private Map<String, StringTimeSeries> stringTimeSeries = new HashMap<>();
    private List<Event> events = new ArrayList<>();
    private transient List<MalformedFlightFileException> exceptions = new ArrayList<>();

    public Flight(FlightMeta meta, Map<String, DoubleTimeSeries> doubleTimeSeries,
                  Map<String, StringTimeSeries> stringTimeSeries, List<Itinerary> itinerary,
                  List<MalformedFlightFileException> exceptions, List<Event> events) {
        fleetId = meta.fleetId;
        uploaderId = meta.uploaderId;
        uploadId = meta.uploadId;

        filename = meta.filename;
        status = exceptions.isEmpty() ? FlightStatus.SUCCESS : FlightStatus.WARNING;
        airframe = meta.airframe;

        systemId = meta.systemId;
        suggestedTailNumber = meta.suggestedTailNumber;
        md5Hash = meta.md5Hash;
        startDateTime = TimeUtils.UTCtoSQL(meta.startDateTime);
        endDateTime = TimeUtils.UTCtoSQL(meta.endDateTime);

        this.itinerary = itinerary;
        this.exceptions = exceptions;
        this.events = events;

        this.stringTimeSeries = new HashMap<>(stringTimeSeries);
        this.doubleTimeSeries = new HashMap<>(doubleTimeSeries);

        if (!doubleTimeSeries.isEmpty()) {
            numberRows = doubleTimeSeries.values().iterator().next().size();
        }

        for (var series : doubleTimeSeries.values())
            assert series.size() == numberRows;
        for (var series : stringTimeSeries.values())
            assert series.size() == numberRows;
    }

    public Flight(Connection connection, ResultSet resultSet) throws SQLException {
        id = resultSet.getInt(1);
        fleetId = resultSet.getInt(2);
        uploaderId = resultSet.getInt(3);
        uploadId = resultSet.getInt(4);

        systemId = resultSet.getString(5);

        airframe = new Airframes.Airframe(connection, resultSet.getInt(6));

        // this will set tailNumber and tailConfirmed
        tailNumber = Tails.getTail(connection, fleetId, systemId);

        startDateTime = resultSet.getString(7);
        endDateTime = resultSet.getString(8);
        filename = resultSet.getString(9);
        md5Hash = resultSet.getString(10);
        numberRows = resultSet.getInt(11);
        status = FlightStatus.valueOf(resultSet.getString(12));

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

        return getFlightsFromQueryString(connection, fleetId, parameters, queryString);
    }

    private static ArrayList<Flight> getFlightsFromQueryString(Connection connection, int fleetId,
                                                               ArrayList<Object> parameters, String queryString) throws SQLException {
        try (PreparedStatement query = connection.prepareStatement(queryString)) {
            query.setInt(1, fleetId);
            setQueryParameters(parameters, query);

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

        return getFlightsFromDb(connection, queryString);
    }

    public static ArrayList<Flight> getFlights(Connection connection, String extraCondition) throws SQLException {
        return getFlights(connection, extraCondition, 0);
    }

    public static ArrayList<Flight> getFlights(Connection connection, String extraCondition, int limit)
            throws SQLException {
        String queryString = "SELECT " + FLIGHT_COLUMNS + " FROM flights WHERE (" + extraCondition + ")";

        if (limit > 0) queryString += " LIMIT 100";

        try (PreparedStatement query = connection.prepareStatement(queryString); ResultSet resultSet =
                query.executeQuery()) {

            ArrayList<Flight> flights = new ArrayList<>();
            while (resultSet.next()) {
                flights.add(new Flight(connection, resultSet));
            }

            return flights;
        }
    }

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

    public static void batchUpdateDatabase(Connection connection, Iterable<Flight> flights)
            throws IOException, SQLException {

        try (PreparedStatement preparedStatement = createPreparedStatement(connection)) {
            for (Flight flight : flights) {
                // Ensure that the `id` values are set. This will grab them from the database if not.
                flight.airframe = new Airframes.Airframe(connection, flight.airframe.getName(), flight.airframe.getType());
                Airframes.setAirframeFleet(connection, flight.airframe.getId(), flight.fleetId);
                Tails.setSuggestedTail(connection, flight.fleetId, flight.systemId, flight.suggestedTailNumber);
                flight.tailNumber = Tails.getTail(connection, flight.fleetId, flight.systemId);

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
                                runwayPreparedStatement, flight.fleetId, flight.id, i);
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

        for (Flight flight : flights) {
            Event.batchInsertion(connection, flight, flight.events);
            
            // Insert coordinate points for heat map processor
            if (!flight.events.isEmpty()) {
                // Process non-proximity events (events with event definition ID != -1)
                List<Event> nonProximityEvents = flight.events.stream()
                    .filter(event -> event.getEventDefinitionId() != -1)
                    .toList();
                
                if (!nonProximityEvents.isEmpty()) {
                    try {
                        HeatmapPointsProcessor.insertCoordinatesForNonProximityEvents(
                            connection, nonProximityEvents, flight);
                    } catch (SQLException e) {
                        System.err.println("Failed to insert proximity points for flight " + flight.getId() + ": " + e.getMessage());
                    }
                }
            }
        }

        try (PreparedStatement processingStatusStatement = connection.prepareStatement("UPDATE flights SET " +
                "status = ? WHERE id = ?")) {

            for (Flight flight : flights) {
                processingStatusStatement.setString(1, flight.status.toString());
                processingStatusStatement.setInt(2, flight.id);
                processingStatusStatement.addBatch();
            }

            processingStatusStatement.executeBatch();
        }

        for (Flight flight : flights) {
            updateAggregateFlightHoursByAirframe(connection, flight);
        }
    }

    /**
     * Updates the v_aggregate_flight_hours_by_airframe table for a given flight.
     * Only updates if the flight status is SUCCESS or WARNING. Do we need to add failed as well?
     */
    public static void updateAggregateFlightHoursByAirframe(Connection connection, Flight flight) throws SQLException {
        // Clarify if we need this
        //  if (flight.status != FlightStatus.SUCCESS && flight.status != FlightStatus.WARNING) return;
        if (flight.startDateTime == null || flight.endDateTime == null) return;
        try {
            java.sql.Timestamp start = java.sql.Timestamp.valueOf(flight.startDateTime);
            java.sql.Timestamp end = java.sql.Timestamp.valueOf(flight.endDateTime);
            double hours = (end.getTime() - start.getTime()) / 1000.0 / 3600.0;
            if (hours <= 0) return;
            String sql = "INSERT INTO v_aggregate_flight_hours_by_airframe (airframe_id, num_flights, total_flight_hours) " +
                         "VALUES (?, 1, ?) " +
                         "ON DUPLICATE KEY UPDATE num_flights = num_flights + 1, total_flight_hours = total_flight_hours + VALUES(total_flight_hours)";
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setInt(1, flight.airframe.getId());
                stmt.setDouble(2, hours);
                stmt.executeUpdate();
            }
        } catch (IllegalArgumentException e) {
            // Ignore flights with invalid date format
        }
    }

    private static PreparedStatement createPreparedStatement(Connection connection) throws SQLException {
        return connection.prepareStatement("""
                            INSERT INTO flights (
                                fleet_id,
                                uploader_id,
                                upload_id,
                                airframe_id,
                                system_id,
                                start_time,
                                end_time,
                                filename,
                                md5_hash,
                                number_rows,
                                status
                            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                Statement.RETURN_GENERATED_KEYS);
    }

    public void insertComputedEvents(Connection connection, List<EventDefinition> eventDefinitions) throws SQLException {
        String query = """
                    INSERT IGNORE INTO `flight_processed` SET
                        fleet_id = ?,
                        flight_id = ?,
                        event_definition_id = ?
                    ;
                """;
        try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            for (var def : eventDefinitions) {
                preparedStatement.setInt(1, fleetId);
                preparedStatement.setInt(2, id);
                preparedStatement.setInt(3, def.getId());
                preparedStatement.addBatch();
            }

            preparedStatement.executeBatch();
        }
    }

    public List<MalformedFlightFileException> getExceptions() {
        return exceptions;
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

    public Airframes.Airframe getAirframe() {
        return airframe;
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
        return airframe.getType().getId();
    }

    /**
     * @return the airframe type for this aircraft
     */
    public String getAirframeType() {
        return airframe.getType().getName();
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

    public FlightStatus getStatus() {
        return status;
    }

    public boolean insertCompleted() {
        return status != FlightStatus.PROCESSING;
    }

    public String getStartDateTime() {
        return startDateTime;
    }

    public String getEndDateTime() {
        return endDateTime;
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

    public DoubleTimeSeries getDoubleTimeSeries(Connection connection, String name) throws SQLException {
        DoubleTimeSeries series = DoubleTimeSeries.getDoubleTimeSeries(connection, id, name);
        this.doubleTimeSeries.put(name, series);
        return series;
    }

    public StringTimeSeries getStringTimeSeries(Connection connection, String name) throws SQLException {
        StringTimeSeries series = StringTimeSeries.getStringTimeSeries(connection, id, name);
        this.stringTimeSeries.put(name, series);
        return series;
    }

    private void addBatch(PreparedStatement preparedStatement) throws SQLException {
        preparedStatement.setInt(1, fleetId);
        preparedStatement.setInt(2, uploaderId);
        preparedStatement.setInt(3, uploadId);
        preparedStatement.setInt(4, airframe.getId());
        preparedStatement.setString(5, systemId);
        preparedStatement.setString(6, startDateTime);
        preparedStatement.setString(7, endDateTime);
        preparedStatement.setString(8, filename);
        preparedStatement.setString(9, md5Hash);
        preparedStatement.setInt(10, numberRows);
        preparedStatement.setString(11, FlightStatus.PROCESSING.toString());

        preparedStatement.addBatch();
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
