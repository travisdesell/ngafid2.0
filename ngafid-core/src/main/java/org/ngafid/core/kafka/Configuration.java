package org.ngafid.core.kafka;

import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;
import org.ngafid.core.Config;

/**
 * Contains static methods to generate properties used for instantiating Kafka producers / consumers.
 */
public final class Configuration {

    private Configuration() {
        // Utility class; not instantiable.
    }

    /**
     * Builds the base Kafka client properties: loads the broker settings from the configured Kafka config file, then
     * applies NGAFID defaults (string key/value serializers and deserializers, the {@code "ngafid"} group id, and
     * disabled auto-commit so offsets are committed manually).
     *
     * @return the base Kafka producer/consumer properties
     * @throws RuntimeException if the Kafka config file cannot be read
     */
    public static Properties getProperties() {
        Properties props = new Properties();
        try {
            props.load(new FileReader(Config.KAFKA_CONFIG_FILE));
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

    /**
     * Builds Kafka properties for the upload topics, starting from {@link #getProperties()} and overriding the value
     * serializer/deserializer to integers (upload records carry integer upload ids as their value).
     *
     * @return the Kafka properties for upload producers/consumers
     * @throws RuntimeException if the Kafka config file cannot be read
     */
    public static Properties getUploadProperties() {
        Properties props = Configuration.getProperties();
        props.put("value.serializer", "org.apache.kafka.common.serialization.IntegerSerializer");
        props.put("value.deserializer", "org.apache.kafka.common.serialization.IntegerDeserializer");
        return props;
    }
}
