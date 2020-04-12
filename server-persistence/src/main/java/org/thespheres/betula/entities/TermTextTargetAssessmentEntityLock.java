/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.entities;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.Date;
import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.Embedded;
import org.thespheres.betula.document.Signee;

/**
 *
 * @author boris.heithecker
 */
@Embeddable
public class TermTextTargetAssessmentEntityLock implements Serializable {

    @Column(name = "LOCK_CURRENT")
    private long lock;
    @Column(name = "LOCK_TIMESTAMP", nullable = true)
    private Timestamp acquired;
    @Column(name = "LOCK_TIMEOUT")
    private long timeout;
    @AttributeOverrides({
        @AttributeOverride(name = "prefix", column = @Column(name = "LOCKHOLDER_SIGNEE_PREFIX")),
        @AttributeOverride(name = "suffix", column = @Column(name = "LOCKHOLDER_SIGNEE_SUFFIX")),
        @AttributeOverride(name = "alias", column = @Column(name = "LOCKHOLDER_SIGNEE_ALIAS"))
    })
    @Embedded
    private EmbeddableSignee holder;

    public TermTextTargetAssessmentEntityLock() {
    }

    public TermTextTargetAssessmentEntityLock(long lock, long timeout, Signee holder) {
        this.lock = lock;
        this.timeout = timeout;
        this.holder = holder != null ? new EmbeddableSignee(holder) : null;
        this.acquired = new Timestamp(System.currentTimeMillis());
    }

    public boolean isValid() {
        long now = System.currentTimeMillis();
        return acquired.getTime() <= now && now < acquired.getTime() + timeout;
    }

    public void invalidate() {
        timeout = 0l;
    }

    public long getLock() {
        return lock;
    }

    public Signee getHolder() {
        return holder != null ? holder.getSignee() : null;
    }

    public Date getTimeout() {
        return new Timestamp(acquired.getTime() + timeout);
    }
}
