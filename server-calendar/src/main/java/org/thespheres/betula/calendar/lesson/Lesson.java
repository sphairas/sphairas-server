/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.calendar.lesson;

import org.thespheres.betula.calendar.util.EmbeddableMarker;
import org.thespheres.betula.calendar.util.EmbeddableUnitId;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.CascadeType;
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
import javax.persistence.MapKeyColumn;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.QueryHint;
import javax.persistence.Table;
import javax.persistence.Version;
import org.thespheres.betula.UnitId;
import org.thespheres.betula.services.scheme.spi.LessonId;

/**
 * @author boris.heithecker
 */
@NamedQueries({
    @NamedQuery(name = "findAllLessons", query = "SELECT l FROM Lesson l", hints = {
        @QueryHint(name = "eclipselink.query-results-cache", value = "true")
    }),
    @NamedQuery(name = "findLessonsForUnit", query = "SELECT l FROM Lesson l "
            + "WHERE l.unit=:unit"),
    @NamedQuery(name = "findLessonForLessonId", query = "SELECT l FROM Lesson l "
            + "WHERE l.lesson=:lesson")})
@Entity
@Table(name = "LESSON")
@Access(AccessType.FIELD)
public class Lesson implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    protected Long id;
    @Version
    @Column(name = "LESSON_VERSION")
    private long version;
    @OneToMany(mappedBy = "lesson", cascade = {CascadeType.ALL}, orphanRemoval = true)
    private Set<VendorLessonMapping> vendorLessonMappings = new HashSet<>();
    @ManyToOne
    @JoinColumns({
        @JoinColumn(name = "LESSON_CALENDAR_ID", referencedColumnName = "CALENDAR_ID"),
        @JoinColumn(name = "LESSON_CALENDAR_AUTHORITY", referencedColumnName = "CALENDAR_AUTHORITY"),
        @JoinColumn(name = "LESSON_CALENDAR_VERSION", referencedColumnName = "CALENDAR_VERSION")})
    private LessonCalendar calendar;
    @Embedded
    @ElementCollection
    @CollectionTable(name = "LESSON_MARKERS")
    protected Set<EmbeddableMarker> markerSet = new HashSet<>();
    @Embedded
    private EmbeddableLessonId lesson;
    @Embedded
    private EmbeddableUnitId unit;
    @OneToMany(mappedBy = "lesson", cascade = {CascadeType.ALL}, orphanRemoval = true)
    private List<WeeklyLessonComponent> times = new ArrayList<>();
    @OneToMany(mappedBy = "lesson")
    private Set<LessonSubscriber> subscribers;
    @ElementCollection
    @MapKeyColumn(name = "PROPERTY_KEY", length = 64)
    @Column(name = "PROPERTY_VALUE")
    @CollectionTable(name = "LESSON_PROPERTIES", joinColumns = {
        @JoinColumn(name = "LESSON_ID", referencedColumnName = "ID", updatable = false, insertable = false)})
    private final Map<String, String> properties = new HashMap<>();

    public Lesson() {
    }

    public Lesson(final LessonCalendar calendar, final LessonId lesson, final UnitId unit) {
        this.calendar = calendar;
        this.lesson = new EmbeddableLessonId(lesson, 0);
        this.unit = new EmbeddableUnitId(unit);
    }

    public long getId() {
        return id;
    }

    public LessonCalendar getCalendar() {
        return calendar;
    }

    public LessonId getLesson() {
        return lesson.getLessonId();
    }

    public UnitId getUnit() {
        return unit.getUnitId();
    }

    public Set<VendorLessonMapping> getVendorLessonMappings() {
        return vendorLessonMappings;
    }

    public List<WeeklyLessonComponent> getTimes() {
        return times;
    }

    public Set<EmbeddableMarker> getMarkerSet() {
        return markerSet;
    }

    public Set<LessonSubscriber> getSubscribers() {
        return subscribers;
    }

    public Map<String, String> getProperties() {
        return properties;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 29 * hash + Objects.hashCode(this.id);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Lesson other = (Lesson) obj;
        return Objects.equals(this.id, other.id);
    }

}
