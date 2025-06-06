package org.ngafid.core.flights;

import org.ngafid.core.flights.Parameters.Unit;
import org.ngafid.core.util.Compression;
import org.ngafid.core.util.NormalizedColumn;

import javax.sql.rowset.serial.SerialBlob;
import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.logging.Logger;

public final class StringTimeSeries {

    public static class StringSeriesName extends NormalizedColumn<StringSeriesName> {
        public StringSeriesName(String name) {
            super(name);
        }

        public StringSeriesName(int id) {
            super(id);
        }

        public StringSeriesName(Connection connection, int id) throws SQLException {
            super(connection, id);
        }

        public StringSeriesName(Connection connection, String string) throws SQLException {
            super(connection, string);
        }

        @Override
        protected String getTableName() {
            return "string_series_names";
        }
    }

    private static final Logger LOG = Logger.getLogger(StringTimeSeries.class.getName());
    private static final int SIZE_HINT = 1 << 12;

    private StringSeriesName name;
    private TypeName dataType;
    private ArrayList<String> timeSeries;
    private int validCount;

    public StringTimeSeries(String name, Unit dataType, int sizeHint) {
        this(name, dataType.toString(), sizeHint);
    }

    public StringTimeSeries(String name, String dataType, int sizeHint) {
        this.name = new StringSeriesName(name);
        this.dataType = new TypeName(dataType);
        this.timeSeries = new ArrayList<>(sizeHint);

        validCount = 0;
    }

    public StringTimeSeries(String name, Unit dataType) {
        this(name, dataType.toString());
    }

    public StringTimeSeries(String name, String dataType) {
        this(name, dataType, SIZE_HINT);
    }

    public StringTimeSeries(Connection connection, String name, Unit dataType) throws SQLException {
        this(connection, name, dataType.toString());
    }

    public StringTimeSeries(Connection connection, String name, String dataType) throws SQLException {
        this(name, dataType, SIZE_HINT);
        setNameId(connection);
        setTypeId(connection);
    }

    public StringTimeSeries(Connection connection, String name, Unit dataType, ArrayList<String> timeSeries)
            throws SQLException {
        this(connection, name, dataType.toString(), timeSeries);
    }

    public StringTimeSeries(Connection connection, String name, String dataType, ArrayList<String> timeSeries)
            throws SQLException {
        this(name, dataType, timeSeries);
        setNameId(connection);
        setTypeId(connection);
    }

    public StringTimeSeries(String name, Unit dataType, ArrayList<String> timeSeries) {
        this(name, dataType.toString(), timeSeries);
    }

    public StringTimeSeries(String name, String dataType, ArrayList<String> timeSeries) {
        this.name = new StringSeriesName(name);
        this.dataType = new TypeName(dataType);
        this.timeSeries = timeSeries;
        validCount = 0;
        for (int i = 0; i < timeSeries.size(); i++) {
            timeSeries.set(i, timeSeries.get(i).trim());
            if (!emptyAt(i)) {
                validCount++;
            }
        }
    }

    // Added to get results for StringTimeSeries
    public StringTimeSeries(Connection connection, ResultSet resultSet)
            throws SQLException, IOException, ClassNotFoundException {

        this.name = new StringSeriesName(connection, resultSet.getInt(1));
        // System.out.println("name: " + name);

        this.dataType = new TypeName(connection, resultSet.getInt(2));
        // System.out.println("data type: " + dataType);

        // System.out.println("length: " + length);
        validCount = resultSet.getInt(4);
        // System.out.println("valid count: " + validCount);

        Blob values = resultSet.getBlob(5);
        byte[] bytes = values.getBytes(1, (int) values.length());
        // System.out.println("values.length: " + (int)values.length());
        values.free();

        // This unchecked caste warning can be fixed but it shouldnt be necessary if we only but ArrayList<String>
        // objects into the StringTimeSeries cache.
        Object inflated = Compression.inflateObject(bytes);

        @SuppressWarnings("unchecked")
        ArrayList<String> array = (ArrayList<String>) inflated;

        this.timeSeries = (ArrayList<String>) array;
    }

    public static StringTimeSeries getStringTimeSeries(Connection connection, int flightId, String name)
            throws SQLException {
        try (PreparedStatement query = connection.prepareStatement(
                "SELECT ss.name_id, ss.data_type_id, ss.length, ss.valid_length, " +
                        "ss.data FROM string_series AS ss INNER JOIN string_series_names " +
                        "AS ssn ON ssn.id = ss.name_id WHERE ssn.name = ? AND ss.flight_id = ?")) {

            query.setString(1, name);
            query.setInt(2, flightId);

            try (ResultSet resultSet = query.executeQuery()) {
                if (resultSet.next()) {
                    try {
                        StringTimeSeries sts = new StringTimeSeries(connection, resultSet);
                        return sts;
                    } catch (IOException | ClassNotFoundException e) {
                        LOG.severe("Failed to read string time series from database due to a serialization error");
                        e.printStackTrace();
                        return null;
                    }
                } else {
                    return null;
                }
            }
        }
    }

    private void setNameId(Connection connection) throws SQLException {
        this.name = new StringSeriesName(connection, name.getName());
    }

    private void setTypeId(Connection connection) throws SQLException {
        this.dataType = new TypeName(connection, dataType.getName());
    }

    public String toString() {
        return "[StringTimeSeries '" + name + "' size: " + timeSeries.size() + ", validCount: " + validCount + "]";
    }

    public void add(String s) {
        if (!s.equals(""))
            validCount++;
        timeSeries.add(s);
    }

    public String get(int i) {
        return timeSeries.get(i);
    }

    public boolean emptyAt(int i) {
        return get(i).isEmpty();
    }

    public String getName() {
        return name.getName();
    }

    public String getDataType() {
        return dataType.getName();
    }

    public String getFirstValid() {
        int position = 0;
        while (position < timeSeries.size()) {
            String current = timeSeries.get(position);
            if (current.equals("")) {
                position++;
            } else {
                return current;
            }
        }
        return null;
    }

    public String getLastValid() {
        int position = timeSeries.size() - 1;
        while (position >= 0) {
            String current = timeSeries.get(position);
            if (current.equals("")) {
                position--;
            } else {
                return current;
            }
        }
        return null;
    }

    public int getLastValidIndex() {
        int position = timeSeries.size() - 1;
        while (position >= 0) {
            String current = timeSeries.get(position);
            if (current.isEmpty()) {
                position--;
            } else {
                return position;
            }
        }

        return -1;
    }

    public int size() {
        return timeSeries.size();
    }

    public int validCount() {
        return validCount;
    }

    public static PreparedStatement createPreparedStatement(Connection connection) throws SQLException {
        return connection.prepareStatement(
                "INSERT INTO string_series " +
                        "(flight_id, name_id, data_type_id, length, valid_length, data) VALUES (?, ?, ?, ?, ?, ?)");
    }

    public void addBatch(Connection connection, PreparedStatement preparedStatement, int flightId)
            throws SQLException, IOException {
        if (name.getId() == -1)
            setNameId(connection);
        if (dataType.getId() == -1)
            setTypeId(connection);

        preparedStatement.setInt(1, flightId);
        preparedStatement.setInt(2, name.getId());
        preparedStatement.setInt(3, dataType.getId());
        preparedStatement.setInt(4, timeSeries.size());
        preparedStatement.setInt(5, validCount);

        // To get rid of extra bytes at the end of the buffer
        byte[] compressed = Compression.compressObject(this.timeSeries);
        Blob seriesBlob = new SerialBlob(compressed);
        preparedStatement.setBlob(6, seriesBlob);

        preparedStatement.addBatch();
    }

    public void updateDatabase(Connection connection, int flightId) throws IOException, SQLException {
        try (PreparedStatement preparedStatement = createPreparedStatement(connection)) {
            this.addBatch(connection, preparedStatement, flightId);

            preparedStatement.executeUpdate();
        }
    }

    public StringTimeSeries subSeries(Connection connection, int from, int until) throws SQLException {
        StringTimeSeries newSeries = new StringTimeSeries(connection, name.getName(), dataType.getName());

        for (int i = from; i < until; i++)
            newSeries.add(this.timeSeries.get(i));

        return newSeries;
    }

    public StringTimeSeries subSeries(int from, int until) {
        StringTimeSeries newSeries = new StringTimeSeries(name.getName(), dataType.getName());

        for (int i = from; i < until; i++)
            newSeries.add(this.timeSeries.get(i));

        return newSeries;
    }
}
