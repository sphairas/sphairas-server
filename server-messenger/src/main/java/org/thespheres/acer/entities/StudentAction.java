/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.acer.entities;

import java.io.Serializable;
import java.util.Objects;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.Embedded;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import org.thespheres.betula.StudentId;

/**
 *
 * @author boris.heithecker
 */
@Embeddable
public class StudentAction implements Serializable {

    public enum Action {

        INCLUDE,
        EXCLUDE,
    }
    private static final long serialVersionUID = 1L;
    @Embedded
    private EmbeddableStudentId student;
    @Enumerated(EnumType.ORDINAL)
    @Column(name = "LIST_ACTION")
    private Action action;

    public StudentAction() {
    }

    public StudentAction(StudentId student, Action action) {
        if (student == null || action == null) {
            throw new IllegalArgumentException();
        }
        this.student = new EmbeddableStudentId(student);
        this.action = action;
    }

    public StudentId getStudentId() {
        return student.getStudentId();
    }

    public Action getAction() {
        return action;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 89 * hash + Objects.hashCode(this.student);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final StudentAction other = (StudentAction) obj;
        return Objects.equals(this.student, other.student);
    }

}
