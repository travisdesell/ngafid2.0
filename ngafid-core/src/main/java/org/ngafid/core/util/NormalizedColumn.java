package org.ngafid.core.util;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public abstract class NormalizedColumn<T> {
    private static final Logger LOG = Logger.getLogger(NormalizedColumn.class.getName());

    private static final ConcurrentHashMap<String, ConcurrentHashMap<String, Integer>> ID_CACHE = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, ConcurrentHashMap<Integer, String>> NAME_CACHE = new ConcurrentHashMap<>();

    private String getCachedName(int id) {
        return NAME_CACHE.computeIfAbsent(getTableName(), (k) -> new ConcurrentHashMap<>()).getOrDefault(id, null);
    }

    private int getCachedId(String name) {
        return ID_CACHE.computeIfAbsent(getTableName(), (k) -> new ConcurrentHashMap<>()).getOrDefault(name, -1);
    }

    private void addToCache(int id, String name) {
        ID_CACHE.computeIfAbsent(getTableName(), (k) -> new ConcurrentHashMap<>()).putIfAbsent(name, id);
        NAME_CACHE.computeIfAbsent(getTableName(), (k) -> new ConcurrentHashMap<>()).putIfAbsent(id, name);
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

    public NormalizedColumn(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public NormalizedColumn(int id) {
        this.id = id;
        this.name = getCachedName(id);
    }

    public NormalizedColumn(String name) {
        this.name = name;
        this.id = getCachedId(name);
    }

    public NormalizedColumn(Connection connection, int id) throws SQLException {
        this.id = id;
        String cachedName = getCachedName(id);

        if (cachedName == null) {
            cachedName = getName(connection);
            addToCache(id, cachedName);
        }

        this.name = cachedName;
    }

    public NormalizedColumn(Connection connection, String name) throws SQLException {
        this.name = name;
        int id = getCachedId(name);

        if (id == -1) {
            id = generateNewId(connection);
            addToCache(id, name);
        }

        this.id = id;
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
