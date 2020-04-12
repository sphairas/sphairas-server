/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.entities.messaging;

import java.io.Serializable;
import java.util.regex.Pattern;
import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.PostLoad;
import javax.persistence.PostPersist;
import javax.persistence.PostUpdate;
import javax.persistence.Table;
import javax.persistence.Transient;
import org.thespheres.betula.entities.BaseTargetAssessmentEntity;

/**
 *
 * @author boris.heithecker
 */
@NamedQueries({
    //    @NamedQuery(name = "findTargetAssessmentPatternForSignee", query = "SELECT cpe.name FROM ChannelPatternEntity cpe, BaseTargetAssessmentEntity btae, IN(btae.signeeInfoentries) si "
    //            + "WHERE si.type='entitled.signee' "
    //            + "AND si.signee=:signee "
    //            + "AND btae.id REGEXP cpe.regex"),
    //    @NamedQuery(name = "findSigneesForPatternChannel", query = "SELECT si.signee FROM BaseTargetAssessmentEntity btae, ChannelPatternEntity cpe, IN(btae.signeeInfoentries) si "
    //            + "WHERE si.type='entitled.signee' "
    //            + "AND cpe.name=:channel "
    //            + "AND btae.id REGEXP cpe.regex"),
    @NamedQuery(name = "findAffectedSigneesForUnitId", query = "SELECT signee FROM SigneeEntity signee, UnitDocumentEntity ude, TermGradeTargetAssessmentEntity tgtae, IN(tgtae.entries) e, IN(tgtae.signeeInfoentries) si "
            + "WHERE si.type='entitled.signee' "
            + "AND si.signee=signee "
            + "AND e.student IN(ude.studentIds) "
            + "AND ude.unit=:unit"),
    @NamedQuery(name = "findAffectedSigneesForPrimaryUnitId", query = "SELECT signee FROM SigneeEntity signee, UnitDocumentEntity ude, TermGradeTargetAssessmentEntity tgtae, IN(ude.markerSet) m, IN(tgtae.entries) e, IN(tgtae.signeeInfoentries) si "
            + "WHERE m.convention='betula-db' AND m.markerId='primary-unit' AND m.subset=NULL "
            + "AND si.type='entitled.signee' "
            + "AND si.signee=signee "
            + "AND e.student IN(ude.studentIds) "
            + "AND ude.unit=:unit"),
    //Doesn't work, scrapped
    @NamedQuery(name = "findAffectedSigneesForStudentCollection", query = "SELECT signee FROM SigneeEntity signee, TermGradeTargetAssessmentEntity tgtae, IN(tgtae.entries) e, IN(tgtae.signeeInfoentries) si "
            + "WHERE si.type='entitled.signee' "
            + "AND si.signee=signee "
            + "AND e.student IN(:students)")})
@Entity
@Table(name = "CHANNEL_PATTERN")
@Access(AccessType.FIELD)
public class ChannelPatternEntity implements Serializable {

    @Id
    @Column(name = "CHANNEL_NAME")
    private String name;
    @Column(name = "PATTERN")
    private String regex;
    @javax.persistence.Version
    @Column(name = "CHANNEL_VERSION")
    private long entityVersion;
    @Transient
    private Pattern pattern;

    public ChannelPatternEntity() {
    }

    @SuppressWarnings("OverridableMethodCallInConstructor")
    public ChannelPatternEntity(String name, String regex) {
        this.name = name;
        this.regex = regex;
    }

    public String getName() {
        return name;
    }

    public String getRegex() {
        return regex;
    }

    void setRegex(String pattern) {
        this.regex = pattern;
    }

    boolean matches(String value) {
        return pattern.matcher(value).matches();
    }

    boolean matches(BaseTargetAssessmentEntity btae) {
        return pattern.matcher(btae.getDocumentId().getId()).matches();
    }

    @PostLoad
    @PostPersist
    @PostUpdate
    void compile() {
        this.pattern = Pattern.compile(regex);
    }

}
