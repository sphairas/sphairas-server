/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.calendar.tickets;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.CollectionTable;
import javax.persistence.ElementCollection;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.QueryHint;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import org.thespheres.betula.calendar.BaseCalendarEntity;
import org.thespheres.betula.calendar.UniqueCalendarComponentEntity;
import org.thespheres.ical.CalendarComponent;
import org.thespheres.ical.UID;

/**
 *
 * @author boris.heithecker
 */
@Entity
@NamedQueries({
    @NamedQuery(
            name = "findAllTicketEntities",
            query = "SELECT e FROM TicketEntity e",
            hints = {
                @QueryHint(name = "eclipselink.query-results-cache", value = "true"),
                @QueryHint(name = "eclipselink.query-results-cache.size", value = "1")
            }),
    @NamedQuery(
            name = "findTicketEntitiesForTicket",
            query = "SELECT DISTINCT e FROM TicketEntity e, IN(e.tickets) t "
            + "WHERE t.ticketId=:ticketId "
            + "AND t.ticketAuthority=:ticketAuthority")})//TODO embedded type ....
@Table(name = "TICKET_CALENDARCOMPONENT")
@Access(AccessType.FIELD)
public class TicketEntity extends UniqueCalendarComponentEntity implements Serializable {

    private static final long serialVersionUID = 1L;
    @Embedded
    @ElementCollection
    @CollectionTable(name = "TICKET_TICKETENTRIES",
            joinColumns = {
                @JoinColumn(name = "TICKETCOMPONENT_SYSUID", referencedColumnName = "UID_SYSID"),
                @JoinColumn(name = "TICKETCOMPONENT_HOST", referencedColumnName = "UID_HOST")},
            uniqueConstraints = {
                @UniqueConstraint(columnNames = {"TICKET_AUTHORITY", "TICKET_ID"})})
    protected Set<EmbeddedableTicketEntry> tickets = new HashSet<>();

    public TicketEntity() {
    }

    public TicketEntity(UID id, BaseCalendarEntity parent) {
        super(CalendarComponent.VEVENT, parent, id);
    }

    public Set<EmbeddedableTicketEntry> getTicketEntries() {
        return tickets;
    }

    public static TicketEntity create(BaseCalendarEntity parent) throws IOException {
        final TicketEntity ret = new TicketEntity(UID.create(), parent);
        parent.getComponents().add(ret);
        return ret;
    }
}
