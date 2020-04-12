/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.entities;

import java.io.Serializable;
import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import org.thespheres.betula.StudentId;

/**
 *
 * @author boris.heithecker
 */
@Entity
@Table(name = "STUDENTID_COLLECTION_CHANGELOG")
@Access(AccessType.FIELD)
public class StudentIdCollectionChangeLog extends BaseChangeLog<StudentId> implements Serializable {

    @AttributeOverrides({
        @AttributeOverride(name = "studentAuthority", column = @Column(name = "LOG_STUDENT_AUTHORITY")),
        @AttributeOverride(name = "studentId", column = @Column(name = "LOG_STUDENT_ID"))
    })
    private EmbeddableStudentId student;

    public StudentIdCollectionChangeLog() {
    }

    public StudentIdCollectionChangeLog(BaseDocumentEntity parent, String property, StudentId student, Action action) {
        super(parent, property, action);
        this.student = student != null ? new EmbeddableStudentId(student) : null;
    }

    @Override
    public StudentId getValue() {
        return student != null ? student.getStudentId() : null;
    }
}
