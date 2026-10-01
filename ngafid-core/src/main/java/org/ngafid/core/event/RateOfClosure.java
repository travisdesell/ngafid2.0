package org.ngafid.core.event;

import java.io.IOException;
import java.sql.*;
import java.util.logging.Logger;
import javax.sql.rowset.serial.SerialBlob;
import org.ngafid.core.util.Compression;

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

    /**
     * Constructs a rate-of-closure series from an in-memory array, recording its length as the series size.
     *
     * @param rateOfClosureArray the per-sample rate-of-closure values
     */
    public RateOfClosure(double[] rateOfClosureArray) {
        this.rateOfClosureArray = rateOfClosureArray;
        this.size = this.rateOfClosureArray.length;
    }

    /**
     * Reconstructs a rate-of-closure series from a result row, decompressing the stored blob (column 1) into an array
     * of the stored length (column 2).
     *
     * @param resultSet the result set positioned on a row with the data blob and size columns
     * @throws SQLException if reading the row fails
     * @throws IOException if decompressing the stored array fails
     */
    public RateOfClosure(ResultSet resultSet) throws SQLException, IOException {
        Blob values = resultSet.getBlob(1);
        int sizeResult = resultSet.getInt(2);
        byte[] bytes = values.getBytes(1, (int) values.length());
        values.free();
        this.rateOfClosureArray = Compression.inflateDoubleArray(bytes, sizeResult);
        this.size = this.rateOfClosureArray.length;
    }

    /**
     * Persists this rate-of-closure series for an event by compressing the array into a blob and inserting a row into
     * {@code rate_of_closure} with the event id, size, and data.
     *
     * @param connection the database connection
     * @param eventId the event this series belongs to
     * @throws IOException if compressing the array fails
     * @throws SQLException if the insert fails
     */
    public void updateDatabase(Connection connection, int eventId) throws IOException, SQLException {
        byte[] blobBytes = Compression.compressDoubleArray(this.rateOfClosureArray);
        Blob rateOfClosureBlob = new SerialBlob(blobBytes);

        try (PreparedStatement preparedStatement =
                connection.prepareStatement("INSERT INTO rate_of_closure (event_id, size, data) VALUES (?,?,?)")) {
            preparedStatement.setInt(1, eventId);
            preparedStatement.setInt(2, this.size);
            preparedStatement.setBlob(3, rateOfClosureBlob);

            LOG.info(preparedStatement.toString());
            preparedStatement.executeUpdate();
        }
    }

    /**
     * Loads the rate-of-closure series stored for an event, decompressing its blob.
     *
     * @param connection the database connection
     * @param eventId the event whose series to load
     * @return the event's rate-of-closure series, or null if none is stored
     * @throws IOException if decompressing the stored array fails
     * @throws SQLException if the query fails
     */
    public static RateOfClosure getRateOfClosureOfEvent(Connection connection, int eventId)
            throws IOException, SQLException {
        try (PreparedStatement query =
                connection.prepareStatement("select data, size from rate_of_closure where event_id = ?")) {
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
