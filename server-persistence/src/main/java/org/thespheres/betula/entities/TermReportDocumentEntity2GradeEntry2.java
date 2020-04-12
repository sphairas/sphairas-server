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
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinColumns;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

//@Entity
//@Table(name = "TERMREPORT2_DOCUMENT_REPORT_GRADE_VALUES2",
//        uniqueConstraints = {
//            @UniqueConstraint(columnNames = {"TERM_AUTHORITY", "TERM_ID", "STUDENT_AUTHORITY", "STUDENT_ID", "TERMGRADE_TARGETASSESSMENT_DOCUMENT_ID", "TERMGRADE_TARGETASSESSMENT_DOCUMENT_AUTHORITY", "TERMGRADE_TARGETASSESSMENT_DOCUMENT_VERSION"})
//        })
//@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
//@Access(AccessType.FIELD)
public class TermReportDocumentEntity2GradeEntry2 implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    protected long id;
    @JoinColumns({
        @JoinColumn(name = "TERMREPORT_DOCUMENT_ID", referencedColumnName = "DOCUMENT_ID", nullable = false),
        @JoinColumn(name = "TERMREPORT_AUTHORITY", referencedColumnName = "DOCUMENT_AUTHORITY", nullable = false),
        @JoinColumn(name = "TERMREPORT_VERSION", referencedColumnName = "DOCUMENT_VERSION", nullable = false)})
    @ManyToOne(fetch = FetchType.LAZY)
    private TermReportDocumentEntity2 document;
    @Column(name = "GRADE_ENTRY_TIMESTAMP")
    protected Timestamp timestamp;
    @Column(name = "GRADE_KEY")
    private String mapKey;
    @Embedded
    private EmbeddableGrade grade;

    public TermReportDocumentEntity2GradeEntry2() {
    }

    public TermReportDocumentEntity2GradeEntry2(String key, EmbeddableGrade grade) {
        this.mapKey = key;
        this.grade = grade;
    }

    public String getKey() {
        return mapKey;
    }

    public EmbeddableGrade getGrade() {
        return grade;
    }

    public void setGrade(EmbeddableGrade grade) {
        this.grade = grade;
    }

}
