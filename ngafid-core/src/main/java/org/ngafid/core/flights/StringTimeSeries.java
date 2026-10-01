package org.ngafid.core.flights;

import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.logging.Logger;
import javax.sql.rowset.serial.SerialBlob;
import org.ngafid.core.flights.Parameters.Unit;
import org.ngafid.core.util.Compression;
import org.ngafid.core.util.NormalizedColumn;

public final class StringTimeSeries {

    public static class StringSeriesName extends NormalizedColumn<StringSeriesName> {
        /**
         * Creates an unresolved string-series name from its string value (no database id assigned yet).
         *
         * @param name the series name
         */
        public StringSeriesName(String name) {
            super(name);
        }

        /**
         * Creates an unresolved string-series name from its database id (the name string is resolved lazily).
         *
         * @param id the series-name id
         */
        public StringSeriesName(int id) {
            super(id);
        }

        /**
         * Resolves a string-series name from its database id, looking up the corresponding name string.
         *
         * @param connection the database connection
         * @param id the series-name id
         * @throws SQLException if the lookup fails
         */
        public StringSeriesName(Connection connection, int id) throws SQLException {
            super(connection, id);
        }

        /**
         * Resolves a string-series name from its string value, looking up (or assigning) its database id.
         *
         * @param connection the database connection
         * @param string the series name
         * @throws SQLException if the lookup or insert fails
         */
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

    /**
     * Constructs an empty string time series with a backing list pre-sized to {@code sizeHint}, using a typed
     * {@link Unit}.
     *
     * @param name the series name
     * @param dataType the series' unit
     * @param sizeHint the initial list capacity (the series starts empty)
     */
    public StringTimeSeries(String name, Unit dataType, int sizeHint) {
        this(name, dataType.toString(), sizeHint);
    }

    /**
     * Constructs an empty string time series with a backing list pre-sized to {@code sizeHint}.
     *
     * @param name the series name
     * @param dataType the series' unit/data-type name
     * @param sizeHint the initial list capacity (the series starts empty)
     */
    public StringTimeSeries(String name, String dataType, int sizeHint) {
        this.name = new StringSeriesName(name);
        this.dataType = new TypeName(dataType);
        this.timeSeries = new ArrayList<>(sizeHint);

        validCount = 0;
    }

    /**
     * Constructs an empty string time series with a default initial capacity, using a typed {@link Unit}.
     *
     * @param name the series name
     * @param dataType the series' unit
     */
    public StringTimeSeries(String name, Unit dataType) {
        this(name, dataType.toString());
    }

    /**
     * Constructs an empty string time series with a default initial capacity.
     *
     * @param name the series name
     * @param dataType the series' unit/data-type name
     */
    public StringTimeSeries(String name, String dataType) {
        this(name, dataType, SIZE_HINT);
    }

    /**
     * Constructs an empty database-aware string time series using a typed {@link Unit}, resolving its name and type
     * ids from the database.
     *
     * @param connection the database connection used to resolve the name/type ids
     * @param name the series name
     * @param dataType the series' unit
     * @throws SQLException if resolving the name or type id fails
     */
    public StringTimeSeries(Connection connection, String name, Unit dataType) throws SQLException {
        this(connection, name, dataType.toString());
    }

    /**
     * Constructs an empty database-aware string time series, resolving its name and type ids from the database.
     *
     * @param connection the database connection used to resolve the name/type ids
     * @param name the series name
     * @param dataType the series' unit/data-type name
     * @throws SQLException if resolving the name or type id fails
     */
    public StringTimeSeries(Connection connection, String name, String dataType) throws SQLException {
        this(name, dataType, SIZE_HINT);
        setNameId(connection);
        setTypeId(connection);
    }

    /**
     * Constructs a database-aware string time series from an existing list of values (using a typed {@link Unit}),
     * resolving its name and type ids from the database.
     *
     * @param connection the database connection used to resolve the name/type ids
     * @param name the series name
     * @param dataType the series' unit
     * @param timeSeries the backing list of string samples
     * @throws SQLException if resolving the name or type id fails
     */
    public StringTimeSeries(Connection connection, String name, Unit dataType, ArrayList<String> timeSeries)
            throws SQLException {
        this(connection, name, dataType.toString(), timeSeries);
    }

    /**
     * Constructs a database-aware string time series from an existing list of values, resolving its name and type
     * ids from the database.
     *
     * @param connection the database connection used to resolve the name/type ids
     * @param name the series name
     * @param dataType the series' unit/data-type name
     * @param timeSeries the backing list of string samples
     * @throws SQLException if resolving the name or type id fails
     */
    public StringTimeSeries(Connection connection, String name, String dataType, ArrayList<String> timeSeries)
            throws SQLException {
        this(name, dataType, timeSeries);
        setNameId(connection);
        setTypeId(connection);
    }

    /**
     * Constructs a string time series wrapping an existing list of values, using a typed {@link Unit}.
     *
     * @param name the series name
     * @param dataType the series' unit
     * @param timeSeries the backing list of string samples
     */
    public StringTimeSeries(String name, Unit dataType, ArrayList<String> timeSeries) {
        this(name, dataType.toString(), timeSeries);
    }

    /**
     * Constructs a string time series wrapping an existing list of values. Each value is trimmed in place, and the
     * valid count is set to the number of non-empty values.
     *
     * @param name the series name
     * @param dataType the series' unit/data-type name
     * @param timeSeries the backing list of string samples (trimmed in place)
     */
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

    /**
     * Reconstructs a persisted string time series from a {@code string_series} result row: its resolved name and
     * type, valid count, and the compressed data blob, which is inflated back into the backing list of strings.
     *
     * @param connection the database connection used to resolve the name and type ids
     * @param resultSet the result set positioned on the row to read
     * @throws SQLException if reading the row or resolving the name/type fails
     * @throws IOException if decompressing the stored data fails
     * @throws ClassNotFoundException if the stored blob cannot be deserialized to a string list
     */
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

    /**
     * Loads a flight's named string time series from the database, or returns {@code null} if the flight has no such
     * series.
     *
     * @param connection the database connection
     * @param flightId the flight whose series to load
     * @param name the string-series name
     * @return the loaded series, or {@code null} if it does not exist
     * @throws SQLException if the query fails
     */
    public static StringTimeSeries getStringTimeSeries(Connection connection, int flightId, String name)
            throws SQLException {
        try (PreparedStatement query =
                connection.prepareStatement("SELECT ss.name_id, ss.data_type_id, ss.length, ss.valid_length, "
                        + "ss.data FROM string_series AS ss INNER JOIN string_series_names "
                        + "AS ssn ON ssn.id = ss.name_id WHERE ssn.name = ? AND ss.flight_id = ?")) {

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

    /**
     * Returns a debug string with the series name, size, and valid (non-empty) count.
     *
     * @return a human-readable summary of this series
     */
    @Override
    public String toString() {
        return "[StringTimeSeries '" + name + "' size: " + timeSeries.size() + ", validCount: " + validCount + "]";
    }

    /**
     * Appends a value to the series, incrementing the valid count when the value is non-empty.
     *
     * @param s the value to append
     */
    public void add(String s) {
        if (!s.equals("")) validCount++;
        timeSeries.add(s);
    }

    /**
     * Returns the value at the given index.
     *
     * @param i the sample index
     * @return the value stored at index {@code i}
     */
    public String get(int i) {
        return timeSeries.get(i);
    }

    /**
     * Reports whether the value at the given index is empty.
     *
     * @param i the sample index
     * @return true if the value at index {@code i} is an empty string
     */
    public boolean emptyAt(int i) {
        return get(i).isEmpty();
    }

    public String getName() {
        return name.getName();
    }

    public String getDataType() {
        return dataType.getName();
    }

    /**
     * Returns the first non-empty value in the series, scanning forward from the start.
     *
     * @return the first non-empty value, or {@code null} if every value is empty
     */
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

    /**
     * Returns the last non-empty value in the series, scanning backward from the end.
     *
     * @return the last non-empty value, or {@code null} if every value is empty
     */
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

    /**
     * Returns the index of the last non-empty value, scanning backward from the end.
     *
     * @return the index of the last non-empty value, or -1 if every value is empty
     */
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

    /**
     * Returns the number of samples in the series.
     *
     * @return the number of samples
     */
    public int size() {
        return timeSeries.size();
    }

    /**
     * Returns the number of valid (non-empty) samples in the series.
     *
     * @return the valid sample count
     */
    public int validCount() {
        return validCount;
    }

    /**
     * Creates a prepared statement for inserting a string-series row (flight id, name/type ids, lengths, and the
     * compressed data blob) into the {@code string_series} table.
     *
     * @param connection the database connection
     * @return a prepared statement for the string-series insert
     * @throws SQLException if the statement cannot be prepared
     */
    public static PreparedStatement createPreparedStatement(Connection connection) throws SQLException {
        return connection.prepareStatement("INSERT INTO string_series "
                + "(flight_id, name_id, data_type_id, length, valid_length, data) VALUES (?, ?, ?, ?, ?, ?)");
    }

    /**
     * Binds this series to the given insert statement and adds it to the statement's batch: it resolves the name and
     * type ids if needed, sets the flight id and lengths, and stores the samples as a compressed blob.
     *
     * @param connection the database connection used to resolve the name/type ids if not already set
     * @param preparedStatement the insert statement (from {@link #createPreparedStatement}) to populate and batch
     * @param flightId the flight id to associate the series with
     * @throws SQLException if setting a parameter or adding the batch fails
     * @throws IOException if compressing the sample data fails
     */
    public void addBatch(Connection connection, PreparedStatement preparedStatement, int flightId)
            throws SQLException, IOException {
        if (name.getId() == -1) setNameId(connection);
        if (dataType.getId() == -1) setTypeId(connection);

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

    /**
     * Persists this series to the database for the given flight by inserting a single {@code string_series} row.
     *
     * @param connection the database connection
     * @param flightId the flight id to associate the series with
     * @throws IOException if compressing the sample data fails
     * @throws SQLException if the insert fails
     */
    public void updateDatabase(Connection connection, int flightId) throws IOException, SQLException {
        try (PreparedStatement preparedStatement = createPreparedStatement(connection)) {
            this.addBatch(connection, preparedStatement, flightId);

            preparedStatement.executeUpdate();
        }
    }

    /**
     * Creates a new database-aware series containing the samples in the half-open index range {@code [from, until)},
     * carrying over this series' name and data type.
     *
     * @param connection the database connection used to resolve the new series' name/type ids
     * @param from the inclusive start index
     * @param until the exclusive end index
     * @return a new series over the requested range
     * @throws SQLException if resolving the name or type id fails
     */
    public StringTimeSeries subSeries(Connection connection, int from, int until) throws SQLException {
        StringTimeSeries newSeries = new StringTimeSeries(connection, name.getName(), dataType.getName());

        for (int i = from; i < until; i++) newSeries.add(this.timeSeries.get(i));

        return newSeries;
    }

    /**
     * Creates a new (non-database-aware) series containing the samples in the half-open index range
     * {@code [from, until)}, carrying over this series' name and data type.
     *
     * @param from the inclusive start index
     * @param until the exclusive end index
     * @return a new series over the requested range
     */
    public StringTimeSeries subSeries(int from, int until) {
        StringTimeSeries newSeries = new StringTimeSeries(name.getName(), dataType.getName());

        for (int i = from; i < until; i++) newSeries.add(this.timeSeries.get(i));

        return newSeries;
    }
}
