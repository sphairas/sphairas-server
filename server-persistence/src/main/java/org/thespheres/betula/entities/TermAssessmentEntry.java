/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.entities;

import java.io.Serializable;
import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Embeddable;
import javax.persistence.Embedded;
import org.thespheres.betula.StudentId;
import org.thespheres.betula.TermId;

@Deprecated
@Embeddable
@Access(AccessType.FIELD)
public class TermAssessmentEntry extends BaseAssessmentEntry<TermId> implements Serializable {

    private static final long serialVersionUID = 1L;
    @Embedded
    private EmbeddableTermId term;

    public TermAssessmentEntry() {
    }

    public TermAssessmentEntry(StudentId student) {
        super(student);
    }

    public TermAssessmentEntry(StudentId student, TermId term) {
        this(student);
        if (term != null) {
            this.term = new EmbeddableTermId(term);
        }
    }

    @Override
    public TermId getGradeId() {
        return term != null ? term.getTermId() : null;
    }

    public void setGradeId(TermId term) {
        this.term = term != null ? new EmbeddableTermId(term) : null;
    }

}
