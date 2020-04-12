/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.entities;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.ManyToMany;
import javax.persistence.Table;
import org.thespheres.betula.document.Signee;

/**
 *
 * @author boris.heithecker
 */
@Entity
@Table(name = "SIGNEE")
@IdClass(Signee.class)
@Access(AccessType.FIELD)
public class SigneeEntity implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @Column(name = "SIGNEE_ID")
    private String prefix;
    @Id
    @Column(name = "SIGNEE_AUTHORITY", length = 64)
    private String suffix;
    @Id
    @Column(name = "SIGNEE_ALIAS")
    private boolean alias;
    @Column(name = "COMMON")
    private String commonName;
    @Embedded
    @ElementCollection
    @CollectionTable(name = "SIGNEE_MARKERS")
    private Set<EmbeddableMarker> markerSet = new HashSet<>();
    @ManyToMany(mappedBy = "signees") //TODO: weg
    private Set<BaseTargetAssessmentEntity> targetAssessments = new HashSet<>();
    @Column(name = "SIGNEE_OTHERGROUPS")
    private String groups = "";
    private transient String[] signeeGroups;
    @Column(name = "UNAME")
    private String username;
    @Column(name = "PW")
    private String password;

    public SigneeEntity() {
    }

    public SigneeEntity(Signee signee, String commonName) {
        this.prefix = signee.getId();
        this.suffix = signee.getAuthority();
        this.alias = signee.isAlias();
        this.commonName = commonName;
    }

    public Signee getSignee() {
        return new Signee(prefix, suffix, alias);
    }

    public String getCommonName() {
        return commonName;
    }

    public void setCommonName(String commonName) {
        this.commonName = commonName;
    }

    public String[] getGroups() {//TODO: field 
        if (signeeGroups == null) {
            if (groups != null && !groups.isEmpty()) {
                String[] other = groups.split(",");
                signeeGroups = Arrays.copyOf(other, other.length + 1);
                signeeGroups[other.length] = "signees";
            } else {
                signeeGroups = new String[]{"signees"};
            }
        }
        return signeeGroups;
    }

    public Set<BaseTargetAssessmentEntity> getTargetAssessments() {
        return targetAssessments;
    }

    public Set<EmbeddableMarker> getMarkerSet() {
        return markerSet;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 67 * hash + Objects.hashCode(this.prefix);
        hash = 67 * hash + Objects.hashCode(this.suffix);
        hash = 67 * hash + (this.alias ? 1 : 0);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final SigneeEntity other = (SigneeEntity) obj;
        if (!Objects.equals(this.prefix, other.prefix)) {
            return false;
        }
        if (!Objects.equals(this.suffix, other.suffix)) {
            return false;
        }
        return this.alias == other.alias;
    }

    @Override
    public String toString() {
        return "org.thespheres.betula.entities.SigneeEntity[ id=" + prefix + " ]";
    }

}
