package org.thespheres.betula.server.beans.clients;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.thespheres.betula.server.beans.AmbiguousDateException;

/**
 * Maps business logic AmbiguousDateException to 400 Bad Request with the
 * exception message as plain text response body.
 */
@Provider
public class AmbiguousDateExceptionMapper implements ExceptionMapper<AmbiguousDateException> {

    @Override
    public Response toResponse(final AmbiguousDateException e) {
        return Response.status(Response.Status.BAD_REQUEST)
                .type("text/plain; charset=UTF-8")
                .entity(ExceptionUtils.getMessage(e))
                .build();
    }
}
