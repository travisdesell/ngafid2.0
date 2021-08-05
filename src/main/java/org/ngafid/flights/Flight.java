package org.ngafid.flights;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Iterator;

import java.nio.charset.StandardCharsets;
import java.nio.file.NoSuchFileException;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.SQLException;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;
import java.util.function.Function;
import java.util.HashSet;
import java.util.Optional;
import java.util.logging.Logger;

import javax.xml.bind.DatatypeConverter;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.ngafid.common.*;
import org.ngafid.Database;
import org.ngafid.airports.Airport;
import org.ngafid.airports.Airports;
import org.ngafid.airports.Runway;
import org.ngafid.terrain.TerrainCache;

import org.ngafid.filters.Filter;

import static org.ngafid.flights.CalculationParameters.*;

/**
 * This class represents a Flight in the NGAFID. It also contains static methods for database interaction
 *
 * @author <a href = tjdvse@rit.edu>Travis Desell @ RIT SE</a>
 * @author <a href = josh@mail.rit.edu>Josh Karns @ RIT SE</a>
 * @author <a href = fa3019@rit.edu>Farhad Akhbardeh @ RIT SE</a>
 * @author <a href = apl1341@cs.rit.edu>Aidan LaBella @ RIT CS</a>
 */

public class Flight {
    private static final Logger LOG = Logger.getLogger(Flight.class.getName());

    private final static double MAX_AIRPORT_DISTANCE_FT = 10000;
    private final static double MAX_RUNWAY_DISTANCE_FT = 100;

    private final static String flightColumns = "id, fleet_id, uploader_id, upload_id, system_id, airframe_id, airframe_type_id, start_time, end_time, filename, md5_hash, number_rows, status, has_coords, has_agl, insert_completed";
    private final static String flightColumnsTails = "id, fleet_id, uploader_id, upload_id, f.system_id, airframe_id, airframe_type_id, start_time, end_time, filename, md5_hash, number_rows, status, has_coords, has_agl, insert_completed";

    private int id = -1;
    private int fleetId = -1;
    private int uploaderId = -1;
    private int uploadId = -1;

    private String filename;
    private int airframeNameId;
    private String airframeName;
    private int airframeTypeId;
    private String airframeType;
    private String systemId;

    private String tailNumber;
    private String suggestedTailNumber;
    private String calculationEndpoint;
    private boolean tailConfirmed;

    private String md5Hash;
    private String startDateTime;
    private String endDateTime;

    //these will be set to true if the flight has
    //latitude/longitude coordinates and can therefore
    //calculate AGL, airport and runway proximity
    //hasAGL also requires an altitudeMSL column
    private boolean hasCoords = false;
    private boolean hasAGL = false;
    private boolean insertCompleted = false;

    private String status;
    private ArrayList<MalformedFlightFileException> exceptions = new ArrayList<MalformedFlightFileException>();

    private int numberRows;
    private String fileInformation;
    private ArrayList<String> dataTypes;
    private ArrayList<String> headers;

    //the tags associated with this flight
    private List<FlightTag> tags = null;

    private HashMap<String, DoubleTimeSeries> doubleTimeSeries = new HashMap<String, DoubleTimeSeries>();
    private HashMap<String, StringTimeSeries> stringTimeSeries = new HashMap<String, StringTimeSeries>();

    private HashMap<String, Double> calculationCriticalValues;

    private ArrayList<Itinerary> itinerary = new ArrayList<Itinerary>();

    public static ArrayList<Flight> getFlightsFromUpload(Connection connection, int uploadId) throws SQLException {
        String queryString = "SELECT " + flightColumns + " FROM flights WHERE upload_id = ?";

        PreparedStatement query = connection.prepareStatement(queryString);
        query.setInt(1, uploadId);

        ResultSet resultSet = query.executeQuery();

        ArrayList<Flight> flights = new ArrayList<Flight>();
        while (resultSet.next()) {
            flights.add(new Flight(connection, resultSet));
        }

        resultSet.close();
        query.close();

        return flights;
    }

    public void remove(Connection connection) throws SQLException {
        String query = "DELETE FROM events WHERE flight_id = ?";
        PreparedStatement preparedStatement = connection.prepareStatement(query);
        preparedStatement.setInt(1, this.id);
        LOG.info(preparedStatement.toString());
        preparedStatement.executeUpdate();
        preparedStatement.close();

        query = "DELETE FROM flight_warnings WHERE flight_id = ?";
        preparedStatement = connection.prepareStatement(query);
        preparedStatement.setInt(1, this.id);
        LOG.info(preparedStatement.toString());
        preparedStatement.executeUpdate();
        preparedStatement.close();

        query = "DELETE FROM flight_processed WHERE flight_id = ?";
        preparedStatement = connection.prepareStatement(query);
        preparedStatement.setInt(1, this.id);
        LOG.info(preparedStatement.toString());
        preparedStatement.executeUpdate();
        preparedStatement.close();

        query = "DELETE FROM itinerary WHERE flight_id = ?";
        preparedStatement = connection.prepareStatement(query);
        preparedStatement.setInt(1, this.id);
        LOG.info(preparedStatement.toString());
        preparedStatement.executeUpdate();
        preparedStatement.close();

        query = "DELETE FROM double_series WHERE flight_id = ?";
        preparedStatement = connection.prepareStatement(query);
        preparedStatement.setInt(1, this.id);
        LOG.info(preparedStatement.toString());
        preparedStatement.executeUpdate();
        preparedStatement.close();

        query = "DELETE FROM string_series WHERE flight_id = ?";
        preparedStatement = connection.prepareStatement(query);
        preparedStatement.setInt(1, this.id);
        LOG.info(preparedStatement.toString());
        preparedStatement.executeUpdate();
        preparedStatement.close();

        query = "DELETE FROM flight_tag_map WHERE flight_id = ?";
        preparedStatement = connection.prepareStatement(query);
        preparedStatement.setInt(1, this.id);
        LOG.info(preparedStatement.toString());
        preparedStatement.executeUpdate();
        preparedStatement.close();

        query = "DELETE FROM flights WHERE id = ?";
        preparedStatement = connection.prepareStatement(query);
        preparedStatement.setInt(1, this.id);
        LOG.info(preparedStatement.toString());
        preparedStatement.executeUpdate();
        preparedStatement.close();
    }

    /**
     * Checks references to {@link DoubleTimeSeries} for this flight and if there is not a required referenece present, throws an exception
     * detailing which parameter is missing and for what calculation
     *
     * @param calculationName is the name of the calculation for which the method is checking for parameters
     * @param seriesNames is the names of the series to check for
     *
     * @throws {@link MalformedFlightFileException} if a required column is missing
     */
    private void checkCalculationParameters(String calculationName, String ... seriesNames) throws MalformedFlightFileException {
        for (String param : seriesNames) {
            if (!this.doubleTimeSeries.keySet().contains(param)) {
                String errMsg = "Cannot calculate '" + calculationName + "' as parameter '" + param + "' was missing.";
                LOG.severe("WARNING: " + errMsg);
                throw new MalformedFlightFileException(errMsg);
            }
        }
    }

    public static ArrayList<Flight> getFlights(Connection connection, int fleetId) throws SQLException {
        return getFlights(connection, fleetId, 0);
    }

    public static ArrayList<Flight> getFlights(Connection connection, int fleetId, int limit) throws SQLException {
        String queryString = "SELECT " + flightColumns + " FROM flights WHERE fleet_id = ?";
        if (limit > 0) queryString += " LIMIT 100";

        PreparedStatement query = connection.prepareStatement(queryString);
        query.setInt(1, fleetId);

        ResultSet resultSet = query.executeQuery();

        ArrayList<Flight> flights = new ArrayList<Flight>();
        while (resultSet.next()) {
            flights.add(new Flight(connection, resultSet));
        }

        resultSet.close();
        query.close();
        return flights;
    }

    public static ArrayList<Flight> getFlights(Connection connection, int fleetId, Filter filter) throws SQLException {
        return getFlights(connection, fleetId, filter, 0);
    }

    /**
     *  Gets the total number of flights for a given fleet and filter. If the filter is null it returns the number of flights
     *  for the fleet.
     *
     u  @param connection is the database connection
     *  @param fleetId is the id of the fleet
     *  @param is the filter to select the flights, can be null.
     *
     *  @return the number of flights for the fleet, given the specified filter (or no filter if the filter is null).
     */
    public static int getNumFlights(Connection connection, int fleetId, Filter filter) throws SQLException {
        ArrayList<Object> parameters = new ArrayList<Object>();

        String queryString;
        if (filter != null) {
            queryString = "SELECT count(id) FROM flights WHERE fleet_id = ? AND (" + filter.toQueryString(fleetId, parameters) + ")";
        } else {
            queryString = "SELECT count(id) FROM flights WHERE fleet_id = ?";
        }

        LOG.info(queryString);

        PreparedStatement query = connection.prepareStatement(queryString);
        query.setInt(1, fleetId);
        if (filter != null) {
            for (int i = 0; i < parameters.size(); i++) {
                LOG.info("setting query parameter " + i + ": " + parameters.get(i));

                if (parameters.get(i) instanceof String) {
                    query.setString(i + 2, (String)parameters.get(i));
                } else if (parameters.get(i) instanceof Double) {
                    query.setDouble(i + 2, (Double)parameters.get(i));
                } else if (parameters.get(i) instanceof Integer) {
                    query.setInt(i + 2, (Integer)parameters.get(i));
                }
            }
        }

        LOG.info(query.toString());
        ResultSet resultSet = query.executeQuery();

        resultSet.next();
        int count = resultSet.getInt(1);
        System.out.println("COUNT IS: "+count);

        resultSet.close();
        query.close();

        return count;
    }


    /**
     *  Gets the total number of flight hours for a given fleet and filter. If the filter is null it returns the number of flight hours
     *  for the fleet.
     *
     *  @param connection is the database connection
     *  @param fleetId is the id of the fleet
     *  @param is the filter to select the flights, can be null.
     *
     *  @return the number of flight hours for the fleet, given the specified filter (or no filter if the filter is null).
     */
    public static long getTotalFlightHours(Connection connection, int fleetId, Filter filter) throws SQLException {
        ArrayList<Object> parameters = new ArrayList<Object>();

        String queryString;
        if (filter != null) {
            queryString  = "SELECT sum(TIMESTAMPDIFF(SECOND, start_time, end_time)) FROM flights WHERE fleet_id = ? AND (" + filter.toQueryString(fleetId, parameters) + ")";
        } else {
            queryString  = "SELECT sum(TIMESTAMPDIFF(SECOND, start_time, end_time)) FROM flights WHERE fleet_id = ?";
        }

        LOG.info(queryString);

        PreparedStatement query = connection.prepareStatement(queryString);
        query.setInt(1, fleetId);
        if (filter != null) {
            for (int i = 0; i < parameters.size(); i++) {
                LOG.info("setting query parameter " + i + ": " + parameters.get(i));

                if (parameters.get(i) instanceof String) {
                    query.setString(i + 2, (String)parameters.get(i));
                } else if (parameters.get(i) instanceof Double) {
                    query.setDouble(i + 2, (Double)parameters.get(i));
                } else if (parameters.get(i) instanceof Integer) {
                    query.setInt(i + 2, (Integer)parameters.get(i));
                }
            }
        }

        LOG.info(query.toString());
        ResultSet resultSet = query.executeQuery();

        resultSet.next();
        long diffSeconds = resultSet.getLong(1);
        System.out.println("total time is: " + diffSeconds);

        resultSet.close();
        query.close();

        return diffSeconds;
    }

    public static ArrayList<Flight> getFlightsSorted(Connection connection, int fleetId, Filter filter, int currentPage, int pageSize, String orderingParameter, boolean isAscending) throws SQLException {
        switch (orderingParameter) {
            case "tail_number":
                return getFlightsSortedByTails(connection, fleetId, filter, currentPage, pageSize, isAscending);
            case "itinerary":
                return getFlightsSortedByOccurencesInTable(connection, fleetId, filter, currentPage, pageSize, "itinerary", isAscending);
            case "flight_tags":
                return getFlightsSortedByOccurencesInTable(connection, fleetId, filter, currentPage, pageSize, "flight_tag_map", isAscending);
            case "events":
                return getFlightsSortedByOccurencesInTable(connection, fleetId, filter, currentPage, pageSize, "events", isAscending);
            case "airports_visited":
                return getFlightsSortedByAirportsVisited(connection, fleetId, filter, currentPage, pageSize, isAscending);
            default:
                return Flight.getFlights(connection, fleetId, filter, " ORDER BY " + orderingParameter + " " + (isAscending ? "ASC" : "DESC") + " LIMIT "+ (currentPage * pageSize) + "," + pageSize);
        }
    }

    public static ArrayList<Flight> getFlights(Connection connection, int fleetId, Filter filter, int currentPage, int pageSize) throws SQLException {
        return Flight.getFlights(connection, fleetId, filter, " LIMIT "+ (currentPage * pageSize) + "," + pageSize);
    }


    /**
     * Retrieves flights ordered by the number of occurences in another table within the database
     *
     * @pre target table MUST have a flight_id foreign key to the flights table
     *
     * @param connection the db connection
     * @param fleetId the fleet ID
     * @param filter the filter being used to retrieve flights
     * @param currentPage the current page the UI is on
     * @param pageSize how large each page is
     * @param tableName the table to search for occurences in and order by
     * @param isAscending whether or not to order in an ascending or descending manner
     *
     * @return an {@link ArrayList} of flights
     * @throws SQLException if there is an issue with the query
     * */
    private static ArrayList<Flight> getFlightsSortedByOccurencesInTable(Connection connection, int fleetId, Filter filter, int currentPage, int pageSize, String tableName, boolean isAscending) throws SQLException {
        ArrayList<Object> parameters = new ArrayList<Object>();

        String queryString = "SELECT " + flightColumns + " FROM(SELECT " + flightColumns + " FROM flights WHERE fleet_id = ? AND " + filter.toQueryString(fleetId, parameters) + ")f LEFT OUTER JOIN(SELECT flight_id FROM " + tableName + ") AS i ON f.id = i.flight_id"
            + " GROUP BY f.id ORDER BY COUNT(i.flight_id) " + (isAscending ? "ASC" : "DESC") + " LIMIT " + (currentPage * pageSize) + "," + pageSize;

        PreparedStatement query = connection.prepareStatement(queryString);
        query.setInt(1, fleetId);
        for (int i = 0; i < parameters.size(); i++) {
            LOG.info("setting query parameter " + i + ": " + parameters.get(i));

            if (parameters.get(i) instanceof String) {
                query.setString(i + 2, (String)parameters.get(i));
            } else if (parameters.get(i) instanceof Double) {
                query.setDouble(i + 2, (Double)parameters.get(i));
            } else if (parameters.get(i) instanceof Integer) {
                query.setInt(i + 2, (Integer)parameters.get(i));
            }
        }

        LOG.info(query.toString());
        ResultSet resultSet = query.executeQuery();

        ArrayList<Flight> flights = new ArrayList<Flight>();
        while (resultSet.next()) {
            flights.add(new Flight(connection, resultSet));
        }

        resultSet.close();
        query.close();

        return flights;
    }
    
    private static ArrayList<Flight> getFlightsSortedByTails(Connection connection, int fleetId, Filter filter, int currentPage, int pageSize, boolean isAscending) throws SQLException {
        ArrayList<Object> parameters = new ArrayList<Object>();

        String queryString = " SELECT " + flightColumnsTails + " FROM(SELECT " + flightColumns + " FROM flights WHERE fleet_id = ? AND " + filter.toQueryString(fleetId, parameters) + ")f LEFT OUTER JOIN(SELECT system_id, tail FROM tails) AS t ON f.system_id = t.system_id ORDER BY t.tail " + (isAscending ? "ASC" : "DESC") + " LIMIT " + (currentPage * pageSize) + "," + pageSize;

        PreparedStatement query = connection.prepareStatement(queryString);
        query.setInt(1, fleetId);
        for (int i = 0; i < parameters.size(); i++) {
            LOG.info("setting query parameter " + i + ": " + parameters.get(i));

            if (parameters.get(i) instanceof String) {
                query.setString(i + 2, (String)parameters.get(i));
            } else if (parameters.get(i) instanceof Double) {
                query.setDouble(i + 2, (Double)parameters.get(i));
            } else if (parameters.get(i) instanceof Integer) {
                query.setInt(i + 2, (Integer)parameters.get(i));
            }
        }

        LOG.info(query.toString());
        ResultSet resultSet = query.executeQuery();

        ArrayList<Flight> flights = new ArrayList<Flight>();
        while (resultSet.next()) {
            flights.add(new Flight(connection, resultSet));
        }

        resultSet.close();
        query.close();

        return flights;
    }

    private static ArrayList<Flight> getFlightsSortedByAirportsVisited(Connection connection, int fleetId, Filter filter, int currentPage, int pageSize, boolean isAscending) throws SQLException {
        ArrayList<Object> parameters = new ArrayList<Object>();

        String queryString = "SELECT " + flightColumns + " FROM (SELECT " + flightColumns + " FROM flights WHERE fleet_id = ? AND " + filter.toQueryString(fleetId, parameters) +")f LEFT OUTER JOIN(SELECT DISTINCT airport, flight_id FROM itinerary)a ON id = a.flight_id GROUP BY f.id ORDER BY COUNT(a.flight_id) " + (isAscending ? "ASC" : "DESC") + " LIMIT " + (currentPage * pageSize) + "," + pageSize;

        PreparedStatement query = connection.prepareStatement(queryString);
        query.setInt(1, fleetId);
        for (int i = 0; i < parameters.size(); i++) {
            LOG.info("setting query parameter " + i + ": " + parameters.get(i));

            if (parameters.get(i) instanceof String) {
                query.setString(i + 2, (String)parameters.get(i));
            } else if (parameters.get(i) instanceof Double) {
                query.setDouble(i + 2, (Double)parameters.get(i));
            } else if (parameters.get(i) instanceof Integer) {
                query.setInt(i + 2, (Integer)parameters.get(i));
            }
        }

        LOG.info(query.toString());
        ResultSet resultSet = query.executeQuery();

        ArrayList<Flight> flights = new ArrayList<Flight>();
        while (resultSet.next()) {
            flights.add(new Flight(connection, resultSet));
        }

        resultSet.close();
        query.close();

        return flights;
    }


    /**
     * This method allows for the query using a given filter to be modified by appending a SQL constraint such as LIMIT or ORDER BY or 
     * combinations thereof
     *
     * @param connection the database connection
     * @param fleetId the fleet id
     * @param filter the filter used to query flights
     * @param constraints the additional query constraints appended to the query
     */
    private static ArrayList<Flight> getFlights(Connection connection, int fleetId, Filter filter, String constraints) throws SQLException {
        ArrayList<Object> parameters = new ArrayList<Object>();

        String queryString = "SELECT " + flightColumns + " FROM flights WHERE fleet_id = ? AND (" + filter.toQueryString(fleetId, parameters) + ")";

        queryString = (constraints != null && !constraints.isEmpty()) ? (queryString + constraints) : queryString;

        LOG.info(queryString);

        PreparedStatement query = connection.prepareStatement(queryString);
        query.setInt(1, fleetId);
        for (int i = 0; i < parameters.size(); i++) {
            LOG.info("setting query parameter " + i + ": " + parameters.get(i));

            if (parameters.get(i) instanceof String) {
                query.setString(i + 2, (String)parameters.get(i));
            } else if (parameters.get(i) instanceof Double) {
                query.setDouble(i + 2, (Double)parameters.get(i));
            } else if (parameters.get(i) instanceof Integer) {
                query.setInt(i + 2, (Integer)parameters.get(i));
            }
        }

        LOG.info(query.toString());
        ResultSet resultSet = query.executeQuery();

        ArrayList<Flight> flights = new ArrayList<Flight>();
        while (resultSet.next()) {
            flights.add(new Flight(connection, resultSet));
        }

        resultSet.close();
        query.close();

        return flights;
    }

    public static ArrayList<Flight> getFlights(Connection connection, int fleetId, Filter filter, int limit) throws SQLException {
        String lim = new String();
        if (limit > 0) {
            lim = " LIMIT 100";
        }
        return getFlights(connection, fleetId, filter, lim);
    }

    public static List<Flight> getFlightsByRange(Connection connection, Filter filter, int fleetId, int lowerId, int upperId) throws SQLException {
        ArrayList<Object> parameters = new ArrayList<Object>();

        String queryString = "SELECT " + flightColumns + " FROM flights WHERE fleet_id = ?" + " AND (" + filter.toQueryString(fleetId, parameters) + ") LIMIT " + lowerId + ", " + (upperId - lowerId);
        PreparedStatement query = connection.prepareStatement(queryString);

        query.setInt(1, fleetId);
        for (int i = 0; i < parameters.size(); i++) {
            LOG.info("setting query parameter " + i + ": " + parameters.get(i));

            if (parameters.get(i) instanceof String) {
                query.setString(i + 2, (String)parameters.get(i));
            } else if (parameters.get(i) instanceof Double) {
                query.setDouble(i + 2, (Double)parameters.get(i));
            } else if (parameters.get(i) instanceof Integer) {
                query.setInt(i + 2, (Integer)parameters.get(i));
            }
        }

        LOG.info(query.toString());

        ResultSet resultSet = query.executeQuery();

        ArrayList<Flight> flights = new ArrayList<Flight>();
        while (resultSet.next()) {
            flights.add(new Flight(connection, resultSet));
        }
        System.out.println(flights);

        resultSet.close();
        query.close();

        return flights;
    }

    public static List<Flight> getFlightsByRange(Connection connection, int fleetId, int lowerId, int upperId) throws SQLException {
        String queryString = "SELECT " + flightColumns + " FROM flights WHERE fleet_id = "+fleetId+" LIMIT "+lowerId+", "+(upperId - lowerId);

        LOG.info(queryString);

        PreparedStatement query = connection.prepareStatement(queryString);
        ResultSet resultSet = query.executeQuery();

        ArrayList<Flight> flights = new ArrayList<Flight>();
        while (resultSet.next()) {
            flights.add(new Flight(connection, resultSet));
        }

        resultSet.close();
        query.close();

        return flights;
    }

    public static int[] getFlightNumbers(Connection connection, int fleetId, Filter filter) throws SQLException{
        String queryString = "SELECT id FROM flights WHERE fleet_id = "+fleetId+" AND airframe_id=1";

        int [] nums = new int[getNumFlights(connection, fleetId, filter)];

        PreparedStatement query = connection.prepareStatement(queryString);
        ResultSet resultSet = query.executeQuery();

        int i = 0;
        while(resultSet.next()){
            nums[i] = resultSet.getInt(1);
            i++;
        }

        resultSet.close();
        query.close();

        return nums;
    }



    public static ArrayList<Flight> getFlights(Connection connection, String extraCondition) throws SQLException {
        return getFlights(connection, extraCondition, 0);
    }

    public static ArrayList<Flight> getFlights(Connection connection, String extraCondition, int limit) throws SQLException {
        ArrayList<Object> parameters = new ArrayList<Object>();

        String queryString = "SELECT " + flightColumns + " FROM flights WHERE (" + extraCondition + ")";

        if (limit > 0) queryString += " LIMIT 100";

        LOG.info(queryString);

        PreparedStatement query = connection.prepareStatement(queryString);
        for (int i = 0; i < parameters.size(); i++) {
            LOG.info("setting query parameter " + i + ": " + parameters.get(i));

            if (parameters.get(i) instanceof String) {
                query.setString(i + 1, (String)parameters.get(i));
            } else if (parameters.get(i) instanceof Double) {
                query.setDouble(i + 1, (Double)parameters.get(i));
            } else if (parameters.get(i) instanceof Integer) {
                query.setInt(i + 1, (Integer)parameters.get(i));
            }
        }

        LOG.info(query.toString());
        ResultSet resultSet = query.executeQuery();

        ArrayList<Flight> flights = new ArrayList<Flight>();
        while (resultSet.next()) {
            flights.add(new Flight(connection, resultSet));
        }

        resultSet.close();
        query.close();

        return flights;
    }


    // Added to use in pitch_db
    public static Flight getFlight(Connection connection, int flightId) throws SQLException {
        String queryString = "SELECT " + flightColumns + " FROM flights WHERE id = ?";
        PreparedStatement query = connection.prepareStatement(queryString);
        query.setInt(1, flightId);

        ResultSet resultSet = query.executeQuery();

        if (resultSet.next()) {
            Flight flight = new Flight(connection, resultSet);

            resultSet.close();
            query.close();

            return flight;
        } else {
            resultSet.close();
            query.close();

            return null;
        }
    }

    /**
     * Gets the CSV filepath from the database for a flight
     * @param connection the database connection
     * @param flightId the id of the flight which we want the CSV file for
     * @return a String with the filepath in unix-format
     * @throws SQLException if there is an error with the database query
     */
    public static String getFilename(Connection connection, int flightId) throws SQLException{
        String queryString = "SELECT filename FROM flights WHERE id = "+flightId; 
        PreparedStatement query = connection.prepareStatement(queryString);

        ResultSet resultSet = query.executeQuery();
        String filename = "";
        if(resultSet.next()){
            filename = resultSet.getString(1);
        }    
        
        return filename;
    }

    /**
     * Generates a unique set of tagIds whose cardinality is not greater than the total number of tags in
     * the database
     * @param connection the database connection
     * @param flightId the flightId to get tag ids for
     * @return a Set of Integers with the tag ids
     * @throws SQLException if there is an error with the database query
     */
    private static Set<Integer> getTagIds(Connection connection, int flightId) throws SQLException{
        String queryString = "SELECT tag_id FROM flight_tag_map WHERE flight_id = ?";
        PreparedStatement query = connection.prepareStatement(queryString);
        query.setInt(1, flightId);
        ResultSet resultSet = query.executeQuery();

        Set<Integer> ids = new HashSet<>();

        while(resultSet.next()){
            ids.add(resultSet.getInt(1));
        }

        resultSet.close();
        query.close();

        return ids;
    }

    /**
     * Creates part of a SQL query to produce only the tags associated with a given flight
     * @param ids the SET of tag ids this flight has
     * @param complement a flag to indicate if the string is used to query for tags that are not associated with this flight
     * @return a String that is usable in a SQL query
     */
    private static String idLimStr(Set<Integer> ids, boolean complement){
        StringBuilder sb = new StringBuilder("WHERE ID " + ( complement ? "!" : "" ) + "= ");
        Iterator<Integer> it = ids.iterator();

        while(it.hasNext()){
            sb.append(it.next());
            if(!it.hasNext()){
                break;
            }
            sb.append(complement ? " AND ID != " : " OR ID = ");
        }

        return sb.toString();
    }

    /**
     * Creates part of a SQL query to produce only the tags associated with a given flight
     * @param ids the array of int ids
     * @param complement a flag to indicate if the string is used to query for tags that are not associated with this flight
     * @return a String that is usable in a SQL query
     */
    private static String idLimStr(int[] ids, String idName, boolean complement){
        StringBuilder sb = new StringBuilder("WHERE "+idName+( complement ? "!" : "" ) + "= ");

        int size = ids.length;
        for(int i = 0; i<size; i++){
            sb.append(ids[i]);
            if(i == size - 1){
                break;
            }
            sb.append(complement ? (" AND " + idName + " != ") : (" OR " + idName + " = "));
        }
           
        return sb.toString();
    }

    /**
     * Gets the tags associated with a given flight
     * @param connection the database connection
     * @param flightId the id of the flight that the tags are retrieved for
     * @return a List of tags
     * @throws SQLException if there is an error with the database query
     */
    public static List<FlightTag> getTags(Connection connection, int flightId) throws SQLException{
        Set<Integer> tagIds = getTagIds(connection, flightId);
        if(tagIds.isEmpty()){
            return null;
        }

        String queryString = "SELECT id, fleet_id, name, description, color FROM flight_tags " + idLimStr(tagIds, false);
        PreparedStatement query = connection.prepareStatement(queryString);
        ResultSet resultSet = query.executeQuery();
        List<FlightTag> tags = new ArrayList<>();

        while(resultSet.next()){
            tags.add(new FlightTag(resultSet));
        }

        resultSet.close();
        query.close();

        return tags;
    }

    /**
     * Gets all the tags for a given fleet
     * @param connection the database connection
     * @param fleetId the fleet to query
     * @return a List with all the tags
     * @throws SQLException if there is an error with the database query
     */
    public static List<FlightTag> getAllTags(Connection connection, int fleetId) throws SQLException{
        String queryString = "SELECT id, fleet_id, name, description, color FROM flight_tags WHERE fleet_id = "+fleetId;
        PreparedStatement query = connection.prepareStatement(queryString);
        ResultSet resultSet = query.executeQuery();
        List<FlightTag> tags = new ArrayList<>();

        while(resultSet.next()){
            tags.add(new FlightTag(resultSet));
        }

        resultSet.close();
        query.close();

        return tags;
    }

    /**
     * Returns a list of all the tag names in the database
     * @param connection the connection to the database
     * @return a List with strings containing the tag names
     * @throws SQLException if there is an error with the database query
     */
    public static List<String> getAllTagNames(Connection connection) throws SQLException{
        String queryString = "SELECT name FROM flight_tags ";
        PreparedStatement query = connection.prepareStatement(queryString);
        ResultSet resultSet = query.executeQuery();
        List<String> tagNames = new ArrayList<>();

        while(resultSet.next()){
            tagNames.add(resultSet.getString(1));
        }

        resultSet.close();
        query.close();

        return tagNames;
    }

    /**
     * Gets the tag id associated with a name
     * @param connection the database connection
     * @param name the name that we want to get the id for
     * @return the id as an integer
     * @throws SQLException if there is an error with the database query
     */
    public static int getTagId(Connection connection, String name) throws SQLException{
        String queryString = "SELECT id FROM flight_tags WHERE name = "+name;
        PreparedStatement query = connection.prepareStatement(queryString);
        ResultSet resultSet = query.executeQuery();

        int id = -1;
        if(resultSet.next()){
            id = resultSet.getInt(1);
        }

        return id;
    }

    /**
     * Gets a specific tag from the database
     * @param connection the database connection
     * @param tagId the tag id to query
     * @return the FlightTag instance associated with the id
     * @throws SQLException if there is an error with the database query
     */
    public static FlightTag getTag(Connection connection, int tagId) throws SQLException{
        String queryString = "SELECT id, fleet_id, name, description, color FROM flight_tags WHERE id = ?";
        PreparedStatement query = connection.prepareStatement(queryString);
        query.setInt(1, tagId);

        ResultSet resultSet = query.executeQuery();

        FlightTag ft = null;

        if(resultSet.next()){
            ft = new FlightTag(resultSet);
        }

        resultSet.close();
        query.close();

        return ft;
    }

    /**
     * Provides a collection of all the tags not yet associated with a given flight
     * @param connection the db connection
     * @param flightId the flightId used to find th unassociated tags
     * @param fleetId the id of the fleet
     * @return a List of FlightTags
     * @throws SQLException if there is an error with the database query
     */
    public static List<FlightTag> getUnassociatedTags(Connection connection, int flightId, int fleetId) throws SQLException{
        Set<Integer> tagIds = getTagIds(connection, flightId);
        if(tagIds.isEmpty()){
            return getAllTags(connection, fleetId);
        }

        System.out.println("TAG NUMS: "+tagIds.toString());

        String queryString = "SELECT id, fleet_id, name, description, color FROM flight_tags " + idLimStr(tagIds, true);
        PreparedStatement query = connection.prepareStatement(queryString);
        ResultSet resultSet = query.executeQuery();
        List<FlightTag> tags = new ArrayList<>();

        while(resultSet.next()){
            tags.add(new FlightTag(resultSet));
        }

        resultSet.close();
        query.close();

        return tags;
    }

    /**
     * Checks to see if a tag already exists in the database
     * Tags are considered unique if they have different names
     * @param connection the connection to the database
     * @param fleetId the fleetId for the fleet
     * @param name the name to check for
     * @return true if the tag already exists, false otherwise
     * @throws SQLException if there is an error with the database query
     */
    public static boolean tagExists(Connection connection, int fleetId, String name) throws SQLException{
        String queryString = "SELECT EXISTS (SELECT * FROM flight_tags WHERE name = '"+name+"' AND fleet_id = "+fleetId+")";
        PreparedStatement query = connection.prepareStatement(queryString);
        ResultSet resultSet = query.executeQuery();

        if(resultSet.next()){
            return resultSet.getBoolean(1);
        }

        return false;
    }

    /**
     * Associates a tag with a given flight ID
     * @param flightId the flightId that the tag will be associated with
     * @param tagId the tagId being associated
     * @param connection the database connection
     * @throws SQLException if there is an error with the database query
     */
    public static void associateTag(int flightId, int tagId, Connection connection) throws SQLException{
        String queryString = "INSERT INTO flight_tag_map (flight_id, tag_id) VALUES(?,?)";

        PreparedStatement query = connection.prepareStatement(queryString);
        query.setInt(1, flightId);
        query.setInt(2, tagId);

        query.executeUpdate();

    }

    /**
     * dissociates tag(s) from a flight
     * @param tagId the tag to dissociate
     * @param connection the database connection
     * @param flightId (vararg) the flightId to dissociate from
     * @throws SQLException if there is an error with the database query
     */
    public static void unassociateTags(int tagId, Connection connection, int ... flightId) throws SQLException{
        String queryString = "DELETE FROM flight_tag_map " + idLimStr(flightId, "flight_id", false) + " AND tag_id = "+tagId;
        PreparedStatement query = connection.prepareStatement(queryString);

        query.executeUpdate();

    }

    /**
     * dissociates all tags from a given flight
     * @param flightId the flight to remove tags from
     * @param connection the connection to the database
     * @throws SQLException if there is an error with the database query
     */
    public static void unassociateAllTags(int flightId, Connection connection) throws SQLException{
        String queryString = "DELETE FROM flight_tag_map WHERE flight_id = "+flightId;
        PreparedStatement query = connection.prepareStatement(queryString);
        query.executeUpdate();
    }

    /**
     * permanently deletes a tag from the database
     * @param tagId the tag to dissociate
     * @param connection the database connection
     * @throws SQLException if there is an error with the database query
     */
    public static void deleteTag(int tagId, Connection connection) throws SQLException{
        String queryString = "DELETE FROM flight_tag_map WHERE tag_id = "+tagId;
        PreparedStatement query = connection.prepareStatement(queryString);
        query.executeUpdate();

        queryString = "DELETE FROM flight_tags WHERE id = "+tagId;
        query = connection.prepareStatement(queryString);
        query.executeUpdate();
    }

    /**
     * Edits a tag that is already in the database
     * @pre @param flightTag is not equal to the tag currently in the db
     * @param connection the database connection
     * @param flightTag the edited flightTag
     * @return the new instance of the flightTag in the database
     * @throws SQLException if there is an error with the database query
     */
    public static FlightTag editTag(Connection connection, FlightTag flightTag) throws SQLException{
        FlightTag current = getTag(connection, flightTag.hashCode());
        String newName = flightTag.getName();
        String newDescription = flightTag.getDescription();
        String newColor = flightTag.getColor();

        if(!current.equals(flightTag)){
            StringBuilder queryString = new StringBuilder("UPDATE flight_tags SET");
            boolean first = true;
            if(!current.getName().equals(newName)){
                queryString.append(" name = '");
                queryString.append(newName);
                queryString.append("' ");
                first = false;
            }
            if(!current.getDescription().equals(newDescription)){
                queryString.append( (first ? " " : ", ") );
                queryString.append("description = '");
                queryString.append(newDescription);
                queryString.append("' ");
                first = false;
            }
            if(!current.getColor().equals(newColor)){
                queryString.append( (first ? " " : ", ") );
                queryString.append("color = '");
                queryString.append(newColor);
                queryString.append("' ");
            }

            queryString.append("WHERE id = "+flightTag.hashCode());
            System.out.println("Query String Update: "+queryString.toString());
            PreparedStatement query = connection.prepareStatement(queryString.toString());
            query.executeUpdate();

            return getTag(connection, flightTag.hashCode());
        }
        return null; //this should never happen, it violates the precondition!
    }

    /**
     * Creates a tag in the database tables
     * @param fleetId the fleetId to use
     * @param flightId the flightId to use 
     * @param name the name of the new tag (has to be unique!)
     * @param description the description of the new tag
     * @param color the color of the new tag
     * @param connection the database connection
     * @return the new FlightTag instance
     * @throws SQLException if there is an error with the database query
     */
    public static FlightTag createTag(int fleetId, int flightId, String name, String description, String color, Connection connection) throws SQLException{
        String queryString = "INSERT INTO flight_tags (fleet_id, name, description, color) VALUES(?,?,?,?)";

        PreparedStatement stmt = connection.prepareStatement(queryString, Statement.RETURN_GENERATED_KEYS);
        
        stmt.setInt(1, fleetId);
        stmt.setString(2, name);
        stmt.setString(3, description);
        stmt.setString(4, color);

        stmt.executeUpdate();

        ResultSet resultSet = stmt.getGeneratedKeys();

        int index = -1;

        if(resultSet.next()){
            index = resultSet.getInt(1);
        }

        System.out.println(index);
        associateTag(flightId, index, connection);

        return new FlightTag(index, fleetId, name, description, color);
    }

    public static void addSimAircraft(Connection connection, int fleetId, String path) throws SQLException{
        String queryString = "INSERT INTO sim_aircraft (fleet_id, path) VALUES(?,?)";

        PreparedStatement query = connection.prepareStatement(queryString);
        query.setInt(1, fleetId);
        query.setString(2, path);

        query.executeUpdate();
    }

    public static void removeSimAircraft(Connection connection, int fleetId, String path) throws SQLException{
        String queryString = "DELETE FROM sim_aircraft WHERE fleet_id = ? AND path = ?";

        PreparedStatement query = connection.prepareStatement(queryString);
        query.setInt(1, fleetId);
        query.setString(2, path);

        query.executeUpdate();
    }

    public static List<String> getSimAircraft(Connection connection, int fleetId) throws SQLException{
        String queryString = "SELECT path FROM sim_aircraft WHERE fleet_id = "+fleetId;

        PreparedStatement query = connection.prepareStatement(queryString);

        ResultSet resultSet = query.executeQuery();

        List<String> paths = new ArrayList<>();
        while(resultSet.next()){
            paths.add(resultSet.getString(1));
        }

        resultSet.close();
        query.close();

        return paths;
    }

    public Flight(Connection connection, ResultSet resultSet) throws SQLException {
        id = resultSet.getInt(1);
        fleetId = resultSet.getInt(2);
        uploaderId = resultSet.getInt(3);
        uploadId = resultSet.getInt(4);

        systemId = resultSet.getString(5);
        airframeNameId = resultSet.getInt(6); 
        airframeName = Airframes.getAirframeName(connection, airframeNameId);

        //this will set tailNumber and tailConfirmed
        tailNumber = Tails.getTail(connection, fleetId, systemId);
        tailConfirmed = Tails.getConfirmed(connection, fleetId, systemId);

        airframeTypeId = resultSet.getInt(7);
        airframeType = Airframes.getAirframeType(connection, airframeTypeId);

        startDateTime = resultSet.getString(8);
        endDateTime = resultSet.getString(9);
        filename = resultSet.getString(10);
        md5Hash = resultSet.getString(11);
        numberRows = resultSet.getInt(12);
        status = resultSet.getString(13);
        hasCoords = resultSet.getBoolean(14);
        hasAGL = resultSet.getBoolean(15);
        insertCompleted = resultSet.getBoolean(16);

        itinerary = Itinerary.getItinerary(connection, id);

        //Populate the tags
        this.tags = getTags(connection, id);
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

    public String getTailNumber(){
        return this.tailNumber;
    }

    /**
     * @return the airframe id for this flight
     */
    public int getAirframeNameId() {
        return airframeNameId;
    }

    /**
     * @return the airframe name for this aircraft
     */
    public String getAirframeName() {
        return airframeName;
    }

    /**
     * @return the airframe type id for this flight
     */
    public int getAirframeTypeId() {
        return airframeTypeId;
    }

    /**
     * @return the airframe type for this aircraft
     */
    public String getAirframeType() {
        return airframeType;
    }


    /**
     * Used for LOCI calcuations to determine if this Aircraft is applicable for a LOC-I index
     *
     * @return true if the aircraft is a Cessna 172SP
     */
    public boolean isC172() {
        return this.airframeName.equals("Cessna 172S");
    }

    public String getFilename() {
        return filename;
    }

    /**
     * Gets the upload id for this flight
     * @return the upload id as an int
     */
    public int getUploadId(){
        return uploadId;
    }

    /**
     * Gets the uploader id for this flight
     * @return the uploader id as an int
     */
    public int getUploaderId(){
        return uploaderId;
    }

    public int getNumberRows() {
        return numberRows;
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

    public void addDoubleTimeSeries(String name, DoubleTimeSeries doubleTimeSeries) {
        this.doubleTimeSeries.put(name, doubleTimeSeries);
    }

    public DoubleTimeSeries getDoubleTimeSeries(String name) {
        return doubleTimeSeries.get(name);
    }

    public StringTimeSeries getStringTimeSeries(String name) {
        return stringTimeSeries.get(name);
    }

    private void setMD5Hash(InputStream inputStream) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte [] hash = md.digest(inputStream.readAllBytes());
            md5Hash = DatatypeConverter.printHexBinary(hash).toLowerCase();

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            System.exit(1);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }

        //System.err.println("MD5 HASH: '" + md5Hash + "'");
    }

    public void calculateStartEndTime(String dateColumnName, String timeColumnName, String offsetColumnName) throws MalformedFlightFileException {
        StringTimeSeries dates = stringTimeSeries.get(dateColumnName);
        StringTimeSeries times = stringTimeSeries.get(timeColumnName);
        StringTimeSeries offsets = stringTimeSeries.get(offsetColumnName);

        if (dates == null) {
            throw new MalformedFlightFileException("Date column '" + dateColumnName + "' did not exist! Cannot set start/end times.");
        }

        if (times == null) {
            throw new MalformedFlightFileException("Time column '" + timeColumnName + "' did not exist! Cannot set start/end times.");
        }

        if (offsets == null) {
            throw new MalformedFlightFileException("Time column '" + offsetColumnName + "' did not exist! Cannot set start/end times.");
        }


        int dateSize = dates.size();
        int timeSize = times.size();
        int offsetSize = offsets.size();

        System.out.println("\tdate size: " + dateSize + ", time size: " + timeSize + ", offset size: " + offsetSize);

        //get the minimum sized length of each of these series, they should all be the same but 
        //if the last column was cut off it might not be the case
        int minSize = dateSize;
        if (minSize < timeSize) minSize = timeSize;
        if (minSize < offsetSize) minSize = offsetSize;

        //find the first non-null time entry
        int start = 0;
        while (start < minSize &&
                (dates.get(start) == null || dates.get(start).equals("") || 
                 times.get(start) == null || times.get(start).equals("") ||
                 offsets.get(start) == null || offsets.get(start).equals("") || offsets.get(start).equals("+19:00"))) {

            start++;
        }

        System.out.println("\tfirst date time and offset not null at index: " + start);

        if (start >= minSize) {
            throw new MalformedFlightFileException("Date, Time or Offset columns were all null! Cannot set start/end times.");
        }

        //find the last full date time offset entry row
        int end = minSize - 1;
        while (end >= 0 &&
                (dates.get(end) == null || dates.get(end).equals("") || 
                 times.get(end) == null || times.get(end).equals("") ||
                 offsets.get(end) == null || offsets.get(end).equals(""))) {

            end--;
        }

        String startDate = dates.get(start);
        String startTime = times.get(start);
        String startOffset = offsets.get(start);

        String endDate = dates.get(end);
        String endTime = times.get(end);
        String endOffset = offsets.get(end);

        System.out.println("\t\t\tfirst not null  " + start + " -- " + startDate + " " + startTime + " " + startOffset);
        System.out.println("\t\t\tlast not null   " + endDate + " " + endTime + " " + endOffset);

        OffsetDateTime startODT = TimeUtils.convertToOffset(startDate, startTime, startOffset, "+00:00");
        OffsetDateTime endODT = TimeUtils.convertToOffset(endDate, endTime, endOffset, "+00:00");

        if (startODT.isAfter(endODT)) {
            startDateTime = null;
            endDateTime = null;

            throw new MalformedFlightFileException("Corrupt time data in flight file, start time was after the end time");
        }

        startDateTime = startODT.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        endDateTime = endODT.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    private void initialize(InputStream inputStream) throws FatalFlightFileException, IOException {
        numberRows = 0;
        ArrayList<ArrayList<String>> csvValues;

        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));

        dataTypes = new ArrayList<String>();
        headers = new ArrayList<String>();

        //file information -- this is the first line
        fileInformation = bufferedReader.readLine();
        //LOG.info("fileInformation line is: " + fileInformation);
        if (fileInformation == null || fileInformation.length() == 0) throw new FatalFlightFileException("The flight file was empty.");
        if (fileInformation.charAt(0) != '#') throw new FatalFlightFileException("First line of the flight file should begin with a '#' and contain flight recorder information.");

        String[] infoParts = fileInformation.split(",");
        airframeName = null;
        try {
            for (int i = 1; i < infoParts.length; i++) {
                if (infoParts[i].trim().length() == 0) continue;

                //System.err.println("splitting key/value: '" + infoParts[i] + "'");
                String subParts[] = infoParts[i].trim().split("=");
                String key = subParts[0];
                String value = subParts[1];

                //System.err.println("key: '" + key + "'");
                //System.err.println("value: '" + value + "'");

                if (key.equals("airframe_name")) {
                    airframeName = value.substring(1, value.length() - 1);

                    //throw an error for 'Unknown Aircraft'
                    if (airframeName.equals("Unknown Aircraft")) {
                        throw new FatalFlightFileException("Flight airframe name was 'Unknown Aircraft', please fix and re-upload so the flight can be properly identified and processed.");
                    }


                    if (airframeName.equals("Diamond DA 40")) {
                        airframeName = "Diamond DA40";

                    } else if (airframeName.equals("Garmin Flight Display") && fleetId == 1 /*This is a hack for UND who has their airframe names set up incorrectly for their helicopters*/) {
                        airframeName = "R44";
                    }

                    if (airframeName.equals("Cessna 172R") ||
                            airframeName.equals("Cessna 172S") ||
                            airframeName.equals("Cessna 172T") ||
                            airframeName.equals("Cessna 182T") ||
                            airframeName.equals("Cirrus SR20") ||
                            airframeName.equals("Diamond DA40") ||
                            airframeName.equals("Diamond DA 40 F") ||
                            airframeName.equals("Diamond DA40NG") ||
                            airframeName.equals("Diamond DA42NG") ||
                            airframeName.equals("PA-28-181") ||
                            airframeName.equals("PA-44-180") ||
                            airframeName.equals("Piper PA-46-500TP Meridian") ||
                            airframeName.contains("Boeing") ||
                            airframeName.contains("Airbus") ||
                            airframeName.contains("Gulfstream") ||
                            airframeName.equals("Beechcraft A36/G36")) {
                        airframeType = "Fixed Wing";
                    } else if (airframeName.equals("R44")) {
                        airframeType = "Rotorcraft";
                    } else {
                        System.err.println("Could not import flight because the aircraft type was unknown for the following airframe name: '" + airframeName + "'");
                        System.err.println("Please add this to the the `airframe_type` table in the database and update this method.");
                        System.exit(1);
                    }

                } else if (key.equals("system_id")) {
                    systemId = value.substring(1, value.length() - 1);
                }
            }
        } catch (Exception e) {
            //LOG.info("parsting flight information threw exception: " + e);
            //e.printStackTrace();
            throw new FatalFlightFileException("Flight information line was not properly formed with key value pairs.", e);
        }

        if (airframeName == null)  throw new FatalFlightFileException("Flight information (first line of flight file) does not contain an 'airframe_name' key/value pair.");
        System.err.println("detected airframe type: '" + airframeName + "'");

        if (systemId == null)  throw new FatalFlightFileException("Flight information (first line of flight file) does not contain an 'system_id' key/value pair.");
        System.err.println("detected airframe type: '" + systemId + "'");

        //the next line is the column data types
        String dataTypesLine = bufferedReader.readLine();
        if (dataTypesLine.charAt(0) != '#') throw new FatalFlightFileException("Second line of the flight file should begin with a '#' and contain column data types.");
        dataTypesLine = dataTypesLine.substring(1);

        dataTypes.addAll( Arrays.asList( dataTypesLine.split("\\,", -1) ) );
        dataTypes.replaceAll(String::trim);

        //the next line is the column headers
        String headersLine = bufferedReader.readLine();
        System.out.println("Headers line is: " + headersLine);
        headers.addAll( Arrays.asList( headersLine.split("\\,", -1) ) );
        headers.replaceAll(String::trim);

        if (dataTypes.size() != headers.size()) {
            throw new FatalFlightFileException("Number of columns in the header line (" + headers.size() + ") != number of columns in the dataTypes line (" + dataTypes.size() + ")");
        }

        //initialize a sub-ArrayList for each column
        csvValues = new ArrayList<ArrayList<String>>();
        for (int i = 0; i < headers.size(); i++) {
            csvValues.add(new ArrayList<String>());
        }

        int lineNumber = 3;
        boolean lastLineWarning = false;

        String line;
        String lastWarning = "";
        while ((line = bufferedReader.readLine()) != null) {

            if (line.contains("Lcl Time")) {
                System.out.println("SKIPPING line[" + lineNumber + "]: " + line + " (THIS SHOULD NOT HAPPEN)");
                continue;
            }

            //if the line is empty, skip it
            if (line.trim().length() == 0) continue;
            //this line is a comment, skip it
            if (line.charAt(0) == '#') continue;

            //split up the values by commas into our array of strings
            String[] values = line.split("\\,", -1);

            if (lastLineWarning) {
                if (values.length != headers.size()) {
                    String newWarning = "ERROR: line " + lineNumber + " had a different number of values (" + values.length + ") than the number of columns in the file (" + headers.size() + ").";
                    System.err.println(newWarning);
                    System.err.println("ERROR: Two line errors in a row means the flight file is corrupt.");
                    lastLineWarning = true;

                    String errorMessage = "Multiple lines the flight file had extra or missing values, which means the flight file is corrupt:\n";
                    errorMessage += lastWarning + "\n";
                    errorMessage += newWarning;

                    throw new FatalFlightFileException(errorMessage);
                } else {
                    throw new FatalFlightFileException("A line in the middle of the flight file was missing values, which means the flight file is corrupt:\n" + lastWarning);
                }
            } else {
                if (values.length != headers.size()) {
                    lastWarning = "WARNING: line " + lineNumber + " had a different number of values (" + values.length + ") than the number of columns in the file. Not an error if it was the last line in the file.";
                    System.err.println(lastWarning);
                    lastLineWarning = true;
                    continue;
                }
            }

            //for each CSV value
            for (int i = 0; i < values.length; i++) {
                //add this to the respective column in the csvValues ArrayList, trimming the whitespace around it
                csvValues.get(i).add( values[i].trim() );
            }

            lineNumber++;
            numberRows++;
        }

        if (lastLineWarning) {
            System.err.println("WARNING: last line of the file was cut short and ignored.");
        }

        System.out.println("parsed " + lineNumber + " lines.");

        for (int i = 0; i < csvValues.size(); i++) {
            //check to see if each column is a column of doubles or a column of strings

            //for each column, find the first non empty value and check to see if it is a double
            boolean isDoubleList = false;
            ArrayList<String> current = csvValues.get(i);

            for (int j = 0; j < current.size(); j++) {
                String currentValue = current.get(j);
                if (currentValue.length() > 0) {
                    try {
                        Double.parseDouble(currentValue);
                        isDoubleList = true;
                    } catch (NumberFormatException e) {
                        isDoubleList = false;
                        break;
                    }
                }
            }

            if (isDoubleList) {
                //System.out.println(headers.get(i) + " is a DOUBLE column, ArrayList size: " + current.size());
                //System.out.println(current);
                DoubleTimeSeries dts = new DoubleTimeSeries(headers.get(i), dataTypes.get(i), current);
                if (dts.validCount() > 0) {
                    doubleTimeSeries.put(headers.get(i), dts);
                } else {
                    System.err.println("WARNING: dropping double column '" + headers.get(i) + "' because all entries were empty.");
                }

            } else {
                //System.out.println(headers.get(i) + " is a STRING column, ArrayList size: " + current.size());
                //System.out.println(current);
                StringTimeSeries sts = new StringTimeSeries(headers.get(i), dataTypes.get(i), current);
                if (sts.validCount() > 0) {
                    stringTimeSeries.put(headers.get(i), sts);
                } else {
                    System.err.println("WARNING: dropping string column '" + headers.get(i) + "' because all entries were empty.");
                }
            }
        }
    }

    private InputStream getReusableInputStream(InputStream inputStream) throws IOException {
        if (inputStream.markSupported() == false) {
            InputStream reusableInputStream = new ByteArrayInputStream(inputStream.readAllBytes());
            inputStream.close();
            // now the stream should support 'mark' and 'reset'
            return reusableInputStream;
        } else {
            return inputStream;
        }
    }

    private void process(InputStream inputStream) throws IOException, FatalFlightFileException {
        initialize(inputStream);

        //TODO: these may be different for different airframes/flight
        //data recorders. depending on the airframe/flight data recorder 
        //we should specify these.

        try {
            calculateStartEndTime("Lcl Date", "Lcl Time", "UTCOfst");
        } catch (MalformedFlightFileException e) {
            exceptions.add(e);
        }

        try {
            calculateAGL("AltAGL", "AltMSL", "Latitude", "Longitude");
        } catch (MalformedFlightFileException e) {
            exceptions.add(e);
        }

        try {
            calculateAirportProximity("Latitude", "Longitude", "AltAGL");
        } catch (MalformedFlightFileException e) {
            exceptions.add(e);
        }

        try {
            calculateTotalFuel(new String[]{"FQtyL", "FQtyR"}, "Total Fuel");
        } catch (MalformedFlightFileException e) {
            exceptions.add(e);
        }

        try {
            calculateLaggedAltMSL("AltMSL", 10, "AltMSL Lag Diff");
        } catch (MalformedFlightFileException e) {
            exceptions.add(e);
        }

        try {
            if (airframeName.equals("Cessna 172S") || airframeName.equals("Cessna 172R")) {
                String egtNames[] = {"E1 EGT1", "E1 EGT2", "E1 EGT3", "E1 EGT4"};
                calculateDivergence(egtNames, "E1 EGT Divergence", "deg F");

            } else if (airframeName.equals("PA-28-181")) {
                String chtNames[] = {"E1 CHT1", "E1 CHT2", "E1 CHT3", "E1 CHT4"};
                calculateDivergence(chtNames, "E1 CHT Divergence", "deg F");

                String egtNames[] = {"E1 EGT1", "E1 EGT2", "E1 EGT3", "E1 EGT4"};
                calculateDivergence(egtNames, "E1 EGT Divergence", "deg F");

            } else if (airframeName.equals("PA-44-180")) {
                String egt1Names[] = {"E1 EGT1", "E1 EGT2", "E1 EGT3", "E1 EGT4"};
                calculateDivergence(egt1Names, "E1 EGT Divergence", "deg F");

                String egt2Names[] = {"E2 EGT1", "E2 EGT2", "E2 EGT3", "E2 EGT4"};
                calculateDivergence(egt2Names, "E2 EGT Divergence", "deg F");


            } else if (airframeName.equals("Cirrus SR20") || airframeName.equals("Cessna 182T") || airframeName.equals("Beechcraft A36/G36")) {
                String chtNames[] = {"E1 CHT1", "E1 CHT2", "E1 CHT3", "E1 CHT4", "E1 CHT5", "E1 CHT6"};
                calculateDivergence(chtNames, "E1 CHT Divergence", "deg F");

                String egtNames[] = {"E1 EGT1", "E1 EGT2", "E1 EGT3", "E1 EGT4", "E1 EGT5", "E1 EGT6"};
                calculateDivergence(egtNames, "E1 EGT Divergence", "deg F");

            } else if (airframeName.equals("Diamond DA 40") || airframeName.equals("Diamond DA 40 F") || airframeName.equals("Diamond DA40")) {
                String chtNames[] = {"E1 CHT1", "E1 CHT2", "E1 CHT3", "E1 CHT4"};
                calculateDivergence(chtNames, "E1 CHT Divergence", "deg F");

                String egtNames[] = {"E1 EGT1", "E1 EGT2", "E1 EGT3", "E1 EGT4"};
                calculateDivergence(egtNames, "E1 EGT Divergence", "deg F");

            } else if (airframeName.equals("R44")) {
                //This is a helicopter, we can't calculate these divergences

            } else if (airframeName.equals("Garmin Flight Display") || airframeName.equals("Diamond DA42NG") || airframeName.equals("Diamond DA40NG") || airframeName.equals("Piper PA-46-500TP Meridian") || airframeName.equals("Unknown Aircraft") || airframeName.contains("Gulfstream") || airframeName.contains("Boeing") || airframeName.contains("Airbus")) {
                LOG.warning("Cannot calculate engine divergences because airframe data recorder does not track CHT and/or EGT: '" + airframeName + "'");
                exceptions.add(new MalformedFlightFileException("Cannot calculate engine variances because airframe '" + airframeName +" does not track CHT and/or EGT"));

            } else {
                LOG.severe("Cannot calculate engine variances! Unknown airframe type: '" + airframeName + "'");
                System.exit(1);
            }

            runLOCICalculations();

        } catch (MalformedFlightFileException e) {
            exceptions.add(e);
        }

        try {
            if (hasCoords && hasAGL) {
                calculateItinerary("GndSpd", "E1 RPM");
            }
        } catch (MalformedFlightFileException e) {
            exceptions.add(e);
        }

    }

    private void checkExceptions() {
        if (exceptions.size() > 0) {
            status = "WARNING";
            System.err.println("Flight produced " + exceptions.size() + " exceptions.");

            /*
               for (MalformedFlightFileException e : exceptions) {
               e.printStackTrace();
               }
               */
        } else {
            status = "SUCCESS";
        }
    }

    private void checkIfExists(Connection connection) throws FlightAlreadyExistsException {
        try {
            PreparedStatement preparedStatement = connection.prepareStatement("SELECT uploads.id, uploads.filename, flights.filename FROM flights, uploads WHERE flights.md5_hash = ?");
            preparedStatement.setString(1, md5Hash);
            ResultSet resultSet = preparedStatement.executeQuery();

            if (resultSet.next()) {
                String uploadFilename = resultSet.getString(2);
                String flightsFilename = resultSet.getString(3);

                preparedStatement.close();
                resultSet.close();

                throw new FlightAlreadyExistsException("Flight already exists in database, uploaded in zip file '" + uploadFilename + "' as '" + flightsFilename + "'");
            }

            preparedStatement.close();
            resultSet.close();

        } catch (SQLException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    public Flight(int fleetId, String zipEntryName, InputStream inputStream, Connection connection) throws IOException, FatalFlightFileException, FlightAlreadyExistsException {
        this.fleetId = fleetId;
        this.filename = zipEntryName;
        this.tailConfirmed = false;

        /*
        if (!filename.contains("/")) {
            throw new FatalFlightFileException("The flight file was not in a directory in the zip file. Flight files should be in a directory with the name of their tail number (or other aircraft identifier).");
        }
        */

        String[] parts = zipEntryName.split("/");
        if (parts.length <= 1) {
            suggestedTailNumber = null;
        } else {
            this.suggestedTailNumber = parts[0];
            if (suggestedTailNumber.equals("")) suggestedTailNumber = null;
        }

        try {
            inputStream = getReusableInputStream(inputStream);

            int length = inputStream.available();
            inputStream.mark(length);
            setMD5Hash(inputStream);

            //check to see if a flight with this MD5 hash already exists in the database
            if (connection != null) checkIfExists(connection);

            inputStream.reset();
            process(inputStream);

        } catch (FatalFlightFileException e) {
            status = "WARNING";
            throw e;
        } catch (IOException e) {
            status = "WARNING";
            throw e;
        }

        checkExceptions();
    }

    /**
     * Runs the Loss of Control/Stall Index calculations
     *
     * @author <a href = "mailto:apl1341@cs.rit.edu">Aidan LaBella @ RIT CS</a>
     */
    public void runLOCICalculations() throws MalformedFlightFileException {
        checkCalculationParameters(STALL_PROB, STALL_DEPENDENCIES);

        if (this.isC172()) {
            CalculatedDoubleTimeSeries cas = new CalculatedDoubleTimeSeries(CAS, "knots", true, this);
            cas.create(index -> {
                DoubleTimeSeries ias = getDoubleTimeSeries(IAS);
                double iasValue = ias.get(index);
                
                if (iasValue < 70.d) {
                    iasValue = (0.7d * iasValue) + 20.667;
                }

                return iasValue;
            });
        }

        CalculatedDoubleTimeSeries vspdCalculated = new CalculatedDoubleTimeSeries(VSPD_CALCULATED, "ft/min", false, this);
        vspdCalculated.create(new VSPDRegression(this));

        CalculatedDoubleTimeSeries densityRatio = new CalculatedDoubleTimeSeries(DENSITY_RATIO, "ratio", false, this);
        densityRatio.create(index -> {
            DoubleTimeSeries baroA = getDoubleTimeSeries(BARO_A);
            DoubleTimeSeries oat = getDoubleTimeSeries(OAT);

            double pressRatio = baroA.get(index) / STD_PRESS_INHG;
            double tempRatio = (273 + oat.get(index)) / 288;

            return pressRatio / tempRatio;
        });

        CalculatedDoubleTimeSeries tasFtMin = new CalculatedDoubleTimeSeries(TAS_FTMIN, "ft/min", false, this);
        tasFtMin.create(index -> {
            DoubleTimeSeries airspeed = this.isC172() ? getDoubleTimeSeries(CAS) : getDoubleTimeSeries(IAS);

            return (airspeed.get(index) * Math.pow(densityRatio.get(index), -0.5)) * ((double) 6076 / 60);
        });

        CalculatedDoubleTimeSeries aoaSimple = new CalculatedDoubleTimeSeries(AOA_SIMPLE, "degrees", true, this);
        aoaSimple.create(index -> {
            DoubleTimeSeries pitch = getDoubleTimeSeries(PITCH);

            double vspdGeo = vspdCalculated.get(index) * Math.pow(densityRatio.get(index), -0.5);
            double fltPthAngle = Math.asin(vspdGeo / tasFtMin.get(index));
            fltPthAngle = fltPthAngle * (180 / Math.PI);
            double value = pitch.get(index) - fltPthAngle;

            return value; 
        });

        CalculatedDoubleTimeSeries stallIndex = new CalculatedDoubleTimeSeries(STALL_PROB, "index", true, this);
        stallIndex.create(index -> {
            return (Math.min(((Math.abs(aoaSimple.get(index) / AOA_CRIT)) * 100), 100)) / 100;
        });

        if (this.isC172()) {
            // We still can only perform a LOC-I calculation on the Skyhawks
            // This can be changed down the road
            checkCalculationParameters(LOCI, LOCI_DEPENDENCIES);
            DoubleTimeSeries hdg = getDoubleTimeSeries(HDG); 
            DoubleTimeSeries hdgLagged = hdg.lag(YAW_RATE_LAG);

            CalculatedDoubleTimeSeries coordIndex = new CalculatedDoubleTimeSeries(PRO_SPIN_FORCE, "index", true, this);
            coordIndex.create(index -> {
                DoubleTimeSeries roll = getDoubleTimeSeries(ROLL);
                DoubleTimeSeries tas = getDoubleTimeSeries(TAS_FTMIN);

                double laggedHdg = hdgLagged.get(index);
                double yawRate = Double.isNaN(laggedHdg) ? 0 : 
                    180 - Math.abs(180 - Math.abs(hdg.get(index) - laggedHdg) % 360);

                double yawComp = yawRate * COMP_CONV; 
                double vrComp = ((tas.get(index) / 60) * yawComp);
                double rollComp = roll.get(index) * COMP_CONV;
                double ctComp = Math.sin(rollComp) * 32.2;
                double value = Math.min(((Math.abs(ctComp - vrComp) * 100) / PROSPIN_LIM), 100);

                return value;
            });
            
            CalculatedDoubleTimeSeries loci = new CalculatedDoubleTimeSeries(LOCI, "index", true, this);
            loci.create(index -> {
                double prob = (stallIndex.get(index) * getDoubleTimeSeries(PRO_SPIN_FORCE).get(index));
                return prob / 100;
            });
        }
    }

    public Flight(int fleetId, String filename, Connection connection) throws IOException, FatalFlightFileException, FlightAlreadyExistsException {
        this.fleetId = fleetId;
        this.filename = filename;
        String[] parts = filename.split("/");
        this.suggestedTailNumber = parts[0];
        this.tailConfirmed = false;

        try {
            File file = new File(this.filename);
            FileInputStream fileInputStream = new FileInputStream(file);

            InputStream inputStream = getReusableInputStream(fileInputStream);

            int length = inputStream.available();
            inputStream.mark(length);
            setMD5Hash(inputStream);

            //check to see if a flight with this MD5 hash already exists in the database
            if (connection != null) checkIfExists(connection);

            inputStream.reset();
            process(inputStream);

       //} catch (FileNotFoundException e) {
       //   System.err.println("ERROR: could not find flight file '" + filename + "'");
       //   exceptions.add(e);
        } catch (FatalFlightFileException e) {
            status = "WARNING";
            throw e;
        } catch (IOException e) {
            status = "WARNING";
            throw e;
        }

        checkExceptions();
    }

    public void calculateLaggedAltMSL(String altMSLColumnName, int lag, String laggedColumnName) throws MalformedFlightFileException {
        headers.add(laggedColumnName);
        dataTypes.add("ft msl");

        DoubleTimeSeries altMSL = doubleTimeSeries.get(altMSLColumnName);
        if (altMSL == null) {
            throw new MalformedFlightFileException("Cannot calculate '" + laggedColumnName + "' as parameter '" + altMSLColumnName + "' was missing.");
        }

        DoubleTimeSeries laggedAltMSL = new DoubleTimeSeries(laggedColumnName, "ft msl");

        for (int i = 0; i < altMSL.size(); i++) {
            if (i < lag) laggedAltMSL.add(0.0);
            else {
                laggedAltMSL.add(altMSL.get(i) - altMSL.get(i - lag));
            }
        }

        doubleTimeSeries.put(laggedColumnName, laggedAltMSL);
    }


    public void calculateDivergence(String[] columnNames, String varianceColumnName, String varianceDataType) throws MalformedFlightFileException {
        headers.add(varianceColumnName);
        dataTypes.add(varianceDataType);

        DoubleTimeSeries columns[] = new DoubleTimeSeries[columnNames.length];
        for (int i = 0; i < columns.length; i++) {
            columns[i] = doubleTimeSeries.get(columnNames[i]);

            if (columns[i] == null) {
                throw new MalformedFlightFileException("Cannot calculate '" + varianceColumnName + "' as parameter '" + columnNames[i] + "' was missing.");
            }
        }

        DoubleTimeSeries variance = new DoubleTimeSeries(varianceColumnName, varianceDataType);

        for (int i = 0; i < columns[0].size(); i++) {
            double max = -Double.MAX_VALUE;
            double min = Double.MAX_VALUE;

            for (int j = 0; j < columns.length; j++) {
                double current = columns[j].get(i);
                if (!Double.isNaN(current) && current > max) max = columns[j].get(i);
                if (!Double.isNaN(current) && current < min) min = columns[j].get(i);
            }

            double v = 0;
            if (max != -Double.MAX_VALUE && min != Double.MAX_VALUE) {
                v = max - min;
            }

            variance.add(v);
        }

        doubleTimeSeries.put(varianceColumnName, variance);
    }


    public void calculateTotalFuel(String[] fuelColumnNames, String totalFuelColumnName) throws MalformedFlightFileException {
        headers.add(totalFuelColumnName);
        dataTypes.add("gals");


        DoubleTimeSeries fuelQuantities[] = new DoubleTimeSeries[fuelColumnNames.length];
        for (int i = 0; i < fuelQuantities.length; i++) {
            fuelQuantities[i] = doubleTimeSeries.get(fuelColumnNames[i]);

            if (fuelQuantities[i] == null) {
                throw new MalformedFlightFileException("Cannot calculate 'Total Fuel' as fuel parameter '" + fuelColumnNames[i] + "' was missing.");
            }
        }

        DoubleTimeSeries totalFuel = new DoubleTimeSeries(totalFuelColumnName, "gals");

        for (int i = 0; i < fuelQuantities[0].size(); i++) {
            double totalFuelValue = 0.0;
            for (int j = 0; j < fuelQuantities.length; j++) {
                totalFuelValue += fuelQuantities[j].get(i);
            }
            totalFuel.add(totalFuelValue);

        }

        doubleTimeSeries.put(totalFuelColumnName, totalFuel);

    }

    public void calculateAGL(String altitudeAGLColumnName, String altitudeMSLColumnName, String latitudeColumnName, String longitudeColumnName) throws MalformedFlightFileException {
        //calculates altitudeAGL (above ground level) from altitudeMSL (mean sea levl)
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

            //should be initialized to false, but lets make sure
            hasCoords = false;
            hasAGL = false;
            throw new MalformedFlightFileException(message);
        }
        hasCoords = true;
        hasAGL = true;

        DoubleTimeSeries altitudeAGLTS = new DoubleTimeSeries(altitudeAGLColumnName, "ft agl");

        for (int i = 0; i < altitudeMSLTS.size(); i++) {
            double altitudeMSL = altitudeMSLTS.get(i);
            double latitude = latitudeTS.get(i);
            double longitude = longitudeTS.get(i);

            //System.err.println("getting AGL for latitude: " + latitude + ", " + longitude);

            if (Double.isNaN(altitudeMSL) || Double.isNaN(latitude) || Double.isNaN(longitude)) {
                altitudeAGLTS.add(Double.NaN);
                //System.err.println("result is: " + Double.NaN);
                continue;
            }

            try {
                int altitudeAGL = TerrainCache.getAltitudeFt(altitudeMSL, latitude, longitude);

                altitudeAGLTS.add(altitudeAGL);

                //the terrain cache will not be able to find the file if the lat/long is outside of the USA
            } catch (NoSuchFileException e) {
                System.err.println("ERROR: could not read terrain file: " + e);

                hasAGL = false;
                throw new MalformedFlightFileException("Could not calculate AGL for this flight as it had latitudes/longitudes outside of the United States.");
            }

            //System.out.println("msl: " + altitudeMSL + ", agl: " + altitudeAGL);
        }

        doubleTimeSeries.put(altitudeAGLColumnName, altitudeAGLTS);
    }

    public void calculateAirportProximity(String latitudeColumnName, String longitudeColumnName, String altitudeAGLColumnName) throws MalformedFlightFileException {
        //calculates if the aircraft is within maxAirportDistance from an airport

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
                message += "'" + altitudeAGLColumnName+ "'";
                count++;
            }

            message += " column";
            if (count >= 2) message += "s";
            message += ".";

            //should be initialized to false, but lets make sure
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

        StringTimeSeries nearestAirportTS = new StringTimeSeries("NearestAirport", "txt");
        stringTimeSeries.put("NearestAirport", nearestAirportTS);
        DoubleTimeSeries airportDistanceTS = new DoubleTimeSeries("AirportDistance", "ft");
        doubleTimeSeries.put("AirportDistance", airportDistanceTS);

        StringTimeSeries nearestRunwayTS = new StringTimeSeries("NearestRunway", "txt");
        stringTimeSeries.put("NearestRunway", nearestRunwayTS);
        DoubleTimeSeries runwayDistanceTS = new DoubleTimeSeries("RunwayDistance", "ft");
        doubleTimeSeries.put("RunwayDistance", runwayDistanceTS);


        for (int i = 0; i < latitudeTS.size(); i++) {
            double latitude = latitudeTS.get(i);
            double longitude = longitudeTS.get(i);
            double altitudeAGL = altitudeAGLTS.get(i);

            MutableDouble airportDistance = new MutableDouble();
            Airport airport = null;
            if (altitudeAGL <= 2000) {
                airport = Airports.getNearestAirportWithin(latitude, longitude, MAX_AIRPORT_DISTANCE_FT, airportDistance);
            }

            if (airport == null) {
                nearestAirportTS.add("");
                airportDistanceTS.add(Double.NaN);
                nearestRunwayTS.add("");
                runwayDistanceTS.add(Double.NaN);

                //System.out.println(latitude + ", " + longitude + ", null, null, null, null");
            } else {
                nearestAirportTS.add(airport.iataCode);
                airportDistanceTS.add(airportDistance.get());

                MutableDouble runwayDistance = new MutableDouble();
                Runway runway = airport.getNearestRunwayWithin(latitude, longitude, MAX_RUNWAY_DISTANCE_FT, runwayDistance);
                if (runway == null) {
                    nearestRunwayTS.add("");
                    runwayDistanceTS.add(Double.NaN);
                    //System.out.println(latitude + ", " + longitude + ", " + airport.iataCode + ", " + airportDistance.get() + ", " + null + ", " + null);
                } else {
                    nearestRunwayTS.add(runway.name);
                    runwayDistanceTS.add(runwayDistance.get());
                    //System.out.println(latitude + ", " + longitude + ", " + airport.iataCode + ", " + airportDistance.get() + ", " + runway.name + ", " + runwayDistance.get());
                }
            }

        }
    }

    public void calculateItinerary(String groundSpeedColumnName, String rpmColumnName) throws MalformedFlightFileException {
        //cannot calculate the itinerary without airport/runway calculate, which requires
        //lat and longs
        if (!hasCoords) return;

        DoubleTimeSeries groundSpeed = doubleTimeSeries.get(groundSpeedColumnName);
        DoubleTimeSeries rpm = doubleTimeSeries.get(rpmColumnName);
        //DoubleTimeSeries groundSpeed = doubleTimeSeries.get("GndSpd");
        //DoubleTimeSeries rpm = doubleTimeSeries.get("E1 RPM");

        StringTimeSeries nearestAirportTS = stringTimeSeries.get("NearestAirport");
        DoubleTimeSeries airportDistanceTS = doubleTimeSeries.get("AirportDistance");
        DoubleTimeSeries altitudeAGL = doubleTimeSeries.get("AltAGL");

        StringTimeSeries nearestRunwayTS = stringTimeSeries.get("NearestRunway");
        DoubleTimeSeries runwayDistanceTS = doubleTimeSeries.get("RunwayDistance");

        if (groundSpeed == null || rpm == null) {
            String message = "Cannot calculate itinerary, flight file had empty or missing ";

            int count = 0;
            if (groundSpeed == null) {
                message += "'" + groundSpeedColumnName + "'";
                count++;
            }

            if (rpm == null) {
                if (count > 0) message += " and ";
                message += "'" + rpmColumnName + "'";
                count++;
            }

            message += " column";
            if (count >= 2) message += "s";
            message += ".";

            //should be initialized to false, but lets make sure
            hasCoords = false;
            throw new MalformedFlightFileException(message);
        }
        hasCoords = true;


        itinerary.clear();

        Itinerary currentItinerary = null;
        for (int i = 1; i < nearestAirportTS.size(); i++) {
            String airport = nearestAirportTS.get(i);
            String runway = nearestRunwayTS.get(i);

            if (airport != null && !airport.equals("")) {
                //We've gotten close to an airport, so create a stop if there
                //isn't one.  If there is one, update the runway being visited.
                //If the airport is a new airport (this shouldn't happen really),
                //then create a new stop.
                if (currentItinerary == null) {
                    currentItinerary = new Itinerary(airport, runway, i, altitudeAGL.get(i), airportDistanceTS.get(i), runwayDistanceTS.get(i), groundSpeed.get(i), rpm.get(i));
                } else if (airport.equals(currentItinerary.getAirport())) {
                    currentItinerary.update(runway, i, altitudeAGL.get(i), airportDistanceTS.get(i), runwayDistanceTS.get(i), groundSpeed.get(i), rpm.get(i));
                } else {
                    currentItinerary.selectBestRunway();
                    if (currentItinerary.wasApproach()) itinerary.add(currentItinerary);
                    currentItinerary = new Itinerary(airport, runway, i, altitudeAGL.get(i), airportDistanceTS.get(i), runwayDistanceTS.get(i), groundSpeed.get(i), rpm.get(i));
                }

            } else {
                //aiport is null, so if there was an airport being visited
                //then we can determine it's runway and add it to the itinerary
                if (currentItinerary != null) {
                    currentItinerary.selectBestRunway();
                    if (currentItinerary.wasApproach()) itinerary.add(currentItinerary);
                }

                //set the currentItinerary to null until we approach another
                //airport
                currentItinerary = null;
            }
        }

        //dont forget to add the last stop in the itinerary if it wasn't set to null
        if (currentItinerary != null) {
            currentItinerary.selectBestRunway();
            if (currentItinerary.wasApproach()) itinerary.add(currentItinerary);
        }

        ///////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // setting and determining itinerary type
        int itinerary_size = itinerary.size();
        for (int i = 0; i < itinerary_size; i++) {
            itinerary.get(i).determineType();
        }
        ///////////////////////////////////////////////////////////////////////////////////////////////////////////////

        System.err.println("Itinerary:");
        for (int i = 0; i < itinerary.size(); i++) {
            System.err.println(itinerary.get(i));
        }
    }

    public void printValues(String[] requestedHeaders) {
        System.out.println("Values:");

        for (int i = 0; i < requestedHeaders.length; i++) {
            if (i > 0) System.out.print(",");
            System.out.printf("%16s", requestedHeaders[i]);
        }
        System.out.println();

        for (int i = 0; i < requestedHeaders.length; i++) {
            String header = requestedHeaders[i];
            if (i > 0) System.out.print(",");

            if (doubleTimeSeries.containsKey(header)) {
                System.out.printf("%16s", doubleTimeSeries.get(header).size());
            } else if (stringTimeSeries.containsKey(header)) {
                System.out.printf("%16s", stringTimeSeries.get(header).size());
            } else {
                System.out.println("ERROR: header '" + header + "' not present in flight file: '" + filename + "'");
                System.exit(1);
            }
        }
        System.out.println();

        for (int row = 0; row < numberRows; row++) {
            boolean first = true;
            for (int i = 0; i < requestedHeaders.length; i++) {
                String header = requestedHeaders[i];

                if (!first) System.out.print(",");
                first = false;

                if (doubleTimeSeries.containsKey(header)) {
                    System.out.printf("%16.8f", doubleTimeSeries.get(header).get(row));
                } else if (stringTimeSeries.containsKey(header)) {
                    System.out.printf("%16s", stringTimeSeries.get(header).get(row));
                } else {
                    System.out.println("ERROR: header '" + header + "' not present in flight file: '" + filename + "'");
                    System.exit(1);
                }
            }
            System.out.println();
        }
        System.out.println();
    }

    public void updateDatabase(Connection connection, int uploadId, int uploaderId, int fleetId) {
        this.fleetId = fleetId;
        this.uploaderId = uploaderId;
        this.uploadId = uploadId;

        try {
            //first check and see if the airframe and tail number already exist in the database for this 
            //flight
            airframeNameId = Airframes.getNameId(connection, airframeName);
            airframeTypeId = Airframes.getTypeId(connection, airframeType);
            Airframes.setAirframeFleet(connection, airframeNameId, fleetId);

            Tails.setSuggestedTail(connection, fleetId, systemId, suggestedTailNumber);
            tailNumber = Tails.getTail(connection, fleetId, systemId);
            tailConfirmed = Tails.getConfirmed(connection, fleetId, systemId);

            PreparedStatement preparedStatement = connection.prepareStatement("INSERT INTO flights (fleet_id, uploader_id, upload_id, airframe_id, airframe_type_id, system_id, start_time, end_time, filename, md5_hash, number_rows, status, has_coords, has_agl, insert_completed, start_timestamp, end_timestamp) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, UNIX_TIMESTAMP(?), UNIX_TIMESTAMP(?))", Statement.RETURN_GENERATED_KEYS);
            preparedStatement.setInt(1, fleetId);
            preparedStatement.setInt(2, uploaderId);
            preparedStatement.setInt(3, uploadId);
            preparedStatement.setInt(4, airframeNameId);
            preparedStatement.setInt(5, airframeTypeId);
            preparedStatement.setString(6, systemId);
            preparedStatement.setString(7, startDateTime);
            preparedStatement.setString(8, endDateTime);
            preparedStatement.setString(9, filename);
            preparedStatement.setString(10, md5Hash);
            preparedStatement.setInt(11, numberRows);
            preparedStatement.setString(12, status);
            preparedStatement.setBoolean(13, hasCoords);
            preparedStatement.setBoolean(14, hasAGL);
            preparedStatement.setBoolean(15, false); //insert not yet completed
            preparedStatement.setString(16, startDateTime);
            preparedStatement.setString(17, endDateTime);

            System.out.println(preparedStatement);
            preparedStatement.executeUpdate();

            ResultSet resultSet = preparedStatement.getGeneratedKeys();
            if (resultSet.next()) {
                int flightId = resultSet.getInt(1);
                this.id = flightId;

                for (DoubleTimeSeries series : doubleTimeSeries.values()) {
                    series.updateDatabase(connection, flightId);
                }

                for (StringTimeSeries series : stringTimeSeries.values()) {
                    series.updateDatabase(connection, flightId);
                }

                for (Exception exception : exceptions) {
                    FlightWarning.insertWarning(connection, flightId, exception.getMessage());
                }

                for (int i = 0; i < itinerary.size(); i++) {
                    itinerary.get(i).updateDatabase(connection, fleetId, flightId, i);
                }

                PreparedStatement ps = connection.prepareStatement("UPDATE flights SET insert_completed = 1 WHERE id = ?");
                ps.setInt(1, this.id);
                ps.executeUpdate();
                ps.close();

            } else {
                System.err.println("ERROR: insertion of flight to the database did not result in an id.  This should never happen.");
                System.exit(1);
            }

            resultSet.close();
            preparedStatement.close();
        } catch (SQLException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Writes the DoubleTimeSeries of this flight to th specified filename
     *
     * @param connection is a connection to the database.
     * @param filename is the output filename.
     */
    public void writeToFile(Connection connection, String filename) throws IOException, SQLException {
        ArrayList<DoubleTimeSeries> series = DoubleTimeSeries.getAllDoubleTimeSeries(connection, id);

        PrintWriter printWriter = new PrintWriter(new FileWriter(filename));

        boolean afterFirst = false;
        printWriter.print("#");
        for (int i = 0; i < series.size(); i++) {
            String name = series.get(i).getName();
            if (name.equals("AirportDistance") || name.equals("RunwayDistance") ||  series.get(i).getMin() == series.get(i).getMax()) {
                System.out.println("Skipping column: '" + name + "'");
                continue;
            }
            System.out.println("'" + name + "' min - max: " + (series.get(i).getMin() - series.get(i).getMax()));

            if (afterFirst) printWriter.print(",");
            printWriter.print(series.get(i).getName());
            afterFirst = true;
        }
        printWriter.println();
        printWriter.flush();

        afterFirst = false;
        printWriter.print("#");
        for (int i = 0; i < series.size(); i++) {
            String name = series.get(i).getName();
            if (name.equals("AirportDistance") || name.equals("RunwayDistance") ||  series.get(i).getMin() == series.get(i).getMax()) continue;
            if (afterFirst) printWriter.print(",");
            printWriter.print(series.get(i).getDataType());
            afterFirst = true;
        }
        printWriter.println();
        printWriter.flush();

        //Skip the first 2 minutes to get rid of initial weird values
        for (int i = 119; i < numberRows; i++) {
            afterFirst = false;
            for (int j = 0; j < series.size(); j++) {
                String name = series.get(j).getName();
                if (name.equals("AirportDistance") || name.equals("RunwayDistance") ||  series.get(j).getMin() == series.get(j).getMax()) continue;
                if (afterFirst) printWriter.print(",");
                printWriter.print(series.get(j).get(i));
                afterFirst = true;
            }
            printWriter.println();
        }

        printWriter.close();
    }

}
