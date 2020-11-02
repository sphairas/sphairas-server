/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.entities;

import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinColumns;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.QueryHint;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import org.thespheres.betula.StudentId;
import org.thespheres.betula.TermId;

/**
 *
 * @author boris.heithecker
 */
@NamedQueries({
    @NamedQuery(name = "TermGradeTargAssessTicketEnt",
            query = "SELECT c FROM TermGradeTargAssessTicketEnt c "
            + "WHERE c.target=:target "
            //            + "AND :studentAuthority is null OR c.studentAuthority=:studentAuthority "
            //            + "AND :studentId is null OR c.studentId=:studentId "
            //            + "AND :termAuthority is null OR c.termAuthority=:termAuthority "
            //            + "AND :termId is null OR c.termId=:termId "
            + "AND :student is null OR c.student=:student "
            + "AND :term is null OR c.term=:term "
            + "AND :signeeType is null OR c.type=:signeeType"),
    @NamedQuery(name = "findNonNullDeleteIntervalTermGradeTargAssessTickets",
            query = "SELECT c FROM TermGradeTargAssessTicketEnt c "
            + "WHERE c.deleteInterval IS NOT NULL",
            hints = {
                @QueryHint(name = "eclipselink.query-results-cache", value = "true"),
                @QueryHint(name = "eclipselink.query-results-cache.size", value = "20")
            })})
@Entity
@Table(name = "TERMGRADE_DOCUMENT_TICKET",
        uniqueConstraints = @UniqueConstraint(columnNames = {"RELATED_BASE_TRGTASS_DOCUMENT_ID",
            "RELATED_BASE_TRGTASS_DOCUMENT_AUTHORITY",
            "RELATED_BASE_TRGTASS_DOCUMENT_VERSION",
            "TERM_ID",
            "TERM_AUTHORITY",
            "STUDENT_ID",
            "STUDENT_AUTHORITY",
            "SIGNGEE_TYPE"}))
public class TermGradeTargAssessTicketEnt extends BaseTicketEntity {//TODO: rename to TermTargetAssessmentTicketEntity

    private static final long serialVersionUID = 1L;
    @JoinColumns({
        @JoinColumn(name = "RELATED_BASE_TRGTASS_DOCUMENT_ID", referencedColumnName = "DOCUMENT_ID"),
        @JoinColumn(name = "RELATED_BASE_TRGTASS_DOCUMENT_AUTHORITY", referencedColumnName = "DOCUMENT_AUTHORITY"),
        @JoinColumn(name = "RELATED_BASE_TRGTASS_DOCUMENT_VERSION", referencedColumnName = "DOCUMENT_VERSION")}
    )
    @ManyToOne(fetch = FetchType.LAZY)
    private BaseTargetAssessmentEntity<?, TermId> target;
    @Embedded
    private EmbeddableTermId term;
    @Embedded
    private EmbeddableStudentId student;
    @Column(name = "SIGNGEE_TYPE", length = 64)
    private String type;
    @Column(name = "DELETE_AFTER_EDIT_INTERVAL", nullable = true)
    private Integer deleteInterval;

    public TermGradeTargAssessTicketEnt() {
    }

    public TermGradeTargAssessTicketEnt(BaseTargetAssessmentEntity<?, TermId> target, TermId term, StudentId student, String signeeType) {
        this.target = target;
        this.term = term != null ? new EmbeddableTermId(term) : null;
        this.student = student != null ? new EmbeddableStudentId(student) : null;
        this.type = signeeType;
    }

    public TermId getTerm() {
        return term != null ? term.getTermId() : null;
    }

    public StudentId getStudent() {
        return student != null ? student.getStudentId() : null;
    }

    public BaseTargetAssessmentEntity<?, TermId> getTarget() {
        return target;
    }

    public String getSigneeType() {
        return type;
    }

    public void setDeleteInterval(Integer deleteInterval) {
        this.deleteInterval = deleteInterval;
    }

    public Integer getDeleteAfterEditInterval() {
        return deleteInterval;
    }

}
