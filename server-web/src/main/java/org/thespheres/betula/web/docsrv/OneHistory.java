/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.web.docsrv;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.thespheres.betula.StudentId;
import org.thespheres.betula.document.DocumentId;
import org.thespheres.betula.document.model.History;
import org.thespheres.betula.services.vcard.VCardStudent;

/**
 *
 * @author boris.heithecker
 */
class OneHistory implements History<VCardStudent, ReportDoc> {

    final VCardStudent student;
    private final ReportDoc report;

    OneHistory(VCardStudent student, ReportDoc report) {
        this.student = student;
        this.report = report;
    }

    @Override
    public List<VCardStudent> getStudents() {
        return Collections.singletonList(student);
    }

    @Override
    public Set<DocumentId> getReportDocuments(StudentId s) {
        if (s.equals(student.getStudentId())) {
            return Collections.singleton(report.getDocumentId());
        } else {
            return Collections.EMPTY_SET;
        }
    }

    @Override
    public ReportDoc getReportDocument(DocumentId did) {
        if (did.equals(report.getDocumentId())) {
            return report;
        } else {
            return null;
        }
    }

}
