package org.ngafid.kafka;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.ngafid.uploads.ProcessUpload;
import org.ngafid.uploads.UploadAlreadyProcessingException;
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
        props.put("max.poll.records", "1");
        props.put("max.poll.interval.ms", "60000");
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
                ConsumerRecords<String, Integer> records = consumer.poll(10000);
                for (ConsumerRecord<String, Integer> record : records) {
                    LOG.info("Processing upload " + record.value());
                    LOG.info("    offset / " + record.offset());
                    LOG.info("    timestamp / " + record.timestamp());

                    try {
                        boolean processedOkay = ProcessUpload.processUpload(record.value());
                        if (!processedOkay) {
                            if (record.topic().equals("upload")) {
                                LOG.info("Processing failed... adding to retry queue");
                                producer.send(new ProducerRecord<String, Integer>("upload-retry", record.value()));
                            } else if (record.topic().equals("upload-retry")) {
                                LOG.info("Processing failed... adding to DLQ");
                                producer.send(new ProducerRecord<String, Integer>("upload-dlq", record.value()));
                            }
                        }
                    } catch (UploadDoesNotExistException e) {
                        System.out.println("WA");
                        LOG.info("Received message to process upload with id " + record.value() + " but that upload was not found in the database.");
                    } catch (UploadAlreadyProcessingException e) {
                        System.out.println("Oh sweet!!!");
                        LOG.info("Upload lock could not be acquired - skipping processing.");
                    }
                }

                consumer.commitSync();
            }
        }
    }
}
