/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.entities;

import java.io.Serializable;
import java.util.Objects;
import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import org.thespheres.betula.assess.GradeFactory;
import org.thespheres.betula.assess.AbstractGrade;
import org.thespheres.betula.assess.Grade;

/**
 *
 * @author boris.heithecker
 */
@Embeddable
@Access(AccessType.FIELD)
public class EmbeddableGrade implements Serializable {

    @Column(name = "GRADE_CONVENTION", length = 64)
    protected String gradeConvention;
    @Column(name = "GRADE_ID", length = 64)
    protected String gradeId;

    protected EmbeddableGrade() {
    }

    public EmbeddableGrade(final Grade original) {
        this.gradeConvention = original.getConvention();
        this.gradeId = original.getId();
    }

    public EmbeddableGrade(final String convention, final String id) {
        this.gradeConvention = convention;
        this.gradeId = id;
    }

    public String getConvention() {
        return gradeConvention;
    }

    public String getId() {
        return gradeId;
    }

    public Grade findGrade() {
        Grade ret = null;
        if (getId() != null && getConvention() != null) {
            ret = GradeFactory.find(getConvention(), getId());
        }
        if (ret == null) {
            ret = new AbstractGrade(getConvention(), getId());
        }
        return ret;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 29 * hash + Objects.hashCode(this.gradeConvention);
        return 29 * hash + Objects.hashCode(this.gradeId);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final EmbeddableGrade other = (EmbeddableGrade) obj;
        if (!Objects.equals(this.gradeConvention, other.gradeConvention)) {
            return false;
        }
        return Objects.equals(this.gradeId, other.gradeId);
    }

    public Object writeReplace() {
        return new Replacer(gradeConvention, gradeId);
    }

    public static class Replacer implements Serializable {

        private final String convention;
        private final String id;

        public Replacer(String convention, String id) {
            this.convention = convention;
            this.id = id;
        }

        public Object readResolve() {
            Grade ret = null;
            if (id != null && convention != null) {
                ret = GradeFactory.find(convention, id);
            }
            if (ret == null) {
                ret = new AbstractGrade(convention, id);
            }
            return ret;
        }
    }
}
