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
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.QueryHint;
import jakarta.persistence.Table;
import org.thespheres.betula.StudentId;
import org.thespheres.betula.TermId;
import org.thespheres.betula.document.DocumentId;

/**
 *
 * @author boris.heithecker
 */
@NamedQueries({
    @NamedQuery(name = "findTermReportForUnit", query = "SELECT tre FROM TermReportDocumentEntity tre, UnitDocumentEntity ude, IN(ude.studentIds) stud "
            + "WHERE ude=:unitEntity "
            + "AND tre.student.studentId=stud.studentId "
            + "AND tre.student.studentAuthority=stud.studentAuthority "
            //            + "AND (:term is null OR tre.term=:term)",
            + "AND tre.term=:term",
            hints = {
                @QueryHint(name = "eclipselink.query-results-cache", value = "true"),
                @QueryHint(name = "eclipselink.query-results-cache.size", value = "50")}),
    @NamedQuery(name = "findTermReport", query = "SELECT c FROM TermReportDocumentEntity c "
            + "WHERE c.student=:student "
            + "AND c.term=:term",
            hints = {
                @QueryHint(name = "eclipselink.query-results-cache", value = "true"),
                @QueryHint(name = "eclipselink.query-results-cache.size", value = "2000")})})
@Entity
@Table(name = "TERMREPORT_DOCUMENT",
        indexes = {
            @Index(columnList = "TERM_AUTHORITY, TERM_ID"),
            @Index(columnList = "STUDENT_AUTHORITY, STUDENT_ID")})
public class TermReportDocumentEntity extends BaseDocumentEntity implements Serializable {

    private static final long serialVersionUID = 1L;
    @Column(name = "FEHLTAGE")
    private Integer fehltage;
    @Column(name = "UNENTSCHULDIGT")
    private Integer unentschuldigt;
    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "gradeConvention", column = @Column(name = "AV_GRADE_CONVENTION", length = 64)),
        @AttributeOverride(name = "gradeId", column = @Column(name = "AV_GRADE_ID", length = 64))
    })
    private EmbeddableGrade avnote;
    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "gradeConvention", column = @Column(name = "SV_GRADE_CONVENTION", length = 64)),
        @AttributeOverride(name = "gradeId", column = @Column(name = "SV_GRADE_ID", length = 64))
    })
    private EmbeddableGrade svnote;
    @ElementCollection
    @MapKeyColumn(name = "POSITION")
    @Column(name = "NOTE_TEXT")
    @CollectionTable(name = "TERMREPORT_DOCUMENT_FREENOTES", joinColumns = {
        @JoinColumn(name = "TERMREPORT_DOCUMENT_ID", referencedColumnName = "DOCUMENT_ID", updatable = false, insertable = false),
        @JoinColumn(name = "TERMREPORT_DOCUMENT_AUTHORITY", referencedColumnName = "DOCUMENT_AUTHORITY", updatable = false, insertable = false),
        @JoinColumn(name = "TERMREPORT_DOCUMENT_VERSION", referencedColumnName = "DOCUMENT_VERSION", updatable = false, insertable = false)})
    private Map<Integer, String> freeNotes = new HashMap<>();
    @ElementCollection
    @MapKeyColumn(name = "NOTE_KEY")
    @Column(name = "NOTE_VALUE", length = 1500)
    @CollectionTable(name = "TERMREPORT_DOCUMENT_NOTES", joinColumns = {
        @JoinColumn(name = "TERMREPORT_DOCUMENT_ID", referencedColumnName = "DOCUMENT_ID", updatable = false, insertable = false),
        @JoinColumn(name = "TERMREPORT_DOCUMENT_AUTHORITY", referencedColumnName = "DOCUMENT_AUTHORITY", updatable = false, insertable = false),
        @JoinColumn(name = "TERMREPORT_DOCUMENT_VERSION", referencedColumnName = "DOCUMENT_VERSION", updatable = false, insertable = false)})
    private Map<String, String> notes = new HashMap<>();
    private EmbeddableStudentId student;
    private EmbeddableTermId term;

    public TermReportDocumentEntity() {
    }

    public TermReportDocumentEntity(DocumentId id, StudentId student, TermId term, SigneeEntity creator) {
        super(id, creator);
        this.student = new EmbeddableStudentId(student);
        this.term = new EmbeddableTermId(term);
    }

    public TermReportDocumentEntity(DocumentId id, StudentId student, TermId term, SigneeEntity creator, Date creationTime) {
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

    public Integer getFehltage() {
        return fehltage;
    }

    public void setFehltage(Integer fehltage) {
        this.fehltage = fehltage;
    }

    public Integer getUnentschuldigt() {
        return unentschuldigt;
    }

    public void setUnentschuldigt(Integer unentschuldigt) {
        this.unentschuldigt = unentschuldigt;
    }

    public EmbeddableGrade getAvnote() {
        return avnote;
    }

    public void setAvnote(EmbeddableGrade avnote) {
        this.avnote = avnote;
    }

    public EmbeddableGrade getSvnote() {
        return svnote;
    }

    public void setSvnote(EmbeddableGrade svnote) {
        this.svnote = svnote;
    }

    public Map<Integer, String> getFreeNotes() {
        return freeNotes;
    }

    public Map<String, String> getNotes() {
        return notes;
    }

}
