package org.ngafid.airsync;

import com.fasterxml.jackson.core.StreamReadFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.kotlin.KotlinModule;

public final class Utility {
    private static final KotlinModule KOTLIN_MODULE = new KotlinModule.Builder().build();
    public static final ObjectMapper OBJECT_MAPPER = JsonMapper.builder()
            .enable(StreamReadFeature.INCLUDE_SOURCE_IN_LOCATION)
            .build()
            .registerModule(new JavaTimeModule())
            .registerModule(KOTLIN_MODULE);

    private Utility() {}
}
