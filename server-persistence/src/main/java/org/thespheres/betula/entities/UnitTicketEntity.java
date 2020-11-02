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
import org.thespheres.betula.TermId;
import org.thespheres.betula.UnitId;

/**
 *
 * @author boris.heithecker
 */
@NamedQueries({
    @NamedQuery(name = "findUnitTicket", query = "SELECT c FROM UnitTicketEntity c "
            + "WHERE c.unit=:unit "
            + "AND c.term=:term "
            + "AND c.type=:signeeType"),
    @NamedQuery(name = "UnitTicketEntity.findUnitTicketForStudent", query = "SELECT DISTINCT c FROM UnitTicketEntity c, UnitDocumentEntity pu, IN(pu.studentIds) stud, IN(pu.markerSet) m " // 
            + "WHERE m.convention='betula-db' AND m.markerId='primary-unit' AND m.subset=NULL "
            + "AND stud.studentId=:studentId AND stud.studentAuthority=:studentAuthority "
            + "AND c.unit.unitId=pu.unit.unitId AND c.unit.unitAuthority=pu.unit.unitAuthority "
            + "AND c.term.termId=:termId AND c.term.termAuthority=:termAuthority "
            + "AND c.type=:signeeType ", hints = {
                @QueryHint(name = "eclipselink.query-results-cache", value = "true"),
                @QueryHint(name = "eclipselink.query-results-cache.size", value = "2000")
            }),
    @NamedQuery(name = "findUnitTicketForDocument", query = "SELECT c FROM UnitTicketEntity c, UnitDocumentEntity pu, "
            + "IN(pu.targets) t, IN(pu.markerSet) m, IN(pu.studentIds) s "
            + "WHERE t=:target "
            + "AND m.convention='betula-db' AND m.markerId='primary-unit' AND m.subset=NULL "
            + "AND c.unit.unitId=pu.unit.unitId AND c.unit.unitAuthority=pu.unit.unitAuthority "
            + "AND c.term.termId=:termId AND c.term.termAuthority=:termAuthority "
            + "AND c.type=:signeeType "
            + "AND s.studentAuthority=:studentAuthority AND s.studentId=:studentId",
            hints = {
                @QueryHint(name = "eclipselink.query-results-cache", value = "true"),
                @QueryHint(name = "eclipselink.query-results-cache.size", value = "10000")
            }),
    @NamedQuery(name = "findUnitTicketForAG", query = "SELECT c FROM UnitTicketEntity c, UnitDocumentEntity pu, BaseTargetAssessmentEntity t, "
            + "IN(t.unitDocs) ag, IN(ag.studentIds) ags, IN(ag.markerSet) agm, IN(pu.markerSet) pum, IN(pu.studentIds) pus "
            + "WHERE t=:target "
            + "AND pum.convention='betula-db' AND pum.markerId='primary-unit' AND pum.subset=NULL "
            + "AND agm.convention='kgs.unterricht' AND agm.markerId='ag' AND agm.subset=NULL "
            + "AND c.unit.unitId=pu.unit.unitId AND c.unit.unitAuthority=pu.unit.unitAuthority "
            + "AND c.term.termId=:termId AND c.term.termAuthority=:termAuthority "
            + "AND c.type=:signeeType "
            + "AND pus.studentAuthority=:studentAuthority AND pus.studentId=:studentId "
            + "AND ags.studentAuthority=:studentAuthority AND ags.studentId=:studentId ",
            hints = {
                @QueryHint(name = "eclipselink.query-results-cache", value = "true"),
                @QueryHint(name = "eclipselink.query-results-cache.size", value = "2500") //was 1000
            })})
@Entity
@Table(name = "UNIT_TICKET")
public class UnitTicketEntity extends BaseTicketEntity {

    private static final long serialVersionUID = 1L;
    @Embedded
    private EmbeddableUnitId unit;
    @Embedded
    private EmbeddableTermId term;
    @Column(name = "SIGNGEE_TYPE", length = 64)
    private String type;
    @Column(name = "TARGET_TYPES", length = 256)
    private String targetType;
    @Embedded
    @ElementCollection
    @CollectionTable(name = "UNIT_TICKET_STUDENTS_EXEMPTIONS", joinColumns = {
        @JoinColumn(name = "TICKET_ID", referencedColumnName = "ID")})
    private Set<EmbeddableStudentId> exempt = new HashSet<>();

    public UnitTicketEntity() {
    }

    public UnitTicketEntity(UnitId unit, TermId term, String signeeType, String targetType) {
        this.unit = new EmbeddableUnitId(unit);
        this.term = new EmbeddableTermId(term);
        this.type = signeeType;
        this.targetType = targetType;
    }

    public UnitId getUnit() {
        return unit.getUnitId();
    }

    public TermId getTerm() {
        return term.getTermId();
    }

    public String getSigneeType() {
        return type;
    }

    public Set<EmbeddableStudentId> getExemptedStudents() {
        return exempt;
    }

    public String getTargetType() {
        return targetType;
    }

}
