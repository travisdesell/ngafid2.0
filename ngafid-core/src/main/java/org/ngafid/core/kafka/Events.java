package org.ngafid.core.kafka;

import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;

import java.util.List;
import java.util.Properties;

/**
 * Utility class for {@link EventObserver} and {@link EventConsumer}.
 */
public enum Events {
    ;

    public static final long MAX_POLL_INTERVAL_MS = 10 * 60 * 1000;
    public static final long N_RECORDS = 50;

    public record EventToCompute(int flightId, int eventId) {
    }

    private static Properties getProperties() {
        Properties props = Configuration.getProperties();
        props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");

        props.put("max.poll.records", String.valueOf(N_RECORDS));
        props.put("max.poll.interval.ms", String.valueOf(MAX_POLL_INTERVAL_MS));

        props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        props.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        props.put("json.deserializer.type", EventToCompute.class.getName());
        return props;
    }

    public static KafkaProducer<String, String> createProducer() {
        return new KafkaProducer<>(getProperties());
    }

    public static KafkaConsumer<String, String> createConsumer() {
        var consumer = new KafkaConsumer<String, String>(getProperties());
        consumer.subscribe(List.of(Topic.EVENT.toString(), Topic.EVENT_RETRY.toString()));
        return consumer;
    }
}
