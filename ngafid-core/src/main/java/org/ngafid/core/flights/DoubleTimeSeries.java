package org.ngafid.core.flights;

import static org.ngafid.core.flights.Parameters.*;

import ch.randelshofer.fastdoubleparser.JavaDoubleParser;
import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.Optional;
import java.util.logging.Logger;
import javax.sql.rowset.serial.SerialBlob;
import org.ngafid.core.util.Compression;
import org.ngafid.core.util.NormalizedColumn;
import org.ngafid.core.util.filters.Pair;

/**
 * A named numeric time series (one column of sampled {@code double} values) for a single flight.
 *
 * <p>Holds the per-sample values alongside cached statistics (length, valid length, min/avg/max) and the series name
 * and data type, and handles persistence to the {@code double_series} table as a compressed blob, lazy decompression
 * on read, and derivation of new computed series from existing ones.
 */
public class DoubleTimeSeries {
    private static final Logger LOG = Logger.getLogger(DoubleTimeSeries.class.getName());
    private static final String DS_COLUMNS = "ds.id, ds.flight_id, ds.name_id, ds.data_type_id, "
            + "ds.length, ds.valid_length, ds.min, ds.avg, ds.max, ds.data";
    private int id = -1;
    private int flightId = -1;
    private DoubleSeriesName name;
    private TypeName dataType;
    // private ArrayList<Double> timeSeries;
    private double[] data;
    private int size = 0;
    // Set this to true if this double time series is temporary and should not be
    // written to the database.
    private boolean temporary = false;
    // Now called size since data.length is the buffer length and size is the number
    // of elements in the buffer
    // private int length = -1;
    private double min = Double.MAX_VALUE;
    private int validCount;
    private double avg;
    private double max = -Double.MAX_VALUE;

    /**
     * Constructs a double time series backed by an existing array, treating the first {@code size} elements as the
     * valid data. The running min, max, average and valid (non-NaN) count are computed up front from that data.
     *
     * @param name the series name
     * @param dataType the series' unit/data-type name
     * @param data the backing array of samples
     * @param size the number of valid elements at the front of {@code data}
     */
    public DoubleTimeSeries(String name, String dataType, double[] data, int size) {
        this.name = new DoubleSeriesName(name);
        this.dataType = new TypeName(dataType);
        this.data = data;
        this.size = size;

        calculateValidCountMinMaxAvg();
    }

    /**
     * Constructs a double time series from a full array, using a typed {@link Unit}. The array's length is the size.
     *
     * @param name the series name
     * @param dataType the series' unit
     * @param data the backing array of samples (its full length is used as the size)
     */
    public DoubleTimeSeries(String name, Unit dataType, double[] data) {
        this(name, dataType.toString(), data);
    }

    /**
     * Constructs a double time series from a full array; the array's length is used as the valid size.
     *
     * @param name the series name
     * @param dataType the series' unit/data-type name
     * @param data the backing array of samples
     */
    public DoubleTimeSeries(String name, String dataType, double[] data) {
        this(name, dataType, data, data.length);
    }

    /**
     * Constructs an empty double time series with a backing buffer pre-sized to {@code sizeHint}, using a typed
     * {@link Unit}.
     *
     * @param name the series name
     * @param dataType the series' unit
     * @param sizeHint the initial buffer capacity (the series starts empty)
     */
    public DoubleTimeSeries(String name, Unit dataType, int sizeHint) {
        this(name, dataType.toString(), sizeHint);
    }

    /**
     * Constructs an empty double time series with a backing buffer pre-sized to {@code sizeHint}.
     *
     * @param name the series name
     * @param dataType the series' unit/data-type name
     * @param sizeHint the initial buffer capacity (the series starts empty)
     */
    public DoubleTimeSeries(String name, String dataType, int sizeHint) {
        this(name, dataType, new double[sizeHint], 0);
    }

    /**
     * Constructs an empty double time series with a default initial capacity, using a typed {@link Unit}.
     *
     * @param name the series name
     * @param dataType the series' unit
     */
    public DoubleTimeSeries(String name, Unit dataType) {
        this(name, dataType.toString());
    }

    /**
     * Constructs an empty double time series with a default initial capacity (16).
     *
     * @param name the series name
     * @param dataType the series' unit/data-type name
     */
    public DoubleTimeSeries(String name, String dataType) {
        this(name, dataType, 16);
    }

    /**
     * Constructs an empty database-aware double time series (buffer pre-sized to {@code sizeHint}) using a typed
     * {@link Unit}, resolving and caching its name and type ids from the database.
     *
     * @param connection the database connection used to resolve the name/type ids
     * @param name the series name
     * @param dataType the series' unit
     * @param sizeHint the initial buffer capacity
     * @throws SQLException if resolving the name or type id fails
     */
    public DoubleTimeSeries(Connection connection, String name, Unit dataType, int sizeHint) throws SQLException {
        this(connection, name, dataType.toString(), sizeHint);
    }

    /**
     * Constructs an empty database-aware double time series (buffer pre-sized to {@code sizeHint}), resolving and
     * caching its name and type ids from the database so it can later be persisted.
     *
     * @param connection the database connection used to resolve the name/type ids
     * @param name the series name
     * @param dataType the series' unit/data-type name
     * @param sizeHint the initial buffer capacity
     * @throws SQLException if resolving the name or type id fails
     */
    public DoubleTimeSeries(Connection connection, String name, String dataType, int sizeHint) throws SQLException {
        this(name, dataType, sizeHint);
        setNameId(connection);
        setTypeId(connection);
    }

    /**
     * Constructs an empty database-aware double time series with a default capacity, using a typed {@link Unit}.
     *
     * @param connection the database connection used to resolve the name/type ids
     * @param name the series name
     * @param dataType the series' unit
     * @throws SQLException if resolving the name or type id fails
     */
    public DoubleTimeSeries(Connection connection, String name, Unit dataType) throws SQLException {
        this(connection, name, dataType.toString());
    }

    /**
     * Constructs an empty database-aware double time series with a default capacity (16).
     *
     * @param connection the database connection used to resolve the name/type ids
     * @param name the series name
     * @param dataType the series' unit/data-type name
     * @throws SQLException if resolving the name or type id fails
     */
    public DoubleTimeSeries(Connection connection, String name, String dataType) throws SQLException {
        this(connection, name, dataType, 16);
    }

    /**
     * Constructs a database-aware double time series by parsing string samples into doubles (using a typed
     * {@link Unit}), and resolves its name and type ids from the database.
     *
     * @param connection the database connection used to resolve the name/type ids
     * @param name the series name
     * @param dataType the series' unit
     * @param stringTimeSeries the raw string samples to parse into doubles
     * @throws SQLException if resolving the name or type id fails
     */
    public DoubleTimeSeries(Connection connection, String name, Unit dataType, ArrayList<String> stringTimeSeries)
            throws SQLException {
        this(connection, name, dataType.toString(), stringTimeSeries);
    }

    /**
     * Constructs a database-aware double time series by parsing string samples into doubles, and resolves its name
     * and type ids from the database.
     *
     * @param connection the database connection used to resolve the name/type ids
     * @param name the series name
     * @param dataType the series' unit/data-type name
     * @param stringTimeSeries the raw string samples to parse into doubles
     * @throws SQLException if resolving the name or type id fails
     */
    public DoubleTimeSeries(Connection connection, String name, String dataType, ArrayList<String> stringTimeSeries)
            throws SQLException {
        this(name, dataType, stringTimeSeries);
        setNameId(connection);
        setTypeId(connection);
    }

    /**
     * Constructs a double time series by parsing string samples into doubles, using a typed {@link Unit}.
     *
     * @param name the series name
     * @param dataType the series' unit
     * @param stringTimeSeries the raw string samples to parse into doubles
     */
    public DoubleTimeSeries(String name, Unit dataType, ArrayList<String> stringTimeSeries) {
        this(name, dataType.toString(), stringTimeSeries);
    }

    /**
     * Constructs a double time series from raw string samples, parsing each into a double. Blank or whitespace-only
     * values become {@code NaN}; leading spaces are skipped before parsing. The min, max, average and valid count
     * are computed as the values are parsed, and a column that is entirely empty logs a warning and yields NaN
     * statistics.
     *
     * @param name the series name
     * @param dataType the series' unit/data-type name
     * @param stringTimeSeries the raw string samples to parse into doubles
     */
    public DoubleTimeSeries(String name, String dataType, ArrayList<String> stringTimeSeries) {
        this.name = new DoubleSeriesName(name);
        this.dataType = new TypeName(dataType);

        this.data = new double[stringTimeSeries.size()];

        int emptyValues = 0;
        avg = 0.0;
        validCount = 0;

        for (int i = 0; i < stringTimeSeries.size(); i++) {
            String currentValue = stringTimeSeries.get(i);

            int offset = 0;
            while (offset < currentValue.length() && currentValue.charAt(offset) == ' ') {
                offset += 1;
            }

            if (currentValue.isEmpty() || offset == currentValue.length()) {
                this.add(Double.NaN);
                emptyValues++;
                continue;
            }

            double currentDouble = JavaDoubleParser.parseDouble(currentValue, offset, currentValue.length() - offset);

            this.add(currentDouble);

            if (Double.isNaN(currentDouble)) continue;
            avg += currentDouble;
            validCount++;

            if (currentDouble > max) max = currentDouble;
            if (currentDouble < min) min = currentDouble;
        }

        if (emptyValues > 0) {
            if (emptyValues == stringTimeSeries.size()) {
                LOG.warning("double column '" + name + "' only had empty values.");
                min = Double.NaN;
                avg = Double.NaN;
                max = Double.NaN;
            }
        }

        avg /= validCount;
    }

    /**
     * Reconstructs a persisted double time series from a {@code double_series} result row: its id, flight id,
     * resolved name and type, size, precomputed statistics, and the compressed sample blob, which is inflated back
     * into the backing array.
     *
     * @param connection the database connection used to resolve the name and type ids
     * @param resultSet the result set positioned on the row to read
     * @throws SQLException if reading the row or resolving the name/type fails
     */
    public DoubleTimeSeries(Connection connection, ResultSet resultSet) throws SQLException {
        id = resultSet.getInt(1);
        flightId = resultSet.getInt(2);
        name = new DoubleSeriesName(connection, resultSet.getInt(3));
        dataType = new TypeName(connection, resultSet.getInt(4));
        size = resultSet.getInt(5);
        validCount = resultSet.getInt(6);
        min = resultSet.getDouble(7);
        avg = resultSet.getDouble(8);
        max = resultSet.getDouble(9);

        Blob values = resultSet.getBlob(10);
        byte[] bytes = values.getBytes(1, (int) values.length());
        values.free();

        try {
            this.data = Compression.inflateDoubleArray(bytes, size);
        } catch (IOException e) {
            throw new RuntimeException("Failed to deserialize a double time series!");
        }
    }

    /**
     * Creates a double time series of the given length whose values are produced by a per-index calculation, using
     * a typed {@link Unit}.
     *
     * @param name the series name
     * @param dataType the series' unit
     * @param length the number of samples to compute
     * @param calculation the function evaluated at each index to produce its value
     * @return the computed series
     */
    public static DoubleTimeSeries computed(String name, Unit dataType, int length, TimeStepCalculation calculation) {
        return computed(name, dataType.toString(), length, calculation);
    }

    /**
     * Creates a double time series of the given length by evaluating {@code calculation} at each index {@code 0}
     * through {@code length - 1}.
     *
     * @param name the series name
     * @param dataType the series' unit/data-type name
     * @param length the number of samples to compute
     * @param calculation the function evaluated at each index to produce its value
     * @return the computed series
     */
    public static DoubleTimeSeries computed(String name, String dataType, int length, TimeStepCalculation calculation) {
        double[] data = new double[length];
        for (int i = 0; i < length; i++) data[i] = calculation.compute(i);

        return new DoubleTimeSeries(name, dataType, data, length);
    }

    /**
     * Looks up the stored minimum and maximum of a named double series for a flight directly from the database,
     * without loading the full series.
     *
     * @param connection the database connection
     * @param flightId the flight whose series to query
     * @param name the double-series name
     * @return a {@code (min, max)} pair, or {@code null} if the flight has no such series
     * @throws SQLException if the query fails
     */
    public static Pair<Double, Double> getMinMax(Connection connection, int flightId, String name) throws SQLException {
        String queryString = "SELECT ds.min, ds.max FROM double_series AS ds INNER JOIN "
                + "double_series_names AS dsn ON ds.name_id = dsn.id WHERE ds.flight_id = ? AND dsn.name = ?";

        try (PreparedStatement query = connection.prepareStatement(queryString)) {
            query.setInt(1, flightId);
            query.setString(2, name);

            try (ResultSet resultSet = query.executeQuery()) {
                if (resultSet.next()) {
                    double min = resultSet.getDouble(1);
                    double max = resultSet.getDouble(2);

                    return new Pair<Double, Double>(min, max);
                }
            }

            return null;
        }
    }

    /**
     * Returns the names of all known double series (the full {@code double_series_names} catalog, ordered by name).
     *
     * @param connection the database connection
     * @param fleetId the fleet id (currently unused; the name catalog is global)
     * @return the alphabetically ordered list of double-series names
     * @throws SQLException if the query fails
     */
    public static ArrayList<String> getAllNames(Connection connection, int fleetId) throws SQLException {
        ArrayList<String> names = new ArrayList<>();

        String queryString = "SELECT name FROM double_series_names ORDER BY name";
        try (PreparedStatement query = connection.prepareStatement(queryString);
                ResultSet resultSet = query.executeQuery()) {

            while (resultSet.next()) {
                String name = resultSet.getString(1);
                names.add(name);
            }

            return names;
        }
    }

    /**
     * Gets an ArrayList of all DoubleTimeSeries for a flight.
     *
     * @param connection is the connection to the database.
     * @param flightId   is the id of the flight.
     * @return An ArrayList of all the DoubleTimeSeries for his flight.
     */
    public static ArrayList<DoubleTimeSeries> getAllDoubleTimeSeries(Connection connection, int flightId)
            throws SQLException, IOException {
        try (PreparedStatement query = connection.prepareStatement("SELECT " + DS_COLUMNS
                + " FROM double_series AS ds INNER JOIN double_series_names AS dsn on "
                + "dsn.id = ds.name_id WHERE ds.flight_id = ? ORDER BY dsn.name")) {
            query.setInt(1, flightId);

            ArrayList<DoubleTimeSeries> allSeries = new ArrayList<DoubleTimeSeries>();

            try (ResultSet resultSet = query.executeQuery()) {
                while (resultSet.next()) {
                    DoubleTimeSeries result = new DoubleTimeSeries(connection, resultSet);
                    allSeries.add(result);
                }
            }

            return allSeries;
        }
    }

    /**
     * Gets an ArrayList of all DoubleTimeSeries for a flight with a given column
     * name.
     *
     * @param connection is the connection to the database.
     * @param flightId   is the id of the flight.
     * @param name       is the column name of the double time series
     * @return a DoubleTimeSeries for his flight and column name, null if it does
     * not exist.
     */
    public static DoubleTimeSeries getDoubleTimeSeries(Connection connection, int flightId, String name)
            throws SQLException {
        try (PreparedStatement query = connection.prepareStatement("SELECT " + DS_COLUMNS
                + " FROM double_series AS ds INNER JOIN double_series_names AS dsn on dsn.id = "
                + "ds.name_id WHERE ds.flight_id = ? AND dsn.name = ?")) {
            query.setInt(1, flightId);
            query.setString(2, name);

            try (ResultSet resultSet = query.executeQuery()) {
                if (resultSet.next()) {
                    DoubleTimeSeries result = new DoubleTimeSeries(connection, resultSet);
                    return result;
                }

                return null;
            }
        }
    }

    /**
     * Creates a prepared statement for inserting a double-series row (flight id, name/type ids, lengths, stored
     * statistics, and the compressed data blob) into the {@code double_series} table.
     *
     * @param connection the database connection
     * @return a prepared statement for the double-series insert
     * @throws SQLException if the statement cannot be prepared
     */
    public static PreparedStatement createPreparedStatement(Connection connection) throws SQLException {
        return connection.prepareStatement(
                "INSERT INTO double_series (flight_id, name_id, data_type_id, length, valid_length, "
                        + "min, avg, max, data) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)");
    }

    /**
     * Returns the already-persisted lagged-by-{@code n} version of a series, if one exists, by looking up the
     * conventionally named series ({@code seriesName} + lag suffix + {@code n}); avoids recomputing a lag that has
     * been stored before.
     *
     * @param connection the database connection
     * @param flightId the flight the series belongs to
     * @param seriesName the base series name
     * @param n the lag amount
     * @return the stored lagged series, or {@link Optional#empty()} if it has not been computed
     * @throws IOException if decompressing the stored series fails
     * @throws SQLException if the lookup query fails
     */
    public static Optional<DoubleTimeSeries> getExistingLaggedSeries(
            Connection connection, int flightId, String seriesName, int n) throws IOException, SQLException {
        String laggedName = seriesName + LAG_SUFFIX + n;

        DoubleTimeSeries laggedSeries = getDoubleTimeSeries(connection, flightId, laggedName);
        if (laggedSeries != null) return Optional.of(laggedSeries);

        return Optional.empty();
    }

    /**
     * Returns the already-persisted leading-by-{@code n} version of a series, if one exists, by looking up the
     * conventionally named series ({@code seriesName} + lead suffix + {@code n}); avoids recomputing a lead that has
     * been stored before.
     *
     * @param connection the database connection
     * @param flightId the flight the series belongs to
     * @param seriesName the base series name
     * @param n the lead amount
     * @return the stored leading series, or {@link Optional#empty()} if it has not been computed
     * @throws IOException if decompressing the stored series fails
     * @throws SQLException if the lookup query fails
     */
    public static Optional<DoubleTimeSeries> getExistingLeadingSeries(
            Connection connection, int flightId, String seriesName, int n) throws IOException, SQLException {
        String laggedName = seriesName + LEAD_SUFFIX + n;

        DoubleTimeSeries leadingSeries = getDoubleTimeSeries(connection, flightId, laggedName);
        if (leadingSeries != null) return Optional.of(leadingSeries);
        return Optional.empty();
    }

    public void setTemporary(boolean temp) {
        this.temporary = temp;
    }

    private void setNameId(Connection connection) throws SQLException {
        this.name = new DoubleSeriesName(connection, name.getName());
    }

    private void setTypeId(Connection connection) throws SQLException {
        this.dataType = new TypeName(connection, dataType.getName());
    }

    private void calculateValidCountMinMaxAvg() {
        if (size <= 0) return;

        min = data[0];
        max = data[0];

        double sum = 0.0;
        for (int i = 1; i < size; i++) {
            if (Double.isNaN(data[i])) continue;

            sum += data[i];

            min = min > data[i] ? data[i] : min;
            max = max < data[i] ? data[i] : max;
            validCount++;
        }

        avg = sum / validCount;
    }

    /**
     * Gets the name of the DoubleTimeSeries.
     *
     * @return the column name of the DoubleTimeSeries
     */
    public String getName() {
        return name.getName();
    }

    /**
     * Gets the minimum value of the DoubleTimeSeries.
     *
     * @return the minimum value of the DoubleTimeSeries
     */
    public double getMin() {
        return min;
    }

    /**
     * Gets the maximum value of the DoubleTimeSeries.
     *
     * @return the maximum value of the DoubleTimeSeries
     */
    public double getMax() {
        return max;
    }

    /**
     * Gets the average value of the DoubleTimeSeries.
     *
     * @return the average value of the DoubleTimeSeries
     */
    public double getAvg() {
        return avg;
    }

    /**
     * Returns a debug string with the series name, size, valid count, and min/avg/max statistics.
     *
     * @return a human-readable summary of this series
     */
    @Override
    public String toString() {
        return "[DoubleTimeSeries '" + name + "' size: " + this.size + ", validCount: " + validCount + ", min: " + min
                + ", avg: " + avg + ", max: " + max + "]";
    }

    /**
     * Appends a value to the series, growing the backing buffer (doubling it) when full and updating the running
     * min, max, average and valid count incrementally. {@code NaN} values are stored but excluded from the
     * statistics.
     *
     * @param d the value to append
     */
    public void add(double d) {
        // Need to resize
        if (this.size == data.length) {
            // Create a new buffer then copy the data to the new buffer.
            double[] oldData = this.data;
            this.data = new double[data.length * 2];
            System.arraycopy(oldData, 0, this.data, 0, oldData.length);
        }

        data[this.size++] = d;

        if (Double.isNaN(d)) return;

        if (validCount == 0) {
            avg = d;
            max = d;
            min = d;
            validCount = 1;
        } else {
            if (d > max) max = d;
            if (d < min) min = d;

            avg = avg * ((double) validCount / (double) (validCount + 1)) + (d / (double) (validCount + 1));

            validCount++;
        }
    }

    /**
     * Returns the value at the given index.
     *
     * @param i the sample index
     * @return the value stored at index {@code i} (may be {@code NaN})
     */
    public double get(int i) {
        return data[i];
    }

    public String getDataType() {
        return dataType.getName();
    }

    /**
     * Returns the number of samples in the series (which may be fewer than the backing buffer's capacity).
     *
     * @return the number of samples
     */
    public int size() {
        return this.size;
    }

    /**
     * Returns the number of valid (non-NaN) samples in the series.
     *
     * @return the valid sample count
     */
    public int validCount() {
        return validCount;
    }

    /**
     * Returns the backing array directly (not a copy), for performance-sensitive callers. The array may be longer
     * than {@link #size()}; only the first {@code size()} elements are valid data.
     *
     * @return the internal backing array
     */
    public double[] innerArray() {
        // double[] data = new double[this.size];
        // System.arraycopy(this.data, 0, data, 0, this.size);
        // This line can be used if arraycopy doesn't work for some reason
        // for (int i = 0; i < this.size(); i ++) data[i] = this.get(i);
        return data;
    }

    /**
     * Returns a copy of the samples in the half-open index range {@code [from, to)}. As a special case, when
     * {@code from == to} the range is treated as {@code [from, from + 1)} (a single element).
     *
     * @param from the inclusive start index
     * @param to the exclusive end index
     * @return a new array containing the requested slice
     */
    // including index from, up until (excluding)
    // if from == to, we assume from was supposed to be from + 1
    public double[] sliceCopy(int from, int to) {
        if (from == to) to += 1;
        double[] slice = new double[to - from];
        System.arraycopy(this.data, from, slice, 0, slice.length);
        return slice;
    }

    /**
     * Binds this series to the given insert statement and adds it to the statement's batch: it resolves the name and
     * type ids if needed, sets the flight id, lengths and statistics (writing SQL NULL for NaN min/avg/max), and
     * stores the samples as a compressed blob.
     *
     * @param connection the database connection used to resolve the name/type ids if not already set
     * @param preparedStatement the insert statement (from {@link #createPreparedStatement}) to populate and batch
     * @param flightIdAdded the flight id to associate the series with
     * @throws SQLException if setting a parameter or adding the batch fails
     * @throws IOException if compressing the sample data fails
     */
    public void addBatch(Connection connection, PreparedStatement preparedStatement, int flightIdAdded)
            throws SQLException, IOException {
        if (this.dataType.getId() == -1) setTypeId(connection);

        if (this.name.getId() == -1) setNameId(connection);

        preparedStatement.setInt(1, flightIdAdded);
        preparedStatement.setInt(2, name.getId());
        preparedStatement.setInt(3, dataType.getId());

        preparedStatement.setInt(4, this.size);
        preparedStatement.setInt(5, validCount);

        if (Double.isNaN(min)) {
            preparedStatement.setNull(6, java.sql.Types.DOUBLE);
        } else {
            preparedStatement.setDouble(6, min);
        }

        if (Double.isNaN(avg)) {
            preparedStatement.setNull(7, java.sql.Types.DOUBLE);
        } else {
            preparedStatement.setDouble(7, avg);
        }

        if (Double.isNaN(max)) {
            preparedStatement.setNull(8, java.sql.Types.DOUBLE);
        } else {
            preparedStatement.setDouble(8, max);
        }

        // UPDATED COMPRESSION CODE
        byte[] compressed = Compression.compressDoubleArray(this.data);
        Blob seriesBlob = new SerialBlob(compressed);

        preparedStatement.setBlob(9, seriesBlob);

        preparedStatement.addBatch();
    }

    /**
     * Persists this series to the database for the given flight by inserting a single {@code double_series} row.
     * Temporary series (see {@code setTemporary}) are skipped and not written.
     *
     * @param connection the database connection
     * @param flightIdToAdd the flight id to associate the series with
     * @throws IOException if compressing the sample data fails
     * @throws SQLException if the insert fails
     */
    public void updateDatabase(Connection connection, int flightIdToAdd) throws IOException, SQLException {
        if (this.temporary) return;
        setTypeId(connection);
        setNameId(connection);

        try (PreparedStatement preparedStatement =
                connection.prepareStatement("INSERT INTO double_series (flight_id, name_id, data_type_id, length, "
                        + "valid_length, min, avg, max, data) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
            this.addBatch(connection, preparedStatement, flightIdToAdd);
            preparedStatement.executeBatch();
        }
    }

    /**
     * Lags a timeseries N indicies
     *
     * @param connection the connection to the database
     * @param n          the number of indicies to lag
     * @return the lagged timeseries
     */
    public DoubleTimeSeries lag(Connection connection, int n) throws IOException, SQLException {
        Optional<DoubleTimeSeries> existingSeries =
                getExistingLaggedSeries(connection, this.flightId, this.name.getName(), n);

        return existingSeries.orElseGet(() -> lag(n));
    }

    /**
     * Computes a new series lagged by {@code n} samples: element {@code i} holds this series' value at {@code i - n},
     * with the first {@code n} elements set to {@code NaN} (no earlier sample exists).
     *
     * @param n the number of samples to lag by
     * @return a new lagged series
     */
    public DoubleTimeSeries lag(int n) {
        DoubleTimeSeries laggedSeries = new DoubleTimeSeries(this.name + LAG_SUFFIX + n, "double");

        for (int i = 0; i < data.length; i++) {
            laggedSeries.add((i >= n) ? data[i - n] : Double.NaN);
        }

        return laggedSeries;
    }

    /**
     * Returns the series led by {@code n} samples, reusing the stored leading series if one has already been
     * persisted, otherwise computing it via {@link #lead(int)}.
     *
     * @param connection the database connection used to look up a stored leading series
     * @param n the number of samples to lead by
     * @return the leading series (stored or freshly computed)
     * @throws IOException if decompressing a stored series fails
     * @throws SQLException if the lookup query fails
     */
    public DoubleTimeSeries lead(Connection connection, int n) throws IOException, SQLException {
        Optional<DoubleTimeSeries> existingSeries =
                getExistingLeadingSeries(connection, this.flightId, this.name.getName(), n);

        if (existingSeries.isPresent()) {
            return existingSeries.get();
        } else {
            return lead(n);
        }
    }

    /**
     * Computes a new series led by {@code n} samples: element {@code i} holds this series' value at {@code i + n},
     * with the last {@code n} elements set to {@code NaN} (no later sample exists).
     *
     * @param n the number of samples to lead by
     * @return a new leading series
     */
    public DoubleTimeSeries lead(int n) {
        DoubleTimeSeries leadingSeries = new DoubleTimeSeries(this.name + LEAD_SUFFIX + n, "double");

        int len = data.length;
        for (int i = 0; i < len; i++) {
            leadingSeries.add((i < len - n) ? data[i + n] : Double.NaN);
        }

        return leadingSeries;
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
    // Creates a new DoubleTimeSeries from a slice in the range [from, until)
    public DoubleTimeSeries subSeries(Connection connection, int from, int until) throws SQLException {
        DoubleTimeSeries newSeries = new DoubleTimeSeries(connection, name.getName(), dataType.getName(), until - from);
        newSeries.size = until - from;
        System.arraycopy(data, from, newSeries.data, 0, until - from);
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
    public DoubleTimeSeries subSeries(int from, int until) {
        DoubleTimeSeries newSeries = new DoubleTimeSeries(name.getName(), dataType.getName(), until - from);
        newSeries.size = until - from;
        System.arraycopy(data, from, newSeries.data, 0, until - from);
        return newSeries;
    }

    public Pair<Double, Double> getMinMax() {
        return new Pair<>(min, max);
    }

    /**
     * Returns the index of the last valid (non-NaN) sample, scanning backward from the end of the series.
     *
     * @return the index of the last non-NaN value, or -1 if the series is empty or entirely NaN
     */
    public int getLastValidIndex() {
        int i = size - 1;
        while (i >= 0 && Double.isNaN(data[i])) {
            i--;
        }
        return i;
    }

    /**
     * Functional interface for computing a series value from its sample index, used by
     * {@link #computed(String, String, int, TimeStepCalculation)}.
     */
    public interface TimeStepCalculation {
        /**
         * Computes the value at the given sample index.
         *
         * @param i the sample index
         * @return the computed value for that index
         */
        double compute(int i);
    }

    /**
     * A normalized name for a double time series, interned in the {@code double_series_names} table.
     *
     * <p>Stores each distinct series name once by id and resolves between name and id through the shared
     * {@link NormalizedColumn} caching and lookup machinery.
     */
    public static class DoubleSeriesName extends NormalizedColumn<DoubleSeriesName> {
        /**
         * Creates an unresolved double-series name from its string value (no database id assigned yet).
         *
         * @param name the series name
         */
        public DoubleSeriesName(String name) {
            super(name);
        }

        /**
         * Creates an unresolved double-series name from its database id (the name string is resolved lazily).
         *
         * @param id the series-name id
         */
        public DoubleSeriesName(int id) {
            super(id);
        }

        /**
         * Resolves a double-series name from its database id, looking up the corresponding name string.
         *
         * @param connection the database connection
         * @param id the series-name id
         * @throws SQLException if the lookup fails
         */
        public DoubleSeriesName(Connection connection, int id) throws SQLException {
            super(connection, id);
        }

        /**
         * Resolves a double-series name from its string value, looking up (or assigning) its database id.
         *
         * @param connection the database connection
         * @param string the series name
         * @throws SQLException if the lookup or insert fails
         */
        public DoubleSeriesName(Connection connection, String string) throws SQLException {
            super(connection, string);
        }

        @Override
        protected String getTableName() {
            return "double_series_names";
        }
    }
}
