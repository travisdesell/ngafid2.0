package org.ngafid.kafka;

import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;

import java.util.List;
import java.util.Properties;

/**
 * Contains an observer and a consumer. The observer watches the database and looks for flights that have finished
 * processing, but have not had an applicable event computed for them. These events to compute are added to the kafka
 * event topic, and the consumer will compute the applicable events.
 * <p>
 * This covers both custom events and normal events, although the consumer will need to be updated in order to properly
 * compute the new custom events.
 */
public enum Event {
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

    protected static KafkaProducer<String, EventToCompute> createProducer() {
        return new KafkaProducer<>(getProperties());
    }

    protected static KafkaConsumer<String, String> createConsumer() {
        var consumer = new KafkaConsumer<String, String>(getProperties());
        consumer.subscribe(List.of(Topic.EVENT.toString(), Topic.EVENT_RETRY.toString()));
        return consumer;
    }
}
