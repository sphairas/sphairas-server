/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.entities;

import java.io.Serializable;
import java.util.Set;
import java.util.Date;
import java.util.HashSet;
import java.util.Objects;
import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.QueryHint;
import javax.persistence.Table;
import org.thespheres.betula.StudentId;
import org.thespheres.betula.TermId;
import org.thespheres.betula.assess.Grade;
import org.thespheres.betula.document.DocumentId;

@NamedQueries({
    //Only AG
    @NamedQuery(name = "TermGradeTargetAssessmentEntity.findAllForSubjectMarkerWithNullSubsetOnly", query = "SELECT DISTINCT t FROM TermGradeTargetAssessmentEntity t, IN(t.entries) e, IN(t.markerSet) m "
            + "WHERE m.convention=:markerConvention AND m.markerId=:markerId AND m.subset=NULL",
            hints = {
                @QueryHint(name = "eclipselink.query-results-cache", value = "true"),
                @QueryHint(name = "eclipselink.query-results-cache.size", value = "100")
            }),
    @NamedQuery(name = "findStudentAGs", query = "SELECT DISTINCT t FROM TermGradeTargetAssessmentEntity t, IN(t.entries) e, IN(t.markerSet) m "
            + "WHERE m.convention='kgs.unterricht' AND m.markerId='ag' AND m.subset=NULL "
            + "AND e.grade.gradeConvention='niedersachsen.teilnahme' AND e.grade.gradeId='tg' "
            + "AND e.term=:term "
            + "AND e.student=:student",
            hints = {
                @QueryHint(name = "eclipselink.query-results-cache", value = "true"),
                @QueryHint(name = "eclipselink.query-results-cache.size", value = "2000")
            }),
    @NamedQuery(name = "findTermGradeTargetAssessmentsForEntitledSignee", query = "SELECT DISTINCT tgtae FROM TermGradeTargetAssessmentEntity tgtae, IN(tgtae.signeeInfoentries) si "
            + "WHERE si.type='entitled.signee' "
            + "AND si.signee=:signee",
            hints = {
                @QueryHint(name = "eclipselink.query-results-cache", value = "true"),
                @QueryHint(name = "eclipselink.query-results-cache.size", value = "100")
            }),
    @NamedQuery(name = "findTermGradeTargetAssessmentsSignees", query = "SELECT DISTINCT tgtae FROM TermGradeTargetAssessmentEntity tgtae, IN(tgtae.signeeInfoentries) si "
            + "WHERE si.type IN :types "
            + "AND si.signee=:signee",
            hints = {
                @QueryHint(name = "eclipselink.query-results-cache", value = "true"),
                @QueryHint(name = "eclipselink.query-results-cache.size", value = "150")
            }),
    //User this query only (!) if subset is null
    @NamedQuery(name = "findTermGradeTargetAssessmentsForSubjectMarkerWithNullSubsetOnly", query = "SELECT DISTINCT t FROM TermGradeTargetAssessmentEntity t, IN(t.entries) e, IN(t.markerSet) m "
            + "WHERE m.convention=:markerConvention AND m.markerId=:markerId AND m.subset=NULL "
            + "AND e.term=:term "
            + "AND e.student=:student",
            hints = {
                @QueryHint(name = "eclipselink.query-results-cache", value = "true"),
                @QueryHint(name = "eclipselink.query-results-cache.size", value = "2000"), //                @QueryHint(name = "eclipselink.read-only", value = "true")
            //                @QueryHint(name = "eclipselink.batch.type", value = "IN"),
            //                @QueryHint(name = "eclipselink.batch", value = "t.entries") eclipselink.read-only
            }),
    @NamedQuery(name = "findTermGradeTargetAssessmentsForUnitEntityStudents", query = "SELECT DISTINCT tgtae FROM TermGradeTargetAssessmentEntity tgtae, UnitDocumentEntity ude, IN(tgtae.entries) e, IN(ude.studentIds) s "
            + "WHERE ude=:unit "
            + "AND e.term=:term "
            + "AND e.student=s",
            //            + "AND e.student.studentAuthority=:s.studentAuthority "
            //            + "AND e.student.studentId=:s.studentId",
            hints = {
                @QueryHint(name = "eclipselink.query-results-cache", value = "true"),
                @QueryHint(name = "eclipselink.query-results-cache.size", value = "100")
            }),
    @NamedQuery(name = "TermGradeTargetAssessmentEntity.findAllTermGradeTargetAssessmentsForUnitEntityStudents", query = "SELECT DISTINCT tgtae FROM TermGradeTargetAssessmentEntity tgtae, UnitDocumentEntity ude, IN(tgtae.entries) e, IN(ude.studentIds) s "
            + "WHERE ude=:unit "
            + "AND e.student=s",
            hints = {
                @QueryHint(name = "eclipselink.query-results-cache", value = "true"),
                @QueryHint(name = "eclipselink.query-results-cache.size", value = "100")
            }),
    @NamedQuery(name = "TermGradeTargetAssessmentEntity.findTermGradeTargetAssessmentsForStudent", query = "SELECT DISTINCT tgtae FROM TermGradeTargetAssessmentEntity tgtae JOIN tgtae.entries e "
            + "WHERE e.student.studentId=:studentId "
            + "AND e.student.studentAuthority=:studentAuthority"),
    //Doesn't work in eclipselink
    @NamedQuery(name = "TermGradeTargetAssessmentEntity.findTermGradeTargetAssessmentsForStudents", query = "SELECT DISTINCT tgtae FROM TermGradeTargetAssessmentEntity tgtae JOIN tgtae.entries e "
            + "WHERE e.student IN :students"),
    //Workaround
    @NamedQuery(name = "TermGradeTargetAssessmentEntity.findTermGradeTargetAssessmentsForStudentsHelper", query = "SELECT DISTINCT tgtae FROM TermGradeTargetAssessmentEntity tgtae, IN(tgtae.entries) e "
            + "WHERE e.student.studentId IN :studentIds "
            + "AND e.student.studentAuthority=:authority"),
        //Workaround2, include changelog
    @NamedQuery(name = "TermGradeTargetAssessmentEntity.findAllTermGradeTargetAssessmentsForUnitEntityStudentsIncludeChangeLogs", query = "SELECT DISTINCT tgtae FROM TermGradeTargetAssessmentEntity tgtae, UnitDocumentEntity ude JOIN TREAT(ude.changeLog AS StudentIdCollectionChangeLog) cl, IN(tgtae.entries) e, IN(ude.studentIds) s "
            + "WHERE ude=:unit "
            + "AND (e.student=s OR cl.student=s)",
            hints = {
                @QueryHint(name = "eclipselink.query-results-cache", value = "true"),
                @QueryHint(name = "eclipselink.query-results-cache.size", value = "100")
            }),
    @NamedQuery(name = "findTargetSigneesForAllUnitDocumentEntityStudents", query = "SELECT DISTINCT signee FROM SigneeEntity signee, TermGradeTargetAssessmentEntity tgtae, UnitDocumentEntity ude, IN(tgtae.entries) e, IN(tgtae.signeeInfoentries) si, IN(ude.studentIds) s "
            + "WHERE signee=si.signee "
            + "AND si.type=:entitlement "
            + "AND ude=:unit "
            + "AND e.student.studentAuthority=s.studentAuthority AND e.student.studentId=s.studentId "
            + "AND e.term=:term"),
    @NamedQuery(name = "findTargetSigneesForSelectedStudents", query = "SELECT DISTINCT signee FROM SigneeEntity signee, TermGradeTargetAssessmentEntity tgtae, IN(tgtae.entries) e, IN(tgtae.signeeInfoentries) si "
            + "WHERE signee=si.signee "
            + "AND si.type=:entitlement "
            + "AND e.student=:student "
            + "AND e.term=:term"),
    @NamedQuery(name = "findAllTargetSigneesForSelectedStudents", query = "SELECT DISTINCT signee FROM SigneeEntity signee, TermGradeTargetAssessmentEntity tgtae, IN(tgtae.entries) e, IN(tgtae.signeeInfoentries) si "
            + "WHERE signee=si.signee "
            + "AND e.student=:student "
            + "AND e.term=:term")
})
//@Cacheable(false)
@Entity
@Table(name = "TERMGRADE_TARGETASSESSMENT_DOCUMENT")
@Access(AccessType.FIELD)
public class TermGradeTargetAssessmentEntity extends GradeTargetAssessmentEntity<TermId> implements Serializable {

    private static final long serialVersionUID = 1L;
//    @ElementCollection
//    @Embedded
//    @CollectionTable(name = "TERMGRADE_TARGETASSESSMENT_DOCUMENT_ENTRIES", //TODO: uniqueConstraints term, student, grade
//            //            foreignKey = @ForeignKey(name = "TRMGRDTRGTSSSSMNTD", value = ConstraintMode.PROVIDER_DEFAULT),
//            uniqueConstraints = {
//                @UniqueConstraint(columnNames = {"TERM_AUTHORITY", "TERM_ID", "STUDENT_AUTHORITY", "STUDENT_ID", "TERMGRADE_TARGETASSESSMENT_DOCUMENT_ID", "TERMGRADE_TARGETASSESSMENT_DOCUMENT_AUTHORITY", "TERMGRADE_TARGETASSESSMENT_DOCUMENT_VERSION"})
//            },
//            indexes = {
//                @Index(columnList = "TERM_AUTHORITY, TERM_ID"),
//                @Index(columnList = "STUDENT_AUTHORITY, STUDENT_ID")},
//            joinColumns = {
//                @JoinColumn(name = "TERMGRADE_TARGETASSESSMENT_DOCUMENT_ID", referencedColumnName = "DOCUMENT_ID", updatable = false, insertable = false),
//                @JoinColumn(name = "TERMGRADE_TARGETASSESSMENT_DOCUMENT_AUTHORITY", referencedColumnName = "DOCUMENT_AUTHORITY", updatable = false, insertable = false),
//                @JoinColumn(name = "TERMGRADE_TARGETASSESSMENT_DOCUMENT_VERSION", referencedColumnName = "DOCUMENT_VERSION", updatable = false, insertable = false)})
//    private final Set<TermAssessmentEntry> entries = new HashSet<>();
    @OneToMany(mappedBy = "document", cascade = {CascadeType.ALL}, orphanRemoval = true)
    private final Set<TermAssessmentEntry2> entries = new HashSet<>();
    @OneToMany(mappedBy = "target", orphanRemoval = true)
    private final Set<TermGradeTargAssessTicketEnt> tickets = new HashSet<>();

    public TermGradeTargetAssessmentEntity() {
    }

    public TermGradeTargetAssessmentEntity(DocumentId id, SigneeEntity creator) {
        super(id, creator);
    }

    public TermGradeTargetAssessmentEntity(DocumentId id, SigneeEntity creator, Date creationTime) {
        super(id, creator, creationTime);
    }

    public String getDocumentClass() {
        return "MultiTargetAssessment.TermId";
    }

    @Override
    public Set<TermAssessmentEntry2> getEntries() {
        return entries;
    }

    public Set<TermGradeTargAssessTicketEnt> getTickets() {
        return tickets;
    }

    @Override
    public TermAssessmentEntry2 addEntry(StudentId student, TermId term, Grade value, java.sql.Timestamp time) {
        final TermAssessmentEntry2 ne = new TermAssessmentEntry2(this, student, term);
        ne.setGrade(value, time);
        entries.add(ne);
        return ne;
    }

    @Override
    public TermAssessmentEntry2 findAssessmentEntry(StudentId student, TermId gradeId) {
        TermAssessmentEntry2 ae = null;
        for (final TermAssessmentEntry2 a : getEntries()) {
            if (a.getStudentId().equals(student) && Objects.equals(a.getGradeId(), gradeId)) {
                ae = a;
                break;
            }
        }
//        if (ae == null && create) {
//            ae = new TermAssessmentEntry2(this, student, gradeId);
//            getEntries().add(ae);
//            Logger.getLogger("ENTRIES").log(Level.INFO, "Added TermAssessmentEntry for {0} {1}", new Object[]{student.toString(), gradeId.toString()});
//        }
        return ae;
    }
}
