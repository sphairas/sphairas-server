/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.calendar.util;

import java.io.Serializable;
import java.util.Objects;
import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import org.thespheres.betula.document.Signee;

/**
 *
 * @author boris.heithecker
 */
@Embeddable
@Access(AccessType.FIELD)
public class EmbeddableSignee implements Serializable {

    @Column(name = "SIGNEE_ID")
    private String prefix;
    @Column(name = "SIGNEE_AUTHORITY", length = 64)
    private String suffix;
    @Column(name = "SIGNEE_ALIAS")
    private boolean alias;

    public EmbeddableSignee() {
    }

    public EmbeddableSignee(Signee signee) {
        this.prefix = signee.getId();
        this.suffix = signee.getAuthority();
        this.alias = signee.isAlias();
    }

    public Signee getSignee() {
        return new Signee(prefix, suffix, alias);
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
        final EmbeddableSignee other = (EmbeddableSignee) obj;
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
