/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.calendar.facade;

import java.util.List;
import javax.ejb.Local;
import org.thespheres.betula.Ticket;
import org.thespheres.betula.calendar.tickets.TicketEntity;
import org.thespheres.ical.CalendarComponent;
import org.thespheres.ical.ICalendar;
import org.thespheres.ical.UID;

/**
 *
 * @author boris.heithecker
 */
@Local
public interface TicketsFacade {

    public ICalendar getICalendar(UID[] restrict, CalendarCompatibilities compat);

    public boolean exists(UID uid);

    public void update(CalendarComponent cc);

    public UID create(CalendarComponent cc);

    public int removeTicketEntries(Ticket ticket);

    public void updateTicket(UID uid, Ticket ticket, boolean remove);

    public List<TicketEntity> findTicketEntitiesForTicket(Ticket ticket);

}
