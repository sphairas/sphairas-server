/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.entities;

import java.io.Serializable;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToMany;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.QueryHint;
import javax.persistence.Table;
import org.thespheres.betula.StudentId;
import org.thespheres.betula.UnitId;
import org.thespheres.betula.document.DocumentId;
import org.thespheres.betula.document.DocumentId.Version;

/**
 *
 * @author boris.heithecker
 */
@Entity
@Table(name = "UNIT_DOCUMENT")
@NamedQueries({
    @NamedQuery(name = "UnitDocumentEntity.findAll", query = "SELECT u FROM UnitDocumentEntity u",
            hints = {
                @QueryHint(name = "eclipselink.query-results-cache", value = "true"),
                @QueryHint(name = "eclipselink.query-results-cache.size", value = "1")
            }),
    @NamedQuery(name = "findPrimaryUnitForStudent", query = "SELECT DISTINCT u FROM UnitDocumentEntity u, IN(u.studentIds) stud, IN(u.markerSet) m " // 
            + "WHERE m.convention='betula-db' AND m.markerId='primary-unit' AND m.subset=NULL "
            + "AND stud.studentId=:studentId AND stud.studentAuthority=:studentAuthority", hints = {
                @QueryHint(name = "eclipselink.query-results-cache", value = "true"),
                @QueryHint(name = "eclipselink.query-results-cache.size", value = "2000")
            }),
    @NamedQuery(name = "findPrimaryUnitChangeLogForStudent", query = "SELECT DISTINCT sccl FROM UnitDocumentEntity u, StudentIdCollectionChangeLog sccl, IN(u.markerSet) m " // 
            + "WHERE m.convention='betula-db' AND m.markerId='primary-unit' AND m.subset=NULL "
            + "AND sccl.baseDocumentEntity=u AND sccl.student.studentId=:studentId AND sccl.student.studentAuthority=:studentAuthority", hints = {
                @QueryHint(name = "eclipselink.query-results-cache", value = "true"),
                @QueryHint(name = "eclipselink.query-results-cache.size", value = "10")
            }),
        //Workaround
    @NamedQuery(name = "findUnitsForStudents", query = "SELECT DISTINCT ude FROM UnitDocumentEntity ude, IN(ude.studentIds) e "
            + "WHERE e.studentId IN :studentIds "
            + "AND e.studentAuthority=:authority"),
    @NamedQuery(name = "findPrimaryUnitForTermTarget", query = "SELECT DISTINCT tgtae FROM TermGradeTargetAssessmentEntity tgtae, UnitDocumentEntity ude, IN(tgtae.entries) e, IN(ude.studentIds) s, IN(ude.markerSet) m  "
            + "WHERE e.term=:term "
            + "AND ude.unit=:unit "
            + "AND m.convention='betula-db' AND m.markerId='primary-unit' AND m.subset=NULL "
            + "AND e.student=s",
            hints = {
                @QueryHint(name = "eclipselink.query-results-cache", value = "true"),
                @QueryHint(name = "eclipselink.query-results-cache.size", value = "100")
            })
})
@Access(AccessType.FIELD)
public class UnitDocumentEntity extends BaseDocumentEntity implements Serializable {

    private static final long serialVersionUID = 1L;
    @Embedded
    @ElementCollection
    @CollectionTable(name = "UNIT_DOCUMENT_STUDENTS",
            //            indexes = {
            //                @Index(columnList = "STUDENT_AUTHORITY, STUDENT_ID")},
            joinColumns = {
                @JoinColumn(name = "UNIT_DOCUMENT_ID", referencedColumnName = "DOCUMENT_ID", updatable = false, insertable = false),
                @JoinColumn(name = "UNIT_DOCUMENT_ID_AUTHORITY", referencedColumnName = "DOCUMENT_AUTHORITY", updatable = false, insertable = false),
                @JoinColumn(name = "UNIT_DOCUMENT_ID_VERSION", referencedColumnName = "DOCUMENT_VERSION", updatable = false, insertable = false)})
    Set<EmbeddableStudentId> studentIds = new HashSet<>();
    private transient StudentIdWrapperSet wrapperSet;
    @ManyToMany(mappedBy = "unitDocs")//TODO CascadeType
    private Set<BaseTargetAssessmentEntity> targets;
    @Embedded
    private EmbeddableUnitId unit;
    @Column(name = "PREFERRED_TERMSCHEDULE_PROVIDER", length = 64)
    private String termScheduleProvider;

    public UnitDocumentEntity() {
        super();
        this.termScheduleProvider = "mk.niedersachsen.de";
    }

    public UnitDocumentEntity(UnitId unit, String suffix, SigneeEntity creator) {
        super(new DocumentId(unit.getAuthority(), unit.getId() + "-" + suffix, Version.LATEST), creator);
        this.unit = new EmbeddableUnitId(unit);
    }

    public UnitDocumentEntity(UnitId unit, String suffix, SigneeEntity creator, Date creationTime) {
        super(new DocumentId(unit.getAuthority(), unit.getId() + "-" + suffix, null), creator, creationTime);
        this.unit = new EmbeddableUnitId(unit);
    }

    public Set<StudentId> getStudentIds() {
        if (wrapperSet == null) {
            wrapperSet = new StudentIdWrapperSet(this);
        }
        return wrapperSet;
    }

    public Set<BaseTargetAssessmentEntity> getTargetAssessments() {
        if (targets == null) {
            targets = new HashSet<>();
        }
        return targets;
    }

    public UnitId getUnitId() {
        return unit.getUnitId();
    }

    public String getTermScheduleProvider() {
        return termScheduleProvider;
    }

    public void setTermScheduleProvider(String termScheduleProvider) {
        this.termScheduleProvider = termScheduleProvider;
    }
}
