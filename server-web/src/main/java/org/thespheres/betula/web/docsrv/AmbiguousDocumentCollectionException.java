/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.web.docsrv;

import java.util.StringJoiner;
import org.openide.util.NbBundle;
import org.thespheres.betula.StudentId;
import org.thespheres.betula.document.DocumentId;
import org.thespheres.betula.server.beans.AmbiguousResultException;

/**
 *
 * @author boris.heithecker
 */
public class AmbiguousDocumentCollectionException extends AmbiguousResultException {

    private DocumentId[] documents = new DocumentId[]{};
    private String studentName;
    private final StudentId student;

    AmbiguousDocumentCollectionException(StudentId student) {
        this.student = student;
    }

    StudentId getStudent() {
        return student;
    }

    void setStudentResolvedName(String sName) {
        this.studentName = sName;
    }

    void setDocumentCollection(DocumentId[] docs) {
        this.documents = docs;
    }

    public DocumentId[] getDocuments() {
        return documents;
    }

    @Override
    public String getMessage() {
        final String name = studentName != null ? studentName : Long.toString(getStudent().getId());
        final StringJoiner sj = new StringJoiner(" ");
        for (DocumentId d : documents) {
            sj.add(d.getId());
        }
        return NbBundle.getMessage(AmbiguousDocumentCollectionException.class, "AmbiguousDocumentCollectionException.message", name, sj.toString());
    }

}
