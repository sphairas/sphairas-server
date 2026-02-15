/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.calendar;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinColumns;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import org.thespheres.ical.Parameter;
import org.thespheres.ical.UID;

/**
 *
 * @author boris.heithecker
 * @param <C>
 */
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
@Table(name = "UNIQUE_CALENDAR_COMPONENT")
@IdClass(UID.class)
@Access(AccessType.FIELD)
public class UniqueCalendarComponentEntity<C extends BaseCalendarEntity> extends AbstractCalendarComponent implements AbstractCalendarComponent.WithParent<C>, Serializable { //

    private static final long serialVersionUID = 1L;
    @Id
    @Column(name = "UID_SYSID")
    private String sysid;
    @Id
    @Column(name = "UID_HOST", length = 64)
    private String host;
    @Embedded
    @ElementCollection
    @CollectionTable(name = "CALENDARCOMPONENT_PROPERTIES", joinColumns = {
        @JoinColumn(name = "UNIQUE_CALENDARCOMPONENT_SYSID", referencedColumnName = "UID_SYSID"),
        @JoinColumn(name = "UNIQUE_CALENDARCOMPONENT_HOST", referencedColumnName = "UID_HOST")})
    @OrderColumn(name = "PROPERTY_ORDER")
    protected List<EmbeddableComponentProperty> cmpr = new ArrayList<>();
    @ManyToMany(mappedBy = "collectionComponents")
    protected Set<CalendarCollectionEntity> collections = new HashSet<>();
    @ManyToOne(targetEntity = BaseCalendarEntity.class)
    @JoinColumns({
        @JoinColumn(name = "BASECALENDAR_ID", referencedColumnName = "CALENDAR_ID"),
        @JoinColumn(name = "BASECALENDAR_AUTHORITY", referencedColumnName = "CALENDAR_AUTHORITY"),
        @JoinColumn(name = "BASECALENDAR_VERSION", referencedColumnName = "CALENDAR_VERSION")})
    protected C calendar;

    public UniqueCalendarComponentEntity() {
        super(null);
    }

    public UniqueCalendarComponentEntity(String name, C parent, UID uid) {
        super(name);
        this.sysid = uid.getId();
        this.host = uid.getAuthority();
        calendar = parent;
    }

    public UID getUID() {
        return new UID(host, sysid);
    }

    @Override
    public C getParent() {
        return calendar;
    }

    public Set<CalendarCollectionEntity> getCollections() {
        return collections;
    }

    @Override
    public List<EmbeddableComponentProperty> getProperties() {
        return cmpr;
    }

    public void addProperty(final String name, final String value, final List<Parameter> params) {
        final EmbeddableComponentProperty prop = new EmbeddableComponentProperty(name, value);
        if (params != null) {
            params.forEach(prop::addParameter);
        }
        cmpr.add(prop);
    }

    public void addProperty(final String name, final String value) {
        addProperty(name, value, null);
    }
}
