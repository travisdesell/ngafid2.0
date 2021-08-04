package org.ngafid.flights;

import org.ngafid.common.Compression;

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

    private String name;
    private String dataType;
    private ArrayList<String> timeSeries;
    private int validCount;


    public StringTimeSeries(String name, String dataType) {
        this.name = name;
        this.dataType = dataType;
        this.timeSeries = new ArrayList<String>();

        validCount = 0;
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


    // Added to get StringTimeSeries
    public static StringTimeSeries getStringTimeSeries(Connection connection, int flightId, String name) throws SQLException, IOException, ClassNotFoundException {
        PreparedStatement query = connection.prepareStatement("SELECT * FROM string_series WHERE flight_id = ? AND name = ?");
        query.setInt(1, flightId);
        query.setString(2, name);
        //LOG.info(query.toString());

        ResultSet resultSet = query.executeQuery();
        if (resultSet.next()) {
            StringTimeSeries sts = new StringTimeSeries(resultSet);
            //System.out.println( "StringTimeSeries.getStringTimeSeries: " + sts.name + "_" + sts.dataType );
            resultSet.close();
            query.close();
            return sts;
        } else {
            resultSet.close();
            query.close();
            return null;
        }
    }

    // Added to get results for StringTimeSeries
    public StringTimeSeries(ResultSet resultSet) throws SQLException, IOException, ClassNotFoundException {

        name = resultSet.getString(3);
        //System.out.println("name: " + name);
        dataType = resultSet.getString(4);
        //System.out.println("data type: " + dataType);
        int length = resultSet.getInt(5);
        //System.out.println("length: " + length);
        validCount = resultSet.getInt(6);
        //System.out.println("valid count: " + validCount);

        Blob values = resultSet.getBlob(7);
        byte[] bytes = values.getBytes(1, (int)values.length());
        //System.out.println("values.length: " + (int)values.length());
        values.free();
        
        // This unchecked caste warning can be fixed but it shouldnt be necessary if we only but ArrayList<String> objects into the StringTimeSeries cache.
        this.timeSeries = (ArrayList<String>) Compression.inflateObject(bytes);
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

    public int size() {
        return timeSeries.size();
    }

    public int validCount() {
        return validCount;
    }

    public void updateDatabase(Connection connection, int flightId) {
        //System.out.println("Updating database for " + this);

        try {
            PreparedStatement preparedStatement = connection.prepareStatement("INSERT INTO string_series (flight_id, name, data_type, length, valid_length, data) VALUES (?, ?, ?, ?, ?, ?)");

            preparedStatement.setInt(1, flightId);
            preparedStatement.setString(2, name);
            preparedStatement.setString(3, dataType);
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

}

