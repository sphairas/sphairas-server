/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.calendar.students;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import org.thespheres.betula.StudentId;
import org.thespheres.betula.calendar.AbstractCardComponent;
import org.thespheres.betula.calendar.EmbeddableComponentProperty;

/**
 *
 * @author boris.heithecker
 */
//@NamedQueries({   //use jpql subquery
//    @NamedQuery(name = "findStudentEntitesForStudents",
//            query = "SELECT se FROM StudentEntity se "
//            + "WHERE se.id=stud.id AND se.authority=stud.authority, IN(:students) AS stud")})
@Entity
@Table(name = "STUDENTENTITY")
@IdClass(StudentId.class)
@Access(AccessType.FIELD)
public class StudentEntity extends AbstractCardComponent implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @Column(name = "STUDENT_ID")
    protected long id;
    @Id
    @Column(name = "STUDENT_AUTHORITY", length = 64)
    protected String authority;
    @Column(name = "BDAY_VALUE")
    private String birthday;
    @Column(name = "BIRTHPLACE_VALUE")
    private String birthplace;
    @Column(name = "STATUS", length = 64)
    protected String status;
    @Embedded
    @ElementCollection
    @CollectionTable(name = "STUDENT_PROPERTIES", joinColumns = {
        @JoinColumn(name = "STUDENT_ID", referencedColumnName = "STUDENT_ID"),
        @JoinColumn(name = "STUDENT_AUTHORITY", referencedColumnName = "STUDENT_AUTHORITY")})
    @OrderColumn(name = "PROPERTY_ORDER")
    protected List<EmbeddableComponentProperty> compProps = new ArrayList<>();

    public StudentEntity() {
    }

    public StudentEntity(StudentId id) {
        this();
        this.id = id.getId();
        this.authority = id.getAuthority();
    }

    public StudentId getStudentId() {
        return new StudentId(authority, id);
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getBirthday() {
        return birthday;
    }

    public void setBirthday(String birthday) {
        this.birthday = birthday;
    }

    public String getBirthplace() {
        return birthplace;
    }

    public void setBirthplace(String birthplace) {
        this.birthplace = birthplace;
    }

    @Override
    public List<EmbeddableComponentProperty> getProperties() {
        return compProps;
    }
}
