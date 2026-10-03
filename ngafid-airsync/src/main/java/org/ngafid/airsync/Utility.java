package org.ngafid.airsync;

import com.fasterxml.jackson.core.StreamReadFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.kotlin.KotlinModule;

/**
 * Shared Jackson {@link ObjectMapper} configuration for deserializing AirSync API JSON responses.
 *
 * <p>Exposes a single pre-configured mapper registered with the Java time and Kotlin modules so that AirSync
 * model classes (Kotlin data classes and {@code java.time} values) deserialize consistently across the module.
 */
public final class Utility {
    private static final KotlinModule KOTLIN_MODULE = new KotlinModule.Builder().build();
    public static final ObjectMapper OBJECT_MAPPER = JsonMapper.builder()
            .enable(StreamReadFeature.INCLUDE_SOURCE_IN_LOCATION)
            .build()
            .registerModule(new JavaTimeModule())
            .registerModule(KOTLIN_MODULE);

    private Utility() {}
}
