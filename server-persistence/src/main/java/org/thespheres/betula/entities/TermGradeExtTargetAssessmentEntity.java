/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.entities;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.Set;
import java.util.Date;
import java.util.HashSet;
import java.util.Objects;
import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import org.thespheres.betula.StudentId;
import org.thespheres.betula.TermId;
import org.thespheres.betula.assess.Grade;
import org.thespheres.betula.document.DocumentId;
import org.thespheres.betula.document.Marker;

@Entity
@Table(name = "TERMGRADEEXT_TARGETASSESSMENT_DOCUMENT")
@Access(AccessType.FIELD)
public class TermGradeExtTargetAssessmentEntity extends GradeTargetAssessmentEntity<TermId> implements Serializable {

    private static final long serialVersionUID = 1L;
    @OneToMany(mappedBy = "document", cascade = {CascadeType.ALL}, orphanRemoval = true)
    private final Set<TermAssessmentEntry2Ext> entries = new HashSet<>();
    @OneToMany(mappedBy = "target", orphanRemoval = true)
    private final Set<TermGradeTargAssessTicketEnt> tickets = new HashSet<>();

    public TermGradeExtTargetAssessmentEntity() {
    }

    public TermGradeExtTargetAssessmentEntity(final DocumentId id, final SigneeEntity creator) {
        super(id, creator);
    }

    public TermGradeExtTargetAssessmentEntity(final DocumentId id, final SigneeEntity creator, final Date creationTime) {
        super(id, creator, creationTime);
    }

    public String getDocumentClass() {
        return "MultiTargetAssessment.TermIdExt";
    }

    @Override
    public Set<TermAssessmentEntry2Ext> getEntries() {
        return entries;
    }

    public Set<TermGradeTargAssessTicketEnt> getTickets() {
        return tickets;
    }

    public TermAssessmentEntry2Ext addEntry(final StudentId student, final TermId term, final Marker section, final Grade value, final java.sql.Timestamp time) {
        final TermAssessmentEntry2Ext ne = new TermAssessmentEntry2Ext(this, student, term, section);
        ne.setGrade(value, time);
        entries.add(ne);
        return ne;
    }

    @Override
    public TermAssessmentEntry2Ext addEntry(StudentId student, TermId term, Grade value, Timestamp time) {
        return addEntry(student, term, null, value, time);
    }

    public TermAssessmentEntry2Ext findAssessmentEntry(final StudentId student, final TermId gradeId, final Marker section) {
        TermAssessmentEntry2Ext ae = null;
        for (final TermAssessmentEntry2Ext a : getEntries()) {
            if (a.getStudentId().equals(student) && Objects.equals(a.getGradeId(), gradeId)) {
                ae = a;
                break;
            }
        }
        return ae;
    }

    @Override
    public TermAssessmentEntry2Ext findAssessmentEntry(StudentId student, TermId gradeId) {
        return findAssessmentEntry(student, gradeId, null);
    }

}
