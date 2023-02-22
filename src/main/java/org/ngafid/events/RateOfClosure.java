package org.ngafid.events;

import org.ngafid.common.Compression;

import javax.sql.rowset.serial.SerialBlob;
import java.io.IOException;
import java.sql.*;
import java.util.logging.Logger;

public class RateOfClosure {

    private static final Logger LOG = Logger.getLogger(RateOfClosure.class.getName());

    private int id;

    private int size;

    private double[] rateOfClosureArray;

    public int getSize() {
        return size;
    }

    public double[] getRateOfClosureArray() {
        return rateOfClosureArray;
    }

    public RateOfClosure(double[] rateOfClosureArray) {
        this.rateOfClosureArray = rateOfClosureArray;
        this.size = this.rateOfClosureArray.length;
    }

    public RateOfClosure(Connection connection, ResultSet resultSet) {

        try {
            Blob values = resultSet.getBlob(1);
            int size = resultSet.getInt(2);
            byte[] bytes = values.getBytes(1, (int)values.length());
            values.free();
            this.rateOfClosureArray = Compression.inflateDoubleArray(bytes, size);
            this.size = this.rateOfClosureArray.length;

        } catch (SQLException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    public void updateDatabase(Connection connection ,int eventId){
        try {
            byte blobBytes[] = Compression.compressDoubleArray(this.rateOfClosureArray);
            Blob rateOfClosureBlob = new SerialBlob(blobBytes);
            PreparedStatement preparedStatement = connection.prepareStatement("INSERT INTO rate_of_closure (event_id, size, data) VALUES (?,?,?)");
            preparedStatement.setInt(1,eventId);
            preparedStatement.setInt(2, this.size);
            preparedStatement.setBlob(3, rateOfClosureBlob);
            LOG.info(preparedStatement.toString());
            preparedStatement.executeUpdate();
            preparedStatement.close();
        } catch (SQLException e) {
            System.err.println("Error commiting rateofclosure for eventid : " + eventId);
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static RateOfClosure getRateOfClosureOfEvent(Connection connection, int eventId){
        try {
            PreparedStatement query = connection.prepareStatement("select data, size from rate_of_closure where event_id = ?");
            LOG.info(query.toString());
            query.setInt(1, eventId);
            ResultSet resultSet = query.executeQuery();
            if(resultSet.next()){
                RateOfClosure rateOfClosure = new RateOfClosure(connection, resultSet);
                return rateOfClosure;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return null;
    }
}
