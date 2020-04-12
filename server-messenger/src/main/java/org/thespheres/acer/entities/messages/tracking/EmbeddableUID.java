/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.acer.entities.messages.tracking;

import java.io.Serializable;
import java.util.Objects;
import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.Id;
import org.thespheres.ical.UID;

/**
 *
 * @author boris.heithecker
 */
@Embeddable
@Access(AccessType.FIELD)
public class EmbeddableUID implements Serializable {

    @Id
    @Column(name = "UID_SYSID")
    private String sysid;
    @Id
    @Column(name = "UID_HOST", length = 64)
    private String host;

    public EmbeddableUID() {
    }

    public EmbeddableUID(UID uid) {
        this.sysid = uid.getId();
        this.host = uid.getAuthority();
    }

    public UID getUID() {
        return new UID(host, sysid);
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 53 * hash + Objects.hashCode(this.sysid);
        hash = 53 * hash + Objects.hashCode(this.host);
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
        final EmbeddableUID other = (EmbeddableUID) obj;
        if (!Objects.equals(this.sysid, other.sysid)) {
            return false;
        }
        return Objects.equals(this.host, other.host);
    }

}
