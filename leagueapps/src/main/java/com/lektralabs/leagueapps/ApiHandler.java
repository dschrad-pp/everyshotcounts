package com.lektralabs.leagueapps;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;

import java.io.IOException;

public abstract class ApiHandler {

    protected ObjectMapper objectMapper;
    protected TypeFactory typeFactory;

    public ApiHandler() {
        this.objectMapper = ObjectMapperFactory.getInstance().getObjectMapper();
        this.typeFactory = objectMapper.getTypeFactory();
    }

    public ObjectMapper getObjectMapper() {
        return objectMapper;
    }

    public TypeFactory getTypeFactory() {
        return typeFactory;
    }

    public abstract String getEndpoint();

    public abstract void handlePayload(int batchCount, String responseBody) throws IOException;

}
