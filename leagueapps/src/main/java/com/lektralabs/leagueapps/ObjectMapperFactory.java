package com.lektralabs.leagueapps;

import com.fasterxml.jackson.core.StreamReadFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;

public class ObjectMapperFactory {
    private ObjectMapper objectMapper;

    private ObjectMapperFactory() {
        this.objectMapper = JsonMapper.builder()
                .configure(StreamReadFeature.INCLUDE_SOURCE_IN_LOCATION, true)
                .enable(StreamReadFeature.INCLUDE_SOURCE_IN_LOCATION)
                .build();
    }

    private static class SingletonHolder {
        private static final ObjectMapperFactory INSTANCE = new ObjectMapperFactory();
    }

    public static ObjectMapperFactory getInstance() {
        return SingletonHolder.INSTANCE;
    }

    public ObjectMapper getObjectMapper() {
        return objectMapper;
    }
}
