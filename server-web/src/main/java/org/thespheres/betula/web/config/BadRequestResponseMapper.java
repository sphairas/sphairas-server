package org.thespheres.betula.web.config;

import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import org.eclipse.microprofile.rest.client.ext.ResponseExceptionMapper;
import org.thespheres.betula.server.beans.clients.BadRequest;

/**
 *
 * @author boris.heithecker
 */
@Provider
public class BadRequestResponseMapper implements ResponseExceptionMapper<BadRequest> {

    @Override
    public boolean handles(int status, MultivaluedMap<String, Object> headers) {
        // Only handle 400 Bad Request (or whatever status you chose)
        return status == Response.Status.BAD_REQUEST.getStatusCode();
    }

    @Override
    public BadRequest toThrowable(Response response) {
        final String msg = response.readEntity(String.class);
        return new BadRequest(msg);
    }

}
