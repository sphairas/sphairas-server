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
import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinColumns;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OrderColumn;
import javax.persistence.Table;
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
    protected final Set<CalendarCollectionEntity> collections = new HashSet<>();
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

    public void addProperty(String name, String value) {
        cmpr.add(new EmbeddableComponentProperty(name, value));
    }

}
