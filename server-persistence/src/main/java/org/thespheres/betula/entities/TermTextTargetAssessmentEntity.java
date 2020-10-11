/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.entities;

import java.util.Collections;
import java.util.Set;
import java.util.Date;
import java.util.HashSet;
import java.util.Objects;
import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.CascadeType;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.QueryHint;
import javax.persistence.Table;
import org.thespheres.betula.StudentId;
import org.thespheres.betula.TermId;
import org.thespheres.betula.document.DocumentId;
import org.thespheres.betula.document.Marker;

@Entity
@Table(name = "TERMTEXT_TARGETASSESSMENT_DOCUMENT")
@NamedQueries({
    @NamedQuery(name = "findTermTextTargetAssessmentEntityForPrimaryUnit", query = "SELECT DISTINCT te FROM TermTextTargetAssessmentEntity te, UnitDocumentEntity u, IN(te.entries) e, IN(u.studentIds) stud, IN(u.markerSet) m " // 
            + "WHERE m.convention='betula-db' AND m.markerId='primary-unit' AND m.subset=NULL "
            + "AND u.unit=:unit "
            + "AND stud.studentId=e.student.studentId AND stud.studentAuthority=e.student.studentAuthority"),
    @NamedQuery(name = "findTermTextTargetAssessmentsForEntitledSignee", query = "SELECT DISTINCT te FROM TermTextTargetAssessmentEntity te, IN(te.signeeInfoentries) si "
            + "WHERE si.type='entitled.signee' "
            + "AND si.signee=:signee",
            hints = {
                @QueryHint(name = "eclipselink.query-results-cache", value = "true"),
                @QueryHint(name = "eclipselink.query-results-cache.size", value = "100")
            }),
    @NamedQuery(name = "findTermTextTargetAssessmentsSignees", query = "SELECT DISTINCT te FROM TermTextTargetAssessmentEntity te, IN(te.signeeInfoentries) si "
            + "WHERE si.type IN :types "
            + "AND si.signee=:signee",
            hints = {
                @QueryHint(name = "eclipselink.query-results-cache", value = "true"),
                @QueryHint(name = "eclipselink.query-results-cache.size", value = "150")
            })})
@Access(AccessType.FIELD)
public class TermTextTargetAssessmentEntity extends BaseTargetAssessmentEntity<String, TermId> {

    private static final long serialVersionUID = 1L;
//    @ElementCollection
//    @Embedded
//    @CollectionTable(name = "TERMTEXT_TARGETASSESSMENT_DOCUMENT_ENTRIES",
//            uniqueConstraints = {//TODO: MACHT PROBLEME MIT MYSQL
//                @UniqueConstraint(columnNames = {"TERM_AUTHORITY", "TERM_ID",
//            "STUDENT_AUTHORITY", "STUDENT_ID",
//            "MARKER_CONVENTION", "MARKER_SUBSET", "MARKER_ID",
//            "TERMTEXT_TARGETASSESSMENT_DOCUMENT_ID", "TERMTEXT_TARGETASSESSMENT_DOCUMENT_AUTHORITY", "TERMTEXT_TARGETASSESSMENT_DOCUMENT_VERSION"})},
//            indexes = {
//                @Index(columnList = "TERM_AUTHORITY, TERM_ID"),
//                @Index(columnList = "STUDENT_AUTHORITY, STUDENT_ID"),
//                @Index(columnList = "MARKER_CONVENTION, MARKER_SUBSET, MARKER_ID")},
//            joinColumns = {
//                @JoinColumn(name = "TERMTEXT_TARGETASSESSMENT_DOCUMENT_ID", referencedColumnName = "DOCUMENT_ID", updatable = false, insertable = false),
//                @JoinColumn(name = "TERMTEXT_TARGETASSESSMENT_DOCUMENT_AUTHORITY", referencedColumnName = "DOCUMENT_AUTHORITY", updatable = false, insertable = false),
//                @JoinColumn(name = "TERMTEXT_TARGETASSESSMENT_DOCUMENT_VERSION", referencedColumnName = "DOCUMENT_VERSION", updatable = false, insertable = false)})
//    private final Set<TermTextAssessmentEntry> entries = new HashSet<>();
    @OneToMany(mappedBy = "document", cascade = {CascadeType.ALL}, orphanRemoval = true)
    private final Set<TermTextAssessmentEntry2> entries = new HashSet<>();
    @OneToMany(mappedBy = "target", orphanRemoval = true)
    private final Set<TermGradeTargAssessTicketEnt> tickets = new HashSet<>();
    @Embedded
    private TermTextTargetAssessmentEntityLock lock;

    public TermTextTargetAssessmentEntity() {
    }

    public TermTextTargetAssessmentEntity(DocumentId id, SigneeEntity creator) {
        super(id, creator);
    }

    public TermTextTargetAssessmentEntity(DocumentId id, SigneeEntity creator, Date creationTime) {
        super(id, creator, creationTime);
    }

    public String getDocumentClass() {
        return "MultiTargetAssessment.TermId";
    }

    public TermTextTargetAssessmentEntityLock getLock() {
        return lock;
    }

    public void setLock(TermTextTargetAssessmentEntityLock lock) {
        this.lock = lock;
    }

    public Set<TermGradeTargAssessTicketEnt> getTickets() {
        return tickets;
    }

    public Set<TermTextAssessmentEntry2> getEntries() {
        return entries;
    }

    public TermTextAssessmentEntry2 findAssessmentEntry(final StudentId student, final TermId gradeId, final Marker section) {
        TermTextAssessmentEntry2 ae = null;
        for (final TermTextAssessmentEntry2 a : getEntries()) {
            if (a.getStudentId().equals(student)
                    && Objects.equals(a.getTermId(), gradeId)
                    && Objects.equals(a.getSection(), section)) {
                ae = a;
                break;
            }
        }
        return ae;
    }

    public TermTextAssessmentEntry2 addEntry(StudentId student, TermId term, final Marker section, String value, java.sql.Timestamp time) {
        final TermTextAssessmentEntry2 ne = new TermTextAssessmentEntry2(this, student, term, section);
        ne.setText(value, time);
        entries.add(ne);
        return ne;
    }

    public Set<StudentId> students(final TermId restrict) { //später mit listener nur neu, wenn sich etwas geändert hat
        synchronized (getEntries()) {
            final HashSet<StudentId> ret = new HashSet<>();
            for (final TermTextAssessmentEntry2 e : getEntries()) {
                if (restrict == null || e.getTermId().equals(restrict)) {
                    ret.add(e.getStudentId());
                }
            }
            return Collections.unmodifiableSet(ret);
        }
    }
}
