package org.thespheres.betula.web.config;

import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.ext.ResponseExceptionMapper;
import org.thespheres.betula.server.beans.AmbiguousDateException;

public class AmbiguousDateResponseMapper implements ResponseExceptionMapper<AmbiguousDateException> {

    @Override
    public boolean handles(final int status, final MultivaluedMap<String, Object> headers) {
        return status == Response.Status.CONFLICT.getStatusCode()
                && "ambiguous-date".equals(headers.getFirst("X-Betula-Error"));
    }

    @Override
    public AmbiguousDateException toThrowable(final Response response) {
        return new AmbiguousDateException(response.readEntity(String.class));
    }
}
