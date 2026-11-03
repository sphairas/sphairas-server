package org.thespheres.betula.server.beans.clients.json;

import jakarta.json.bind.adapter.JsonbAdapter;
import org.thespheres.betula.document.DocumentId;

public class DocumentIdAdapter implements JsonbAdapter<DocumentId, String> {

    @Override
    public String adaptToJson(final DocumentId document) {
        return document != null ? document.toString() : null;
    }

    @Override
    public DocumentId adaptFromJson(final String value) {
        return value != null ? DocumentId.valueOf(value) : null;
    }
}
