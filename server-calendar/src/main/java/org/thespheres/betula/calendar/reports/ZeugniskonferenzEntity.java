/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.calendar.reports;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import org.thespheres.betula.TermId;
import org.thespheres.betula.UnitId;
import org.thespheres.betula.calendar.UniqueCalendarComponentEntity;
import org.thespheres.betula.calendar.tickets.TicketsCalendar;
import org.thespheres.betula.calendar.util.EmbeddableTermId;
import org.thespheres.betula.calendar.util.EmbeddableUnitId;
import org.thespheres.ical.CalendarComponent;
import org.thespheres.ical.UID;

/**
 *
 * @author boris.heithecker
 */
@NamedQueries({@NamedQuery(name = "findZeugniskonferenzen",
            query = "SELECT DISTINCT z FROM ZeugniskonferenzEntity z"),
    @NamedQuery(name = "findZeugniskonferenz",
            query = "SELECT DISTINCT z FROM ZeugniskonferenzEntity z WHERE z.unit=:unit AND z.term=:term AND z.category=:category"),
    @NamedQuery(name = "findZeugniskonferenzCategoryNull",
            query = "SELECT DISTINCT z FROM ZeugniskonferenzEntity z WHERE z.unit=:unit AND z.term=:term AND z.category is null"),
    @NamedQuery(name = "findZeugniskonferenzenForUnit",
            query = "SELECT DISTINCT z FROM ZeugniskonferenzEntity z WHERE z.unit=:unit")})  //NEW org.thespheres.betula.calendar.UID(z.host, z.sysid)  funktioniert nicht
@Entity
@Table(name = "ZEUGNISKONFERENZ_CALENDARCOMPONENT")
//, uniqueConstraints = {
//    @UniqueConstraint(columnNames = {"UNIT_AUTHORITY", "UNIT_ID", "TERM_AUTHORITY", "TERM_ID"})})
@Access(AccessType.FIELD)
public class ZeugniskonferenzEntity extends UniqueCalendarComponentEntity<TicketsCalendar> implements Serializable {

    private static final long serialVersionUID = 1L;
    @Embedded
    private EmbeddableUnitId unit;
    @Embedded
    private EmbeddableTermId term;
    @Column(name = "CATEGORY", nullable = true)
    protected String category;
    //TODO copy this to TermREport in Persistence, two columnss.....
    //TODO rename table to ZEUGNISEVENT-CALCMP, add column CATEGORIES, add CATEGORIES to query and UNIQUECONSTRAINT
    @Embedded
    @ElementCollection
    @CollectionTable(name = "ZEUGNISDATE_RELATED_REPORTS",
            joinColumns = {
                @JoinColumn(name = "ZEUGNISDATE_COMPONENT_SYSID", referencedColumnName = "UID_SYSID"),
                @JoinColumn(name = "ZEUGNISDATE_COMPONENT_HOST", referencedColumnName = "UID_HOST")})
    protected Set<EmbeddableStringDocumentIdMapValue> relatedReports = new HashSet<>();
    @Column(name = "ZEUGNISKONFERENZ_LOCATION")
    protected String location;

    public ZeugniskonferenzEntity() {
    }

    private ZeugniskonferenzEntity(UID id, TicketsCalendar parent, TermId term, UnitId unit) {
        super(CalendarComponent.VEVENT, parent, id);
        this.term = new EmbeddableTermId(term);
        this.unit = new EmbeddableUnitId(unit);
    }

    public static ZeugniskonferenzEntity create(TicketsCalendar parent, TermId term, UnitId unit) throws IOException {
        ZeugniskonferenzEntity ret = new ZeugniskonferenzEntity(UID.create(), parent, term, unit);
        parent.getComponents().add(ret);
        return ret;
    }

    public UnitId getUnit() {
        return unit.getUnitId();
    }

    public TermId getTerm() {
        return term.getTermId();
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String reportType) {
        this.category = reportType;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public Set<EmbeddableStringDocumentIdMapValue> getRelatedReports() {
        return relatedReports;
    }
}
