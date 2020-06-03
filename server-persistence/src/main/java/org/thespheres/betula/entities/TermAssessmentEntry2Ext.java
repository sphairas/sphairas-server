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
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import org.thespheres.betula.StudentId;
import org.thespheres.betula.TermId;
import org.thespheres.betula.document.Marker;

@Entity
@Table(name = "TERMGRADEEXT_TARGETASSESSMENT_DOCUMENT_ENTRIES2",
        uniqueConstraints = {
            @UniqueConstraint(name = "INDEX_TRMGRDEXT_TRGTSSMNTDCMNT_NTRS2_TRMAUT_TRMID", columnNames = {
        "STUDENT_AUTHORITY", "STUDENT_ID",
        "TERM_AUTHORITY", "TERM_ID",
        "MARKER_CONVENTION", "MARKER_SUBSET", "MARKER_ID",
        "TERMGRADE_TARGETASSESSMENT_DOCUMENT_ID", "TERMGRADE_TARGETASSESSMENT_DOCUMENT_AUTHORITY", "TERMGRADE_TARGETASSESSMENT_DOCUMENT_VERSION"})
        },
        indexes = {
            @Index(columnList = "TERM_AUTHORITY, TERM_ID", name = "INDEX_TERMGRADEEXT_TARGETASSESSMENT_DOCUMENT_ENTRIES2_TERM"),
            @Index(columnList = "STUDENT_AUTHORITY, STUDENT_ID", name = "INDEX_TERMGRADEEXT_TARGETASSESSMENT_DOCUMENT_ENTRIES2_STUDENT"),
            @Index(columnList = "MARKER_CONVENTION, MARKER_SUBSET, MARKER_ID", name = "INDEX_TERMGRADEEXT_TARGETASSESSMENT_DOCUMENT_ENTRIES2_MARKER")})
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
@Access(AccessType.FIELD)
public class TermAssessmentEntry2Ext extends TermAssessmentEntry2 implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    protected long id;
    @Embedded
    private EmbeddableMarker sectionMarker;

    public TermAssessmentEntry2Ext() {
    }

    public TermAssessmentEntry2Ext(final TermGradeExtTargetAssessmentEntity parent, final StudentId student, final TermId term, final Marker section) {
        super(parent, student);
        this.sectionMarker = section != null ? new EmbeddableMarker(section) : null;
    }

    public Marker getSection() {
        return sectionMarker != null ? sectionMarker.getMarker() : null;
    }

    public EmbeddableMarker getSectionMarker() {
        return sectionMarker;
    }

}
