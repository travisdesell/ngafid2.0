package org.ngafid.kafka;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.TopicPartition;
import org.ngafid.uploads.ProcessUpload;
import org.ngafid.uploads.UploadAlreadyLockedException;
import org.ngafid.uploads.UploadDoesNotExistException;

import java.sql.SQLException;
import java.time.Duration;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
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
public class UploadConsumer implements AutoCloseable {
    private static final Logger LOG = Logger.getLogger(UploadConsumer.class.getName());

    private static long MAX_POLL_INTERVAL_MS = 15 * 1000;
    // This should not be modified.
    private static long N_RECORDS = 1;

    public static void main(String[] args) {


        try (UploadConsumer consumer = new UploadConsumer()) {
            consumer.run();
        } catch (Exception e) {
            LOG.severe("Error in fleet consumer:");
            LOG.severe(e.getMessage());
            e.printStackTrace();
        }
    }

    ArrayBlockingQueue<ConsumerRecord<String, Integer>> taskQueue = new ArrayBlockingQueue<>(2);
    ArrayBlockingQueue<RecordResult> resultQueue = new ArrayBlockingQueue<>(2);
    AtomicBoolean done = new AtomicBoolean(false);
    Worker worker = new Worker(Thread.currentThread(), taskQueue, resultQueue, done);
    Thread workerThread = new Thread(worker);

    KafkaConsumer<String, Integer> consumer;
    KafkaProducer<String, Integer> producer;

    private UploadConsumer() {
        Properties props = Configuration.getUploadProperties();
        props.put("max.poll.records", String.valueOf(N_RECORDS));
        props.put("max.poll.interval.ms", String.valueOf(MAX_POLL_INTERVAL_MS));

        workerThread.start();

        consumer = new KafkaConsumer<>(props);
        consumer.subscribe(Set.of("upload", "upload-retry"));

        producer = new KafkaProducer<>(props);
    }

    @Override
    public void close() throws Exception {
        consumer.close();
        producer.close();
    }

    private void run() {

        while (!done.get()) {
            ConsumerRecords<String, Integer> records = consumer.poll(Duration.ofMillis(MAX_POLL_INTERVAL_MS / 2));

            // If we are paused
            if (!consumer.paused().isEmpty()) {
                pausedStep();
            } else if (!records.isEmpty()) {
                step(records);
            }
        }
    }

    private void pausedStep() {
        try {
            var result = resultQueue.poll(MAX_POLL_INTERVAL_MS / 2, TimeUnit.MILLISECONDS);
            if (result == null)
                return;

            if (result.retry) {
                if (result.record.topic().equals("upload")) {
                    LOG.info("Processing failed... adding to retry queue");
                    producer.send(new ProducerRecord<>("upload-retry", result.record.value()));
                } else if (result.record.topic().equals("upload-retry")) {
                    LOG.info("Processing failed... adding to DLQ");
                    producer.send(new ProducerRecord<>("upload-dlq", result.record.value()));
                }
            }

            var offsetMap = Map.of(
                    new TopicPartition(result.record.topic(), result.record.partition()),
                    new OffsetAndMetadata(result.record.offset()));

            consumer.commitSync(offsetMap);
            consumer.resume(consumer.paused());

        } catch (InterruptedException e) {
            // Indicates a SQL exception on the consumer thread -- we should terminate.
            done.set(true);
        }
    }

    private void step(ConsumerRecords<String, Integer> records) {
        // Pull all fleets and subscribe to them
        if (records.count() != 1)
            throw new RuntimeException("Expected only one record but received " + records.count());

        for (ConsumerRecord<String, Integer> record : records) {
            LOG.info("Processing upload " + record.value());
            LOG.info("    offset / " + record.offset());
            LOG.info("    timestamp / " + record.timestamp());

            try {
                taskQueue.put(record);
                consumer.pause(consumer.assignment());
            } catch (InterruptedException e) {
                done.set(true);
                workerThread.interrupt();
                throw new RuntimeException(e);
            }
        }

    }

    record RecordResult(ConsumerRecord<String, Integer> record, boolean retry) {
    }

    record Worker(Thread mainThread, BlockingQueue<ConsumerRecord<String, Integer>> taskQueue,
                  BlockingQueue<RecordResult> resultQueue,
                  AtomicBoolean done) implements Runnable {

        @Override
        public void run() {
            while (!done.get()) {
                try {
                    var record = taskQueue.take();

                    LOG.info("Processing upload " + record.value());
                    LOG.info("    offset / " + record.offset());
                    LOG.info("    timestamp / " + record.timestamp());

                    boolean processedOkay = false;
                    try {
                        processedOkay = ProcessUpload.processUpload(record.value());

                    } catch (UploadDoesNotExistException e) {
                        LOG.info("Received message to process upload with id " + record.value() + " but that upload was not found in the database.");
                        continue;
                    } catch (UploadAlreadyLockedException e) {
                        LOG.info("Upload lock could not be acquired.");
                    } catch (SQLException e) {
                        e.printStackTrace();
                        mainThread.interrupt();
                        throw new RuntimeException(e);
                    }

                    resultQueue.put(new RecordResult(record, !processedOkay));
                } catch (InterruptedException e) {
                    // Ignore
                }
            }
        }
    }
}
