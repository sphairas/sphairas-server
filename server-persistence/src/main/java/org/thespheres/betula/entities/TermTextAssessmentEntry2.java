/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.entities;

import java.io.Serializable;
import java.sql.Timestamp;
import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
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
import org.thespheres.betula.document.Marker;
import org.thespheres.betula.entities.config.AppProperties;

@Entity
@Table(name = "TERMTEXT_TARGETASSESSMENT_DOCUMENT_ENTRIES2",
        uniqueConstraints = {
            @UniqueConstraint(columnNames = {
        "STUDENT_AUTHORITY", "STUDENT_ID",
        "TERM_AUTHORITY", "TERM_ID",
        "MARKER_CONVENTION", "MARKER_SUBSET", "MARKER_ID",
        "TERMGRADE_TARGETASSESSMENT_DOCUMENT_ID", "TERMGRADE_TARGETASSESSMENT_DOCUMENT_AUTHORITY", "TERMGRADE_TARGETASSESSMENT_DOCUMENT_VERSION"})
        },
        indexes = {
            @Index(columnList = "TERM_AUTHORITY, TERM_ID"),
            @Index(columnList = "STUDENT_AUTHORITY, STUDENT_ID"),
            @Index(columnList = "MARKER_CONVENTION, MARKER_SUBSET, MARKER_ID")})
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
@Access(AccessType.FIELD)
public class TermTextAssessmentEntry2 implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    protected long id;
    @JoinColumns({
        @JoinColumn(name = "TERMGRADE_TARGETASSESSMENT_DOCUMENT_ID", referencedColumnName = "DOCUMENT_ID", nullable = false),
        @JoinColumn(name = "TERMGRADE_TARGETASSESSMENT_DOCUMENT_AUTHORITY", referencedColumnName = "DOCUMENT_AUTHORITY", nullable = false),
        @JoinColumn(name = "TERMGRADE_TARGETASSESSMENT_DOCUMENT_VERSION", referencedColumnName = "DOCUMENT_VERSION", nullable = false)})
    @ManyToOne(fetch = FetchType.LAZY)
    private TermTextTargetAssessmentEntity document;
    @Column(name = "ASSESSMENTENTRY_TIMESTAMP")
    protected Timestamp timestamp;
    @Embedded
    private EmbeddableTermId term;
    @Embedded
    protected EmbeddableStudentId student;
    @Column(name = "TERMTEXT_ASSESSMENT", length = 8192)
    protected String text;
    @Embedded
    private EmbeddableMarker sectionMarker;

    public TermTextAssessmentEntry2() {
    }

    public TermTextAssessmentEntry2(final TermTextTargetAssessmentEntity parent, final StudentId student, final TermId term, final Marker section) {
        this.document = parent;
        this.student = new EmbeddableStudentId(student);
        this.term = term != null ? new EmbeddableTermId(term) : null;
        this.sectionMarker = section != null ? new EmbeddableMarker(section) : null;
    }

    public StudentId getStudentId() {
        return student.getStudentId();
    }

    public TermId getTermId() {
        return term != null ? term.getTermId() : null;
    }

    public Marker getSection() {
        return sectionMarker != null ? sectionMarker.getMarker() : null;
    }

    public EmbeddableMarker getSectionMarker() {
        return sectionMarker;
    }

    public String getText() {
        return text;
    }

    public boolean setText(final String value, final Timestamp timestamp) {
        final Timestamp ts = timestamp != null ? timestamp : new Timestamp(System.currentTimeMillis());
        final boolean replaceAssessEntriesWithEqualTimestamps = Boolean.getBoolean(AppProperties.REPLACE_IF_EQUAL_TIMESTAMP);
        if (this.timestamp == null || this.timestamp.before(ts) || (this.timestamp.equals(ts) && replaceAssessEntriesWithEqualTimestamps)) {
            this.text = value;
            this.timestamp = ts;
            return true;
        } else {
            return false;
        }
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }

}
