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
import javax.persistence.Entity;
import javax.persistence.Table;

/**
 *
 * @author boris.heithecker
 */
@Entity
@Table(name = "MAIL_STATUS_TRACKER")
@Access(AccessType.FIELD)
public class MailStatusTracker extends BaseTracker {

    @Column(name = "STATUS", length = 32)
    private String status = "";
    @Column(name = "SEND")
    private java.sql.Timestamp send;

    public MailStatusTracker() {
        this(new java.sql.Timestamp(System.currentTimeMillis()));
    }

    public MailStatusTracker(java.sql.Timestamp send) {
        this.send = send;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Timestamp getSend() {
        return send;
    }

}
