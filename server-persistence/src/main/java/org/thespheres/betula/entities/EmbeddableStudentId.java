/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.entities;

import java.io.Serializable;
import java.util.Objects;
import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import org.thespheres.betula.StudentId;

/**
 *
 * @author boris.heithecker
 */
@Embeddable
@Access(AccessType.FIELD)
public class EmbeddableStudentId implements Serializable {

    @Column(name = "STUDENT_AUTHORITY", length = 64)
    private String studentAuthority;
    @Column(name = "STUDENT_ID")
    private Long studentId;

    public EmbeddableStudentId() {
    }

    public EmbeddableStudentId(StudentId id) {
        this.studentAuthority = id.getAuthority();
        this.studentId = id.getId();
    }

    public StudentId getStudentId() {
        return new StudentId(studentAuthority, studentId);
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 47 * hash + Objects.hashCode(this.studentAuthority);
        return 47 * hash + Objects.hashCode(this.studentId);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final EmbeddableStudentId other = (EmbeddableStudentId) obj;
        if (!Objects.equals(this.studentAuthority, other.studentAuthority)) {
            return false;
        }
        return Objects.equals(this.studentId, other.studentId);
    }

}
