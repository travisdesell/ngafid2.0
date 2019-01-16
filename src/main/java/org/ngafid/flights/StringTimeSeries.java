package org.ngafid.flights;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import java.io.IOException;

import java.sql.Blob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import java.sql.ResultSet;

import java.util.ArrayList;
import java.util.logging.Logger;

import javax.sql.rowset.serial.SerialBlob;


public class StringTimeSeries {
    private static final Logger LOG = Logger.getLogger(StringTimeSeries.class.getName());

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
    public static StringTimeSeries getStringTimeSeries(Connection connection, int flightId, String name) throws SQLException {
        PreparedStatement query = connection.prepareStatement("SELECT * FROM string_series WHERE flight_id = ? AND name = ?");
        query.setInt(1, flightId);
        query.setString(2, name);
        LOG.info(query.toString());

        ResultSet resultSet = query.executeQuery();
        if (resultSet.next()) {
            StringTimeSeries sts = new StringTimeSeries(resultSet);
            System.out.println( "StringTimeSeries.getStringTimeSeries: " + sts.name + "_" + sts.dataType );
            return sts;
        } else {
            return null;
        }
    }

    // Added to get results for StringTimeSeries
    public StringTimeSeries(ResultSet resultSet) throws SQLException {

        name = resultSet.getString(3);
        System.out.println("name: " + name);
        dataType = resultSet.getString(4);
        System.out.println("data type: " + dataType);
        int length = resultSet.getInt(5);
        System.out.println("length: " + length);
        validCount = resultSet.getInt(6);
        System.out.println("valid count: " + validCount);

        Blob values = resultSet.getBlob(7);
        byte[] bytes = values.getBytes(1, (int)values.length());
        System.out.println("values.length: " + (int)values.length());
        values.free();

        //timeSeries = new ArrayList<String>();
        try {
            ObjectInputStream inputStream = new ObjectInputStream(new ByteArrayInputStream(bytes));
            timeSeries = (ArrayList<String>)inputStream.readObject();
            /*
            while (inputStream.available() > 0) {
                int stringLength = inputStream.readInt();
                System.out.println("stringLength: " + stringLength);

                bytes = new byte[stringLength];
                inputStream.read(bytes, 0, stringLength);
                String str = bytes.toString();

                timeSeries.add(str);
                System.out.println(str);
            }
            */
            System.out.println(timeSeries.toString());
            for (int i = 0; i < timeSeries.size(); i++) {
                System.out.println(timeSeries.get(i));
            }
            inputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return;
    }
    /////////

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

            ByteArrayOutputStream bout = new ByteArrayOutputStream();

            final ObjectOutputStream oos = new ObjectOutputStream(bout);
            oos.writeObject(timeSeries);
            /*
            for (int i = 0; i < timeSeries.size(); i++) {
                if (timeSeries.get(i) == null || timeSeries.get(i).length() == 0) {
                    oos.writeInt(0);
                } else {
                    oos.writeInt(timeSeries.get(i).length());
                    oos.writeChars(timeSeries.get(i));
                }
            }
            */
            oos.close();

            System.err.println(preparedStatement);

            Blob seriesBlob = new SerialBlob(bout.toByteArray());

            preparedStatement.setBlob(6, seriesBlob);
            preparedStatement.executeUpdate();
            preparedStatement.close();

        } catch (SQLException e) {
            e.printStackTrace();
            System.exit(1);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

}

