package org.ngafid.flights;

import ch.randelshofer.fastdoubleparser.JavaDoubleParser;
import org.ngafid.common.Compression;
import org.ngafid.common.NormalizedColumn;
import org.ngafid.common.filters.Pair;

import javax.sql.rowset.serial.SerialBlob;
import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.Optional;
import java.util.logging.Logger;

import static org.ngafid.flights.Parameters.*;

public class DoubleTimeSeries {
    private static final Logger LOG = Logger.getLogger(DoubleTimeSeries.class.getName());
    private static final String DS_COLUMNS = "ds.id, ds.flight_id, ds.name_id, ds.data_type_id, " +
            "ds.length, ds.valid_length, ds.min, ds.avg, ds.max, ds.data";
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

    // Construct from an array
    public DoubleTimeSeries(String name, String dataType, double[] data, int size) {
        this.name = new DoubleSeriesName(name);
        this.dataType = new TypeName(dataType);
        this.data = data;
        this.size = size;

        calculateValidCountMinMaxAvg();
    }

    public DoubleTimeSeries(String name, Unit dataType, double[] data) {
        this(name, dataType.toString(), data);
    }

    public DoubleTimeSeries(String name, String dataType, double[] data) {
        this(name, dataType, data, data.length);
    }

    public DoubleTimeSeries(String name, Unit dataType, int sizeHint) {
        this(name, dataType.toString(), sizeHint);
    }

    public DoubleTimeSeries(String name, String dataType, int sizeHint) {
        this(name, dataType, new double[sizeHint], 0);
    }

    public DoubleTimeSeries(String name, Unit dataType) {
        this(name, dataType.toString());
    }

    public DoubleTimeSeries(String name, String dataType) {
        this(name, dataType, 16);
    }

    public DoubleTimeSeries(Connection connection, String name, Unit dataType, int sizeHint) throws SQLException {
        this(connection, name, dataType.toString(), sizeHint);
    }

    public DoubleTimeSeries(Connection connection, String name, String dataType, int sizeHint) throws SQLException {
        this(name, dataType, sizeHint);
        setNameId(connection);
        setTypeId(connection);
    }

    public DoubleTimeSeries(Connection connection, String name, Unit dataType) throws SQLException {
        this(connection, name, dataType.toString());
    }

    public DoubleTimeSeries(Connection connection, String name, String dataType) throws SQLException {
        this(connection, name, dataType, 16);
    }

    public DoubleTimeSeries(Connection connection, String name, Unit dataType, ArrayList<String> stringTimeSeries)
            throws SQLException {
        this(connection, name, dataType.toString(), stringTimeSeries);
    }

    public DoubleTimeSeries(Connection connection, String name, String dataType, ArrayList<String> stringTimeSeries)
            throws SQLException {
        this(name, dataType, stringTimeSeries);
        setNameId(connection);
        setTypeId(connection);
    }

    public DoubleTimeSeries(String name, Unit dataType, ArrayList<String> stringTimeSeries) {
        this(name, dataType.toString(), stringTimeSeries);
    }

    public DoubleTimeSeries(String name, String dataType, ArrayList<String> stringTimeSeries) {
        this.name = new DoubleSeriesName(name);
        this.dataType = new TypeName(dataType);

        this.data = new double[stringTimeSeries.size()];

        int emptyValues = 0;
        avg = 0.0;
        validCount = 0;

        for (int i = 0; i < stringTimeSeries.size(); i++) {
            String currentValue = stringTimeSeries.get(i);
            if (currentValue.length() == 0) {
                this.add(Double.NaN);
                emptyValues++;
                continue;
            }
            double currentDouble = JavaDoubleParser.parseDouble(stringTimeSeries.get(i));

            this.add(currentDouble);

            if (Double.isNaN(currentDouble))
                continue;
            avg += currentDouble;
            validCount++;

            if (currentDouble > max)
                max = currentDouble;
            if (currentDouble < min)
                min = currentDouble;
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

    public DoubleTimeSeries(Connection connection, ResultSet resultSet) throws SQLException, IOException {
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

        this.data = Compression.inflateDoubleArray(bytes, size);
    }

    public static DoubleTimeSeries computed(String name, Unit dataType, int length, TimeStepCalculation calculation) {
        return computed(name, dataType.toString(), length, calculation);
    }

    public static DoubleTimeSeries computed(String name, String dataType, int length, TimeStepCalculation calculation) {
        double[] data = new double[length];
        for (int i = 0; i < length; i++)
            data[i] = calculation.compute(i);

        return new DoubleTimeSeries(name, dataType, data, length);
    }

    public static Pair<Double, Double> getMinMax(Connection connection, int flightId, String name) throws SQLException {
        String queryString = "SELECT ds.min, ds.max FROM double_series AS ds INNER JOIN " +
                "double_series_names AS dsn ON ds.name_id = dsn.id WHERE ds.flight_id = ? AND dsn.name = ?";

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
                + " FROM double_series AS ds INNER JOIN double_series_names AS dsn on " +
                "dsn.id = ds.name_id WHERE ds.flight_id = ? ORDER BY dsn.name")) {
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
            throws IOException, SQLException {
        try (PreparedStatement query = connection.prepareStatement("SELECT " + DS_COLUMNS
                + " FROM double_series AS ds INNER JOIN double_series_names AS dsn on dsn.id = " +
                "ds.name_id WHERE ds.flight_id = ? AND dsn.name = ?")) {
            query.setInt(1, flightId);
            query.setString(2, name);

            LOG.info(query.toString());

            try (ResultSet resultSet = query.executeQuery()) {
                if (resultSet.next()) {
                    DoubleTimeSeries result = new DoubleTimeSeries(connection, resultSet);
                    return result;
                }

                return null;
            }
        }
    }

    public static PreparedStatement createPreparedStatement(Connection connection) throws SQLException {
        return connection.prepareStatement(
                "INSERT INTO double_series (flight_id, name_id, data_type_id, length, valid_length, " +
                        "min, avg, max, data) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)");
    }

    public static Optional<DoubleTimeSeries> getExistingLaggedSeries(Connection connection, int flightId,
                                                                     String seriesName, int n) throws IOException,
            SQLException {
        String laggedName = seriesName + LAG_SUFFIX + n;

        DoubleTimeSeries laggedSeries = getDoubleTimeSeries(connection, flightId, laggedName);
        if (laggedSeries != null)
            return Optional.of(laggedSeries);

        return Optional.empty();
    }

    public static Optional<DoubleTimeSeries> getExistingLeadingSeries(Connection connection, int flightId,
                                                                      String seriesName, int n) throws IOException,
            SQLException {
        String laggedName = seriesName + LEAD_SUFFIX + n;

        DoubleTimeSeries leadingSeries = getDoubleTimeSeries(connection, flightId, laggedName);
        if (leadingSeries != null)
            return Optional.of(leadingSeries);
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
        if (size <= 0)
            return;

        min = data[0];
        max = data[0];

        double sum = 0.0;
        for (int i = 1; i < size; i++) {
            if (Double.isNaN(data[i]))
                continue;

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

    public String toString() {
        return "[DoubleTimeSeries '" + name + "' size: " + this.size + ", validCount: " + validCount + ", min: " + min
                + ", avg: " + avg + ", max: " + max + "]";
    }

    public void add(double d) {
        // Need to resize
        if (this.size == data.length) {
            // Create a new buffer then copy the data to the new buffer.
            double[] oldData = this.data;
            this.data = new double[data.length * 2];
            System.arraycopy(oldData, 0, this.data, 0, oldData.length);
        }

        data[this.size++] = d;

        if (Double.isNaN(d))
            return;

        if (validCount == 0) {
            avg = d;
            max = d;
            min = d;
            validCount = 1;
        } else {
            if (d > max)
                max = d;
            if (d < min)
                min = d;

            avg = avg * ((double) validCount / (double) (validCount + 1)) + (d / (double) (validCount + 1));

            validCount++;
        }
    }

    public double get(int i) {
        return data[i];
    }

    public String getDataType() {
        return dataType.getName();
    }

    public int size() {
        return this.size;
    }

    public int validCount() {
        return validCount;
    }

    public double[] innerArray() {
        // double[] data = new double[this.size];
        // System.arraycopy(this.data, 0, data, 0, this.size);
        // This line can be used if arraycopy doesn't work for some reason
        // for (int i = 0; i < this.size(); i ++) data[i] = this.get(i);
        return data;
    }

    // including index from, up until (excluding)
    // if from == to, we assume from was supposed to be from + 1
    public double[] sliceCopy(int from, int to) {
        if (from == to)
            to += 1;
        double[] slice = new double[to - from];
        System.arraycopy(this.data, from, slice, 0, slice.length);
        return slice;
    }

    public void addBatch(Connection connection, PreparedStatement preparedStatement, int flightIdAdded)
            throws SQLException, IOException {
        if (this.dataType.getId() == -1)
            setTypeId(connection);

        if (this.name.getId() == -1)
            setNameId(connection);

        LOG.info("name id = " + name.getId());

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

    public void updateDatabase(Connection connection, int flightIdToAdd) throws IOException, SQLException {
        if (this.temporary)
            return;
        setTypeId(connection);
        setNameId(connection);

        try (PreparedStatement preparedStatement = connection.prepareStatement(
                "INSERT INTO double_series (flight_id, name_id, data_type_id, length, " +
                        "valid_length, min, avg, max, data) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
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
        Optional<DoubleTimeSeries> existingSeries = getExistingLaggedSeries(connection, this.flightId,
                this.name.getName(), n);

        return existingSeries.orElseGet(() -> lag(n));
    }

    public DoubleTimeSeries lag(int n) {
        DoubleTimeSeries laggedSeries = new DoubleTimeSeries(this.name + LAG_SUFFIX + n, "double");

        for (int i = 0; i < data.length; i++) {
            laggedSeries.add((i >= n) ? data[i - n] : Double.NaN);
        }

        return laggedSeries;
    }

    public DoubleTimeSeries lead(Connection connection, int n) throws IOException, SQLException {
        Optional<DoubleTimeSeries> existingSeries = getExistingLeadingSeries(connection, this.flightId,
                this.name.getName(), n);

        if (existingSeries.isPresent()) {
            return existingSeries.get();
        } else {
            return lead(n);
        }
    }

    public DoubleTimeSeries lead(int n) {
        DoubleTimeSeries leadingSeries = new DoubleTimeSeries(this.name + LEAD_SUFFIX + n, "double");

        int len = data.length;
        for (int i = 0; i < len; i++) {
            leadingSeries.add((i < len - n) ? data[i + n] : Double.NaN);
        }

        return leadingSeries;
    }

    // Creates a new DoubleTimeSeries from a slice in the range [from, until)
    public DoubleTimeSeries subSeries(Connection connection, int from, int until) throws SQLException {
        DoubleTimeSeries newSeries = new DoubleTimeSeries(connection, name.getName(), dataType.getName(), until - from);
        newSeries.size = until - from;
        System.arraycopy(data, from, newSeries.data, 0, until - from);
        return newSeries;
    }

    public DoubleTimeSeries subSeries(int from, int until) throws SQLException {
        DoubleTimeSeries newSeries = new DoubleTimeSeries(name.getName(), dataType.getName(), until - from);
        newSeries.size = until - from;
        System.arraycopy(data, from, newSeries.data, 0, until - from);
        return newSeries;
    }

    public interface TimeStepCalculation {
        double compute(int i);
    }

    public static class DoubleSeriesName extends NormalizedColumn<DoubleSeriesName> {
        private static Class<DoubleSeriesName> clazz = DoubleSeriesName.class;

        public DoubleSeriesName(String name) {
            super(clazz, name);
        }

        public DoubleSeriesName(int id) {
            super(clazz, id);
        }

        public DoubleSeriesName(Connection connection, int id) throws SQLException {
            super(clazz, connection, id);
        }

        public DoubleSeriesName(Connection connection, String string) throws SQLException {
            super(clazz, connection, string);
        }

        @Override
        protected String getTableName() {
            return "double_series_names";
        }
    }
}
