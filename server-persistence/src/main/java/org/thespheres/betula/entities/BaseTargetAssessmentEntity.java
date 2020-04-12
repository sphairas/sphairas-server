/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.entities;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.MapKey;
import javax.persistence.Table;
import org.thespheres.betula.Identity;
import org.thespheres.betula.assess.TargetDocument;
import org.thespheres.betula.document.DocumentId;
import org.thespheres.betula.document.Signee;

/**
 *
 * @author boris.heithecker
 * @param <G>
 * @param <I>
 */
@Entity
@Table(name = "BASE_TARGETASSESSMENT_DOCUMENT")
@Access(AccessType.FIELD)
public abstract class BaseTargetAssessmentEntity<G, I extends Identity> extends BaseDocumentEntity implements TargetDocument {

    private static final long serialVersionUID = 1L;
    public static final String BASE_TARGETASSESSMENT_DOCUMENT_SIGNEEINFO = "BASE_TARGETASSESSMENT_DOCUMENT_SIGNEEINFO_ENTRIES";
//    final transient Object SIGNEELOCK = new Object();
    @ManyToMany
    @JoinTable(name = "BASE_TARGETASSESSMENT_DOCUMENT_UNIT_DOCUMENTS",
            joinColumns = {
                @JoinColumn(name = "BASE_TARGET_DOCUMENT_ID", referencedColumnName = "DOCUMENT_ID"),
                @JoinColumn(name = "BASE_TARGET_DOCUMENT_AUTHORITY", referencedColumnName = "DOCUMENT_AUTHORITY"),
                @JoinColumn(name = "BASE_TARGET_DOCUMENT_VERSION", referencedColumnName = "DOCUMENT_VERSION")},
            inverseJoinColumns = {
                @JoinColumn(name = "UNIT_DOCUMENT_ID", referencedColumnName = "DOCUMENT_ID"),
                @JoinColumn(name = "UNIT_DOCUMENT_AUTHORITY", referencedColumnName = "DOCUMENT_AUTHORITY"),
                @JoinColumn(name = "UNIT_DOCUMENT_VERSION", referencedColumnName = "DOCUMENT_VERSION")})
    private final Set<UnitDocumentEntity> unitDocs = new HashSet<>();
    @Embedded
    @ElementCollection
    @MapKey(name = "type")
    @CollectionTable(name = "BASE_TARGETASSESSMENT_DOCUMENT_SIGNEEINFO_ENTRIES",
            joinColumns = {
                @JoinColumn(name = "BASE_TARGETASSESSMENT_ID", referencedColumnName = "DOCUMENT_ID"),
                @JoinColumn(name = "BASE_TARGETASSESSMENT_AUTHORITY", referencedColumnName = "DOCUMENT_AUTHORITY"),
                @JoinColumn(name = "BASE_TARGETASSESSMENT_VERSION", referencedColumnName = "DOCUMENT_VERSION")})
    protected Map<String, EmbeddableSigneeInfo> signeeInfoentries = new HashMap<>();
    @ManyToMany
    @JoinTable(name = "BASE_TARGETASSESSMENT_DOCUMENT_SIGNEES",
            joinColumns = {
                @JoinColumn(name = "DOCUMENT_ID", referencedColumnName = "DOCUMENT_ID"),
                @JoinColumn(name = "DOCUMENT_AUTHORITY", referencedColumnName = "DOCUMENT_AUTHORITY"),
                @JoinColumn(name = "DOCUMENT_VERSION", referencedColumnName = "DOCUMENT_VERSION")},
            inverseJoinColumns = {
                @JoinColumn(name = "SIGNEE_ID", referencedColumnName = "SIGNEE_ID"),
                @JoinColumn(name = "SIGNEE_AUTHORITY", referencedColumnName = "SIGNEE_AUTHORITY"),
                @JoinColumn(name = "SIGNEE_ALIAS", referencedColumnName = "SIGNEE_ALIAS")})
    private final Set<SigneeEntity> signees = new HashSet<>();
    @Column(name = "CONVENTION", length = 64)
    private String preferredConvention;
    @Column(name = "TARGET_TYPE_DISPLAY_HINT")
    private String targetType;

    public BaseTargetAssessmentEntity() {
        super();
    }

    protected BaseTargetAssessmentEntity(DocumentId id, SigneeEntity creator) {
        super(id, creator);
    }

    protected BaseTargetAssessmentEntity(DocumentId id, SigneeEntity creator, Date creationTime) {
        super(id, creator, creationTime);
    }

    @Override
    public String getTargetType() {
        return targetType;
    }

    public void setTargetType(String targetType) {
        this.targetType = targetType;
    }

    @Override
    public String getPreferredConvention() {
        return preferredConvention;
    }

    public void setPreferredConvention(String preferredConvention) {
        this.preferredConvention = preferredConvention;
    }

    public Set<UnitDocumentEntity> getUnitDocs() {
        return unitDocs;
    }

    public Map<String, EmbeddableSigneeInfo> getEmbeddableSignees() {
        return signeeInfoentries;
    }

    @Override
    public Map<String, Signee> getSignees() {
        final Map<String, Signee> ret = new HashMap<>();
        for (Map.Entry<String, EmbeddableSigneeInfo> entry : getEmbeddableSignees().entrySet()) {
            ret.put(entry.getKey(), entry.getValue().getSignee());
        }
        return ret;
    }

    public SigneeEntity addSigneeInfo(final SigneeEntity se, final String type) {
//        synchronized (SIGNEELOCK) {
        final EmbeddableSigneeInfo previousInfo = signeeInfoentries.get(type);
        final SigneeEntity previous = previousInfo == null ? null : previousInfo.getSigneeEntity();
        if (Objects.equals(se, previous)) {
            return previous;
        }
        if (previous != null) {
//            for (Iterator<EmbeddableSigneeInfo> it = signeeInfoentries.values().iterator(); it.hasNext();) {
//                EmbeddableSigneeInfo si = it.next();
//                if (si.getSignee().equals(previous.getSignee())) {
//                    return previousInfo.getSigneeEntity();
//                }
//            }
            //no signeeInfo found
            previous.getTargetAssessments().remove(this);
            signees.remove(previous);
        }
        if (se != null) {
          final  EmbeddableSigneeInfo toAdd = new EmbeddableSigneeInfo(se, type);
//                previousInfo = signeeInfoentries.put(type, toAdd);
            signeeInfoentries.put(type, toAdd);
            signees.add(se);
            se.getTargetAssessments().add(this);
        } else {
//            previousInfo = signeeInfoentries.remove(type);
            signeeInfoentries.remove(type);
        }
        return previous; //previousInfo != null ? previousInfo.getSigneeEntity() : null;
//        }

    }
}
