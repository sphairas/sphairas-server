package org.thespheres.betula.server.beans.clients;

import java.util.List;

public class ReportMarkers {

    public String document;
    public List<String> markers;

    public ReportMarkers() {
    }

    public ReportMarkers(final String document, final List<String> markers) {
        this.document = document;
        this.markers = markers;
    }
}
