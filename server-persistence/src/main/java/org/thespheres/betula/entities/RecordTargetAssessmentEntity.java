/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.entities;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.CollectionTable;
import javax.persistence.ElementCollection;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.Table;
import org.thespheres.betula.RecordId;
import org.thespheres.betula.StudentId;
import org.thespheres.betula.assess.Grade;
import org.thespheres.betula.document.DocumentId;

/**
 *
 * @author boris.heithecker
 */
@Entity
@Table(name = "RECORD_TARGETASSESSMENT_DOCUMENT")
@Access(AccessType.FIELD)
public class RecordTargetAssessmentEntity extends GradeTargetAssessmentEntity<RecordId> implements Serializable {

    private static final long serialVersionUID = 1L;
    @ElementCollection()
    @Embedded
    @CollectionTable(name = "RECORD_TARGETASSESSMENT_DOCUMENT_ENTRIES",
            joinColumns = {
                @JoinColumn(name = "RECORD_TARGETASSESSMENT_DOCUMENT_ID", referencedColumnName = "DOCUMENT_ID", updatable = false, insertable = false),
                @JoinColumn(name = "RECORD_TARGETASSESSMENT_DOCUMENT_AUTHORITY", referencedColumnName = "DOCUMENT_AUTHORITY", updatable = false, insertable = false),
                @JoinColumn(name = "RECORD_TARGETASSESSMENT_DOCUMENT_VERSION", referencedColumnName = "DOCUMENT_VERSION", updatable = false, insertable = false)})
    private final Set<RecordAssessmentEntry2> entries = new HashSet<>();

    public RecordTargetAssessmentEntity() {
        super();
    }

    public RecordTargetAssessmentEntity(DocumentId id, SigneeEntity creator) {
        super(id, creator);
    }

    public RecordTargetAssessmentEntity(DocumentId id, SigneeEntity creator, Date creationTime) {
        super(id, creator, creationTime);
    }

    @Override
    public Set<RecordAssessmentEntry2> getEntries() {
        return entries;
    }

    public List<RecordAssessmentEntry2> findAssesmentEntries(final StudentId student, final RecordId record) {
        final List<RecordAssessmentEntry2> ret = new ArrayList<>();
        for (final RecordAssessmentEntry2 e : getEntries()) {
            if (e.getStudentId().equals(student) && Objects.equals(e.getGradeId(), record)) {
                ret.add(e);
            }
        }
        return ret;
    }

    @Override
    public RecordAssessmentEntry2 findAssessmentEntry(StudentId student, RecordId gradeId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public RecordAssessmentEntry2 addEntry(final StudentId student, final RecordId record, final Grade value, final java.sql.Timestamp time) {
        final RecordAssessmentEntry2 ne = new RecordAssessmentEntry2(this, student, record);
        ne.setGrade(value, time);
        entries.add(ne);
        return ne;
    }
}
