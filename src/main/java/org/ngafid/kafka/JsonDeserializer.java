package org.ngafid.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.common.serialization.Deserializer;

import java.util.Map;

public class JsonDeserializer<T> implements Deserializer<T> {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private Class<T> targetType;

    @SuppressWarnings("unchecked")
    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {
        // Retrieve the class type from config if provided
        String targetTypeStr = (String) configs.get("json.deserializer.type");
        if (targetTypeStr != null) {
            try {
                targetType = (Class<T>) Class.forName(targetTypeStr);
            } catch (ClassNotFoundException e) {
                throw new RuntimeException("Class not found for JSON deserialization", e);
            }
        }
    }

    @Override
    public T deserialize(String topic, byte[] data) {
        try {
            return data == null ? null : objectMapper.readValue(data, targetType);
        } catch (Exception e) {
            throw new RuntimeException("Error deserializing JSON message", e);
        }
    }

    @Override
    public void close() {
        // Nothing to close
    }
}
