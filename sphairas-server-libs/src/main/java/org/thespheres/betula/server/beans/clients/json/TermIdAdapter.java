package org.thespheres.betula.server.beans.clients.json;

import jakarta.json.bind.adapter.JsonbAdapter;
import org.thespheres.betula.TermId;

public class TermIdAdapter implements JsonbAdapter<TermId, String> {

    @Override
    public String adaptToJson(final TermId term) {
        return term != null ? term.toString() : null;
    }

    @Override
    public TermId adaptFromJson(final String value) {
        return value != null ? TermId.valueOf(value) : null;
    }
}
