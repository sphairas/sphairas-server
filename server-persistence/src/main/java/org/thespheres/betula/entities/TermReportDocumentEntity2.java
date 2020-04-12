/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.entities;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.Index;
import javax.persistence.MapKey;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.QueryHint;
import javax.persistence.Table;
import org.thespheres.betula.StudentId;
import org.thespheres.betula.TermId;
import org.thespheres.betula.document.DocumentId;

/**
 *
 * @author boris.heithecker
 */
//@NamedQueries({
//    @NamedQuery(name = "findTermReport2ForUnit", query = "SELECT tre FROM TermReportDocumentEntity2 tre, UnitDocumentEntity ude, IN(ude.studentIds) stud "
//            + "WHERE ude=:unitEntity "
//            + "AND tre.student.studentId=stud.studentId "
//            + "AND tre.student.studentAuthority=stud.studentAuthority "
//            + "AND tre.term=:term",
//            hints = {
//                @QueryHint(name = "eclipselink.query-results-cache", value = "true"),
//                @QueryHint(name = "eclipselink.query-results-cache.size", value = "50")}),
//    @NamedQuery(name = "findTermReport2", query = "SELECT c FROM TermReportDocumentEntity2 c "
//            + "WHERE c.student=:student "
//            + "AND c.term=:term",
//            hints = {
//                @QueryHint(name = "eclipselink.query-results-cache", value = "true"),
//                @QueryHint(name = "eclipselink.query-results-cache.size", value = "2000")})})
//@Entity
//@Table(name = "TERMREPORT_DOCUMENT2",
//        indexes = {
//            @Index(columnList = "TERM_AUTHORITY, TERM_ID"),
//            @Index(columnList = "STUDENT_AUTHORITY, STUDENT_ID")})
public class TermReportDocumentEntity2 extends BaseDocumentEntity implements Serializable {

    private static final long serialVersionUID = 1L;
    @MapKey(name = "mapKey")
    @OneToMany(mappedBy = "document", cascade = {CascadeType.ALL}, orphanRemoval = true)
    private final Map<String, TermReportDocumentEntity2TextEntry2> reportTextValues = new HashMap<>();
    @MapKey(name = "mapKey")
    @OneToMany(mappedBy = "document", cascade = {CascadeType.ALL}, orphanRemoval = true)
    private final Map<String, TermReportDocumentEntity2GradeEntry2> reportGradeValues = new HashMap<>();
    private EmbeddableStudentId student;
    private EmbeddableTermId term;

    public TermReportDocumentEntity2() {
    }

    public TermReportDocumentEntity2(DocumentId id, StudentId student, TermId term, SigneeEntity creator) {
        super(id, creator);
        this.student = new EmbeddableStudentId(student);
        this.term = new EmbeddableTermId(term);
    }

    public TermReportDocumentEntity2(DocumentId id, StudentId student, TermId term, SigneeEntity creator, Date creationTime) {
        super(id, creator, creationTime);
        this.student = new EmbeddableStudentId(student);
        this.term = new EmbeddableTermId(term);
    }

    public TermId getTerm() {
        return term.getTermId();
    }

    public StudentId getStudent() {
        return student.getStudentId();
    }

}
