package org.ngafid.flights;

import org.ngafid.common.Compression;
import org.ngafid.flights.SeriesNames;
import org.ngafid.flights.TypeNames;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import java.io.IOException;

import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.sql.Blob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import java.sql.ResultSet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

import javax.sql.rowset.serial.SerialBlob;


public class StringTimeSeries {
    private static final Logger LOG = Logger.getLogger(StringTimeSeries.class.getName());
    private static final int COMPRESSION_LEVEL = Deflater.DEFAULT_COMPRESSION;
    private static final int SIZE_HINT = 256;

    private int nameId = -1;
    private String name;
    private int typeId = -1;
    private String dataType;
    private ArrayList<String> timeSeries;
    private int validCount;


    public StringTimeSeries(String name, String dataType, int sizeHint) {
        this.name = name;
        this.dataType = dataType;
        this.timeSeries = new ArrayList<String>(sizeHint);

        validCount = 0;
    }

    public StringTimeSeries(String name, String dataType) {
        this(name, dataType, SIZE_HINT);
    }

    public StringTimeSeries(Connection connection, String name, String dataType) throws SQLException {
        this(name, dataType, SIZE_HINT);
        setNameId(connection);
        setTypeId(connection);
    }

    public StringTimeSeries(Connection connection, String name, String dataType, ArrayList<String> timeSeries) throws SQLException {
        this(name, dataType, timeSeries);
        setNameId(connection);
        setTypeId(connection);
    }

    public StringTimeSeries(String name, String dataType, ArrayList<String> timeSeries) {
        this.name = name;
        this.dataType = dataType;
        this.timeSeries = timeSeries;
        validCount = 0;
        for (int i = 0; i < timeSeries.size(); i++) {
            if (!timeSeries.get(i).equals("")) {
                validCount++;
            }
        }
    } 

    // Added to get results for StringTimeSeries
    public StringTimeSeries(Connection connection, ResultSet resultSet) throws SQLException, IOException, ClassNotFoundException {

        this.nameId = resultSet.getInt(1);
        this.name = SeriesNames.getStringName(connection, this.nameId);
        //System.out.println("name: " + name);

        this.typeId = resultSet.getInt(2);
        this.dataType = TypeNames.getName(connection, this.typeId);
        //System.out.println("data type: " + dataType);

        int length = resultSet.getInt(3);
        //System.out.println("length: " + length);
        validCount = resultSet.getInt(4);
        //System.out.println("valid count: " + validCount);

        Blob values = resultSet.getBlob(5);
        byte[] bytes = values.getBytes(1, (int)values.length());
        //System.out.println("values.length: " + (int)values.length());
        values.free();
        
        // This unchecked caste warning can be fixed but it shouldnt be necessary if we only but ArrayList<String> objects into the StringTimeSeries cache.
        this.timeSeries = (ArrayList<String>) Compression.inflateObject(bytes);
    }
    
    public static StringTimeSeries getStringTimeSeries(Connection connection, int flightId, String name) throws SQLException {
        PreparedStatement query = connection.prepareStatement("SELECT ss.name_id, ss.data_type_id, ss.length, ss.valid_length, ss.data FROM string_series AS ss INNER JOIN string_series_names AS ssn ON ssn.id = ss.name_id WHERE ssn.name = ? AND ss.flight_id = ?");

        query.setString(1, name);
        query.setInt(2, flightId);

        //LOG.info(query.toString());

        ResultSet resultSet = query.executeQuery();
        if (resultSet.next()) {
            try {
                StringTimeSeries sts = new StringTimeSeries(connection, resultSet);
                //System.out.println( "StringTimeSeries.getStringTimeSeries: " + sts.name + "_" + sts.dataType );
                resultSet.close();
                query.close();
                return sts;
            } catch (IOException | ClassNotFoundException e) {
                LOG.severe("Failed to read string time series from database due to a serialization error");
                e.printStackTrace();
                return null;
            } finally {
                resultSet.close();
                query.close();
            }
        } else {
            resultSet.close();
            query.close();
            return null;
        }
    }
    
    private void setNameId(Connection connection) throws SQLException {
        this.nameId = SeriesNames.getStringNameId(connection, name);
    }

    private void setTypeId(Connection connection) throws SQLException {
        this.typeId = TypeNames.getId(connection, dataType);
    }
    public String toString() {
        return "[StringTimeSeries '" + name + "' size: " + timeSeries.size() + ", validCount: " + validCount + "]";
    }

    public void add(String s) {
        if (!s.equals("")) validCount++;
        timeSeries.add(s);
    }

    public String get(int i) {
        return timeSeries.get(i);
    }

    public String getName() {
        return name;
    }

    public String getDataType() {
        return dataType;
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

    public String[] getLastValidAndIndex() {
        int position = timeSeries.size() - 1;
        while (position >= 0) {
            String current = timeSeries.get(position);
            if (current.equals("")) {
                position--;
            } else {
                return new String[]{current, String.valueOf(position)};
            }
        }
        return null;
    }

    public int size() {
        return timeSeries.size();
    }

    public int validCount() {
        return validCount;
    }

    public void updateDatabase(Connection connection, int flightId) {

        try {
            if (nameId == -1)
                setNameId(connection);
            if (typeId == -1) 
                setTypeId(connection);

            PreparedStatement preparedStatement = connection.prepareStatement("INSERT INTO string_series (flight_id, name_id, data_type_id, length, valid_length, data) VALUES (?, ?, ?, ?, ?, ?)");

            preparedStatement.setInt(1, flightId);
            preparedStatement.setInt(2, nameId);
            preparedStatement.setInt(3, typeId);
            preparedStatement.setInt(4, timeSeries.size());
            preparedStatement.setInt(5, validCount);

            // To get rid of extra bytes at the end of the buffer
            byte[] compressed = Compression.compressObject(this.timeSeries);
            Blob seriesBlob = new SerialBlob(compressed);

            preparedStatement.setBlob(6, seriesBlob);
            preparedStatement.executeUpdate();
            preparedStatement.close();
            seriesBlob.free();

        } catch (SQLException | IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    public StringTimeSeries subSeries(Connection connection, int from, int until) throws SQLException {
        StringTimeSeries newSeries = new StringTimeSeries(connection, name, dataType);

        for (int i = from; i < until; i++)
          newSeries.add(this.timeSeries.get(i));

        return newSeries;
    }

    public StringTimeSeries subSeries(int from, int until) throws SQLException {
        StringTimeSeries newSeries = new StringTimeSeries(name, dataType);

        for (int i = from; i < until; i++)
          newSeries.add(this.timeSeries.get(i));

        return newSeries;
    }
}

