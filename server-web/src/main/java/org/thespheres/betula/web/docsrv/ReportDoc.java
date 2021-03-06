/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.web.docsrv;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import org.thespheres.betula.TermId;
import org.thespheres.betula.assess.Grade;
import org.thespheres.betula.document.DocumentId;
import org.thespheres.betula.document.Marker;
import org.thespheres.betula.document.model.Subject;
import org.thespheres.betula.document.util.AbstractReportDocument;

/**
 *
 * @author boris.heithecker
 */
class ReportDoc extends AbstractReportDocument {

    ReportDoc(DocumentId report, TermId term, Map<Subject, Grade> map, Marker[] m, LocalDate reportDate) {
        super(report, term, map, reportDate);
        Arrays.stream(m).forEach(markers::add);
    }

    @Override
    public String getDisplayLabel() {
        return getDocumentId().getId();
    }

    Set<Marker> getMarkerSet() {
        return markers;
    }

}
