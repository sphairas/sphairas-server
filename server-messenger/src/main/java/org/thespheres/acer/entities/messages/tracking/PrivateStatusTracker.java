/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.acer.entities.messages.tracking;

import java.sql.Timestamp;
import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.Table;
import org.thespheres.acer.entities.EmbeddableSignee;
import org.thespheres.betula.document.Signee;

/**
 *
 * @author boris.heithecker
 */
@Entity
@Table(name = "PRIVATE_STATUS_TRACKER")
@Access(AccessType.FIELD)
public class PrivateStatusTracker extends BaseTracker {

    @Column(name = "PRIORITY")
    private int privatePriority;
    @Column(name = "MARKED_READ")
    private java.sql.Timestamp read;
    @Column(name = "DISPLAY_COLLAPSED")
    private boolean collapsed = false;
    @Embedded
    private EmbeddableSignee signee;

    public PrivateStatusTracker() {
    }

    public PrivateStatusTracker(Signee signee) {
        this.signee = new EmbeddableSignee(signee);
    }

    public Signee getSignee() {
        return signee.getSignee();
    }

    public int getPrivatePriority() {
        return privatePriority;
    }

    public void setPrivatePriority(int privatePriority) {
        this.privatePriority = privatePriority;
    }

    public boolean isCollapsed() {
        return collapsed;
    }

    public void setCollapsed(boolean collapsed) {
        this.collapsed = collapsed;
    }

    public Timestamp getRead() {
        return read;
    }

    public void setRead(Timestamp read) {
        this.read = read;
    }

}
