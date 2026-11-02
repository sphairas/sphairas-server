package org.thespheres.betula.web.config;

import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.ext.ResponseExceptionMapper;
import org.thespheres.betula.server.beans.TermReportDataException;

public class TermReportDataResponseMapper implements ResponseExceptionMapper<TermReportDataException> {

    @Override
    public boolean handles(final int status, final MultivaluedMap<String, Object> headers) {
        return status == Response.Status.CONFLICT.getStatusCode()
                && "term-report-data".equals(headers.getFirst("X-Betula-Error"));
    }

    @Override
    public TermReportDataException toThrowable(final Response response) {
        return new TermReportDataException(response.readEntity(String.class));
    }
}
