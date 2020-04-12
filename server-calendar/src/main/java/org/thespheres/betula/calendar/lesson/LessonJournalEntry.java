/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.calendar.lesson;

import org.thespheres.betula.calendar.util.EmbeddableRecordId;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
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
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinColumns;
import javax.persistence.ManyToOne;
import javax.persistence.OrderColumn;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import org.thespheres.betula.RecordId;
import org.thespheres.betula.calendar.AbstractCalendarComponent;
import org.thespheres.betula.calendar.EmbeddableComponentProperty;
import org.thespheres.ical.CalendarComponent;
import org.thespheres.ical.impl.ParameterList;

/**
 *
 * @author boris.heithecker
 */
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
@Table(name = "LESSON_JOURNAL_ENTRY")
@Access(AccessType.FIELD)
public class LessonJournalEntry extends AbstractCalendarComponent implements AbstractCalendarComponent.WithParent<WeeklyLessonComponent>, Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    @ManyToOne
    @JoinColumns({
        @JoinColumn(name = "WEEKLY_LESSON_SYSUID", referencedColumnName = "UID_SYSID"),
        @JoinColumn(name = "WEEKLY_LESSON_HOST", referencedColumnName = "UID_HOST")})
    private WeeklyLessonComponent lesson;
    @Column(name = "DESCRIPTION_VALUE", length = 4096)
    private String description;
    @Column(name = "DESCRIPTION_PARAMETERS")
    private final ParameterList descriptionParameters = new ParameterList();
    @Column(name = "ENTRY_SEQUENCE")
    private Integer journalEntrySequence;
    @Embedded
    private EmbeddableRecordId record;
    @Column(name = "RECURRENCE_ID")
    @Temporal(TemporalType.TIMESTAMP)
    private Date recurrenceId;
    @Embedded
    @ElementCollection
    @CollectionTable(name = "LESSON_JOURNAL_PROPERTIES")
    @OrderColumn(name = "LESSON_JOURNAL_ORDER")
    protected List<EmbeddableComponentProperty> journalProperties = new ArrayList<>();

    public LessonJournalEntry() {
        super(CalendarComponent.VJOURNAL);
    }

    public LessonJournalEntry(final WeeklyLessonComponent parent, final RecordId record) {
        this();
        this.lesson = parent;
    }

    public Long getId() {
        return id;
    }

    @Override
    public WeeklyLessonComponent getParent() {
        return lesson;
    }

    public RecordId getRecord() {
        return record.getRecordId();
    }

    @Override
    public List<EmbeddableComponentProperty> getProperties() {
        return journalProperties;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (id != null ? id.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof LessonJournalEntry)) {
            return false;
        }
        LessonJournalEntry other = (LessonJournalEntry) object;
        return !((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id)));
    }

}
