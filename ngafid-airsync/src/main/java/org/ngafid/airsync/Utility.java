package org.ngafid.airsync;

import com.fasterxml.jackson.core.StreamReadFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.kotlin.KotlinModule;

public class Utility {
    private static KotlinModule kotlinModule = new KotlinModule.Builder().build();
    public static final ObjectMapper OBJECT_MAPPER = JsonMapper.builder()
            .enable(StreamReadFeature.INCLUDE_SOURCE_IN_LOCATION)
            .build()
            .registerModule(new JavaTimeModule())
            .registerModule(kotlinModule);

}
