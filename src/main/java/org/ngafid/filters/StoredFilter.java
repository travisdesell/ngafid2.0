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
     * @param name is the common name given to the stored filter by the user.
     * @param filter is the filter as a {@link String} in the form of a JSON object.
     */
    private String name, filter;

    /**
     * Default constructor
     * This constructor can also be used to instatiate an object that can be interpreted by GSON to get a JSON object
     *
     * @param name is the common name given by the user
     * @param filter is the filter in JSON form
     */
    private StoredFilter(String name, String filter) {
        this.name = name;
        this.filter = filter;
    }

    /**
     * This method queries the database for the users stored filters.
     *
     * @param connection is the SQL database connection
     * @param user is the user that has the stored filters
     *
     * @return an {@link List} with {@link StoredFilter} instances, a collection of all that belong to the user.
     *
     * @throws SQLException in the event there is an issue with the SQL query.
     */
    public static List<StoredFilter> getStoredFilters(Connection connection, User user) throws SQLException {
        PreparedStatement query = connection.prepareStatement("SELECT name, filter_json FROM stored_filters WHERE user_id = ?");

        query.setInt(1, user.getId());
        ResultSet resultSet = query.executeQuery();

        List<StoredFilter> filters = new ArrayList<>();

        while (resultSet.next()) {
            StoredFilter filterResponse = 
                new StoredFilter(resultSet.getString(1), resultSet.getString(2));
            filters.add(filterResponse);
        }

        return filters;
    }

    /**
     * Stores a filter and returns its instance as a {@link StoredFilter}
     *
     * @param connection is the SQL database connection
     * @param user is the user storing the filter
     * 
     * @return a {@link StoredFilter} instance containing the data upon successful insertion into the db
     *
     * @throws SQLException in the event there is an issue with the SQL query.
     */
    public static StoredFilter storeFilter(Connection connection, User user, String filterJSON, String name) throws SQLException {
        PreparedStatement query = connection.prepareStatement("INSERT INTO stored_filters (filter_json, name, user_id) VALUES (?,?,?)");

        query.setString(1, filterJSON);
        query.setString(2, name);
        query.setInt(3, user.getId());

        query.executeUpdate();

        return new StoredFilter(name, filterJSON);
    }

    /**
     * String representation of a stored filter
     *
     * @return this class as represented as a {@link String}
     */
    public String toString() {
        return "StoredFilter name: " + this.name + "; filter JSON: " + this.filter; 
    }
}
