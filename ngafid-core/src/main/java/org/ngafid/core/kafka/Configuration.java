package org.ngafid.core.kafka;

import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

/**
 * Contains static methods to generate properties used for instantiating Kafka producers / consumers.
 */
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
        props.put("enable.auto.commit", "false");
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


}
