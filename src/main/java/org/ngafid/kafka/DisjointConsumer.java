package org.ngafid.kafka;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.ngafid.common.filters.Pair;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

/**
 * Utility base class for kafka consumer main classes.
 * <p>
 * Kafka consumers that take a long time to process messages may get kicked out of their group. To address this, this
 * class separates the actual processing of messages from the Kafka polling-loop. Then, the program can continue to
 * make contact with the kafka broker and prevent getting kicked out.
 * <p>
 * This continued contact is done by pausing all topics etc. on the consumer and then calling poll -- no records will be
 * obtained but this will inform the broker that the consumer is still a part of the group.
 *
 * @param <K>
 * @param <V>
 */
public abstract class DisjointConsumer<K, V> implements AutoCloseable {
    private static final Logger LOG = Logger.getLogger(UploadConsumer.class.getName());

    protected final Thread mainThread;
    protected final ArrayBlockingQueue<ConsumerRecords<K, V>> taskQueue = new ArrayBlockingQueue<>(1);
    protected final ArrayBlockingQueue<RecordsResult<K, V>> resultQueue = new ArrayBlockingQueue<>(1);
    protected final AtomicBoolean done = new AtomicBoolean(false);
    protected final Worker worker;
    protected final Thread workerThread;

    private final KafkaConsumer<K, V> consumer;
    private final KafkaProducer<K, V> producer;

    protected DisjointConsumer(Thread mainThread, KafkaConsumer<K, V> consumer, KafkaProducer<K, V> producer) {
        this.mainThread = mainThread;
        this.consumer = consumer;
        consumer.subscribe(Set.of(getTopicName(), getRetryTopicName()));
        this.producer = producer;

        this.worker = new Worker();
        this.workerThread = new Thread(worker);
        this.workerThread.start();
    }

    @Override
    public void close() throws Exception {
        consumer.close();
        producer.close();
    }

    protected void run() {
        try {
            while (!done.get()) {
                LOG.info("Polling...");
                if (consumer.paused().isEmpty()) {
                    step(consumer.poll(Duration.ofMillis(getMaxPollIntervalMS())));
                } else {
                    // Very short timeout since we expect to receive no records
                    var _norecords = consumer.poll(Duration.ofMillis(1));
                    pausedStep();
                }
            }
        } catch (Exception e) {
            done.set(true);
            workerThread.interrupt();
            LOG.info("Exiting because of " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void pausedStep() {
        try {
            var result = resultQueue.poll(getMaxPollIntervalMS() / 2, TimeUnit.MILLISECONDS);
            if (result == null)
                return;

            consumer.resume(consumer.paused());

            for (int i = 0; i < result.retry.length; i++) {
                var retry = result.retry[i];
                var record = result.records.get(i);
                if (record != null) {
                    if (retry) {
                        if (record.topic().equals(getTopicName())) {
                            LOG.info("Processing failed... adding to retry queue");
                            producer.send(new ProducerRecord<>(getRetryTopicName(), record.value()));
                        } else if (record.topic().equals(getRetryTopicName())) {
                            LOG.info("Processing failed... adding to DLQ");
                            producer.send(new ProducerRecord<>(getDLTTopicName(), record.value()));
                        }
                    }
                }
            }

            consumer.commitSync();

        } catch (InterruptedException e) {
            // Indicates a SQL exception on the consumer thread -- we should terminate.
            done.set(true);
        }
    }

    protected abstract String getTopicName();

    protected abstract String getRetryTopicName();

    protected abstract String getDLTTopicName();

    protected void step(ConsumerRecords<K, V> records) {
        // Pull all fleets and subscribe to them
        try {
            taskQueue.put(records);
            consumer.pause(consumer.assignment());
        } catch (InterruptedException e) {
            done.set(true);
            workerThread.interrupt();
            throw new RuntimeException(e);
        }
    }

    protected abstract long getMaxPollIntervalMS();

    /**
     * Processes a record. This method should not throw exceptions without interrupting the main thread to ensure a clean
     * shutdown of the JVM.
     *
     * @param record the record to process
     * @return a pair of the record that was processed and a boolean representing whether the processing was successful.
     * The first value should be null if the record should not be added to a retry queue regardless of outcome.
     */
    protected abstract Pair<ConsumerRecord<K, V>, Boolean> process(ConsumerRecord<K, V> record);

    protected record RecordsResult<K, V>(List<ConsumerRecord<K, V>> records, boolean[] retry) {
    }

    protected final class Worker implements Runnable {
        private Worker() {
        }

        @Override
        public void run() {
            while (!done.get()) {
                try {
                    var records = taskQueue.take();
                    var recordList = new ArrayList<ConsumerRecord<K, V>>(records.count());

                    boolean[] retry = new boolean[records.count()];
                    int i = 0;

                    for (var record : records) {
                        LOG.info("Processing upload " + record.value());
                        LOG.info("    offset / " + record.offset());
                        LOG.info("    timestamp / " + record.timestamp());

                        var result = process(record);
                        recordList.add(result.first());
                        retry[i++] = result.second();
                    }

                    resultQueue.put(new RecordsResult<K, V>(recordList, retry));
                } catch (InterruptedException e) {
                    // Ignore -- read done to see if we should stop.
                }
            }
        }
    }
}
