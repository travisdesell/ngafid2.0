package org.ngafid.kafka;

import org.apache.kafka.clients.producer.KafkaProducer;

import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

public enum Configuration {
    ;

    public static Properties getProperties() {
        Properties props = new Properties();
        try {
            props.load(new FileReader("src/main/resources/connect-standalone.properties"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        props.put("group.id", "ngafid");
        props.put("enable.auto.commit", "true");
        props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        props.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        return props;
    }

    public static Properties getUploadProperties() {
        Properties props = Configuration.getProperties();
        props.put("value.serializer", "org.apache.kafka.common.serialization.IntegerSerializer");
        props.put("value.deserializer", "org.apache.kafka.common.serialization.IntegerDeserializer");
        return props;
    }

    public static KafkaProducer<String, Integer> getUploadProducer() {
        return new KafkaProducer<>(getUploadProperties());
    }
}
