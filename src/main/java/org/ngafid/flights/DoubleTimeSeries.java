package org.ngafid.flights;

import java.io.IOException;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;

import java.sql.Blob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import java.util.ArrayList;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.stream.DoubleStream;
import java.util.zip.Deflater;

import org.ngafid.Database;
import org.ngafid.common.Compression;
import org.ngafid.filters.Pair;

import static org.ngafid.flights.Parameters.*;

import javax.sql.rowset.serial.SerialBlob;

public class DoubleTimeSeries {
    private static final Logger LOG = Logger.getLogger(DoubleTimeSeries.class.getName());
    private static final int COMPRESSION_LEVEL = Deflater.DEFAULT_COMPRESSION;
    private static final String DS_COLUMNS = "ds.id, ds.flight_id, ds.name_id, ds.data_type_id, ds.length, ds.valid_length, ds.min, ds.avg, ds.max, ds.data";

    private int id = -1;
    private int flightId = -1;
    private int nameId;
    private String name;
    private int typeId;
    private String dataType;
    // private ArrayList<Double> timeSeries;
    private double[] data;
    private int size = 0;

    // Set this to true if this double time series is temporary and should not be written to the database.
    private boolean temporary = false;

    // Now called size since data.length is the buffer length and size is the number of elements in the buffer
    // private int length = -1;
    private double min = Double.MAX_VALUE;
    private int validCount;
    private double avg;
    private double max = -Double.MAX_VALUE;

    // Construct from an array
    public DoubleTimeSeries(String name, String dataType, double[] data, int size) {
        this.name = name;
        this.dataType = dataType;
        this.data = data;
        this.size = size;

        calculateValidCountMinMaxAvg();
    }

    public DoubleTimeSeries(String name, String dataType, double[] data) {
        this(name, dataType, data, data.length);
    }

    public DoubleTimeSeries(String name, String dataType, int sizeHint) {
        this(name, dataType, new double[sizeHint], 0);
     }

    public DoubleTimeSeries(String name, String dataType) {
        this(name, dataType, 16);
    }

    public DoubleTimeSeries(Connection connection, String name, String dataType, int sizeHint) throws SQLException {
        this(name, dataType, sizeHint);
        setNameId(connection);
        setTypeId(connection);
    }

    public DoubleTimeSeries(Connection connection, String name, String dataType) throws SQLException {
        this(connection, name, dataType, 16);
    }

    public DoubleTimeSeries(Connection connection, String name, String dataType, ArrayList<String> stringTimeSeries) throws SQLException {
        this(name, dataType, stringTimeSeries);
        setNameId(connection);
        setTypeId(connection);
    }

    public DoubleTimeSeries(String name, String dataType, ArrayList<String> stringTimeSeries) {
        this.name = name;
        this.dataType = dataType;

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
            double currentDouble = Double.parseDouble(stringTimeSeries.get(i));

            this.add(currentDouble);

            if (Double.isNaN(currentDouble)) continue;
            avg += currentDouble;
            validCount++;

            if (currentDouble > max) max = currentDouble;
            if (currentDouble < min) min = currentDouble;
        }

        if (emptyValues > 0) {
            //System.err.println("WARNING: double column '" + name + "' had " + emptyValues + " empty values.");
            if (emptyValues == stringTimeSeries.size()) {
                System.err.println("WARNING: double column '" + name + "' only had empty values.");
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
        nameId = resultSet.getInt(3);
        name = SeriesNames.getDoubleName(connection, nameId);
        typeId = resultSet.getInt(4);
        dataType = TypeNames.getName(connection, typeId);
        size = resultSet.getInt(5);
        validCount = resultSet.getInt(6);
        min = resultSet.getDouble(7);
        avg = resultSet.getDouble(8);
        max = resultSet.getDouble(9);

        Blob values = resultSet.getBlob(10);
        byte[] bytes = values.getBytes(1, (int)values.length());
        values.free();
        
        this.data = Compression.inflateDoubleArray(bytes, size);
    }
  
    public interface TimeStepCalculation {
        double compute(int i);
    }

    public void setTemporary(boolean temp) {
        this.temporary = temp;
    }

    public static DoubleTimeSeries computed(String name, String dataType, int length, TimeStepCalculation calculation) {
        double[] data = new double[length];
        for (int i = 0; i < length; i++)
            data[i] = calculation.compute(i);
        
        return new DoubleTimeSeries(name, dataType, data, length);
    }

    public static Pair<Double,Double> getMinMax(Connection connection, int flightId, String name) throws SQLException {
        String queryString = "SELECT ds.min, ds.max FROM double_series AS ds INNER JOIN double_series_names AS dsn ON ds.name_id = dsn.id WHERE ds.flight_id = ? AND dsn.name = ?";
        PreparedStatement query = connection.prepareStatement(queryString);
        query.setInt(1, flightId);
        query.setString(2, name);

        //LOG.info(query.toString());
        ResultSet resultSet = query.executeQuery();

        if (resultSet.next()) {
            double min = resultSet.getDouble(1);
            double max = resultSet.getDouble(2);

            resultSet.close();
            query.close();
            return new Pair<Double,Double>(min, max);
        }

        return null;
    }

    public static ArrayList<String> getAllNames(Connection connection, int fleetId) throws SQLException {
        ArrayList<String> names = new ArrayList<>();

        String queryString = "SELECT name FROM double_series_names ORDER BY name";
        PreparedStatement query = connection.prepareStatement(queryString);

        //LOG.info(query.toString());
        ResultSet resultSet = query.executeQuery();

        while (resultSet.next()) {
            //double series name existed in the database, return the id
            String name = resultSet.getString(1);
            names.add(name);
        }

        resultSet.close();
        query.close();

        return names;
    }


    /**
     * Gets an ArrayList of all DoubleTimeSeries for a flight.
     *
     * @param connection is the connection to the database.
     * @param flightId is the id of the flight.
     * @return An ArrayList of all the DoubleTimeSeries for his flight.
     */
    public static ArrayList<DoubleTimeSeries> getAllDoubleTimeSeries(Connection connection, int flightId) throws SQLException, IOException {
        PreparedStatement query = connection.prepareStatement("SELECT " + DS_COLUMNS + " FROM double_series AS ds INNER JOIN double_series_names AS dsn on dsn.id = ds.name_id WHERE ds.flight_id = ? ORDER BY dsn.name");

        query.setInt(1, flightId);
        LOG.info(query.toString());

        ArrayList<DoubleTimeSeries> allSeries = new ArrayList<DoubleTimeSeries>();
        ResultSet resultSet = query.executeQuery();
        while (resultSet.next()) {
            DoubleTimeSeries result = new DoubleTimeSeries(connection, resultSet);
            allSeries.add(result);
        }
        resultSet.close();
        query.close();

        return allSeries;
    }

    /**
     * Gets an ArrayList of all DoubleTimeSeries for a flight with a given column name.
     *
     * @param connection is the connection to the database.
     * @param flightId is the id of the flight.
     * @param name is the column name of the double time series
     * @return a DoubleTimeSeries for his flight and column name, null if it does not exist.
     */
    public static DoubleTimeSeries getDoubleTimeSeries(Connection connection, int flightId, String name) throws SQLException {
        PreparedStatement query = connection.prepareStatement("SELECT " + DS_COLUMNS + " FROM double_series AS ds INNER JOIN double_series_names AS dsn on dsn.id = ds.name_id WHERE ds.flight_id = ? AND dsn.name = ?");

        query.setInt(1, flightId);
        query.setString(2, name);
        LOG.info(query.toString());

        ResultSet resultSet = query.executeQuery();
        if (resultSet.next()) {
            try {
                DoubleTimeSeries result = new DoubleTimeSeries(connection, resultSet);
                return result;
            } catch (IOException e) {
                LOG.severe("Encountered IOException while reading double time series from database. This should not happen.");
                System.exit(1);
            } finally {
                resultSet.close();
                query.close();
            }
            return null; // This is unreachable
        } else {
            //TODO: should probably throw an exception
            resultSet.close();
            query.close();
            return null;
        }
    }

    private void setNameId(Connection connection) throws SQLException {
        this.nameId = SeriesNames.getDoubleNameId(connection, name);
    }

    private void setTypeId(Connection connection) throws SQLException {
        this.typeId = TypeNames.getId(connection, dataType);
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
        }

        avg = sum / validCount;
    }

    /**
     * Gets the name of the DoubleTimeSeries.
     * @return the column name of the DoubleTimeSeries
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the minimum value of the DoubleTimeSeries.
     * @return the minimum value of the DoubleTimeSeries
     */
    public double getMin() {
        return min;
    }

    /**
     * Gets the maximum value of the DoubleTimeSeries.
     * @return the maximum value of the DoubleTimeSeries
     */
    public double getMax() {
        return max;
    }

    /**
     * Gets the average value of the DoubleTimeSeries.
     * @return the average value of the DoubleTimeSeries
     */
    public double getAvg() {
        return avg;
    }

    public String toString() {
        return "[DoubleTimeSeries '" + name + "' size: " + this.size + ", validCount: " + validCount + ", min: " + min + ", avg: " + avg + ", max: " + max + "]";
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
            if (d > max) max = d;
            if (d < min) min = d;

            avg =   avg * ((double) validCount / (double) (validCount + 1))
                    + (d / (double) (validCount + 1));

            validCount++;
        }
    }

    public double get(int i) {
        return data[i];
    }

    public String getDataType() {
        return dataType;
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
        if (from == to) to += 1;
        double[] slice = new double[to - from];
        System.arraycopy(this.data, from, slice, 0, slice.length);
        return slice;
    }

    public static PreparedStatement createPreparedStatement(Connection connection) throws SQLException {
        return connection.prepareStatement("INSERT INTO double_series (flight_id, name_id, data_type_id, length, valid_length, min, avg, max, data) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)");
    }

    public void addBatch(Connection connection, PreparedStatement preparedStatement, int flightId) throws SQLException, IOException {
        if (typeId == -1)
            setTypeId(connection);
        if (nameId == -1)
            setNameId(connection);

        preparedStatement.setInt(1, flightId);
        preparedStatement.setInt(2, nameId);
        preparedStatement.setInt(3, typeId);

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

    public void updateDatabase(Connection connection, int flightId) {
        //System.out.println("Updating database for " + this);
        if (this.temporary)
            return;
        try {
            if (typeId == -1)
                setTypeId(connection);
            if (nameId == -1)
                setNameId(connection);

            PreparedStatement preparedStatement = connection.prepareStatement("INSERT INTO double_series (flight_id, name_id, data_type_id, length, valid_length, min, avg, max, data) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)");
            this.addBatch(connection, preparedStatement, flightId);
            preparedStatement.executeBatch();
            preparedStatement.close();
        } catch (SQLException | IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    public static Optional<DoubleTimeSeries> getExistingLaggedSeries(Connection connection, int flightId, String seriesName, int n) {
        String laggedName = seriesName + LAG_SUFFIX + n;

        try {
            DoubleTimeSeries laggedSeries = getDoubleTimeSeries(connection, flightId, laggedName);
            if (laggedSeries != null) return Optional.of(laggedSeries);
        } catch (SQLException se) {
            se.printStackTrace();
        }

        return Optional.empty();
    }

    public static Optional<DoubleTimeSeries> getExistingLeadingSeries(Connection connection, int flightId, String seriesName, int n) {
        String laggedName = seriesName + LEAD_SUFFIX + n;

        try {
            DoubleTimeSeries leadingSeries = getDoubleTimeSeries(connection, flightId, laggedName);
            if (leadingSeries != null) return Optional.of(leadingSeries);
        } catch (SQLException se) {
            se.printStackTrace();
        }

        return Optional.empty();
    }

    /**
     * Lags a timeseries N indicies
     */
    public DoubleTimeSeries lag(Connection connection, int n) throws SQLException {
        Optional<DoubleTimeSeries> existingSeries = getExistingLaggedSeries(Database.getConnection(), this.flightId, this.name, n);

        if (existingSeries.isPresent()) {
            return existingSeries.get();
        } else {
            return lag(n);
        }
    }

    public DoubleTimeSeries lag(int n) {
        DoubleTimeSeries laggedSeries = new DoubleTimeSeries(this.name + LAG_SUFFIX + n, "double");

        for (int i = 0; i < data.length; i++) {
            laggedSeries.add((i >= n) ? data[i - n] : Double.NaN);
        }

        return laggedSeries;   
    }

    public DoubleTimeSeries lead(Connection connection, int n) throws SQLException {
        Optional<DoubleTimeSeries> existingSeries = getExistingLeadingSeries(Database.getConnection(), this.flightId, this.name, n);

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
        DoubleTimeSeries newSeries = new DoubleTimeSeries(connection, name, dataType, until - from);
        newSeries.size = until - from;
        System.arraycopy(data, from, newSeries.data, 0, until - from);
        return newSeries;
    }

    public DoubleTimeSeries subSeries(int from, int until) throws SQLException {
        DoubleTimeSeries newSeries = new DoubleTimeSeries(name, dataType, until - from);
        newSeries.size = until - from;
        System.arraycopy(data, from, newSeries.data, 0, until - from);
        return newSeries;
    }
}

