/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.entities;

import java.util.HashSet;
import java.util.Set;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.QueryHint;
import javax.persistence.Table;
import org.thespheres.betula.StudentId;
import org.thespheres.betula.TermId;

/**
 *
 * @author boris.heithecker
 */
@NamedQueries({
    @NamedQuery(name = "findStudentsTicket", query = "SELECT t FROM StudentsTicketEntity t, IN(t.students) stud "
            + "WHERE stud.studentId=:studentId AND stud.studentAuthority=:studentAuthority "
            + "AND t.term=:term "
            + "AND t.type=:signeeType",
            hints = {
                @QueryHint(name = "eclipselink.query-results-cache", value = "true"),
                @QueryHint(name = "eclipselink.query-results-cache.size", value = "1000")
            }),
    @NamedQuery(name = "findStudentsTicketNullTermAndType", query = "SELECT t FROM StudentsTicketEntity t, IN(t.students) s "
            + "WHERE s=:student")})
@Entity
@Table(name = "STUDENTS_TICKET")
public class StudentsTicketEntity extends BaseTicketEntity {

    private static final long serialVersionUID = 1L;
    @Embedded
    private EmbeddableTermId term;
    @Column(name = "SIGNGEE_TYPE", length = 64)
    private String type;
    @Embedded
    @ElementCollection
    @CollectionTable(name = "STUDENTS_TICKET_STUDENTS", joinColumns = {
        @JoinColumn(name = "TICKET_ID", referencedColumnName = "ID")})
    private Set<EmbeddableStudentId> students;

    public StudentsTicketEntity() {
    }

    public StudentsTicketEntity(TermId term, String type, StudentId[] studs) {
        this.term = term != null ? new EmbeddableTermId(term) : null;
        this.type = type;
        this.students = new HashSet<>();
        for (StudentId s : studs) {
            this.students.add(new EmbeddableStudentId(s));
        }
    }

    public Set<EmbeddableStudentId> getStudents() {
        if (students == null) {
            students = new HashSet<>();
        }
        return students;
    }

    public String getSigneeType() {
        return type;
    }

    public TermId getTerm() {
        return term != null ? term.getTermId() : null;
    }

}
