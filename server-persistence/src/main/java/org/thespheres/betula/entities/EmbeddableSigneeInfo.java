/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.entities;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.Objects;
import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.JoinColumn;
import javax.persistence.JoinColumns;
import org.thespheres.betula.document.Signee;

/**
 *
 * @author boris.heithecker
 */
@Embeddable
@Access(AccessType.FIELD)
public class EmbeddableSigneeInfo implements Serializable {

    @JoinColumns({
        @JoinColumn(name = "SIGNEE_ID", referencedColumnName = "SIGNEE_ID", updatable = false),
        @JoinColumn(name = "SIGNEE_AUTHORITY", referencedColumnName = "SIGNEE_AUTHORITY", updatable = false),
        @JoinColumn(name = "SIGNEE_ALIAS", referencedColumnName = "SIGNEE_ALIAS", updatable = false)
    })
    protected SigneeEntity signee;
    @Column(name = "SIGNGEE_TYPE", length = 64, updatable = false)
    protected String type;

    //Persisten only!!!!!!!!!!1
    public EmbeddableSigneeInfo() {
    }

    EmbeddableSigneeInfo(SigneeEntity signee, String type) {
        this.signee = signee;
        this.type = type;
//        this.parent = parent;
    }

    public Signee getSignee() {
        return signee != null ? signee.getSignee() : null;
    }

    public SigneeEntity getSigneeEntity() {
        return signee;
    }

    public String getSigneeType() {
        return type;
    }

    static org.thespheres.betula.document.Timestamp timestampOrNull(Timestamp timestamp) {
        return timestamp != null ? new org.thespheres.betula.document.Timestamp(timestamp) : null;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 59 * hash + Objects.hashCode(this.signee);
        hash = 59 * hash + Objects.hashCode(this.type);
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
        final EmbeddableSigneeInfo other = (EmbeddableSigneeInfo) obj;
        if (!Objects.equals(this.signee, other.signee)) {
            return false;
        }
        return Objects.equals(this.type, other.type);
    }

}
