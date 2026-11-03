package org.thespheres.betula.server.beans.clients;

import java.util.List;

public class TargetDocumentMarkers {

    public String document;
    public List<String> markers;

    public TargetDocumentMarkers() {
    }

    public TargetDocumentMarkers(final String document, final List<String> markers) {
        this.document = document;
        this.markers = markers;
    }
}
