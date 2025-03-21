package org.ngafid.core.event;

import org.ngafid.core.util.Compression;

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

    public RateOfClosure(ResultSet resultSet) throws SQLException, IOException {
        Blob values = resultSet.getBlob(1);
        int sizeResult = resultSet.getInt(2);
        byte[] bytes = values.getBytes(1, (int) values.length());
        values.free();
        this.rateOfClosureArray = Compression.inflateDoubleArray(bytes, sizeResult);
        this.size = this.rateOfClosureArray.length;
    }

    public void updateDatabase(Connection connection, int eventId) throws IOException, SQLException {
        byte[] blobBytes = Compression.compressDoubleArray(this.rateOfClosureArray);
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
