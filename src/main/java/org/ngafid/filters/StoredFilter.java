package org.ngafid.filters;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import java.util.ArrayList;
import java.util.List;

import org.ngafid.accounts.User;

/**
 * This class contains data pertaining to a {@link User}'s stored {@link Filter} that they wish to apply in future
 * use cases in the NGAFID.
 *
 * @author <a href=mailto:apl1341@cs.rit.edu>Aidan LaBella @ RIT CS</a>
 */
public class StoredFilter {
    /**
     * @param name   is the common name given to the stored filter by the user.
     * @param filter is the filter as a {@link String} in the form of a JSON object.
     */
    private String name, filter, color;

    /**
     * Default constructor
     * This constructor can also be used to instatiate an object that can be interpreted by GSON to get a JSON object
     *
     * @param name   is the common name given by the user
     * @param filter is the filter in JSON form
     * @param color  is the color of this filter in hex
     */
    private StoredFilter(String name, String filter, String color) {
        this.name = name;
        this.filter = filter;
        this.color = color;
    }

    /**
     * This method queries the database for the users stored filters.
     *
     * @param connection is the SQL database connection
     * @param fleetId    is the id of the fleet that the filter belongs to
     *
     * @return an {@link List} with {@link StoredFilter} instances, a collection of all that belong to the user.
     *
     * @throws SQLException in the event there is an issue with the SQL query.
     */
    public static List<StoredFilter> getStoredFilters(Connection connection, int fleetId) throws SQLException {
        try (PreparedStatement query = connection
                .prepareStatement("SELECT name, filter_json, color FROM stored_filters WHERE fleet_id = " + fleetId);
                ResultSet resultSet = query.executeQuery()) {

            List<StoredFilter> filters = new ArrayList<>();

            while (resultSet.next()) {
                StoredFilter filterResponse = new StoredFilter(resultSet.getString(1), resultSet.getString(2),
                        resultSet.getString(3));
                filters.add(filterResponse);
            }

            return filters;
        }
    }

    /**
     * Stores a filter and returns its instance as a {@link StoredFilter}
     *
     * @param connection is the SQL database connection
     * @param fleetId    is the id of the fleet that the filter belongs to
     * @param color      is the color of this filter in hex
     * 
     * @return a {@link StoredFilter} instance containing the data upon successful insertion into the db
     *
     * @throws SQLException in the event there is an issue with the SQL query.
     */
    public static StoredFilter storeFilter(Connection connection, int fleetId, String filterJSON, String name,
            String color) throws SQLException {
        try (PreparedStatement query = connection
                .prepareStatement("INSERT INTO stored_filters (filter_json, name, fleet_id, color) VALUES (?,?,?,?)")) {
            query.setString(1, filterJSON);
            query.setString(2, name);
            query.setInt(3, fleetId);
            query.setString(4, color);

            query.executeUpdate();
        }

        return new StoredFilter(name, filterJSON, color);
    }

    /**
     * This method removes a stored filter for a {@link User} from the database using its primary key (fleetId, name)
     *
     * @param connection is the SQL database connection
     * @param fleetId    is the id of the fleet that the filter belongs to
     * @param name       is the name of the filter to be removed
     *
     * @throws SQLException in the event there is an issue with the SQL query.
     */
    public static void removeFilter(Connection connection, int fleetId, String name) throws SQLException {
        try (PreparedStatement query = connection
                .prepareStatement("DELETE FROM stored_filters WHERE fleet_id = ? AND name = ?")) {
            query.setInt(1, fleetId);
            query.setString(2, name);
            query.executeUpdate();
        }
    }

    /**
     * Checks if a StoredFilter exists in the database already using is primary key
     *
     * @param connection is the SQL database connection
     * @param fleetId    is the id of the fleet that the filter belongs to
     * @param name       is the name of the filter to be removed
     *
     * @return a boolean representing the presence of the filter in the database
     *
     * @throws SQLException in the event there is an issue with the SQL query.
     */
    public static boolean filterExists(Connection connection, int fleetId, String name) throws SQLException {
        try (PreparedStatement query = connection.prepareStatement(
                "SELECT EXISTS(SELECT name, fleet_id FROM stored_filters WHERE fleet_id = ? AND name = ?)")) {
            query.setInt(1, fleetId);
            query.setString(2, name);

            try (ResultSet resultSet = query.executeQuery()) {

                if (resultSet.next()) {
                    return resultSet.getBoolean(1);
                }

                return false;
            }
        }
    }

    /**
     * This method modifies a filter that already exists in the Database.
     *
     * @param connection  is the SQL database connection
     * @param fleetId     is the Fleet ID
     * @param filterJSON  is the new filter JSON
     * @param currentName is the name of the filter that already exists
     * @param newName     is the new name to be given to the {@link StoredFilter}
     * @param color       is the color of this filter in hex
     *
     * @throws SQLException in the event there is an issue with the SQL query.
     */
    public static void modifyFilter(Connection connection, int fleetId, String filterJSON, String currentName,
            String newName, String color) throws SQLException {
        try (PreparedStatement query = connection.prepareStatement(
                "UPDATE stored_filters SET fleet_id = ?, name = ?, filter_json = ?, color = ? WHERE name = ? AND fleet_id = ?")) {
            query.setInt(1, fleetId);
            query.setString(2, newName);
            query.setString(3, filterJSON);
            query.setString(4, color);
            query.setString(5, currentName);
            query.setInt(6, fleetId);

            query.executeUpdate();
        }
    }

    /**
     * String representation of a stored filter
     *
     * @return this class as represented as a {@link String}
     */
    public String toString() {
        return "StoredFilter name: " + this.name + "; filter JSON: " + this.filter + " color: " + this.color;
    }
}
