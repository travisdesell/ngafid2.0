package org.ngafid.events;

import org.ngafid.common.Compression;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Blob;
import java.sql.SQLException;
import javax.sql.rowset.serial.SerialBlob;
import java.io.IOException;
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

    public RateOfClosure(ResultSet resultSet) throws SQLException, IOException {
        Blob values = resultSet.getBlob(1);
        byte[] bytes = values.getBytes(1, (int) values.length());
        values.free();

        try {
            this.rateOfClosureArray = (double[]) Compression.inflateObject(bytes);
        } catch (ClassNotFoundException e) {
            throw new Error("Failed to decompress object from database - did you change the compression code?");
        }

        this.size = this.rateOfClosureArray.length;
    }

    public void updateDatabase(Connection connection, int eventId) throws IOException, SQLException {
        byte blobBytes[] = Compression.compressObject(this.rateOfClosureArray);
        Blob rateOfClosureBlob = new SerialBlob(blobBytes);

        try (PreparedStatement preparedStatement = connection
                .prepareStatement("INSERT INTO rate_of_closure (event_id, size, data) VALUES (?,?,?)")) {
            preparedStatement.setInt(1, eventId);
            preparedStatement.setInt(2, this.size);
            preparedStatement.setBlob(3, rateOfClosureBlob);

            LOG.info(preparedStatement.toString());
            preparedStatement.executeUpdate();
        }
    }

    public static RateOfClosure getRateOfClosureOfEvent(Connection connection, int eventId)
            throws IOException, SQLException {
        try (PreparedStatement query = connection
                .prepareStatement("select data, size from rate_of_closure where event_id = ?")) {
            query.setInt(1, eventId);

            LOG.info(query.toString());

            try (ResultSet resultSet = query.executeQuery()) {
                if (resultSet.next()) {
                    RateOfClosure rateOfClosure = new RateOfClosure(resultSet);
                    return rateOfClosure;
                }
            }
        }

        return null;
    }
}
