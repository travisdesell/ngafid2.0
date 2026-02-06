package org.ngafid.core.util;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public abstract class NormalizedColumn<T> {
    private static final Logger LOG = Logger.getLogger(NormalizedColumn.class.getName());

    private static final ConcurrentHashMap<String, ConcurrentHashMap<String, Integer>> ID_CACHE =
            new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, ConcurrentHashMap<Integer, String>> NAME_CACHE =
            new ConcurrentHashMap<>();

    private String getCachedName(int columnId) {
        return NAME_CACHE.computeIfAbsent(getTableName(), (k) -> new ConcurrentHashMap<>())
                .getOrDefault(columnId, null);
    }

    private int getCachedId(String columnName) {
        return ID_CACHE.computeIfAbsent(getTableName(), (k) -> new ConcurrentHashMap<>())
                .getOrDefault(columnName, -1);
    }

    private void addToCache(int columnId, String columnName) {
        ID_CACHE.computeIfAbsent(getTableName(), (k) -> new ConcurrentHashMap<>())
                .putIfAbsent(columnName, columnId);
        NAME_CACHE.computeIfAbsent(getTableName(), (k) -> new ConcurrentHashMap<>())
                .putIfAbsent(columnId, columnName);
    }

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

    public NormalizedColumn(int columnId, String columnName) {
        this.id = columnId;
        this.name = columnName;
    }

    public NormalizedColumn(int columnId) {
        this.id = columnId;
        this.name = getCachedName(columnId);
    }

    public NormalizedColumn(String columnName) {
        this.name = columnName;
        this.id = getCachedId(columnName);
    }

    public NormalizedColumn(Connection connection, int columnId) throws SQLException {
        this.id = columnId;
        String cachedName = getCachedName(columnId);

        if (cachedName == null) {
            cachedName = getName(connection);
            addToCache(columnId, cachedName);
        }

        this.name = cachedName;
    }

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
