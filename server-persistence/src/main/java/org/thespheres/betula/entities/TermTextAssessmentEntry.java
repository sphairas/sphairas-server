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
import javax.persistence.Embeddable;
import javax.persistence.Embedded;
import org.thespheres.betula.StudentId;
import org.thespheres.betula.TermId;
import org.thespheres.betula.document.Marker;
import org.thespheres.betula.entities.config.AppProperties;

@Embeddable
@Access(AccessType.FIELD)
public class TermTextAssessmentEntry implements Serializable {

    private static final long serialVersionUID = 1L;
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

    public TermTextAssessmentEntry() {
    }

    public TermTextAssessmentEntry(final StudentId student, final TermId term, final Marker section) {
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

    public String getText() {
        return text;
    }

    public boolean setText(String value, Timestamp timestamp) {
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
