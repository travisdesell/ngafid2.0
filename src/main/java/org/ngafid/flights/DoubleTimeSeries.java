package org.ngafid.flights;

import java.io.ObjectOutputStream;
import java.io.IOException;

import java.sql.Blob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import java.util.ArrayList;

public class DoubleTimeSeries {
    private String name;
    private ArrayList<Double> timeSeries;

    private double min = Double.MAX_VALUE;
    private int validCount;
    private double avg;
    private double max = -Double.MAX_VALUE;

    public DoubleTimeSeries(String name) {
        timeSeries = new ArrayList<Double>();

        min = Double.NaN;
        avg = Double.NaN;
        max = Double.NaN;

        validCount = 0;
    }

    public DoubleTimeSeries(String name, ArrayList<String> stringTimeSeries) {
        this.name = name;

        timeSeries = new ArrayList<Double>();

        int emptyValues = 0;
        avg = 0.0;
        validCount = 0;

        for (int i = 0; i < stringTimeSeries.size(); i++) {
            String currentValue = stringTimeSeries.get(i);
            if (currentValue.length() == 0) {
                //System.err.println("WARNING: double column '" + name + "' value[" + i + "] is empty.");
                timeSeries.add(Double.NaN);
                emptyValues++;
                continue;
            }
            double currentDouble = Double.parseDouble(stringTimeSeries.get(i));

            timeSeries.add(currentDouble);

            if (currentDouble == Double.NaN) continue;
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

        //System.out.println("double column '" + name + "' statistics, " + validCount + " values, min: " + min + ", avg: " + avg + ", max: " + max);
    }


    public void add(double d) {
        if (d == Double.NaN) {
            timeSeries.add(d);
            return;
        }

        if (validCount == 0) {
            min = Double.MAX_VALUE;
            max = -Double.MAX_VALUE;
            avg = 0;

            timeSeries.add(d);
            avg = d;
            max = d;
            min = d;
            validCount = 1;
        } else {
            timeSeries.add(d);

            if (d > max) max = d;
            if (d < min) min = d;

            avg = avg * ((double)validCount / (double)(validCount + 1)) + (d / (double)(validCount + 1));

            validCount++;
        }
    }

    public double get(int i) {
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
            PreparedStatement preparedStatement = connection.prepareStatement("INSERT INTO double_series (flight_id, name, length, valid_length, min, avg, max, values) VALUES (?, ?, ?, ?, ?, ?, ?, ?)");

            preparedStatement.setInt(1, flightId);
            preparedStatement.setString(2, name);

            preparedStatement.setInt(3, timeSeries.size());
            preparedStatement.setInt(4, validCount);

            preparedStatement.setDouble(5, min);
            preparedStatement.setDouble(6, avg);
            preparedStatement.setDouble(7, max);

            Blob seriesBlob = connection.createBlob();
            final ObjectOutputStream oos = new ObjectOutputStream(seriesBlob.setBinaryStream(1));
            for (int i = 0; i < timeSeries.size(); i++) {
                oos.writeDouble(timeSeries.get(i));
            }
            oos.close();

            //preparedStatement.setBlob(8, seriesBlob);

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

