package org.thespheres.betula.server.beans.clients;

import jakarta.ws.rs.ext.ParamConverter;
import java.time.Instant;
import java.util.Date;

public class DateConverter implements ParamConverter<Date> {

    @Override
    public Date fromString(final String value) {
        return value == null || value.isBlank() ? null : Date.from(Instant.parse(value.trim()));
    }

    @Override
    public String toString(final Date value) {
        return value == null ? null : value.toInstant().toString();
    }
}
