/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.entities;

import java.sql.Timestamp;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import javax.persistence.MappedSuperclass;
import org.thespheres.betula.Identity;
import org.thespheres.betula.StudentId;
import org.thespheres.betula.assess.Grade;
import org.thespheres.betula.document.DocumentId;

/**
 *
 * @author boris.heithecker
 * @param <I>
 */
@MappedSuperclass
public abstract class GradeTargetAssessmentEntity<I extends Identity> extends BaseTargetAssessmentEntity<Grade, I> {

    protected GradeTargetAssessmentEntity() {
    }

    protected GradeTargetAssessmentEntity(final DocumentId id, final SigneeEntity creator) {
        super(id, creator);
    }

    protected GradeTargetAssessmentEntity(final DocumentId id, final SigneeEntity creator, final Date creationTime) {
        super(id, creator, creationTime);
    }

    public Grade select(final StudentId student, final I gradeId) {
        final BaseAssessmentEntry<I> ae = findAssessmentEntry(student, gradeId);
        return ae != null ? ae.getGrade() : null;
    }

    public Timestamp timestamp(final StudentId student, final I gradeId) {
        final BaseAssessmentEntry<I> ae = findAssessmentEntry(student, gradeId);
        return ae != null ? ae.getTimestamp() : null;
    }

    public Set<StudentId> students(final Identity restrict) { //später mit listener nur neu, wenn sich etwas geändert hat
        synchronized (getEntries()) {
            final HashSet<StudentId> ret = new HashSet<>();
            for (final BaseAssessmentEntry e : getEntries()) {
                if (restrict == null || e.getGradeId().equals(restrict)) {
                    ret.add(e.getStudentId());
                }
            }
            return Collections.unmodifiableSet(ret);
        }
    }

    public abstract <E extends BaseAssessmentEntry<I>> E addEntry(final StudentId student, final I term, Grade value, java.sql.Timestamp time);

    public abstract BaseAssessmentEntry<I> findAssessmentEntry(final StudentId student, final I gradeId);

    public abstract Set<? extends BaseAssessmentEntry<I>> getEntries();

}
