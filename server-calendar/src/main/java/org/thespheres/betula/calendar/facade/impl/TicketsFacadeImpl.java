/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.calendar.facade.impl;

import java.io.IOException;
import java.util.List;
import java.util.StringJoiner;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.LockModeType;
import javax.persistence.TypedQuery;
import org.thespheres.betula.Ticket;
import org.thespheres.betula.calendar.config.PropertyNames;
import org.thespheres.betula.calendar.facade.TicketsFacade;
import org.thespheres.betula.calendar.tickets.EmbeddedableTicketEntry;
import org.thespheres.betula.calendar.tickets.TicketEntity;
import org.thespheres.betula.calendar.tickets.TicketTimerBean;
import org.thespheres.betula.calendar.tickets.TicketsCalendar;
import org.thespheres.betula.document.DocumentId;
import org.thespheres.betula.server.beans.annot.Authority;
import org.thespheres.ical.CalendarComponent;
import org.thespheres.ical.CalendarComponentProperty;
import org.thespheres.ical.ICalendar;
import org.thespheres.ical.InvalidComponentException;
import org.thespheres.ical.Parameter;
import org.thespheres.ical.UID;
import org.thespheres.ical.builder.ICalendarBuilder;

/**
 *
 * @author boris.heithecker
 */
@LocalBean
@Stateless
public class TicketsFacadeImpl extends FixedCalendarFacade<TicketsCalendar, TicketEntity> implements TicketsFacade {

    @EJB
    private TicketTimerBean ticketTimerBean;
    @Authority
    @Inject
    private String authority;

    public TicketsFacadeImpl() {
        super(TicketEntity.class);
    }

    @Override
    public TicketsCalendar getCalendar() {
        final DocumentId tc = new DocumentId(authority, PropertyNames.TICKETS_CALENDAR_ID, DocumentId.Version.UNSPECIFIED);
        TicketsCalendar calendar = getEntityManager().find(TicketsCalendar.class, tc);
        if (calendar == null) {
            calendar = new TicketsCalendar(tc);
            getEntityManager().persist(calendar);
        }
        return calendar;
    }

    @Override
    public ICalendar getICalendar(UID[] restrict) {
        return super.getICalendar(restrict);
    }

    @Override
    public boolean exists(UID uid) {
        TicketEntity te = getEntityManager().find(entityClass, uid);
        return te != null && te.getParent().equals(getCalendar());
    }

    @Override
    public UID create(CalendarComponent cc) {
        if (cc.getUID() != null) {
            throw new IllegalArgumentException("Cannot create calendar component with existing UID.");
        }
        TicketEntity te;
        try {
            te = TicketEntity.create(getCalendar());
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
        updateComponentProperties(cc, te);
        getEntityManager().persist(te);
        return te.getUID();
    }

    @Override
    public void update(CalendarComponent cc) {
        final UID uid = cc.getUID();
        final TicketEntity te;
        if ((te = find(uid, LockModeType.OPTIMISTIC_FORCE_INCREMENT)) != null) {
            updateComponentProperties(cc, te);
            edit(te);
        } else {
            throw new IllegalArgumentException(cc.getUID() + " does not exist, cannot be updated.");
        }
    }

    @Override
    public void updateTicket(UID uid, Ticket ticket, boolean remove) {
        final TicketEntity te = find(uid, LockModeType.OPTIMISTIC_FORCE_INCREMENT);
        if (te != null) {
            final EmbeddedableTicketEntry ete = new EmbeddedableTicketEntry(ticket);
            boolean merge;
            if (remove) {
                merge = te.getTicketEntries().remove(ete);
            } else {
                merge = te.getTicketEntries().add(ete);
                if (merge) {
                    ticketTimerBean.addEvent(te.getDtstart(), ticket);
                }
            }
            if (merge) {
                edit(te);
            }
        }
    }

    @Override
    public int removeTicketEntries(Ticket ticket) {
        final List<TicketEntity> l = findTicketEntitiesForTicketImpl(ticket, LockModeType.OPTIMISTIC_FORCE_INCREMENT);
        int ret = 0;
        for (TicketEntity te : l) {
            final EmbeddedableTicketEntry ete = new EmbeddedableTicketEntry(ticket);
            boolean contained = te.getTicketEntries().remove(ete);
            if (contained) {
                ++ret;
                edit(te);
            }
        }
        return ret;
    }

    @Override
    public List<TicketEntity> findTicketEntitiesForTicket(Ticket ticket) {
        return findTicketEntitiesForTicketImpl(ticket, LockModeType.OPTIMISTIC);
    }

    private List<TicketEntity> findTicketEntitiesForTicketImpl(final Ticket ticket, final LockModeType lmt) {
        final TypedQuery<TicketEntity> q = getEntityManager().createNamedQuery("findTicketEntitiesForTicket", TicketEntity.class);
        q.setParameter("ticketId", ticket.getId())
                .setParameter("ticketAuthority", ticket.getAuthority())
                .setLockMode(lmt);
        if (q.getResultList().size() > 1) {
            final StringJoiner sj = new StringJoiner(", ");
            q.getResultList().stream()
                    .map(TicketEntity::getUID)
                    .map(UID::toString)
                    .forEach(sj::add);
            //This should never happen, see UniqueConstraint in ....
            Logger.getLogger(TicketsFacadeImpl.class.getCanonicalName()).log(Level.WARNING, "Found multiple tickets calendar component for ticket {0}: {1}. Please remove all but one.", new Object[]{ticket.toString(), sj.toString()});
        } else if (q.getResultList().isEmpty()) {
            Logger.getLogger(TicketsFacadeImpl.class.getCanonicalName()).log(Level.WARNING, "Found no ticket calendar component for ticket {0}. Remove orphaned ticket.", ticket.toString());
        }
        return q.getResultList();
    }

//    @Override
//    protected void addPropertiesToICalendarBody(ICalendarBuilder cb) throws InvalidComponentException {
//        final DocumentId id = getCalendar().getDocumentId();
//        cb.addProperty("X-CALENDAR-ID", id.getId(), new Parameter("x-calendar-authority", id.getAuthority()), new Parameter("x-calendar-version", id.getVersion().getVersion()));
//    }
    @Override
    protected void addEntityPropertiesToComponent(ICalendarBuilder.CalendarComponentBuilder ccb, TicketEntity ut) throws InvalidComponentException {
        super.addEntityPropertiesToComponent(ccb, ut);
        if (ut.getStatus() == null) {
            ccb.addProperty(CalendarComponentProperty.STATUS, "CONFIRMED");
        }
        //
        for (EmbeddedableTicketEntry te : ut.getTicketEntries()) {
            final Ticket ticket = te.getTicket();
            ccb.addProperty("X-TICKET", Long.toString(ticket.getId()), new Parameter("x-ticket-authority", ticket.getAuthority()));
        }
    }
}
