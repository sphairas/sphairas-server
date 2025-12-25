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
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import org.thespheres.ical.CalendarComponent;

/**
 *
 * @author boris.heithecker
 */
@Entity
@Table(name = "TIMEZONE_CALENDARCOMPONENT")
@Access(AccessType.FIELD)
public class TimezoneEntity extends AbstractCalendarComponent implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @Column(name = "TIMEZONE_ID")
    private String id;
    @ManyToMany
    @JoinTable(name = "TIMEZONE_BASECALENDAR",
            joinColumns = {
                @JoinColumn(name = "TIMEZONE_ID", referencedColumnName = "TIMEZONE_ID")},
            inverseJoinColumns = {
                @JoinColumn(name = "BASECALENDAR_ID", referencedColumnName = "CALENDAR_ID"),
                @JoinColumn(name = "BASECALENDAR_AUTHORITY", referencedColumnName = "CALENDAR_AUTHORITY"),
                @JoinColumn(name = "BASECALENDAR_VERSION", referencedColumnName = "CALENDAR_VERSION")})
    protected Set<BaseCalendarEntity> calendars = new HashSet<>();
    @Embedded
    @ElementCollection
    @CollectionTable(name = "TIMEZONE_PROPERTIES", joinColumns = {
        @JoinColumn(name = "TIMEZONE_ID", referencedColumnName = "TIMEZONE_ID")})
    @OrderColumn(name = "TIMEZONE_PROPERTY_ORDER")
    protected List<EmbeddableComponentProperty> timeZoneProperties = new ArrayList<>();

    public TimezoneEntity() {
        super(CalendarComponent.VTIMEZONE);
    }

    public String getTimezoneId() {
        return id;
    }

    @Override
    public List<EmbeddableComponentProperty> getProperties() {
        return timeZoneProperties;
    }

    public Set<BaseCalendarEntity> getCalendars() {
        return calendars;
    }

}
