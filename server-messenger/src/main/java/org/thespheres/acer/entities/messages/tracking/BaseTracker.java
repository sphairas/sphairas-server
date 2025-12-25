/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.acer.entities.messages.tracking;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import org.thespheres.acer.entities.BaseMessage;

/**
 *
 * @author boris.heithecker
 */
@Entity
@Table(name = "BASE_TRACKER")
@Inheritance(strategy = InheritanceType.JOINED)
@Access(AccessType.FIELD)
public class BaseTracker implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "BASE_TRACKER_ID")
    private Long id;
    @jakarta.persistence.Version
    @Column(name = "BASE_TRACKER_VERSION")
    private long entityVersion;
    @ManyToMany(mappedBy = "trackers")
    private Set<BaseMessage> messages = new HashSet<>();

    public Long getId() {
        return id;
    }

    public Set<BaseMessage> getMessages() {
        return messages;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (id != null ? id.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof BaseTracker)) {
            return false;
        }
        BaseTracker other = (BaseTracker) object;
        return !((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id)));
    }

    @Override
    public String toString() {
        return "org.thespheres.acer.entities.messages.tracking.BaseTracker[ id=" + id + " ]";
    }

}
