package org.ngafid.events;

import org.ngafid.common.Compression;

import javax.sql.rowset.serial.SerialBlob;
import java.io.IOException;
import java.sql.Blob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.logging.Logger;

public class RateOfClosure {

    private static final Logger LOG = Logger.getLogger(RateOfClosure.class.getName());

    private int id;

    private List<Double> rateOfClosureList;

    public RateOfClosure(List<Double> rateOfClosureList) {
        this.rateOfClosureList = rateOfClosureList;
    }

    public void updateDatabase(Connection connection ,int eventId){
        try {
            byte blobBytes[] = Compression.compressObject(this.rateOfClosureList);
            Blob rateOfClosureBlob = new SerialBlob(blobBytes);
            PreparedStatement preparedStatement = connection.prepareStatement("INSERT INTO rateofclosure (eventId, roc) VALUES (?,?)");
            preparedStatement.setInt(1,eventId);
            preparedStatement.setBlob(2, rateOfClosureBlob);
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
}
