package org.ngafid.core.util;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Base class for a value stored in a normalization table as an id-to-name mapping, resolving one from the other.
 *
 * <p>Subclasses name the backing table (and optionally the id/name columns); this class resolves a column's id from
 * its name or vice versa, consulting a shared per-table in-memory cache first and falling back to the database,
 * inserting a new row (and generating an id) when a name is not yet present. Caches are static and shared across all
 * instances of a given table.
 *
 * @param <T> the concrete normalized-column subtype
 */
public abstract class NormalizedColumn<T> {
    private static final Logger LOG = Logger.getLogger(NormalizedColumn.class.getName());

    private static final ConcurrentHashMap<String, ConcurrentHashMap<String, Integer>> ID_CACHE =
            new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, ConcurrentHashMap<Integer, String>> NAME_CACHE =
            new ConcurrentHashMap<>();

    private String getCachedName(int columnId) {
        return NAME_CACHE
                .computeIfAbsent(getTableName(), (k) -> new ConcurrentHashMap<>())
                .getOrDefault(columnId, null);
    }

    private int getCachedId(String columnName) {
        return ID_CACHE.computeIfAbsent(getTableName(), (k) -> new ConcurrentHashMap<>())
                .getOrDefault(columnName, -1);
    }

    private void addToCache(int columnId, String columnName) {
        ID_CACHE.computeIfAbsent(getTableName(), (k) -> new ConcurrentHashMap<>())
                .putIfAbsent(columnName, columnId);
        NAME_CACHE
                .computeIfAbsent(getTableName(), (k) -> new ConcurrentHashMap<>())
                .putIfAbsent(columnId, columnName);
    }

    /**
     * Returns the name of the normalization table this column type is stored in. Subclasses supply the table that holds
     * the id-to-name mapping; it also keys the per-table id and name caches.
     *
     * @return the database table name backing this normalized column
     */
    protected abstract String getTableName();

    protected String getNameColumn() {
        return "name";
    }

    protected String getIdColumn() {
        return "id";
    }

    private String getNameQuery() {
        return "SELECT " + getNameColumn() + " FROM " + getTableName() + " WHERE " + getIdColumn() + " = ?";
    }

    private String getIdQuery() {
        return "SELECT " + getIdColumn() + " FROM " + getTableName() + " WHERE " + getNameColumn() + " = ?";
    }

    private String getInsertionQuery() {
        return "INSERT IGNORE INTO " + getTableName() + " SET " + getNameColumn() + " = ?";
    }

    private final int id;
    private final String name;

    /**
     * Constructs a normalized column from an already-known id and name, without consulting the cache or database.
     *
     * @param columnId the column's id
     * @param columnName the column's name
     */
    public NormalizedColumn(int columnId, String columnName) {
        this.id = columnId;
        this.name = columnName;
    }

    /**
     * Constructs a normalized column from an id, resolving its name from the in-memory cache only; the name is left
     * {@code null} if the id has not been cached yet (no database lookup is performed).
     *
     * @param columnId the column's id
     */
    public NormalizedColumn(int columnId) {
        this.id = columnId;
        this.name = getCachedName(columnId);
    }

    /**
     * Constructs a normalized column from a name, resolving its id from the in-memory cache only; the id is left
     * {@code -1} if the name has not been cached yet (no database lookup is performed).
     *
     * @param columnName the column's name
     */
    public NormalizedColumn(String columnName) {
        this.name = columnName;
        this.id = getCachedId(columnName);
    }

    /**
     * Constructs a normalized column from an id, resolving its name from the database on a cache miss and caching the
     * result for later lookups.
     *
     * @param connection the database connection used to resolve the name
     * @param columnId the column's id
     * @throws SQLException if the name lookup fails
     */
    public NormalizedColumn(Connection connection, int columnId) throws SQLException {
        this.id = columnId;
        String cachedName = getCachedName(columnId);

        if (cachedName == null) {
            cachedName = getName(connection);
            addToCache(columnId, cachedName);
        }

        this.name = cachedName;
    }

    /**
     * Constructs a normalized column from a name, resolving its id from the database on a cache miss; if the name does
     * not yet exist in the table it is inserted and a new id generated, and the resulting mapping is cached.
     *
     * @param connection the database connection used to resolve or insert the name
     * @param columnName the column's name
     * @throws SQLException if the id lookup or insertion fails
     */
    public NormalizedColumn(Connection connection, String columnName) throws SQLException {
        this.name = columnName;
        int cachedId = getCachedId(columnName);

        if (cachedId == -1) {
            cachedId = generateNewId(connection);
            addToCache(cachedId, columnName);
        }

        this.id = cachedId;
    }

    private String getName(Connection connection) throws SQLException {
        try (PreparedStatement query = connection.prepareStatement(getNameQuery())) {
            query.setInt(1, id);
            try (ResultSet resultSet = query.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getString(1);
                } else {
                    return null;
                }
            }
        }
    }

    private Integer getId(Connection connection) throws SQLException {
        try (PreparedStatement query = connection.prepareStatement(this.getIdQuery())) {
            query.setString(1, name);
            try (ResultSet resultSet = query.executeQuery()) {
                if (resultSet.next()) {
                    var i = resultSet.getInt(1);
                    return i;
                } else {
                    return generateNewId(connection);
                }
            }
        }
    }

    private int generateNewId(Connection connection) throws SQLException {
        try (PreparedStatement insertQuery = connection.prepareStatement(getInsertionQuery())) {
            insertQuery.setString(1, name);
            insertQuery.executeUpdate();
        }
        return getId(connection);
    }

    public int getId() {
        return this.id;
    }

    public String getName() {
        return this.name;
    }
}
