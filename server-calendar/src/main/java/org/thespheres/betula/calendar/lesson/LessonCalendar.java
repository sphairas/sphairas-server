/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.calendar.lesson;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.MapKeyColumn;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import org.thespheres.betula.calendar.BaseCalendarEntity;
import org.thespheres.betula.document.DocumentId;

/**
 *
 * @author boris.heithecker
 */
@Entity
@Table(name = "LESSON_CALENDAR")
@Access(AccessType.FIELD)
public class LessonCalendar extends BaseCalendarEntity<WeeklyLessonComponent> implements Serializable {

    @Column(name = "PROVIDER", length = 64)
    private String provider;
    @ElementCollection
    @MapKeyColumn(name = "PROPERTY_KEY", length = 64)
    @Column(name = "PROPERTY_VALUE")
    @CollectionTable(name = "LESSON_CALENDAR_PROPERTIES", joinColumns = {
        @JoinColumn(name = "CALENDAR_ID", referencedColumnName = "CALENDAR_ID", updatable = false, insertable = false),
        @JoinColumn(name = "CALENDAR_AUTHORITY", referencedColumnName = "CALENDAR_AUTHORITY", updatable = false, insertable = false),
        @JoinColumn(name = "CALENDAR_VERSION", referencedColumnName = "CALENDAR_VERSION", updatable = false, insertable = false)})
    private final Map<String, String> properties = new HashMap<>();
    @Column(name = "CALENDAR_CLASS", length = 64)
    private String calendarClass;
    @Column(name = "CURRENT_SEQUENCE")
    private Integer sequence;
    @Column(name = "CURRENT_SEQUENCE_PERIOD_BEGIN")
    @Temporal(TemporalType.TIMESTAMP)
    private Date currentSequenceBegin;

    public LessonCalendar() {
    }

    public LessonCalendar(final DocumentId id, final String provider, final String calendarClass) {
        super(id);
        this.provider = provider;
        this.calendarClass = calendarClass;
    }

    public String getProvider() {
        return provider;
    }

    public String getCalendarClass() {
        return calendarClass;
    }

    public Map<String, String> getProperties() {
        return properties;
    }

    public Integer getSequence() {
        return sequence;
    }

    public void setSequence(Integer sequence) {
        this.sequence = sequence;
    }

    public Date getCurrentSequenceBegin() {
        return currentSequenceBegin;
    }

    public void setCurrentSequenceBegin(Date currentSequenceBegin) {
        this.currentSequenceBegin = currentSequenceBegin;
    }

}
