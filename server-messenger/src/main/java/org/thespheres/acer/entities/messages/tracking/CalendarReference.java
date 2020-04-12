/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.acer.entities.messages.tracking;

import org.thespheres.acer.entities.EmbeddableDocumentId;
import java.util.Date;
import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import org.thespheres.betula.document.DocumentId;
import org.thespheres.ical.UID;

/**
 *
 * @author boris.heithecker
 */
@Entity
@Table(name = "CALENDAR_REFERENCE_TRACKER")
@Access(AccessType.FIELD)
public class CalendarReference extends BaseTracker {

    @AttributeOverrides({
        @AttributeOverride(name = "authority", column = @Column(name = "CALENDAR_DOCUMENT_AUTHORITY")),
        @AttributeOverride(name = "id", column = @Column(name = "CALENDAR_DOCUMENT_ID")),
        @AttributeOverride(name = "version", column = @Column(name = "CALENDAR_DOCUMENT_VERSION"))})
    private EmbeddableDocumentId calendar;
    @AttributeOverrides({
        @AttributeOverride(name = "sysid", column = @Column(name = "COMPONENT_SYSID")),
        @AttributeOverride(name = "host", column = @Column(name = "COMPONENT_HOST"))})
    private EmbeddableUID uid;
    @Column(name = "RECURRENCE_ID")
    @Temporal(TemporalType.TIMESTAMP)
    private Date recurrenceid;
    @Column(name = "LAYERED_UPDATE_LAYER", length = 64)
    private String layer;
    @Column(name = "UPDATE_TIMESTAMP")
    @Temporal(TemporalType.TIMESTAMP)
    private Date timestamp;

    public CalendarReference() {
    }

    public CalendarReference(EmbeddableDocumentId calendar, EmbeddableUID uid, Date recurrenceid) {
        this.calendar = calendar;
        this.uid = uid;
        this.recurrenceid = recurrenceid;
    }

    public DocumentId getCalendar() {
        return calendar != null ? calendar.getDocumentId() : null;
    }

    public UID getUID() {
        return uid != null ? uid.getUID() : null;
    }

    public Date getRecurrenceid() {
        return recurrenceid;
    }

    public String getLayer() {
        return layer;
    }

    public void setLayer(String layer) {
        this.layer = layer;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

}
