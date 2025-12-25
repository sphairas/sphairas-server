/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.calendar;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinColumns;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
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
