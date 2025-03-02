package org.ngafid.kafka;

import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;

import java.util.List;
import java.util.Properties;

/**
 * Utility class for {@link EventObserver} and {@link EventConsumer}.
 */
public enum Events {
    ;

    protected record EventToCompute(int flightId, int eventId) {
    }

    private static Properties getProperties() {
        Properties props = Configuration.getProperties();
        props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");

        props.put("value.serializer", "org.ngafid.kafka.JsonSerializer");
        props.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        props.put("json.deserializer.type", EventToCompute.class.getName());
        return props;
    }

    static KafkaProducer<String, EventToCompute> createProducer() {
        return new KafkaProducer<>(getProperties());
    }

    static KafkaConsumer<String, String> createConsumer() {
        var consumer = new KafkaConsumer<String, String>(getProperties());
        consumer.subscribe(List.of(Topic.EVENT.toString(), Topic.EVENT_RETRY.toString()));
        return consumer;
    }
}
