package org.ngafid.core.kafka;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.TopicPartition;
import org.jetbrains.annotations.NotNull;
import org.ngafid.core.util.filters.Pair;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;
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
    private static final Logger LOG = Logger.getLogger(DisjointConsumer.class.getName());

    protected final Thread mainThread;
    protected final LinkedBlockingQueue<ConsumerRecords<K, V>> taskQueue = new LinkedBlockingQueue<>();
    protected final LinkedBlockingQueue<RecordsResult<K, V>> resultQueue = new LinkedBlockingQueue<>();
    protected final AtomicBoolean done = new AtomicBoolean(false);
    protected final AtomicBoolean workerProcessing = new AtomicBoolean(false);
    protected final Worker worker;
    protected final Thread workerThread;
    protected boolean paused = false;

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

    private void commit(RecordsResult<K, V> result) {
        // ConsumerRecords are sorted by topic partition, thus in the event of multiple records from the same partition,
        // we will commit 1 past the highest offset from a given partition we see.
        Map<TopicPartition, OffsetAndMetadata> offsets = new HashMap<>(result.records.size());
        for (int i = 0; i < result.records.size(); i++) {
            var record = result.records.get(i);
            var retry = result.retry[i];

            offsets.put(
                    new TopicPartition(record.topic(), record.partition()),
                    new OffsetAndMetadata(record.offset() + 1)
            );

            if (retry) {
                if (record.topic().equals(getTopicName())) {
                    LOG.info("Processing failed... adding to retry queue '" + getRetryTopicName() + "'");
                    producer.send(new ProducerRecord<>(getRetryTopicName(), record.value()));
                } else if (record.topic().equals(getRetryTopicName())) {
                    LOG.info("Processing failed... adding to DLQ '" + getDLTTopicName() + "'");
                    producer.send(new ProducerRecord<>(getDLTTopicName(), record.value()));
                }
            }
        }

        consumer.commitSync(offsets);
    }

    protected void run() {
        try {
            while (!done.get()) {
                LOG.info("Polling topics: [" + String.join(", ", consumer.subscription()) + "]");

                long consumerWaitTimeMS, resultQueueWaitTimeMS;
                if (workerProcessing.get()) {
                    consumerWaitTimeMS = 1000;
                    resultQueueWaitTimeMS = (long) (getMaxPollIntervalMS() * 0.75);
                } else {
                    consumerWaitTimeMS = (long) (getMaxPollIntervalMS() * 0.75);
                    resultQueueWaitTimeMS = 1000;
                }

                var records = consumer.poll(Duration.ofMillis(consumerWaitTimeMS));
                if (!records.isEmpty()) {
                    taskQueue.put(records);
                }

                var result = resultQueue.poll(resultQueueWaitTimeMS, TimeUnit.MILLISECONDS);
                if (result == null)
                    continue;

                commit(result);
            }
        } catch (Exception e) {
            done.set(true);
            workerThread.interrupt();
            LOG.info("Exiting because of " + e.getMessage());
            e.printStackTrace();
        }
    }

    protected void preProcess(ConsumerRecords<K, V> records) {
    }

    protected abstract String getTopicName();

    protected abstract String getRetryTopicName();

    protected abstract String getDLTTopicName();

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

    protected record RecordsResult<K, V>(@NotNull List<ConsumerRecord<K, V>> records, boolean[] retry) {
    }

    protected final class Worker implements Runnable {
        private Worker() {
        }

        @Override
        public void run() {
            while (!done.get()) {
                try {
                    var records = taskQueue.take();
                    workerProcessing.set(true);
                    var recordList = new ArrayList<ConsumerRecord<K, V>>(records.count());

                    boolean[] retry = new boolean[records.count()];
                    int i = 0;

                    preProcess(records);
                    for (var record : records) {
                        var result = process(record);
                        recordList.add(result.first());
                        retry[i++] = result.second();
                    }

                    resultQueue.put(new RecordsResult<K, V>(recordList, retry));
                    workerProcessing.set(false);
                } catch (InterruptedException e) {
                    // Ignore -- read done to see if we should stop.
                }
            }
        }
    }
}
