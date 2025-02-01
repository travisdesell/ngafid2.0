package org.ngafid.kafka;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.ngafid.uploads.ProcessUpload;
import org.ngafid.uploads.UploadDoesNotExistException;

import java.sql.SQLException;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * Kafka consumer that reads messages from the `upload` and `upload-retry` topics.
 * <p>
 * The messages are simply upload IDs. Unless the upload has been deleted from the database, the upload will be re-imported.
 * Uploads should be restricted from deletion on the front end if the upload status is PROCESSING.
 */
public class UploadConsumer {
    private static final Logger LOG = Logger.getLogger(UploadConsumer.class.getName());

    public static void main(String[] args) {
        Properties props = Configuration.getUploadProperties();

        try {
            fleetUploadConsumerMain(props);
        } catch (Exception e) {
            LOG.severe("Error in fleet consumer:");
            LOG.severe(e.getMessage());
            e.printStackTrace();
        }
    }

    private static void fleetUploadConsumerMain(Properties props) throws SQLException {
        try (KafkaConsumer<String, Integer> consumer = new KafkaConsumer<>(props);
             KafkaProducer<String, Integer> producer = new KafkaProducer<>(props)) {
            consumer.subscribe(List.of("upload", "upload-retry"));

            while (true) {
                // Pull all fleets and subscribe to them
                ConsumerRecords<String, Integer> records = consumer.poll(1000);
                for (ConsumerRecord<String, Integer> record : records) {
                    try {
                        boolean processedOkay = ProcessUpload.processUpload(record.value());
                        if (!processedOkay) {
                            if (record.topic().equals("upload")) {
                                producer.send(new ProducerRecord<String, Integer>("upload-retry", record.value()));
                            } else if (record.topic().equals("upload-retry")) {
                                producer.send(new ProducerRecord<String, Integer>("upload-dlq", record.value()));
                            }
                        }
                    } catch (UploadDoesNotExistException e) {
                        LOG.info("Received message to process upload with id " + record.value() + " but that upload was not found in the database.");
                    }

                }
            }
        }
    }
}
