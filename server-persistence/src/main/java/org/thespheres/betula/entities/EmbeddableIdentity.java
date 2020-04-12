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

/**
 *
 * @author boris.heithecker
 */
@Embeddable
@Access(AccessType.FIELD)
public class EmbeddableIdentity implements Serializable {

    @Column(name = "IDENTITY_AUTHORITY", length = 64)
    private String authority;
    @Column(name = "IDENTITY_ID", length = 64)
    private String id;

    public EmbeddableIdentity() {
    }

    public EmbeddableIdentity(String authority, String id) {
        this.authority = authority;
        this.id = id;
    }

    public String getAuthority() {
        return authority;
    }

    public String getId() {
        return id;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 79 * hash + Objects.hashCode(this.authority);
        return 79 * hash + Objects.hashCode(this.id);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final EmbeddableIdentity other = (EmbeddableIdentity) obj;
        if (!Objects.equals(this.authority, other.authority)) {
            return false;
        }
        return Objects.equals(this.id, other.id);
    }

}
