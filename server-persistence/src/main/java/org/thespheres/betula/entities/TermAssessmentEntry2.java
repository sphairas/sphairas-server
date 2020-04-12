/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.entities;

import java.io.Serializable;
import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinColumns;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import org.thespheres.betula.StudentId;
import org.thespheres.betula.TermId;
import org.thespheres.betula.assess.Grade;
import org.thespheres.betula.document.DocumentId;

@Entity
@Table(name = "TERMGRADE_TARGETASSESSMENT_DOCUMENT_ENTRIES2",
        uniqueConstraints = {
            @UniqueConstraint(columnNames = {"TERM_AUTHORITY", "TERM_ID", "STUDENT_AUTHORITY", "STUDENT_ID", "TERMGRADE_TARGETASSESSMENT_DOCUMENT_ID", "TERMGRADE_TARGETASSESSMENT_DOCUMENT_AUTHORITY", "TERMGRADE_TARGETASSESSMENT_DOCUMENT_VERSION"})
        },
        indexes = {
            @Index(columnList = "TERM_AUTHORITY, TERM_ID"),
            @Index(columnList = "STUDENT_AUTHORITY, STUDENT_ID")})
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
@Access(AccessType.FIELD)
public class TermAssessmentEntry2 extends BaseAssessmentEntry<TermId> implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    protected long id;
    @JoinColumns({
        @JoinColumn(name = "TERMGRADE_TARGETASSESSMENT_DOCUMENT_ID", referencedColumnName = "DOCUMENT_ID", nullable = false),
        @JoinColumn(name = "TERMGRADE_TARGETASSESSMENT_DOCUMENT_AUTHORITY", referencedColumnName = "DOCUMENT_AUTHORITY", nullable = false),
        @JoinColumn(name = "TERMGRADE_TARGETASSESSMENT_DOCUMENT_VERSION", referencedColumnName = "DOCUMENT_VERSION", nullable = false)})
    @ManyToOne(fetch = FetchType.LAZY)
    private BaseTargetAssessmentEntity<Grade, TermId> document;
    @Embedded
    private EmbeddableTermId term;

    public TermAssessmentEntry2() {
    }

    public TermAssessmentEntry2(final GradeTargetAssessmentEntity<TermId> parent, final StudentId student) {
        super(student);
        this.document = parent;
    }

    public TermAssessmentEntry2(final GradeTargetAssessmentEntity<TermId> parent, final StudentId student, final TermId term) {
        this(parent, student);
        if (term != null) {
            this.term = new EmbeddableTermId(term);
        }
    }

    public DocumentId getDocument() {
        return document.getDocumentId();
    }

    @Override
    public TermId getGradeId() {
        return term != null ? term.getTermId() : null;
    }

    public void setGradeId(TermId term) {
        this.term = term != null ? new EmbeddableTermId(term) : null;
    }

}
