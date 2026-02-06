package org.ngafid.core.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.apache.kafka.common.serialization.Serializer;

public class JsonSerializer<T> implements Serializer<T> {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {
        // No special configuration needed
    }

    @Override
    public byte[] serialize(String topic, T data) {
        try {
            return data == null ? null : objectMapper.writeValueAsBytes(data);
        } catch (Exception e) {
            throw new RuntimeException("Error serializing JSON message", e);
        }
    }

    @Override
    public void close() {
        // Nothing to close
    }
}
