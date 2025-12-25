/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.acer.entities.messages.tracking;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 *
 * @author boris.heithecker
 */
@Entity
@Table(name = "UPDATE_TRACKER")
@Access(AccessType.FIELD)
public class UpdateTracker extends BaseTracker {

    @Column(name = "LAST_UPDATE")
    private java.sql.Timestamp update;

    public java.sql.Timestamp getUpdated() {
        return update;
    }

    public void setUpdated(java.sql.Timestamp update) {
        this.update = update;
    }
}
