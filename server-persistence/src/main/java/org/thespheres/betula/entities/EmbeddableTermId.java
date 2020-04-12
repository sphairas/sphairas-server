/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.entities;

import java.io.Serializable;
import java.util.Objects;
import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import org.thespheres.betula.TermId;

/**
 *
 * @author boris.heithecker
 */
@Embeddable
@Access(AccessType.FIELD)
public class EmbeddableTermId implements Serializable {

    @Column(name = "TERM_AUTHORITY", length = 64)
    private String termAuthority;
    @Column(name = "TERM_ID", length = 64)
    private int termId;

    public EmbeddableTermId() {
    }

    public EmbeddableTermId(TermId id) {
        this.termAuthority = id.getAuthority();
        this.termId = id.getId();
    }

    public TermId getTermId() {
        return new TermId(termAuthority, termId);
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 79 * hash + Objects.hashCode(this.termAuthority);
        hash = 79 * hash + this.termId;
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
        final EmbeddableTermId other = (EmbeddableTermId) obj;
        if (!Objects.equals(this.termAuthority, other.termAuthority)) {
            return false;
        }
        return this.termId == other.termId;
    }

}
