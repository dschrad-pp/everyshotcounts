package com.lektralabs.thrones.pallbearer.api.exception;

import com.lektralabs.thrones.pallbearer.api.model.partial.GenericApiResponse;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class InvalidUuidExceptionMapper implements ExceptionMapper<IllegalArgumentException> {

    @Override
    public Response toResponse(IllegalArgumentException e) {
        return Response.status(Response.Status.BAD_REQUEST)
                .type(MediaType.APPLICATION_JSON)
                .entity(new GenericApiResponse<>(400, "Invalid UUID format in request", null))
                .build();
    }
}
