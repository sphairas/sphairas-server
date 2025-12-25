/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.calendar;

import java.io.Serializable;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinColumns;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;

/**
 * @author boris.heithecker
 * @param <U>
 */
@MappedSuperclass
@Inheritance(strategy = InheritanceType.JOINED)
@Table(name = "BASE_LAYERED_UPDATE")
@Access(AccessType.FIELD)
public abstract class LayeredUpdate<U extends UniqueCalendarComponentEntity> extends AbstractCalendarComponent implements Serializable {

    //Null is allowed!!!!
    @ManyToOne
    @JoinColumns({
        @JoinColumn(name = "CALENDARCOMPONENT_SYSUID", referencedColumnName = "UID_SYSID"),
        @JoinColumn(name = "CALENDARCOMPONENT_HOST", referencedColumnName = "UID_HOST")})
    @OrderColumn(name = "UPDATED_ORDER")
    protected U updated;
    @Column(name = "RECURRENCE")
    @Temporal(TemporalType.TIMESTAMP)
    private Date recurrence;
    @Column(name = "LAYER", length = 64)
    private String layer;

    public LayeredUpdate() {
        this(null, null, null);
    }

    protected LayeredUpdate(final String componentType, final U reference, final Date recurrence) {
        super(componentType);
        this.updated = reference;
        this.recurrence = recurrence;
        setDtstart(recurrence);
    }

    @Override
    public List<EmbeddableComponentProperty> getProperties() {
        return Collections.EMPTY_LIST;
    }

    public U getUpdatedComponent() {
        return updated;
    }

    public String getLayer() {
        return layer;
    }

    public void setLayer(String layer) {
        this.layer = layer;
    }

}
