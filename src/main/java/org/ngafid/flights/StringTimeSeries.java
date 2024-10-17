package org.ngafid.flights;

import org.ngafid.common.Compression;
import org.ngafid.common.NormalizedColumn;
import org.ngafid.flights.Parameters.Unit;

import java.io.IOException;
import java.io.Serializable;
import java.sql.Blob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import java.sql.ResultSet;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

import javax.sql.rowset.serial.SerialBlob;

public class StringTimeSeries {

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

    public static interface StringSeriesData {
        public abstract String get(int i);

        public abstract void add(String s);

        public abstract int size();
    }

    /**
     * Since we use char (16 bits) to represent our indices, we can only have 2^16 unique values.
     * This may eplode if you attempt to use more values than that.
     */
    public static class SparseStringSeriesData implements Serializable, StringSeriesData {
        public char[] indices;
        public int length = 0;
        public HashMap<String, Integer> index;
        public ArrayList<String> values;

        public SparseStringSeriesData(char[] indices, int length, HashMap<String, Integer> index,
                ArrayList<String> values) {
            this.indices = indices;
            this.length = length;
            this.index = index;
            this.values = values;
        }

        private char intern(String s) {
            return (char) this.index.computeIfAbsent(s, key -> {
                int val = this.values.size();
                this.values.add(key);
                return val;
            }).intValue();
        }

        @Override
        public String get(int i) {
            return values.get(indices[i]);
        }

        @Override
        public void add(String s) {
            if (this.length == this.indices.length) {
                char[] oldData = this.indices;
                this.indices = new char[oldData.length * 2];
                System.arraycopy(oldData, 0, this.indices, 0, oldData.length);
            }

            this.indices[this.length++] = intern(s);
        }

        @Override
        public int size() {
            return length;
        }

    }

    public static class DenseStringSeriesData implements Serializable, StringSeriesData {
        ArrayList<String> values;

        public DenseStringSeriesData(ArrayList<String> values) {
            this.values = values;
        }

        @Override
        public String get(int i) {
            return this.values.get(i);
        }

        @Override
        public void add(String s) {
            this.values.add(s);
        }

        @Override
        public int size() {
            return values.size();
        }
    }

    StringSeriesData data;

    private int validCount;

    public StringTimeSeries(String name, Unit dataType, int sizeHint) {
        this(name, dataType.toString(), sizeHint);
    }

    public StringTimeSeries(String name, String dataType, int sizeHint) {
        this.name = new StringSeriesName(name);
        this.dataType = new TypeName(dataType);

        this.data = new DenseStringSeriesData(new ArrayList<>(sizeHint));

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

    public StringTimeSeries(Connection connection, String name, Unit dataType,
            ArrayList<? extends CharSequence> timeSeries)
            throws SQLException {
        this(connection, name, dataType.toString(), timeSeries);
    }

    public StringTimeSeries(Connection connection, String name, String dataType,
            ArrayList<? extends CharSequence> timeSeries)
            throws SQLException {
        this(name, dataType, timeSeries);
        setNameId(connection);
        setTypeId(connection);
    }

    public StringTimeSeries(String name, Unit dataType, ArrayList<? extends CharSequence> timeSeries) {
        this(name, dataType.toString(), timeSeries);
    }

    public static CharSequence trim(CharSequence input) {
        if (input == null) {
            return null; // Handle null input gracefully
        }

        int start = 0;
        int end = input.length();

        // Find the first non-whitespace character from the start
        while (start < end && Character.isWhitespace(input.charAt(start))) {
            start++;
        }

        // Find the last non-whitespace character from the end
        while (end > start && Character.isWhitespace(input.charAt(end - 1))) {
            end--;
        }

        // Return the sub-sequence with trimmed whitespace
        return input.subSequence(start, end);
    }

    private static double DENSE_THRESHOLD = 0.2;

    static {
        String threshold = System.getenv("DENSE_STRING_SERIES_THRESHOLD");
        if (threshold != null) {
            try {
                double t = Double.parseDouble(threshold);
                DENSE_THRESHOLD = t;
            } catch (NumberFormatException e) {

            }
        }
    }

    public <T extends CharSequence> StringTimeSeries(String name, String dataType, ArrayList<T> timeSeries) {
        this.name = new StringSeriesName(name);
        this.dataType = new TypeName(dataType);

        validCount = 0;

        int i = 0;
        int threshold = (int) Math.min(Character.MAX_VALUE, (DENSE_THRESHOLD * timeSeries.size()));
        Set<T> uniqueValues = new HashSet<>(Math.max(0, threshold));

        while (uniqueValues.size() <= threshold && i < timeSeries.size()) {
            uniqueValues.add(timeSeries.get(i++));
        }

        if (uniqueValues.size() > threshold) {
            ArrayList<String> values = new ArrayList<>();
            for (var value : timeSeries)
                values.add(value.toString());

            this.data = new DenseStringSeriesData(values);
        } else {
            HashMap<String, Integer> index = new HashMap<>();
            ArrayList<String> values = new ArrayList<>();
            char[] indices = new char[timeSeries.size()];

            for (var value : uniqueValues) {
                index.put(value.toString(), values.size());
                values.add(value.toString());
            }

            for (i = 0; i < timeSeries.size(); i++)
                indices[i] = (char) index.get(timeSeries.get(i).toString()).intValue();

            this.data = new SparseStringSeriesData(indices, indices.length, index, values);
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

        StringSeriesData data = (StringSeriesData) inflated;
        this.data = data;
    }

    public static StringTimeSeries getStringTimeSeries(Connection connection, int flightId, String name)
            throws IOException, SQLException {
        try (PreparedStatement query = connection.prepareStatement(
                "SELECT ss.name_id, ss.data_type_id, ss.length, ss.valid_length, ss.data FROM string_series AS ss INNER JOIN string_series_names AS ssn ON ssn.id = ss.name_id WHERE ssn.name = ? AND ss.flight_id = ?")) {

            query.setString(1, name);
            query.setInt(2, flightId);

            try (ResultSet resultSet = query.executeQuery()) {
                if (resultSet.next()) {
                    try {
                        StringTimeSeries sts = new StringTimeSeries(connection, resultSet);
                        return sts;
                    } catch (ClassNotFoundException e) {
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
        return "[StringTimeSeries '" + name + "' size: " + data.size() + ", validCount: " + validCount + "]";
    }

    public void add(String s) {
        this.data.add(s);
    }

    public String get(int i) {
        return this.data.get(i);
    }

    public String getName() {
        return name.getName();
    }

    public String getDataType() {
        return dataType.getName();
    }

    public String getFirstValid() {
        int position = 0;
        while (position < this.data.size()) {
            String current = this.get(position);
            if (current.equals("")) {
                position++;
            } else {
                return current;
            }
        }
        return null;
    }

    public String getLastValid() {
        int position = this.size() - 1;
        while (position >= 0) {
            String current = this.get(position);
            if (current.equals("")) {
                position--;
            } else {
                return current;
            }
        }
        return null;
    }

    public String[] getLastValidAndIndex() {
        int position = this.size() - 1;
        while (position >= 0) {
            String current = this.get(position);
            if (current.equals("")) {
                position--;
            } else {
                return new String[] { current, String.valueOf(position) };
            }
        }
        return null;
    }

    public int size() {
        return this.data.size();
    }

    public int validCount() {
        return validCount;
    }

    public static PreparedStatement createPreparedStatement(Connection connection) throws SQLException {
        return connection.prepareStatement(
                "INSERT INTO string_series (flight_id, name_id, data_type_id, length, valid_length, data) VALUES (?, ?, ?, ?, ?, ?)");
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
        preparedStatement.setInt(4, this.data.size());
        preparedStatement.setInt(5, validCount);

        // To get rid of extra bytes at the end of the buffer
        byte[] compressed = Compression.compressObject(this.data);
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
            newSeries.add(this.get(i));

        return newSeries;
    }

    public StringTimeSeries subSeries(int from, int until) throws SQLException {
        StringTimeSeries newSeries = new StringTimeSeries(name.getName(), dataType.getName());

        for (int i = from; i < until; i++)
            newSeries.add(this.get(i));

        return newSeries;
    }
}
