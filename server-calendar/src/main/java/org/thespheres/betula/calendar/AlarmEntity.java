/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.calendar;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinColumns;
import javax.persistence.ManyToOne;
import javax.persistence.OrderColumn;
import javax.persistence.Table;
import org.thespheres.ical.CalendarComponent;

/**
 *
 * @author boris.heithecker
 */
@Entity
@Table(name = "ALARM")
@Access(AccessType.FIELD)
public class AlarmEntity extends AbstractCalendarComponent implements AbstractCalendarComponent.WithParent<UniqueCalendarComponentEntity>, Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "ALARM_ID")
    private Long id;
    @ManyToOne
    @JoinColumns({
        @JoinColumn(name = "CALENDARCOMPONENT_SYSUID", referencedColumnName = "UID_SYSID"),
        @JoinColumn(name = "CALENDARCOMPONENT_HOST", referencedColumnName = "UID_HOST")})
    private UniqueCalendarComponentEntity component;
    @Embedded
    @ElementCollection
    @CollectionTable(name = "ALARM_PROPERTIES", joinColumns = {
        @JoinColumn(name = "ALARMENTITY_ID", referencedColumnName = "ALARM_ID")})
    @OrderColumn(name = "ALARM_PROPERTY_ORDER")
    protected List<EmbeddableComponentProperty> alarmProperties = new ArrayList<>();

    public AlarmEntity() {
        super(CalendarComponent.VALARM);
    }

    public AlarmEntity(UniqueCalendarComponentEntity parent) {
        this();
        this.component = parent;
    }

    public Long getAlarmId() {
        return id;
    }

    @Override
    public List<EmbeddableComponentProperty> getProperties() {
        return alarmProperties;
    }

    @Override
    public UniqueCalendarComponentEntity getParent() {
        return component;
    }

}
