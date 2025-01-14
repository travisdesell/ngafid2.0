package org.ngafid.common;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;

public abstract class NormalizedColumn<T> {
  private static final HashMap<String, Integer> ID_CACHE = new HashMap<>();
  private static final HashMap<Integer, String> NAME_CACHE = new HashMap<>();

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
    this.name = null;
  }

  public NormalizedColumn(String name) {
    this.name = name;
    this.id = -1;
  }

  public NormalizedColumn(Connection connection, int id) throws SQLException {
    this.id = id;
    String cachedName = NAME_CACHE.get(id);

    if (cachedName == null) {
      cachedName = getName(connection);
      ID_CACHE.put(cachedName, id);
      NAME_CACHE.put(id, cachedName);
    }

    this.name = cachedName;
  }

  public NormalizedColumn(Connection connection, String name) throws SQLException {
    this.name = name;
    Integer cachedId = ID_CACHE.get(name);

    if (cachedId == null) {
      cachedId = getId(connection);
      if (cachedId == null) {
        cachedId = generateNewId(connection);
        ID_CACHE.put(name, cachedId);
        NAME_CACHE.put(cachedId, name);
      }
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
