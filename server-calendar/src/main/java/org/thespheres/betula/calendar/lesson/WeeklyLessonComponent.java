/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.calendar.lesson;

import org.thespheres.betula.calendar.util.EmbeddablePeriodId;
import java.io.Serializable;
import java.time.DayOfWeek;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.JoinColumns;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import org.thespheres.betula.calendar.UniqueCalendarComponentEntity;
import org.thespheres.betula.services.scheme.spi.PeriodId;
import org.thespheres.ical.CalendarComponent;
import org.thespheres.ical.UID;
import org.thespheres.ical.impl.ParameterList;

/**
 * @author boris.heithecker
 */
@Entity
@Table(name = "WEEKLY_LESSON_CALENDARCOMPONENT")
@Access(AccessType.FIELD)
public class WeeklyLessonComponent extends UniqueCalendarComponentEntity<LessonCalendar> implements Serializable {

    @ManyToOne
    @JoinColumns({
        @JoinColumn(name = "LESSON_ID", referencedColumnName = "ID")})
    private Lesson lesson;
    @Column(name = "LOCATION_VALUE")
    private String location;
    @Column(name = "LOCATION_PARAMETERS")
    private final ParameterList locationParameters = new ParameterList();
    @Column(name = "EXDATES", length = 1024)
    private String exDates;
    @Embedded
    private EmbeddablePeriodId period;
    @Column(name = "DAY_OF_WEEK")
    private Integer dayOfWeek;
    //Diese updates brauchen eine RecurrenceId, siehe LessonUnit
    @OneToMany(mappedBy = "updated", cascade = {CascadeType.ALL}, orphanRemoval = true)
    private List<LessonUpdate> updates;
    @OneToMany(mappedBy = "lesson", cascade = {CascadeType.ALL}, orphanRemoval = true)
    private Set<LessonJournalEntry> journalEntries;
    @ManyToMany(mappedBy = "components")
    private Set<VendorLessonMapping> vendorLessonMappings = new HashSet<>();

    public WeeklyLessonComponent() {
    }

    @SuppressWarnings("LeakingThisInConstructor")
    public WeeklyLessonComponent(final UID uid, final Lesson lesson) {
        super(CalendarComponent.VEVENT, lesson.getCalendar(), uid);
        this.lesson = lesson;
        lesson.getCalendar().getComponents().add(this);
    }

    public Lesson getLesson() {
        return lesson;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public PeriodId getPeriod() {
        return period == null ? null : period.getPeriodId();
    }

    public void setPeriod(PeriodId period) {
        this.period = new EmbeddablePeriodId(period);
    }

    public DayOfWeek getDayOfWeek() {
        return dayOfWeek != null ? DayOfWeek.of(dayOfWeek) : null;
    }

    public void setDayOfWeek(DayOfWeek dayOfWeek) {
        this.dayOfWeek = dayOfWeek == null ? null : dayOfWeek.getValue();
    }

    public String getExDates() {
        return exDates;
    }

    public void setExDates(String exWeeks) {
        this.exDates = exWeeks;
    }

    public List<LessonUpdate> getUpdates() {
        return updates;
    }

    public Set<VendorLessonMapping> getVendorLessonMappings() {
        return vendorLessonMappings;
    }

}
