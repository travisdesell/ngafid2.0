package org.ngafid.kafka;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.ngafid.common.filters.Pair;
import org.ngafid.uploads.ProcessUpload;
import org.ngafid.uploads.UploadDoesNotExistException;

import java.sql.SQLException;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * Kafka consumer that reads messages from the `upload` and `upload-retry` topics.
 * <p>
 * The design of this consumer is more sophisticated than the other consumers in order to handle the potentially long
 * processing time of individual messages (i.e. uploads). The consumer needs to communicate with the Kafka broker
 * every MAX_POLL_INTERVAL_MS or else risk being kicked out of the pool. However, if this value is very high it will take
 * a while for a new UploadConsumer to be admitted to the pool.
 * <p>
 * The consumer poll method is what sends a heartbeat message to the broker. We ensure this happens every MAX_POLL_INTERVAL_MS / 2 milliseconds
 * by using a separate processing thread to do the actual processing. While this thread is processing an upload, the main
 * thread will pause all topics / partitions thereby ensuring calls to poll return nothing. Once the processing has finished,
 * the consumer will un-pause the paused topics and partitions and obtain another task etc.
 * <p>
 * The messages are simply upload IDs. Unless the upload has been deleted from the database, the upload will be re-imported.
 * Uploads should be restricted from deletion on the front end if the upload status is PROCESSING.
 */
public class UploadConsumer extends DisjointConsumer<String, Integer> {
    private static final Logger LOG = Logger.getLogger(UploadConsumer.class.getName());

    private static long MAX_POLL_INTERVAL_MS = 10 * 60 * 1000;
    // This should not be modified.
    private static long N_RECORDS = 1;

    public static void main(String[] args) {
        Properties props = Configuration.getUploadProperties();
        props.put("max.poll.records", String.valueOf(N_RECORDS));
        props.put("max.poll.interval.ms", String.valueOf(MAX_POLL_INTERVAL_MS));

        var consumer = new KafkaConsumer<String, Integer>(props);
        var producer = new KafkaProducer<String, Integer>(props);

        try (UploadConsumer uploadConsumer = new UploadConsumer(consumer, producer)) {
            uploadConsumer.run();
        } catch (Exception e) {
            LOG.severe("Error in fleet consumer:");
            LOG.severe(e.getMessage());
            e.printStackTrace();
        }
    }

    protected UploadConsumer(KafkaConsumer<String, Integer> consumer, KafkaProducer<String, Integer> producer) {
        super(Thread.currentThread(), consumer, producer);
    }

    @Override
    protected String getTopicName() {
        return Topic.UPLOAD.toString();
    }

    @Override
    protected String getRetryTopicName() {
        return Topic.UPLOAD_RETRY.toString();
    }

    @Override
    protected String getDLTTopicName() {
        return Topic.UPLOAD_DLQ.toString();
    }

    @Override
    protected long getMaxPollIntervalMS() {
        return MAX_POLL_INTERVAL_MS;
    }

    @Override
    protected Pair<ConsumerRecord<String, Integer>, Boolean> process(ConsumerRecord<String, Integer> record) {
        try {
            LOG.info("Processing upload " + record.value());
            LOG.info("    offset / " + record.offset());
            LOG.info("    timestamp / " + record.timestamp());

            var processedOkay = ProcessUpload.processUpload(record.value());
            return new Pair<>(record, !processedOkay);
        } catch (SQLException e) {
            mainThread.interrupt();
            e.printStackTrace();
            throw new RuntimeException(e);
        } catch (UploadDoesNotExistException e) {
            return new Pair<>(record, false);
        }
    }

}
