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
import javax.persistence.MappedSuperclass;
import org.thespheres.betula.Identity;
import org.thespheres.betula.StudentId;
import org.thespheres.betula.assess.Grade;
import org.thespheres.betula.entities.config.AppProperties;

/**
 *
 * @author boris.heithecker
 * @param <I>
 */
@MappedSuperclass
@Access(AccessType.FIELD)
public abstract class BaseAssessmentEntry<I extends Identity> implements Serializable {

    @Embedded
    protected EmbeddableStudentId student;
    @Embedded
    protected EmbeddableGrade grade;
    @Column(name = "ASSESSMENTENTRY_TIMESTAMP")
    protected Timestamp timestamp;

    public BaseAssessmentEntry() {
    }

    public BaseAssessmentEntry(StudentId student) {
        this.student = new EmbeddableStudentId(student);
    }

    public StudentId getStudentId() {
        return student.getStudentId();
    }

    public Grade getGrade() {
        return grade != null ? grade.findGrade() : null;
    }

    public EmbeddableGrade getEmbeddableGrade() {
        return grade;
    }

    public boolean setGrade(Grade grade, java.sql.Timestamp timestamp) { //Date timestamp) {
        final Timestamp ts = timestamp != null ? timestamp : new Timestamp(System.currentTimeMillis());
        final boolean replaceAssessEntriesWithEqualTimestamps = Boolean.getBoolean(AppProperties.REPLACE_IF_EQUAL_TIMESTAMP);
        if (this.timestamp == null || this.timestamp.before(ts) || (this.timestamp.equals(ts) && replaceAssessEntriesWithEqualTimestamps)) {
            this.grade = grade instanceof EmbeddableGrade ? (EmbeddableGrade) grade : new EmbeddableGrade(grade);
            this.timestamp = ts;
            return true;
        } else {
            return false;
        }
    }

    public java.sql.Timestamp getTimestamp() {
        return timestamp;
    }

    public abstract I getGradeId();

}
