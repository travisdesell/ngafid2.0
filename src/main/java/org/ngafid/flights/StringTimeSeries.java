package org.ngafid.flights;

import java.io.ObjectOutputStream;
import java.io.IOException;

import java.sql.Blob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import java.util.ArrayList;

public class StringTimeSeries {
    private String name;
    private ArrayList<String> timeSeries;
    private int validCount;


    public StringTimeSeries(String name) {
        this.name = name;
        this.timeSeries = new ArrayList<String>();
    }

    public StringTimeSeries(String name, ArrayList<String> timeSeries) {
        this.name = name;
        this.timeSeries = timeSeries;

        validCount = 0;
        for (int i = 0; i < timeSeries.size(); i++) {
            if (!timeSeries.get(i).equals("")) {
                validCount++;
            }
        }
    }

    public void add(String s) {
        timeSeries.add(s);
    }

    public String get(int i) {
        return timeSeries.get(i);
    }

    public int size() {
        return timeSeries.size();
    }

    public int validCount() {
        return validCount;
    }

    public void updateDatabase(Connection connection, int flightId) {
        try {
            PreparedStatement preparedStatement = connection.prepareStatement("INSERT INTO string_series (flight_id, name, length, values) VALUES (?, ?, ?, ?)");

            preparedStatement.setInt(1, flightId);
            preparedStatement.setString(2, name);
            preparedStatement.setInt(3, timeSeries.size());

            Blob seriesBlob = connection.createBlob();
            final ObjectOutputStream oos = new ObjectOutputStream(seriesBlob.setBinaryStream(1));
            for (int i = 0; i < timeSeries.size(); i++) {
                oos.writeInt(timeSeries.get(i).length());
                oos.writeChars(timeSeries.get(i));
            }
            oos.close();

            //preparedStatement.setBlob(4, seriesBlob);

            System.err.println(preparedStatement);
        } catch (SQLException e) {
            e.printStackTrace();
            System.exit(1);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

}

