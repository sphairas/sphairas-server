package org.thespheres.betula.server.beans.clients.json;

import jakarta.json.bind.adapter.JsonbAdapter;
import org.thespheres.betula.assess.Grade;
import org.thespheres.betula.assess.GradeFactory;

public class GradeAdapter implements JsonbAdapter<Grade, String> {

    @Override
    public String adaptToJson(final Grade g) {
        return g.toString();
    }

    @Override
    public Grade adaptFromJson(final String json) {
        return GradeFactory.resolve(json);
    }
}
