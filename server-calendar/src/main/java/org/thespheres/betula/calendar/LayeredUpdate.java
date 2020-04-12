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
import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinColumns;
import javax.persistence.ManyToOne;
import javax.persistence.MappedSuperclass;
import javax.persistence.OrderColumn;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

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
