package com.company.auth.api;

import jakarta.ws.rs.core.*;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.util.Map;
import java.util.UUID;

@Provider
public class GenericExceptionMapper implements ExceptionMapper<Throwable> {

    @Override
    public Response toResponse(Throwable ex) {
        String traceId = UUID.randomUUID().toString();

        ApiError body = new ApiError(
                "INTERNAL_SERVER_ERROR",
                "Unexpected error occurred",
                traceId,
                Map.of("exception", ex.getClass().getSimpleName())
        );

        return Response.status(500)
                .type(MediaType.APPLICATION_JSON)
                .entity(body)
                .build();
    }
}
