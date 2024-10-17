package org.ngafid.flights;

import java.sql.Connection;
import java.sql.SQLException;

import org.ngafid.common.NormalizedColumn;

public class SeriesName extends NormalizedColumn<SeriesName> {
  public SeriesName(String name) {
    super(name);
  }

  public SeriesName(int id) {
    super(id);
  }

  public SeriesName(Connection connection, int id) throws SQLException {
    super(connection, id);
  }

  public SeriesName(Connection connection, String string) throws SQLException {
    super(connection, string);
  }

  @Override
  protected String getTableName() {
    return "series_names";
  }
}
