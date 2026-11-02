package org.thespheres.betula.server.beans.clients.json;

import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.json.bind.JsonbConfig;
import jakarta.json.bind.adapter.JsonbAdapter;
import jakarta.ws.rs.ext.ContextResolver;
import jakarta.ws.rs.ext.Provider;

/**
 * Shared JSON-B configuration for service and REST clients.
 */
@Provider
public class JsonbContextResolver implements ContextResolver<Jsonb> {

    private final Jsonb jsonb = JsonbBuilder.create(new JsonbConfig()
            .withAdapters(new JsonbAdapter[]{new GradeAdapter(), new MarkerAdapter()}));

    @Override
    public Jsonb getContext(final Class<?> type) {
        return jsonb;
    }
}
