package org.ngafid.kafka;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.ngafid.common.Database;
import org.ngafid.uploads.Upload;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * Kafka producer that watches the uploads table, looking for uploads that have completed processing.
 * <p>
 * When an upload with status 'UPLOADED' is detected, a message will be added to the `upload` topic for processing.
 */
public class UploadObserverProducer {
    private static final Logger LOG = Logger.getLogger(UploadObserverProducer.class.getName());

    public static void main(String[] args) throws Exception {
        Properties props = Configuration.getProperties();

        KafkaProducer<String, Integer> producer = new KafkaProducer<>(props);

        // To be run as an auto-restarting daemon:
        while (true) {
            try (Connection connection = Database.getConnection();
                 PreparedStatement uploadsStatement = connection.prepareStatement(
                         "SELECT id, status FROM uploads WHERE status = '" + Upload.Status.UPLOADED.name() + "' ORDER BY id ASC LIMIT 100");
                 ResultSet uploads = uploadsStatement.executeQuery()) {
                while (uploads.next()) {
                    int uploadId = uploads.getInt("id");

                    producer.send(new ProducerRecord<>("upload", uploads.getInt("id")));

                    Upload.updateStatus(connection, uploadId, Upload.Status.ENQUEUED);
                }
            } catch (SQLException e) {
                LOG.severe("Error while producing records: ");
                LOG.severe(e.getMessage());
            }

            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
            }
        }
    }
}
