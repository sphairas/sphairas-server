package org.thespheres.betula.server.beans.clients.json;

import jakarta.json.bind.adapter.JsonbAdapter;
import org.thespheres.betula.document.Marker;
import org.thespheres.betula.document.MarkerFactory;

public class MarkerAdapter implements JsonbAdapter<Marker, String> {

    @Override
    public String adaptToJson(final Marker m) {
        return m.toString();
    }

    @Override
    public Marker adaptFromJson(final String json) {
        return MarkerFactory.resolve(json);
    }
}
